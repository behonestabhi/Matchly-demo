# Matchly — AI-Powered Recruitment & Candidate Intelligence Platform

Matchly automates resume screening, candidate–job matching, skill-gap analysis,
interview-question generation, applicant tracking, and recruiter analytics using
NLP, embeddings, and LLMs.

This repository contains the **full design set and a complete, runnable
implementation** of the platform: 8 Spring Boot apps, a Python AI service, a
React frontend, plus Docker/K8s/Terraform/CI.

> ⚠️ **Read [STATUS.md](STATUS.md) first.** It states exactly what is
> implemented, what is verified, and what is scaffolded. In short: the **AI
> service** and **frontend** are built and verified locally; the **Java
> services** are written to compile and run but were generated without a local
> Maven/Docker toolchain, so they build via the Dockerized Maven stage and have
> not been integration-tested end-to-end yet.

## Running it locally

```bash
cp .env.example .env          # set JWT_SECRET (and optionally an LLM key)
docker compose up --build     # builds every image (Maven runs inside Docker)
```

Then: frontend → http://localhost:8088 · gateway → http://localhost:8080 ·
AI docs → http://localhost:8000/docs · each service exposes `/swagger-ui.html`
and `/actuator/health`. See the [Makefile](Makefile) for shortcuts
(`make up`, `make ai-test`, `make fe-dev`).

## Design documents

| Document | What it covers |
|---|---|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | System architecture, service decomposition, event-driven design, AI/ML pipelines, cross-cutting concerns, deployment topology, and key technical decisions (ADRs). |
| [docs/DATA_MODELS.md](docs/DATA_MODELS.md) | Database-per-service schemas, the vector store, Kafka event contracts, and Redis key design. |
| [docs/API_CONTRACTS.md](docs/API_CONTRACTS.md) | Gateway routing and the REST contracts for every service, plus conventions (errors, pagination, versioning, idempotency). |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Phased milestones (M0–M7), exit criteria, dependencies, risk register, and how each phase maps to the PRD's success metrics. |

## Architecture at a glance

```
React + TypeScript (SPA)
        │  HTTPS
        ▼
API Gateway (Spring Cloud Gateway)  ──auth/JWT validation, routing, rate limiting
        │
        ├─► Auth Service ────────┐
        ├─► Candidate Service     │  Spring Boot microservices
        ├─► Job Service           │  (database-per-service, PostgreSQL)
        ├─► Application/ATS Service│
        ├─► Matching Service      │
        ├─► Interview Service      │
        └─► Analytics Service ────┘
                │   ▲
        Kafka event bus (choreography + async work)
                │   ▲
                ▼   │  (sync REST for low-latency reads; async events for heavy work)
        AI Service (Python / FastAPI)
        parsing · embeddings · matching · skill-gap · LLM interview gen · success predictor
                │
        PostgreSQL (+ pgvector) · Redis · S3 · Model registry · LLM (Gemini/OpenAI)
```

## Two deviations from the PRD (deliberate)

1. **Added an Application/ATS Service.** The PRD lists six services but its ATS
   feature (applications, pipeline stages, recruiter comments, activity logs) is
   an aggregate that links candidates and jobs and doesn't belong cleanly inside
   either. It gets its own service. See ADR-002.
2. **Vector store = pgvector, not a new database.** The PRD names PostgreSQL +
   Redis and requires semantic matching. Rather than introduce a dedicated vector
   DB, we use the `pgvector` extension on the AI service's PostgreSQL instance.
   See ADR-003.

## Repository structure (target)

```
matchly/
├── README.md
├── docs/                       # this design set
├── infra/
│   ├── docker/                 # docker-compose for local dev (postgres, redis, kafka, services)
│   ├── k8s/                    # kustomize base + dev/staging/prod overlays
│   └── terraform/              # AWS: EKS, RDS, MSK, ElastiCache, ECR, ALB, S3
├── platform/
│   └── api-gateway/            # Spring Cloud Gateway
├── services/
│   ├── auth-service/           # Spring Boot — identity, JWT/OAuth2, RBAC
│   ├── candidate-service/      # Spring Boot — profiles, resumes, parsed data
│   ├── job-service/            # Spring Boot — job postings
│   ├── application-service/    # Spring Boot — ATS pipeline, status, comments, activity log
│   ├── matching-service/       # Spring Boot — orchestrates AI matching, persists scores/ranks
│   ├── interview-service/      # Spring Boot — interview-question generation
│   └── analytics-service/      # Spring Boot — read models, dashboards, reports
├── ai-service/                 # Python / FastAPI — parsing, embeddings, matching, LLM, ML
│   ├── app/                    # API layer
│   ├── pipelines/              # parsing, embedding, matching, skill-gap pipelines
│   ├── ml/                     # success-predictor training + serving
│   └── prompts/                # versioned LLM prompt templates
├── frontend/                   # React + TypeScript + Tailwind
├── libs/
│   ├── java-common/            # shared DTOs, security, event contracts
│   └── schemas/                # Avro/JSON event schemas (schema registry source of truth)
└── .github/workflows/          # CI/CD (GitHub Actions)
```
