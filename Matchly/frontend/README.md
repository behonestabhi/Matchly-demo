# Matchly — Frontend

React + TypeScript + Vite + Tailwind CSS single-page app for the Matchly AI
recruitment platform. It talks to the API gateway documented in
[`../docs/API_CONTRACTS.md`](../docs/API_CONTRACTS.md).

## Stack

- Vite + React 18 + TypeScript
- Tailwind CSS v3
- react-router-dom v6
- A small typed `fetch` wrapper (`src/api/client.ts`) — no extra HTTP deps.

## Configuration

All API calls are prefixed with `VITE_API_BASE`.

```bash
cp .env.example .env
# VITE_API_BASE=http://localhost:8080/api/v1   (default)
```

The JWT access token is stored in `localStorage` (`matchly.accessToken`) and
attached as `Authorization: Bearer <token>`. On any `401` the token is cleared
and the user is redirected to `/login`.

## Develop

```bash
npm install
npm run dev      # http://localhost:5173
```

## Build

```bash
npm run build    # type-checks (tsc -b) then bundles to dist/
npm run preview  # serve the production build locally
```

## Docker

The image builds the bundle and serves it with nginx (SPA fallback). The API
base is baked in at build time via the `VITE_API_BASE` build arg.

```bash
docker build --build-arg VITE_API_BASE=http://localhost:8080/api/v1 -t matchly-frontend .
docker run -p 8088:80 matchly-frontend
```

Or via the repo root compose file (frontend is published on `:8088`):

```bash
docker compose up --build frontend
```

## Pages & roles

RBAC roles: CANDIDATE, RECRUITER, HIRING_MANAGER, ADMIN.

| Route | Roles | Purpose |
|---|---|---|
| `/login`, `/register` | public | Auth |
| `/jobs`, `/jobs/:id` | any | List/search jobs, job detail, candidate apply |
| `/candidate` | CANDIDATE | Profile, resume upload + parse polling, applications, skill gap |
| `/recruiter` | RECRUITER / HIRING_MANAGER / ADMIN | Create & publish jobs |
| `/recruiter/jobs/:jobId/candidates` | recruiter roles | Ranked candidates + score breakdown |
| `/recruiter/jobs/:jobId/pipeline` | recruiter roles | Kanban pipeline board |
| `/recruiter/jobs/:jobId/interview` | recruiter roles | Generate interview questions (async poll) |
| `/analytics` | recruiter roles | Overview cards, funnel, in-demand skills |

## Project layout

```
src/
  api/client.ts        fetch wrapper + typed endpoint functions
  auth/                AuthContext, ProtectedRoute
  types/index.ts       DTO types mirroring API_CONTRACTS
  components/          Layout, Nav, Card, ScoreBar, KanbanColumn, …
  hooks/useAsync.ts    load/error/data helper
  pages/               Login, Register, Jobs, JobDetail, dashboards, …
```
