-- Matching Service schema (DATA_MODELS §5) for matching_db.
-- IDs are UUID. Timestamps are UTC.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()

-- ---------------------------------------------------------------------------
-- match_scores
--   The explainable weighted blend persisted per (candidate, job, model).
--   Rankings are derived by querying this table ordered by final_score.
-- ---------------------------------------------------------------------------
CREATE TABLE match_scores (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id   UUID NOT NULL,
    job_id         UUID NOT NULL,
    final_score    NUMERIC(4,3) NOT NULL,        -- 0.000 .. 1.000
    semantic_score NUMERIC(4,3),
    skill_overlap  NUMERIC(4,3),
    experience_fit NUMERIC(4,3),
    education_fit  NUMERIC(4,3),
    model_version  TEXT NOT NULL,
    scored_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (candidate_id, job_id, model_version)
);

CREATE INDEX idx_match_scores_job_score      ON match_scores (job_id, final_score DESC);
CREATE INDEX idx_match_scores_candidate_score ON match_scores (candidate_id, final_score DESC);

-- ---------------------------------------------------------------------------
-- skill_gaps
-- ---------------------------------------------------------------------------
CREATE TABLE skill_gaps (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id   UUID NOT NULL,
    job_id         UUID NOT NULL,
    missing_skills JSONB NOT NULL,               -- [{skill, importance}]
    roadmap        JSONB,                        -- LLM-generated plan (nullable)
    generated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (candidate_id, job_id)
);

CREATE INDEX idx_skill_gaps_candidate_job ON skill_gaps (candidate_id, job_id);

-- ---------------------------------------------------------------------------
-- success_predictions
-- ---------------------------------------------------------------------------
CREATE TABLE success_predictions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id  UUID NOT NULL,
    job_id        UUID NOT NULL,
    probability   NUMERIC(4,3) NOT NULL,
    model_version TEXT NOT NULL,
    features      JSONB,                          -- snapshot for explainability/audit
    predicted_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_success_predictions_candidate_job ON success_predictions (candidate_id, job_id);
