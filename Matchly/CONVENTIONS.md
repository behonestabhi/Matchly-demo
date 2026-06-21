# Build Conventions (canonical — all services must conform)

These are the shared contracts every component is built against. If you change a
port or name here, change it in `docker-compose.yml` and the service config too.

## Versions
- **Java 17**, **Spring Boot 3.2.5**, Maven build (Dockerized — `maven:3.9-eclipse-temurin-17`).
- **Python 3.11+** (FastAPI), **Node 20+** (React/Vite).
- Group id `com.matchly`; package `com.matchly.<service>`.

## Ports
| Component | Port | DB name |
|---|---|---|
| api-gateway | 8080 | — |
| auth-service | 8081 | auth_db |
| candidate-service | 8082 | candidate_db |
| job-service | 8083 | job_db |
| application-service | 8084 | application_db |
| matching-service | 8085 | matching_db |
| interview-service | 8086 | interview_db |
| analytics-service | 8087 | analytics_db |
| ai-service (FastAPI) | 8000 | ai_db |
| frontend (nginx) | 8088 | — |
| PostgreSQL | 5432 | (all of the above) |
| Redis | 6379 | — |
| Kafka (KRaft) | 9092 | — |

## Infra hostnames (inside docker-compose network)
`postgres`, `redis`, `kafka`, plus each service's compose name (e.g. `auth-service`).
Local (non-docker) runs default to `localhost`. All config is env-overridable.

## Database
- One PostgreSQL container, one database **per service** (created by
  `infra/docker/postgres/init-databases.sh`). This honors database-per-service
  while keeping local dev to a single container.
- Each service manages its own schema with **Flyway** (`V1__init.sql`).
- `ai_db` has the `pgvector` extension (image: `pgvector/pgvector:pg16`).

## Auth model
- **auth-service** issues JWT (HS256, shared secret `JWT_SECRET`) with `sub`,
  `roles`, `email` claims.
- **api-gateway** validates the JWT and forwards trusted headers downstream:
  `X-User-Id`, `X-User-Roles`, `X-Correlation-Id`.
- Downstream services **trust** those headers (a lightweight filter reads them);
  they do not re-verify the JWT. This is gateway-enforced perimeter auth.

## API paths
- Public paths are `/api/v1/<resource>/...` exactly as in `docs/API_CONTRACTS.md`.
- The gateway routes by prefix **without** rewriting, so each service maps the
  full `/api/v1/...` path.
- Every Spring service exposes `/actuator/health` (Spring Actuator) and Swagger
  UI at `/swagger-ui.html`.

## Kafka
- Single KRaft broker at `kafka:9092`. JSON-serialized events.
- Topics (partition key in parens): `candidate.events` (candidateId),
  `resume.parsed` (candidateId), `job.events` (jobId), `job.embedded` (jobId),
  `application.events` (applicationId), `matching.events` (candidateId),
  `interview.events` (candidateId). Each has a `<topic>.DLT` dead-letter topic.
- Common event envelope (see `libs/schemas/event-envelope.schema.json`):
  `{ eventId, eventType, occurredAt, correlationId, version, payload }`.

## Per-component Dockerfile expectation
- Spring services: multi-stage (maven build → `eclipse-temurin:17-jre`), expose
  their port, run the jar.
- AI service: `python:3.11-slim`, install requirements, `uvicorn app.main:app`.
- Frontend: node build → `nginx:alpine` serving static + proxy config.
