# Matchly — API Gateway

The single entry point for all external traffic. Built on **Spring Cloud Gateway
(reactive)**. It routes by path prefix to the owning microservice, validates the
HS256 JWT at the edge, and forwards trusted identity headers downstream.

- **Java** 17 · **Spring Boot** 3.2.5 · **Spring Cloud** 2023.0.1
- **Port** `8080`
- **Package** `com.matchly.gateway`

## What it does

- **Routing** — config-driven, by path prefix, with **no rewrite** (services own
  the full `/api/v1/...` path, per `CONVENTIONS.md`).
- **Correlation id** — generates an `X-Correlation-Id` if the client didn't send
  one, propagates it downstream, and echoes it on the response.
- **JWT validation** — on every non-public path, verifies the bearer token's
  HS256 signature, issuer and expiry using the shared `JWT_SECRET`. On success it
  **strips any client-supplied identity headers** and injects trusted
  `X-User-Id` and `X-User-Roles` for downstream services to consume. On failure
  it returns `401` (RFC-7807 problem body with the correlation id).
- **CORS** — allows the configured frontend origins.
- **Rate limiting** — optional Redis-backed `RequestRateLimiter` (off by default;
  see below).

## Routes

| Path prefix | Routed to | Default local URI |
|---|---|---|
| `/api/v1/auth/**` | auth-service | `http://localhost:8081` |
| `/api/v1/candidates/**`, `/api/v1/resumes/**` | candidate-service | `http://localhost:8082` |
| `/api/v1/jobs/**` | job-service | `http://localhost:8083` |
| `/api/v1/applications/**` | application-service | `http://localhost:8084` |
| `/api/v1/matching/**` | matching-service | `http://localhost:8085` |
| `/api/v1/interviews/**` | interview-service | `http://localhost:8086` |
| `/api/v1/analytics/**` | analytics-service | `http://localhost:8087` |

In the `docker` profile each `uri` defaults to the compose service name
(e.g. `http://auth-service:8081`). Every host is overridable via
`*_SERVICE_URI` env vars.

### Public (no-auth) paths

`/api/v1/auth/**`, `/actuator/**`, and Swagger (`/swagger-ui`, `/v3/api-docs`).
Everything else requires a valid `Authorization: Bearer <jwt>`.

## Configuration (env-overridable)

| Variable | Default | Meaning |
|---|---|---|
| `JWT_SECRET` | `change-me-...` | HS256 verification secret (must match auth-service). |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:8088,http://localhost:5173` | Allowed frontend origins. |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` (`redis`/`6379` in docker) | Redis for rate limiting. |
| `RATE_LIMIT_ENABLED` | `false` | Turn on the Redis `RequestRateLimiter`. |
| `RATE_LIMIT_REPLENISH` | `10` | Tokens/sec refill. |
| `RATE_LIMIT_BURST` | `20` | Bucket capacity. |
| `AUTH_SERVICE_URI` … `ANALYTICS_SERVICE_URI` | per-profile defaults | Per-service host overrides. |
| `SPRING_PROFILES_ACTIVE` | — | Set to `docker` inside compose. |

## Rate limiting

Disabled by default so the gateway runs without Redis. To enable, set
`RATE_LIMIT_ENABLED=true` (and make sure `REDIS_HOST`/`REDIS_PORT` point at a live
Redis). When enabled, a `RedisRateLimiter` and a `KeyResolver` (keyed by
`X-User-Id`, falling back to client IP) are registered; attach the
`RequestRateLimiter` filter to a route to enforce it. Leaving it off lets the
gateway degrade gracefully when Redis is absent.

## Run locally

```bash
export JWT_SECRET=change-me-to-a-long-random-secret-at-least-32-chars
# Optionally point routes at running services:
export AUTH_SERVICE_URI=http://localhost:8081

mvn spring-boot:run
# Gateway: http://localhost:8080
# Health:  http://localhost:8080/actuator/health
```

Example end-to-end (auth-service must be running on 8081):

```bash
# Login through the gateway → get a token
curl -X POST localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"jane@example.com","password":"supersecret"}'

# Call a protected route — gateway validates the JWT and injects X-User-Id
curl localhost:8080/api/v1/jobs -H 'Authorization: Bearer eyJ...'
```

## Run via Docker

```bash
docker build -t matchly/api-gateway .
# Or from the repo root:
docker compose up --build api-gateway
```

Multi-stage image (`maven:3.9-eclipse-temurin-17` → `eclipse-temurin:17-jre`).

## Notes / caveats

- Downstream services **trust** the injected `X-User-Id` / `X-User-Roles` headers
  and do not re-verify the JWT — this is the gateway-enforced perimeter described
  in `CONVENTIONS.md`. The gateway strips any inbound copies of those headers so
  clients can't spoof identity.
- Rate limiting is included but off by default (graceful degradation without Redis).
- Not compiled in this environment (no Maven/Docker available at authoring time).
