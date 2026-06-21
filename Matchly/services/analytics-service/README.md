# Analytics Service

Recruiter analytics. Consumes domain events and serves dashboard read models.

- **Port:** 8087 · **Database:** `analytics_db` · **Package:** `com.matchly.analytics`
- **Schema:** DATA_MODELS §7 (`funnel_daily`, `time_to_hire`, `skill_demand`,
  `conversion_metrics`, plus a `processed_events` dedup table), Flyway-managed.

## How it works
A set of `@KafkaListener`s consume the platform's events and project them into
denormalized read models (the source of truth stays in the owning services):

| Topic | Event | Projection |
|---|---|---|
| `application.events` | `ApplicationSubmitted` | funnel APPLIED++, seed time-to-hire, conversion |
| `application.events` | `StageChanged` | funnel stage++, time-to-hire on OFFER |
| `job.events` | `JobPosted` | skill_demand tally per required skill |
| `matching.events` | `MatchScored` | observed (no read-model column yet) |
| `interview.events` | `QuestionsGenerated` | observed (no read-model column yet) |

Idempotency: each event id is claimed in `processed_events` before projecting,
so replays are no-ops.

## API (`/api/v1/analytics`)
| Method | Path | Notes |
|---|---|---|
| GET | `/overview` | Applications, shortlisted, hires, avg time-to-hire. |
| GET | `/funnel?jobId=` | Stage totals + per-day breakdown. |
| GET | `/time-to-hire?from=&to=` | ISO-8601 window (optional); points + average. |
| GET | `/conversion?jobId=` | Shortlist/offer rates per day + averages. |
| GET | `/skills/in-demand?window=30d` | Most in-demand skills in the window. |
| GET | `/reports/export?type=overview\|skills` | CSV export. |

Swagger UI: `/swagger-ui.html` · Health: `/actuator/health`.

## Config (env-overridable)
`SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`,
`KAFKA_CONSUMER_GROUP`, and the `matchly.topics.*` topic names.

## Known simplifications
- `MatchScored` / `QuestionsGenerated` are consumed but not yet projected into a
  dedicated read model (logged only).
- Report export is CSV only (PDF is a future enhancement).
- Not locally compiled (no Maven/Docker in the build env). See repo `STATUS.md`.
