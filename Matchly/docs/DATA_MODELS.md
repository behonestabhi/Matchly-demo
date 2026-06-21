# Matchly — Data Models

> Principle: **database per service.** Each service below owns its own schema in
> its own PostgreSQL database. No service reads another's tables; data crosses
> boundaries only via APIs or Kafka events. IDs are UUIDs. All tables carry
> `created_at`/`updated_at` (omitted below for brevity). Timestamps are UTC.

## 1. Auth Service — `auth_db`

```sql
users (
  id              UUID PK,
  email           CITEXT UNIQUE NOT NULL,
  password_hash   TEXT,                 -- null for OAuth-only accounts
  full_name       TEXT NOT NULL,
  status          TEXT NOT NULL,        -- ACTIVE | SUSPENDED | PENDING
  email_verified  BOOLEAN NOT NULL DEFAULT false
)

roles (
  id    UUID PK,
  name  TEXT UNIQUE NOT NULL           -- CANDIDATE | RECRUITER | HIRING_MANAGER | ADMIN
)

user_roles (
  user_id  UUID FK→users,
  role_id  UUID FK→roles,
  PRIMARY KEY (user_id, role_id)
)

refresh_tokens (
  id          UUID PK,
  user_id     UUID FK→users,
  token_hash  TEXT NOT NULL,           -- store hash, never the token
  expires_at  TIMESTAMPTZ NOT NULL,
  revoked     BOOLEAN NOT NULL DEFAULT false,
  jti         UUID NOT NULL            -- ties to access-token denylist in Redis
)

oauth_accounts (
  id            UUID PK,
  user_id       UUID FK→users,
  provider      TEXT NOT NULL,         -- google | linkedin
  provider_uid  TEXT NOT NULL,
  UNIQUE (provider, provider_uid)
)
```

## 2. Candidate Service — `candidate_db`

The authoritative store of profiles and the *structured* parsed resume. Raw
files live in S3; the embedding vector lives in the AI service.

```sql
candidates (
  id          UUID PK,
  user_id     UUID NOT NULL,           -- references Auth user (logical, not FK)
  headline    TEXT,
  location    TEXT,
  phone       TEXT,
  links       JSONB                    -- {github, linkedin, portfolio}
)

resumes (
  id              UUID PK,
  candidate_id    UUID FK→candidates,
  s3_key          TEXT NOT NULL,
  file_name       TEXT NOT NULL,
  mime_type       TEXT NOT NULL,       -- application/pdf | ...docx
  parse_status    TEXT NOT NULL,       -- PENDING | PARSED | FAILED
  embedding_ref   UUID,                -- id of vector row in AI service
  is_primary      BOOLEAN NOT NULL DEFAULT true
)

-- structured result of parsing (populated on ResumeParsed event)
parsed_resumes (
  id            UUID PK,
  resume_id     UUID FK→resumes UNIQUE,
  full_name     TEXT,
  email         TEXT,
  phone         TEXT,
  total_exp_yrs NUMERIC(4,1),
  raw_text      TEXT                   -- normalized extracted text
)

candidate_skills (
  candidate_id  UUID FK→candidates,
  skill_id      UUID,                  -- canonical id from skill taxonomy
  skill_name    TEXT NOT NULL,
  source        TEXT,                  -- RESUME | SELF_DECLARED
  PRIMARY KEY (candidate_id, skill_id)
)

experiences (
  id            UUID PK,
  resume_id     UUID FK→resumes,
  company       TEXT, title TEXT,
  start_date    DATE, end_date DATE,   -- end null = current
  description   TEXT
)

education (
  id          UUID PK, resume_id UUID FK→resumes,
  institution TEXT, degree TEXT, field TEXT, level TEXT, -- level: HS|DIPLOMA|BACHELOR|MASTER|PHD
  start_year  INT, end_year INT
)

projects (
  id UUID PK, resume_id UUID FK→resumes,
  name TEXT, description TEXT, tech_stack TEXT[]
)

certifications (
  id UUID PK, resume_id UUID FK→resumes,
  name TEXT, issuer TEXT, issued_at DATE
)
```

## 3. Job Service — `job_db`

```sql
jobs (
  id            UUID PK,
  recruiter_id  UUID NOT NULL,         -- Auth user
  title         TEXT NOT NULL,
  description   TEXT NOT NULL,         -- full JD text
  location      TEXT,
  employment_type TEXT,                -- FULL_TIME | CONTRACT | INTERN
  min_exp_yrs   NUMERIC(4,1),
  max_exp_yrs   NUMERIC(4,1),
  education_level TEXT,
  status        TEXT NOT NULL,         -- DRAFT | OPEN | CLOSED
  embedding_ref UUID,                  -- AI service vector id for the JD
  published_at  TIMESTAMPTZ
)

job_required_skills (
  job_id     UUID FK→jobs,
  skill_id   UUID,                     -- canonical taxonomy id
  skill_name TEXT NOT NULL,
  weight     NUMERIC(3,2) DEFAULT 1.0, -- importance, used by matching
  required   BOOLEAN DEFAULT true,     -- must-have vs nice-to-have
  PRIMARY KEY (job_id, skill_id)
)
```

## 4. Application / ATS Service — `application_db`

```sql
applications (
  id            UUID PK,
  candidate_id  UUID NOT NULL,
  job_id        UUID NOT NULL,
  current_stage TEXT NOT NULL,         -- APPLIED|SCREENING|SHORTLISTED|INTERVIEW|OFFER|REJECTED
  source        TEXT,                  -- DIRECT | REFERRAL | IMPORT
  UNIQUE (candidate_id, job_id)
)

stage_transitions (
  id             UUID PK,
  application_id UUID FK→applications,
  from_stage     TEXT,                 -- null on first
  to_stage       TEXT NOT NULL,
  actor_id       UUID,                 -- recruiter who moved it
  reason         TEXT,
  occurred_at    TIMESTAMPTZ NOT NULL
)

comments (
  id             UUID PK,
  application_id UUID FK→applications,
  author_id      UUID NOT NULL,
  body           TEXT NOT NULL,
  visibility     TEXT NOT NULL         -- INTERNAL | SHARED
)

activity_log (                         -- append-only audit trail
  id             UUID PK,
  application_id UUID,
  actor_id       UUID,
  action         TEXT NOT NULL,        -- STAGE_CHANGED | COMMENTED | SCORE_UPDATED | ...
  payload        JSONB,
  occurred_at    TIMESTAMPTZ NOT NULL
)
```

## 5. Matching Service — `matching_db`

```sql
match_scores (
  id              UUID PK,
  candidate_id    UUID NOT NULL,
  job_id          UUID NOT NULL,
  final_score     NUMERIC(4,3) NOT NULL,  -- 0.000..1.000
  semantic_score  NUMERIC(4,3),
  skill_overlap   NUMERIC(4,3),
  experience_fit  NUMERIC(4,3),
  education_fit   NUMERIC(4,3),
  model_version   TEXT NOT NULL,
  scored_at       TIMESTAMPTZ NOT NULL,
  UNIQUE (candidate_id, job_id, model_version)
)

skill_gaps (
  id            UUID PK,
  candidate_id  UUID NOT NULL,
  job_id        UUID NOT NULL,
  missing_skills JSONB NOT NULL,       -- [{skill, importance}]
  roadmap       JSONB,                 -- LLM-generated learning plan (nullable)
  generated_at  TIMESTAMPTZ
)

success_predictions (
  id            UUID PK,
  candidate_id  UUID NOT NULL,
  job_id        UUID NOT NULL,
  probability   NUMERIC(4,3) NOT NULL,
  model_version TEXT NOT NULL,
  features      JSONB,                 -- snapshot for explainability/audit
  predicted_at  TIMESTAMPTZ NOT NULL
)
```

> Rankings ("top candidates for a job") are derived by querying `match_scores`
> ordered by `final_score`, cached in Redis (`match:rank:{jobId}`), and busted on
> `MatchScored`.

## 6. Interview Service — `interview_db`

```sql
question_sets (
  id            UUID PK,
  candidate_id  UUID NOT NULL,
  job_id        UUID NOT NULL,
  status        TEXT NOT NULL,         -- GENERATING | READY | FAILED
  llm_model     TEXT,                  -- e.g. gemini-1.5 / gpt-4o
  prompt_version TEXT
)

questions (
  id              UUID PK,
  question_set_id UUID FK→question_sets,
  category        TEXT NOT NULL,       -- TECHNICAL | BEHAVIORAL | SCENARIO
  question        TEXT NOT NULL,
  suggested_answer TEXT,               -- rubric / model answer for the recruiter
  difficulty      TEXT,                -- EASY | MEDIUM | HARD
  ordinal         INT
)
```

## 7. Analytics Service — `analytics_db` (read models)

Denormalized, event-sourced projections; never the source of truth.

```sql
funnel_daily (
  job_id UUID, day DATE,
  applied INT, screening INT, shortlisted INT, interview INT, offer INT, rejected INT,
  PRIMARY KEY (job_id, day)
)

time_to_hire (
  job_id UUID, application_id UUID,
  applied_at TIMESTAMPTZ, hired_at TIMESTAMPTZ,
  days_to_hire NUMERIC(6,2),
  PRIMARY KEY (application_id)
)

skill_demand (
  day DATE, skill_id UUID, skill_name TEXT, job_count INT,
  PRIMARY KEY (day, skill_id)
)

conversion_metrics (
  job_id UUID, day DATE,
  applications INT, shortlist_rate NUMERIC(4,3), offer_rate NUMERIC(4,3),
  PRIMARY KEY (job_id, day)
)
```

## 8. AI Service — `ai_db` (PostgreSQL + pgvector)

```sql
CREATE EXTENSION IF NOT EXISTS vector;

embeddings (
  id          UUID PK,
  owner_type  TEXT NOT NULL,           -- RESUME | JOB
  owner_id    UUID NOT NULL,           -- resume_id or job_id
  model       TEXT NOT NULL,           -- e.g. bge-small-en-v1.5
  vector      vector(384) NOT NULL,    -- dim matches the model
  UNIQUE (owner_type, owner_id, model)
)
-- HNSW index for fast ANN search
CREATE INDEX ON embeddings USING hnsw (vector vector_cosine_ops);

ml_models (                            -- success-predictor registry metadata
  id          UUID PK,
  name        TEXT,                    -- success_predictor
  version     TEXT,
  artifact_uri TEXT,                   -- S3 path to serialized model
  metrics     JSONB,                   -- {accuracy, precision, recall, f1}
  trained_at  TIMESTAMPTZ,
  is_active   BOOLEAN
)

skill_taxonomy (
  id        UUID PK,
  canonical TEXT NOT NULL,             -- "JavaScript"
  aliases   TEXT[] NOT NULL            -- ["JS","ECMAScript"]
)
```

## 9. Kafka topics & event contracts

Topics are partitioned by the natural aggregate key (e.g. `candidateId`,
`jobId`) to preserve per-entity ordering. Schemas governed by a schema registry
(`libs/schemas`). Envelope is common to all events:

```jsonc
{
  "eventId": "uuid",          // dedup key for idempotency
  "eventType": "ResumeParsed",
  "occurredAt": "ISO-8601",
  "correlationId": "uuid",    // propagated from gateway for tracing
  "version": 1,
  "payload": { /* per-event */ }
}
```

| Topic | Key | Producer | Key events (payload shape) | Consumers |
|---|---|---|---|---|
| `candidate.events` | candidateId | Candidate | `ResumeUploaded{candidateId,resumeId,s3Key}`, `ProfileUpdated{...}` | AI, Analytics |
| `resume.parsed` | candidateId | AI | `ResumeParsed{candidateId,resumeId,skills[],expYrs,embeddingRef}` | Candidate, Matching, Analytics |
| `job.events` | jobId | Job | `JobPosted{jobId,recruiterId,requiredSkills[]}`, `JobUpdated`, `JobClosed` | AI, Matching, Analytics |
| `job.embedded` | jobId | AI | `JobEmbedded{jobId,embeddingRef}` | Job, Matching |
| `application.events` | applicationId | Application | `ApplicationSubmitted{candidateId,jobId}`, `StageChanged{from,to,actorId}` | Matching, Analytics |
| `matching.events` | candidateId | Matching | `MatchScored{candidateId,jobId,finalScore}`, `SkillGapComputed{...}` | Analytics, Application |
| `interview.events` | candidateId | Interview | `QuestionsGenerated{candidateId,jobId,setId}` | Analytics |
| `*.DLQ` | — | all | dead-lettered messages after retry exhaustion | ops/manual replay |

### Ordering & idempotency notes
- A candidate's `ResumeUploaded` then `ResumeParsed` must be observed in order →
  same partition key (`candidateId`).
- Every consumer records `eventId` in Redis (`idemp:{consumer}:{eventId}`) before
  acting; replays are no-ops.

## 10. Skill taxonomy (why it matters)
Matching and skill-gap both depend on normalizing skill strings to canonical
IDs (`§8 skill_taxonomy`). Without it, "k8s" on a resume vs "Kubernetes" in a JD
reads as a missing skill and tanks the match score. The taxonomy is seeded from
a public skills ontology (e.g. ESCO/O*NET subset) and extended over time.
