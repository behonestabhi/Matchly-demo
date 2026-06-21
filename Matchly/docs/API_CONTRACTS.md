# Matchly — API Contracts

> All external traffic enters through the **API Gateway**. Paths below are the
> public (gateway) paths. The gateway validates the JWT, strips it, and forwards
> trusted identity headers (`X-User-Id`, `X-User-Roles`, `X-Correlation-Id`) to
> the owning service. AI service endpoints are **internal only** (not exposed
> through the gateway).

## Conventions

- **Base & versioning:** `/api/v1/...`. Breaking changes bump the version.
- **Auth:** `Authorization: Bearer <jwt>` on everything except `/auth/register`,
  `/auth/login`, `/auth/refresh`, `/auth/oauth/*`.
- **RBAC** annotations below: 🟦 CANDIDATE · 🟩 RECRUITER · 🟨 HIRING_MANAGER · 🟥 ADMIN.
- **Errors** — uniform problem shape (RFC 7807-ish):
  ```json
  { "type":"about:blank", "title":"Validation failed", "status":400,
    "detail":"email must be valid", "correlationId":"...", "errors":[...] }
  ```
- **Pagination** — cursor or page params: `?page=0&size=20&sort=field,desc`;
  responses wrap as `{ "content":[...], "page":{number,size,totalElements,totalPages} }`.
- **Idempotency** — mutating POSTs that trigger async work accept an
  `Idempotency-Key` header.
- **Async** — endpoints that kick off heavy work return `202 Accepted` with a
  resource you can poll.

## Gateway routing

| Path prefix | Routed to |
|---|---|
| `/api/v1/auth/**` | Auth Service |
| `/api/v1/candidates/**`, `/api/v1/resumes/**` | Candidate Service |
| `/api/v1/jobs/**` | Job Service |
| `/api/v1/applications/**` | Application/ATS Service |
| `/api/v1/matching/**` | Matching Service |
| `/api/v1/interviews/**` | Interview Service |
| `/api/v1/analytics/**` | Analytics Service |

---

## Auth Service

| Method | Path | Roles | Description |
|---|---|---|---|
| POST | `/auth/register` | public | Create account (role defaults to CANDIDATE; RECRUITER via invite/admin). |
| POST | `/auth/login` | public | Returns `{accessToken, refreshToken, expiresIn}`. |
| POST | `/auth/refresh` | public | Rotate refresh token, issue new access token. |
| POST | `/auth/logout` | any | Revoke refresh token; denylist current `jti`. |
| GET  | `/auth/oauth/{provider}` | public | Begin OAuth2 (google\|linkedin). |
| GET  | `/auth/oauth/{provider}/callback` | public | Complete OAuth2, issue tokens. |
| GET  | `/auth/me` | any | Current user + roles. |
| POST | `/auth/users/{id}/roles` | 🟥 | Assign/revoke roles. |

```jsonc
// POST /auth/login  →  200
{ "accessToken":"eyJ...", "refreshToken":"...", "expiresIn":900, "tokenType":"Bearer" }
```

---

## Candidate Service

| Method | Path | Roles | Description |
|---|---|---|---|
| GET  | `/candidates/me` | 🟦 | My profile. |
| PUT  | `/candidates/me` | 🟦 | Update profile (headline, location, links). |
| GET  | `/candidates/{id}` | 🟩🟨 | View a candidate (recruiter/hiring-mgr). |
| POST | `/candidates/me/resume` | 🟦 | Upload resume (multipart). **202** — parsing async. |
| GET  | `/resumes/{id}` | 🟦🟩🟨 | Resume + `parseStatus`; structured data once `PARSED`. |
| GET  | `/candidates/me/skill-gap?jobId=` | 🟦 | Skill-gap for a job (proxied to Matching). |

```jsonc
// POST /candidates/me/resume  (multipart: file)  →  202
{ "resumeId":"...", "parseStatus":"PENDING",
  "poll":"/api/v1/resumes/{resumeId}" }

// GET /resumes/{id}  →  200 (after parsing)
{ "id":"...", "parseStatus":"PARSED",
  "candidate":{ "fullName":"...", "email":"...", "totalExpYrs":4.5 },
  "skills":["Java","Spring","Kafka"],
  "experiences":[...], "education":[...], "projects":[...], "certifications":[...] }
```

---

## Job Service

| Method | Path | Roles | Description |
|---|---|---|---|
| POST | `/jobs` | 🟩 | Create job (DRAFT). |
| PUT  | `/jobs/{id}` | 🟩 | Update job. |
| POST | `/jobs/{id}/publish` | 🟩 | OPEN the job → emits `JobPosted` (triggers embedding). |
| POST | `/jobs/{id}/close` | 🟩 | CLOSE the job. |
| GET  | `/jobs` | any | List/search jobs (filters: skills, location, exp). Cached. |
| GET  | `/jobs/{id}` | any | Job detail. |

```jsonc
// POST /jobs  →  201
{ "title":"Backend Engineer",
  "description":"...",
  "minExpYrs":3, "maxExpYrs":6, "educationLevel":"BACHELOR",
  "requiredSkills":[ {"skillName":"Java","weight":1.0,"required":true},
                     {"skillName":"Kafka","weight":0.8,"required":false} ] }
```

---

## Application / ATS Service

| Method | Path | Roles | Description |
|---|---|---|---|
| POST | `/applications` | 🟦 | Apply to a job → emits `ApplicationSubmitted`. |
| GET  | `/applications/me` | 🟦 | Candidate's applications + current stage (tracking). |
| GET  | `/applications?jobId=` | 🟩🟨 | Pipeline for a job (board view). |
| PATCH| `/applications/{id}/stage` | 🟩🟨 | Move stage → emits `StageChanged`, writes audit. |
| POST | `/applications/{id}/comments` | 🟩🟨 | Add recruiter comment. |
| GET  | `/applications/{id}/activity` | 🟩🟨 | Activity/audit log. |

```jsonc
// PATCH /applications/{id}/stage  →  200
{ "toStage":"SHORTLISTED", "reason":"Strong skill match" }

// GET /applications/me  →  200
[ { "id":"...", "job":{ "id":"...","title":"Backend Engineer" },
    "currentStage":"INTERVIEW", "appliedAt":"...", "lastUpdated":"..." } ]
```

---

## Matching Service

| Method | Path | Roles | Description |
|---|---|---|---|
| GET  | `/matching/jobs/{jobId}/candidates` | 🟩🟨 | Ranked candidates for a job (top-N, paginated). |
| GET  | `/matching/candidates/{candidateId}/jobs` | 🟦 | Best-matching jobs for a candidate. |
| GET  | `/matching/score?candidateId=&jobId=` | 🟦🟩🟨 | Single match score breakdown. |
| GET  | `/matching/skill-gap?candidateId=&jobId=` | 🟦🟩🟨 | Missing skills + roadmap. |
| POST | `/matching/jobs/{jobId}/rescore` | 🟩 | Force re-rank (e.g. after JD change). **202**. |
| GET  | `/matching/predict?candidateId=&jobId=` | 🟩🟨 | Success probability (ML). |

```jsonc
// GET /matching/score?candidateId=..&jobId=..  →  200
{ "candidateId":"...", "jobId":"...",
  "finalScore":0.92,
  "breakdown":{ "semantic":0.95,"skillOverlap":0.88,"experienceFit":0.90,"educationFit":1.0 },
  "modelVersion":"match-v1.2", "scoredAt":"..." }

// GET /matching/jobs/{jobId}/candidates?size=20  →  200
{ "content":[ { "candidateId":"...","name":"...","finalScore":0.92,"topSkills":[...] } ],
  "page":{ "number":0,"size":20,"totalElements":134,"totalPages":7 } }
```

---

## Interview Service

| Method | Path | Roles | Description |
|---|---|---|---|
| POST | `/interviews` | 🟩🟨 | Generate questions for {candidateId, jobId}. **202** (LLM async). |
| GET  | `/interviews/{id}` | 🟩🟨 | Question set; `status` = GENERATING\|READY\|FAILED. |
| GET  | `/interviews?candidateId=&jobId=` | 🟩🟨 | Existing sets for a pair. |

```jsonc
// POST /interviews  →  202
{ "id":"...", "status":"GENERATING", "poll":"/api/v1/interviews/{id}" }

// GET /interviews/{id}  →  200 (when READY)
{ "id":"...", "status":"READY", "llmModel":"gemini-1.5",
  "questions":[
    {"category":"TECHNICAL","question":"Explain Kafka consumer groups.","difficulty":"MEDIUM",
     "suggestedAnswer":"..."},
    {"category":"BEHAVIORAL","question":"Describe a conflict you resolved...","difficulty":"EASY"},
    {"category":"SCENARIO","question":"Your service is dropping messages under load...","difficulty":"HARD"}
  ] }
```

---

## Analytics Service

| Method | Path | Roles | Description |
|---|---|---|---|
| GET | `/analytics/overview` | 🟩🟨 | Apps received, shortlisted, hires, avg time-to-hire. |
| GET | `/analytics/funnel?jobId=` | 🟩🟨 | Stage-by-stage funnel. |
| GET | `/analytics/time-to-hire?from=&to=` | 🟩🟨 | Time-to-hire trend. |
| GET | `/analytics/conversion?jobId=` | 🟩🟨 | Conversion/shortlist/offer rates. |
| GET | `/analytics/skills/in-demand?window=30d` | 🟩🟨 | Most in-demand skills. |
| GET | `/analytics/reports/export?type=` | 🟩🟨 | CSV/PDF export. |

---

## AI Service (internal — not via gateway)

Called by Matching and Interview services over the cluster network only.

| Method | Path | Description |
|---|---|---|
| POST | `/internal/parse` | Parse a resume (text or s3Key) → structured JSON. Usually invoked by the Kafka consumer, exposed for reprocessing. |
| POST | `/internal/embed` | Embed text → upsert into pgvector, return `embeddingRef`. |
| POST | `/internal/score` | `{candidateEmbeddingRef, jobEmbeddingRef, candidateSkills, jobRequiredSkills, expYrs, expBand, eduLevel}` → score breakdown. |
| POST | `/internal/skill-gap` | `{candidateSkills, jobRequiredSkills}` → missing skills (+ optional LLM roadmap). |
| POST | `/internal/interview/generate` | `{resume, jd, skills}` → categorized questions (LLM). |
| POST | `/internal/predict` | `{features}` → success probability + model version. |
| GET  | `/internal/health`, `/internal/models` | Liveness + active model versions. |

```jsonc
// POST /internal/score  →  200
{ "finalScore":0.92,
  "semantic":0.95, "skillOverlap":0.88, "experienceFit":0.90, "educationFit":1.0,
  "missingSkills":["Redis","Kubernetes"],
  "modelVersion":"match-v1.2" }
```
