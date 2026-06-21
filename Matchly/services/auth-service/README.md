# Matchly — Auth Service

Owns user accounts and roles, and issues the HS256 JWTs that the API gateway
validates. Part of the Matchly recruitment platform; conforms to
`../../CONVENTIONS.md`, `../../docs/API_CONTRACTS.md`, and `../../docs/DATA_MODELS.md` (§1).

- **Java** 17 · **Spring Boot** 3.2.5 · **Maven**
- **Port** `8081`
- **Database** `auth_db` (PostgreSQL), schema managed by **Flyway**
- **Package** `com.matchly.auth`

## What it does

- Registers users (BCrypt-hashed passwords), default role `CANDIDATE`.
- Logs users in and issues an **access token** (JWT, HS256) + an opaque
  **refresh token** (stored only as a SHA-256 hash).
- Rotates refresh tokens (old one revoked, new pair issued).
- Revokes refresh tokens on logout.
- Returns the current user + roles, resolved from the gateway-forwarded
  `X-User-Id` header *or* a verified bearer token.
- Lets an `ADMIN` assign/revoke roles.

The JWT carries claims `sub` (userId), `email`, `roles` (list), plus a unique
`jti`. It is signed with the shared `JWT_SECRET`, which the gateway uses to verify.

## Endpoints

All under `/api/v1/auth`.

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/register` | public | Create account (role defaults to `CANDIDATE`). Returns a user summary (`201`). |
| POST | `/login` | public | Returns `{accessToken, refreshToken, expiresIn, tokenType:"Bearer"}`. |
| POST | `/refresh` | public | Rotate refresh token, issue a new access token. |
| POST | `/logout` | any | Revoke a refresh token (`204`, idempotent). |
| GET  | `/me` | any | Current user + roles (`X-User-Id` header or bearer token). |
| POST | `/users/{id}/roles` | ADMIN | Assign/revoke roles. Body: `{ "op":"ASSIGN"\|"REVOKE", "roles":["RECRUITER"] }`. |
| GET  | `/oauth/{provider}` | public | **STUB** — returns `501 Not Implemented`. |
| GET  | `/oauth/{provider}/callback` | public | **STUB** — returns `501 Not Implemented`. |

Plus `/actuator/health` and Swagger UI at `/swagger-ui.html`.

### Examples

```bash
# Register
curl -X POST localhost:8081/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"jane@example.com","password":"supersecret","fullName":"Jane Doe"}'

# Login
curl -X POST localhost:8081/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"jane@example.com","password":"supersecret"}'
# → { "accessToken":"eyJ...", "refreshToken":"...", "expiresIn":900, "tokenType":"Bearer" }

# Current user (direct call with bearer token)
curl localhost:8081/api/v1/auth/me -H 'Authorization: Bearer eyJ...'

# Refresh
curl -X POST localhost:8081/api/v1/auth/refresh \
  -H 'Content-Type: application/json' -d '{"refreshToken":"..."}'
```

## Configuration (all env-overridable)

| Variable | Default | Meaning |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/auth_db` | JDBC URL (docker profile: host `postgres`). |
| `SPRING_DATASOURCE_USERNAME` | `matchly` | DB user. |
| `SPRING_DATASOURCE_PASSWORD` | `matchly` | DB password. |
| `JWT_SECRET` | `change-me-...` | HS256 signing secret (≥ 32 chars; shared with gateway). |
| `JWT_ACCESS_TTL_SECONDS` | `900` | Access-token lifetime. |
| `JWT_REFRESH_TTL_SECONDS` | `1209600` | Refresh-token lifetime (14 days). |
| `SPRING_PROFILES_ACTIVE` | — | Set to `docker` inside compose. |

## Run locally

Requires a PostgreSQL with an `auth_db` database (the compose Postgres creates
one). Flyway applies `V1__init.sql` (also enables the `citext` and `pgcrypto`
extensions) and seeds the four roles on startup.

```bash
# Point at a local Postgres:
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/auth_db
export SPRING_DATASOURCE_USERNAME=matchly SPRING_DATASOURCE_PASSWORD=matchly
export JWT_SECRET=change-me-to-a-long-random-secret-at-least-32-chars

mvn spring-boot:run
# → http://localhost:8081/swagger-ui.html
```

## Run via Docker

```bash
# Build the image (Maven runs inside the build stage; no local Maven needed):
docker build -t matchly/auth-service .

# Or, from the repo root, bring up the whole stack:
docker compose up --build auth-service
```

The image is multi-stage (`maven:3.9-eclipse-temurin-17` → `eclipse-temurin:17-jre`).

## Notes / caveats

- **OAuth2 is stubbed.** The `oauth_accounts` table and entity exist, but the
  `google`/`linkedin` handshake is not implemented; both OAuth endpoints return
  `501`.
- **Security is intentionally permissive.** This service sits *behind* the
  gateway, which enforces the perimeter. Spring Security here permits all
  endpoints, stays stateless, and only provides BCrypt password hashing; the
  ADMIN check on role management is enforced in the controller against
  `X-User-Roles` / token roles.
- Access-token denylisting by `jti` (Redis) is modeled in the schema but lives
  in the gateway / a future iteration; logout revokes the refresh token here.
- Not compiled in this environment (no Maven/Docker available at authoring time).
