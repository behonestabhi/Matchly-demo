# Matchly — Application / ATS Service

Owns the **application** aggregate (the join of a candidate and a job) plus its
lifecycle: pipeline stages, recruiter comments and an append-only activity log.
Conforms to `../../CONVENTIONS.md`, `../../docs/API_CONTRACTS.md`, and
`../../docs/DATA_MODELS.md` (§4, §9).

- **Java** 17 · **Spring Boot** 3.2.5 · **Maven**
- **Port** `8084`
- **Database** `application_db` (PostgreSQL), schema managed by **Flyway**
- **Package** `com.matchly.application`

## What it does

- **Apply** a candidate to a job: creates an `APPLIED` application, writes the
  initial `stage_transition` (`null → APPLIED`) and an `activity_log` entry, then
  produces an `ApplicationSubmitted` event.
- **Move stages** through the pipeline, enforcing the legal transition graph
  (`APPLIED → SCREENING → SHORTLISTED → INTERVIEW → OFFER`; `REJECTED` reachable
  from any non-terminal stage). Each move writes a transition + activity entry
  and produces a `StageChanged` event.
- **Comment** on an application (visibility `INTERNAL` | `SHARED`).
- Serve a candidate's **own applications** (tracking) and a **pipeline board** for
  a job, plus the **activity/audit trail**.

Identity is taken from the gateway-forwarded `X-User-Id` / `X-User-Roles` headers
(a lightweight `HeaderAuthFilter`); this service trusts the perimeter and does not
re-verify JWTs.

## Stages & transitions

```
APPLIED ──► SCREENING ──► SHORTLISTED ──► INTERVIEW ──► OFFER
   │            │              │              │
   └────────────┴──────────────┴──────────────┴──────► REJECTED
```

`OFFER` and `REJECTED` are terminal. Illegal transitions return `400`.

## Endpoints

All under `/api/v1/applications`. RBAC: 🟦 CANDIDATE · 🟩 RECRUITER · 🟨 HIRING_MANAGER.

| Method | Path | Roles | Description |
|---|---|---|---|
| POST  | `/applications` | 🟦 | Apply to a job (`201`). Body `{ "jobId":"<uuid>", "source":"DIRECT" }`. Emits `ApplicationSubmitted`. |
| GET   | `/applications/me` | 🟦 | Candidate's applications + current stage (uses `X-User-Id`). |
| GET   | `/applications?jobId=<uuid>` | 🟩🟨 | Pipeline board for a job. |
| PATCH | `/applications/{id}/stage` | 🟩🟨 | Move stage (`200`). Body `{ "toStage":"SHORTLISTED", "reason":"..." }`. Emits `StageChanged`. |
| POST  | `/applications/{id}/comments` | 🟩🟨 | Add a comment (`201`). Body `{ "body":"...", "visibility":"INTERNAL" }`. |
| GET   | `/applications/{id}/activity` | 🟩🟨 | Activity / audit log. |

Plus `/actuator/health` and Swagger UI at `/swagger-ui.html`.

### Examples

```bash
# Apply (as candidate) — gateway would inject X-User-Id; shown here directly.
curl -X POST localhost:8084/api/v1/applications \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 11111111-1111-1111-1111-111111111111' \
  -d '{"jobId":"22222222-2222-2222-2222-222222222222","source":"DIRECT"}'

# Move stage (as recruiter)
curl -X PATCH localhost:8084/api/v1/applications/<appId>/stage \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 33333333-3333-3333-3333-333333333333' \
  -H 'X-User-Roles: RECRUITER' \
  -d '{"toStage":"SCREENING","reason":"Looks promising"}'

# Pipeline board for a job
curl 'localhost:8084/api/v1/applications?jobId=22222222-2222-2222-2222-222222222222' \
  -H 'X-User-Id: 33333333-3333-3333-3333-333333333333' \
  -H 'X-User-Roles: RECRUITER'
```

## Events produced

To topic `application.events` (key = `applicationId`), wrapped in the common
envelope `{ eventId, eventType, occurredAt, correlationId, version, payload }`:

| eventType | payload |
|---|---|
| `ApplicationSubmitted` | `{ applicationId, candidateId, jobId }` |
| `StageChanged` | `{ applicationId, fromStage, toStage, actorId }` |

Consumed by the Matching and Analytics services. This service consumes nothing.

## Configuration (all env-overridable)

| Variable | Default (local) | Default (docker) | Meaning |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/application_db` | `...//postgres:5432/...` | JDBC URL. |
| `SPRING_DATASOURCE_USERNAME` | `matchly` | `matchly` | DB user. |
| `SPRING_DATASOURCE_PASSWORD` | `matchly` | `matchly` | DB password. |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | `kafka:9092` | Kafka brokers. |
| `KAFKA_TOPIC_APPLICATION_EVENTS` | `application.events` | — | Output topic name. |
| `SPRING_PROFILES_ACTIVE` | — | `docker` | Activates `application-docker.yml`. |

## Run locally

Requires PostgreSQL (`application_db`) and a Kafka broker. Flyway applies
`V1__init.sql` on startup.

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/application_db
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
mvn spring-boot:run
# → http://localhost:8084/swagger-ui.html
```

## Run via Docker

```bash
docker build -t matchly/application-service .
# or, from the repo root:
docker compose up --build application-service
```

Multi-stage image (`maven:3.9-eclipse-temurin-17` → `eclipse-temurin:17-jre`).

## Notes / caveats

- **Perimeter trust.** Authorization relies on trusted `X-User-Id` /
  `X-User-Roles` headers set by the gateway; no JWT is verified here.
- **Idempotency on apply.** A `UNIQUE (candidate_id, job_id)` constraint plus a
  pre-check returns `409` on a duplicate application (race-safe).
- **Fail-soft events.** Kafka publish failures are logged, not propagated — the
  write has already committed and the API still succeeds.
- **Append-only audit.** `activity_log` is only ever inserted into; the
  `payload` column is JSONB.
- Not compiled in this environment (no Maven/Docker available at authoring time).
