# Job Service

Owns **job postings** and their lifecycle. Part of the Matchly platform.

- **Port:** 8083 · **Database:** `job_db` · **Package:** `com.matchly.job`
- **Schema:** DATA_MODELS §3 (`jobs`, `job_required_skills`), managed by Flyway.

## Responsibilities
- CRUD for job postings (title, JD, location, experience band, education, required skills).
- Lifecycle: `DRAFT → OPEN → CLOSED`.
- On **publish**: embeds the JD via the AI service (`POST /internal/embed`, best-effort —
  the job still publishes if the AI service is down) and produces `JobPosted`.
- On **close**: produces `JobClosed`.
- Search/list with filters (skill, location, min experience) and pagination.

## API (`/api/v1/jobs`)
| Method | Path | Notes |
|---|---|---|
| POST | `/jobs` | Create a DRAFT job. Recruiter taken from gateway identity (`X-User-Id`). |
| PUT | `/jobs/{id}` | Update fields; non-null fields replace stored values. |
| POST | `/jobs/{id}/publish` | Embed JD, open the job, emit `JobPosted`. |
| POST | `/jobs/{id}/close` | Close the job, emit `JobClosed`. |
| GET | `/jobs` | Search. Params: `status` (default `OPEN`), `skill`, `location`, `minExp`, `page`, `size`. |
| GET | `/jobs/{id}` | Job detail + required skills. |

Swagger UI: `/swagger-ui.html` · Health: `/actuator/health`.

## Events produced (topic `job.events`, key = jobId)
- `JobPosted` → `{ jobId, recruiterId, requiredSkills:[{skillName,weight,required}], minExpYrs, maxExpYrs }`
- `JobClosed` → `{ jobId }`

## Run
- **Whole stack:** from repo root, `docker compose up --build job-service` (with `postgres` + `kafka`).
- **Locally:** needs Postgres `job_db` and (optionally) Kafka + AI service.
  `mvn spring-boot:run` (or build the image — the Dockerfile runs the Maven build, so no local Maven needed).

## Config (env-overridable)
`SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `AI_SERVICE_URL`,
`HTTP_CLIENT_CONNECT_TIMEOUT_MS`, `HTTP_CLIENT_READ_TIMEOUT_MS`, `KAFKA_TOPIC_JOB_EVENTS`.
The `docker` profile points hosts at the compose service names.
