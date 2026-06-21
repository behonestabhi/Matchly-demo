# Matchly — Interview Service

Generates categorized interview questions for a `{candidateId, jobId}` pair by
delegating to the AI service, then persists the result and emits a
`QuestionsGenerated` event. Part of the Matchly recruitment platform; conforms to
`../../CONVENTIONS.md`, `../../docs/API_CONTRACTS.md`, and
`../../docs/DATA_MODELS.md` (§6, §9).

- **Java** 17 · **Spring Boot** 3.2.5 · **Maven**
- **Port** `8086`
- **Database** `interview_db` (PostgreSQL), schema managed by **Flyway** (`V1__init.sql`)
- **Package** `com.matchly.interview`

## What it does

- `POST /api/v1/interviews` creates a `question_sets` row in status `GENERATING`
  and immediately returns **202** `{id, status, poll}`.
- Asynchronously (`@Async` pool), it calls the AI service
  `POST {AI_SERVICE_URL}/internal/interview/generate`, persists the returned
  questions, flips the set to `READY` (or `FAILED` on error), and publishes
  `QuestionsGenerated` to `interview.events` keyed by `candidateId`.
- If `resumeText` / `jobDescription` / `skills` are not supplied in the request,
  it optionally enriches them from candidate-service / job-service via
  `RestClient` (configurable URLs, **graceful fallback to empty** on any error).

## Endpoints

All under `/api/v1/interviews`. Plus `/actuator/health` and Swagger UI at
`/swagger-ui.html`.

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/interviews` | Generate questions for `{candidateId, jobId}` (optional `resumeText`, `jobDescription`, `skills`). Returns **202** `{id, status:"GENERATING", poll}`. |
| GET  | `/api/v1/interviews/{id}` | Question set: `status` (`GENERATING`\|`READY`\|`FAILED`), `llmModel`, and `questions[]`. |
| GET  | `/api/v1/interviews?candidateId=&jobId=` | Existing sets for a candidate/job (either or both filters; newest first). |

### Examples

```bash
# Kick off generation (gateway forwards X-User-Id / X-User-Roles)
curl -X POST localhost:8086/api/v1/interviews \
  -H 'Content-Type: application/json' \
  -d '{"candidateId":"11111111-1111-1111-1111-111111111111",
       "jobId":"22222222-2222-2222-2222-222222222222",
       "skills":["Java","Kafka","Spring"]}'
# → 202 { "id":"...", "status":"GENERATING", "poll":"/api/v1/interviews/{id}" }

# Poll until READY
curl localhost:8086/api/v1/interviews/{id}

# List sets for a pair
curl 'localhost:8086/api/v1/interviews?candidateId=...&jobId=...'
```

## Events

| Direction | Topic | Event | Key | Payload |
|---|---|---|---|---|
| Produces | `interview.events` | `QuestionsGenerated` | `candidateId` | `{candidateId, jobId, questionSetId}` |

All events use the common envelope `{eventId, eventType, occurredAt, correlationId, version, payload}` (`occurredAt` is ISO-8601 UTC).

## Configuration (all env-overridable)

| Variable | Default (local / docker) | Meaning |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/interview_db` / `...//postgres:5432/...` | JDBC URL. |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `matchly` / `matchly` | DB credentials. |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` / `kafka:9092` | Kafka broker. |
| `AI_SERVICE_URL` | `http://localhost:8000` / `http://ai-service:8000` | AI service base URL. |
| `AI_SERVICE_CONNECT_TIMEOUT_MS` | `3000` | AI connect timeout. |
| `AI_SERVICE_READ_TIMEOUT_MS` | `30000` | AI read timeout (LLM calls can be slow). |
| `CANDIDATE_SERVICE_URL` | _empty_ / `http://candidate-service:8082` | Optional resume/skills lookup. Blank = disabled. |
| `JOB_SERVICE_URL` | _empty_ / `http://job-service:8083` | Optional JD lookup. Blank = disabled. |
| `SPRING_PROFILES_ACTIVE` | — | Set to `docker` inside compose. |

## Run locally

Requires a PostgreSQL with an `interview_db` database and a Kafka broker (the
compose stack provides both). Flyway applies `V1__init.sql` on startup.

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/interview_db
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export AI_SERVICE_URL=http://localhost:8000
mvn spring-boot:run
# → http://localhost:8086/swagger-ui.html
```

## Run via Docker

```bash
docker build -t matchly/interview-service .
# Or from the repo root:
docker compose up --build interview-service
```

Multi-stage image (`maven:3.9-eclipse-temurin-17` → `eclipse-temurin:17-jre`).

## Notes / caveats

- **Auth is perimeter-trust.** `HeaderAuthFilter` reads gateway-forwarded
  `X-User-Id` / `X-User-Roles` / `X-Correlation-Id`; no JWT verification here.
- **Async generation** runs on a bounded thread pool. The AI call has explicit
  connect/read timeouts and is wrapped in try/catch — failures mark the set
  `FAILED` (with `errorDetail`) rather than crashing the worker.
- **Candidate/job enrichment is best-effort.** If the URLs are blank or the
  calls fail, generation proceeds with whatever was provided in the request.
- Not compiled in this environment (no Maven/Docker available at authoring time).
```
