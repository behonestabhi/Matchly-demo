# Matchly AI Service

Stateless Python/FastAPI compute service for the Matchly recruitment platform:
resume parsing, text embeddings, semantic matching, skill-gap analysis,
interview-question generation, and success prediction.

- **HTTP-only** (no Kafka). It is stateless compute called over REST by the Java
  microservices (Matching, Interview). Listens on **port 8000**.
- **Fail-soft by design.** Heavy/optional dependencies (sentence-transformers,
  spaCy, PostgreSQL/pgvector, an LLM API) are used when available and degrade to
  light, deterministic fallbacks otherwise, so the core logic always works.

## Endpoints

All under `/internal` (the service is not exposed through the API gateway).

| Method | Path | Description |
|---|---|---|
| GET  | `/internal/health` | Liveness: `{status, version}`. |
| GET  | `/internal/models` | Active model/component versions. |
| POST | `/internal/parse` | Resume text/base64 file → structured JSON. |
| POST | `/internal/embed` | Embed text → `{embeddingRef, dim, model}` (pgvector or in-memory). |
| POST | `/internal/score` | Explainable weighted match score + breakdown. |
| POST | `/internal/skill-gap` | Missing/matched skills + optional roadmap. |
| POST | `/internal/interview/generate` | Categorized interview questions. |
| POST | `/internal/predict` | Success probability (ML). |

Interactive API docs: `http://localhost:8000/docs`.

### Matching score (ARCHITECTURE §6.1)

```
final = 0.45 * semantic       # cosine(resume_emb, jd_emb), 0..1
      + 0.35 * skillOverlap   # taxonomy-normalized required-skill coverage
      + 0.15 * experienceFit  # candidate years vs required band
      + 0.05 * educationFit   # degree-level match
```

Skills are normalized through a ~80-entry skill taxonomy so synonyms
("JS" ≡ "JavaScript", "k8s" ≡ "Kubernetes") do not register as false gaps.

## Configuration

All config is environment-driven (see `.env.example`); every value has a safe
offline default. Notable variables:

- `EMBEDDING_MODEL`, `EMBEDDING_DIM` — embedding model id and fallback dimension.
- `DATABASE_URL` — when set, embeddings persist to PostgreSQL + pgvector;
  otherwise an in-memory store is used.
- `LLM_PROVIDER` (`template` | `openai` | `gemini`), `OPENAI_API_KEY`,
  `GEMINI_API_KEY` — enable LLM-backed interview/roadmap generation; defaults to
  the offline `template` generator.

## Run locally

```bash
cd ai-service
python -m pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Optional upgrades (not in `requirements.txt`, imported lazily):

```bash
pip install sentence-transformers       # better embeddings
pip install "psycopg[binary]"           # pgvector persistence (needs DATABASE_URL)
pip install spacy && python -m spacy download en_core_web_sm   # better name NER
pip install pymupdf                      # faster PDF extraction (pdfplumber otherwise)
```

## Tests

```bash
cd ai-service
python -m pytest -q
```

Tests cover cosine similarity, the weighted blend, skill normalization,
skill-gap (incl. template roadmap), resume parsing from inline text, and the
interview template generator. They run with only the light dependencies.

## Docker

```bash
docker build -t matchly-ai-service ./ai-service
docker run -p 8000:8000 matchly-ai-service
```

The service is wired into the repo's root `docker-compose.yml` as `ai-service`
(port 8000, `DATABASE_URL` → `ai_db`).

## Layout

```
ai-service/
  app/          FastAPI app, config, schemas, routers
  pipelines/    parsing, embeddings, matching, skill_taxonomy, skillgap, interview
  ml/           predictor (sklearn train-on-synthetic + heuristic fallback)
  tests/        matching, skillgap, parsing, interview
```
