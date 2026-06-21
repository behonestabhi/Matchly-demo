# Matching Service

The headline service: scores candidates against jobs and serves ranked results.

- **Port:** 8085 · **Database:** `matching_db` · **Package:** `com.matchly.matching`
- **Schema:** DATA_MODELS §5 (`match_scores`, `skill_gaps`, `success_predictions`), Flyway-managed.

## How scoring works
1. **Trigger** — consumes `ApplicationSubmitted` from `application.events`, *or* an
   on-demand `GET /score` for a pair with no persisted score.
2. **Fetch** — pulls the candidate's matching profile from candidate-service
   (`GET /internal/candidates/{id}/matching-profile`) and the job from job-service
   (`GET /api/v1/jobs/{id}`).
3. **Score** — calls the AI service `POST /internal/score` (the explainable
   weighted blend: semantic + skill overlap + experience + education).
4. **Persist + emit** — upserts `match_scores` + `skill_gaps`, produces
   `MatchScored` and `SkillGapComputed` to `matching.events` (key = candidateId).

Reads of persisted scores never need the AI service; only on-demand compute does,
and it degrades to **503** if a dependency (candidate/job/AI) is down (fail-soft).

## API (`/api/v1/matching`)
| Method | Path | Notes |
|---|---|---|
| GET | `/jobs/{jobId}/candidates` | Ranked candidates (paginated, highest score first). |
| GET | `/candidates/{candidateId}/jobs` | Best-matching jobs for a candidate. |
| GET | `/score?candidateId=&jobId=` | Score + breakdown; computes on demand if absent. |
| GET | `/skill-gap?candidateId=&jobId=` | Missing skills + LLM roadmap. |
| POST | `/jobs/{jobId}/rescore` | Re-score the job's known candidates (202). |
| GET | `/predict?candidateId=&jobId=` | ML success probability (AI `/internal/predict`). |

Swagger UI: `/swagger-ui.html` · Health: `/actuator/health`.

## Events
- **Consumes** `application.events` (`ApplicationSubmitted`) — String/JSON values.
- **Produces** `matching.events` (`MatchScored`, `SkillGapComputed`), key = candidateId.

## Config (env-overridable, prefix `matchly.*`)
`SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`,
`CANDIDATE_SERVICE_URL`, `JOB_SERVICE_URL`, `AI_SERVICE_URL`,
`HTTP_CONNECT_TIMEOUT_MS`, `HTTP_READ_TIMEOUT_MS`, `MATCH_MODEL_VERSION`.

## Known simplifications
- Idempotency on the consumer relies on the score upsert (replays re-score the
  same pair rather than being skipped) — adequate, not a dedicated dedup store.
- Predictor features `certificationCount`/`projectCount` default to 0 (not yet
  surfaced by the candidate matching profile).
- Not locally compiled (no Maven/Docker in the build env) — built via the
  Dockerized Maven stage. See repo `STATUS.md`.
