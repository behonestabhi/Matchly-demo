# Matchly — Candidate Service

Owns candidate **profiles** and the *structured* parsed resume. Conforms to
`../../CONVENTIONS.md`, `../../docs/API_CONTRACTS.md`, and
`../../docs/DATA_MODELS.md` (§2).

- **Java** 17 · **Spring Boot** 3.2.5 · **Maven**
- **Port** `8082`
- **Database** `candidate_db` (PostgreSQL), schema managed by **Flyway**
- **Package** `com.matchly.candidate`

## What it does

- Stores/serves candidate profiles (`headline`, `location`, `phone`, `links`).
- Accepts a resume upload (multipart), stores the bytes, and **parses it
  asynchronously** by orchestrating HTTP calls to the AI service.
- Exposes parse status + structured data (skills, experiences, education,
  projects, certifications) once parsing completes.
- Proxies skill-gap requests to the matching-service.

### Resume parse flow (HTTP-orchestrated — NOT Kafka-consumed)

1. `POST /candidates/me/resume` stores the file, inserts a `resumes` row with
   `parse_status=PENDING`, emits **`ResumeUploaded`** to `candidate.events`, and
   returns **`202 Accepted`** with a poll URL.
2. Asynchronously (`@Async` pool) the service:
   - calls AI `POST {AI_SERVICE_URL}/internal/parse` with the file bytes;
   - persists `parsed_resumes`, `candidate_skills`, `experiences`, `education`,
     `projects`, `certifications`;
   - calls AI `POST {AI_SERVICE_URL}/internal/embed` with the resume text and
     stores the returned `embedding_ref`;
   - sets `parse_status=PARSED` and produces **`ResumeParsed`** to `resume.parsed`
     (key = `candidateId`).
3. On any AI failure the resume is marked `parse_status=FAILED` (no
   `ResumeParsed` event). All AI calls use a timed-out `RestClient` and are
   wrapped in try/catch + logging, so a slow or down AI service never wedges the
   request thread.

## Endpoints

| Method | Path | Auth (role) | Description |
|---|---|---|---|
| GET  | `/api/v1/candidates/me` | 🟦 CANDIDATE | My profile (provisioned lazily on first access). |
| PUT  | `/api/v1/candidates/me` | 🟦 | Update profile (`headline`, `location`, `phone`, `links`). |
| GET  | `/api/v1/candidates/{id}` | 🟩🟨 | View a candidate by id. |
| POST | `/api/v1/candidates/me/resume` | 🟦 | Upload resume (multipart `file`). **202** — parsing async. |
| GET  | `/api/v1/resumes/{id}` | 🟦🟩🟨 | Resume `parseStatus`; structured data once `PARSED`. |
| GET  | `/api/v1/candidates/me/skill-gap?jobId=` | 🟦 | Skill-gap for a job (proxied to matching-service). |

Plus `/actuator/health` and Swagger UI at `/swagger-ui.html`.

Roles are advisory here: this service trusts the gateway-forwarded `X-User-Id` /
`X-User-Roles` headers (read by `HeaderAuthFilter`) and does not re-verify the
JWT.

### Examples

```bash
# Upload a resume (gateway would forward X-User-Id; shown explicitly here)
curl -X POST localhost:8082/api/v1/candidates/me/resume \
  -H 'X-User-Id: 11111111-1111-1111-1111-111111111111' \
  -F 'file=@resume.pdf;type=application/pdf'
# → 202 { "resumeId":"...", "parseStatus":"PENDING", "poll":"/api/v1/resumes/{resumeId}" }

# Poll for parse result
curl localhost:8082/api/v1/resumes/<resumeId>

# Update profile
curl -X PUT localhost:8082/api/v1/candidates/me \
  -H 'X-User-Id: 11111111-1111-1111-1111-111111111111' \
  -H 'Content-Type: application/json' \
  -d '{"headline":"Backend Engineer","location":"Pune","links":{"github":"https://github.com/x"}}'

# Skill-gap (proxied to matching-service)
curl 'localhost:8082/api/v1/candidates/me/skill-gap?jobId=<jobId>' \
  -H 'X-User-Id: 11111111-1111-1111-1111-111111111111'
```

## Events produced

| Topic | Key | eventType | payload |
|---|---|---|---|
| `candidate.events` | `candidateId` | `ResumeUploaded` | `{candidateId, resumeId, s3Key, fileName, mimeType}` |
| `resume.parsed` | `candidateId` | `ResumeParsed` | `{candidateId, resumeId, skills[], totalExpYrs, embeddingRef}` |

All events use the common envelope `{eventId, eventType, occurredAt,
correlationId, version, payload}` (JSON).

## Configuration (all env-overridable)

| Variable | Default (local) | Meaning |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/candidate_db` | JDBC URL (docker profile: host `postgres`). |
| `SPRING_DATASOURCE_USERNAME` | `matchly` | DB user. |
| `SPRING_DATASOURCE_PASSWORD` | `matchly` | DB password. |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers (docker profile: `kafka:9092`). |
| `AI_SERVICE_URL` | `http://localhost:8000` | AI service base URL (parse + embed). |
| `MATCHING_SERVICE_URL` | `http://localhost:8085` | Matching service base URL (skill-gap proxy). |
| `RESUME_STORAGE_DIR` | `./data/resumes` | Local directory for stored resume bytes (stands in for S3). |
| `HTTP_CLIENT_CONNECT_TIMEOUT_MS` | `3000` | Outbound connect timeout. |
| `HTTP_CLIENT_READ_TIMEOUT_MS` | `30000` | Outbound read timeout. |
| `SPRING_PROFILES_ACTIVE` | — | Set to `docker` inside compose. |

## Run locally

Requires a PostgreSQL with a `candidate_db` database and a Kafka broker (the
compose stack provides both). Flyway applies `V1__init.sql` on startup.

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/candidate_db
export SPRING_DATASOURCE_USERNAME=matchly SPRING_DATASOURCE_PASSWORD=matchly
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export AI_SERVICE_URL=http://localhost:8000
export MATCHING_SERVICE_URL=http://localhost:8085
export RESUME_STORAGE_DIR=./data/resumes

mvn spring-boot:run
# → http://localhost:8082/swagger-ui.html
```

## Run via Docker

```bash
docker build -t matchly/candidate-service .
# or, from the repo root:
docker compose up --build candidate-service
```

The image is multi-stage (`maven:3.9-eclipse-temurin-17` → `eclipse-temurin:17-jre`).

## Notes / caveats

- **Local file storage stands in for S3.** `ResumeStorage` writes raw bytes
  under `RESUME_STORAGE_DIR`; the stored relative path is recorded as the
  resume's `s3_key`. In a real deployment this component is swapped for an S3
  client. The Docker image declares a `/data/resumes` volume.
- **Parsing is HTTP-orchestrated, not Kafka-consumed.** The service produces
  `ResumeUploaded` / `ResumeParsed` but does not consume any topic.
- **AI / matching calls are resilient.** Both use a `RestClient` with
  connect/read timeouts; AI failures mark the resume `FAILED`, and a matching
  outage yields a `503` ProblemDetail from the skill-gap endpoint.
- The skill-gap response is relayed verbatim from matching-service (the
  candidate-service does not own that contract shape).
- Not compiled in this environment (no Maven/Docker available at authoring time).
```
