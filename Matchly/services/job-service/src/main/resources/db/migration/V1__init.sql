-- Job Service schema (DATA_MODELS §3) for job_db.
-- IDs are UUID. All tables carry created_at / updated_at (UTC).
-- The embedding vector lives in the AI service (referenced by embedding_ref).

CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()

-- ---------------------------------------------------------------------------
-- jobs
-- ---------------------------------------------------------------------------
CREATE TABLE jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recruiter_id    UUID NOT NULL,                     -- Auth user (logical, not FK)
    title           TEXT NOT NULL,
    description     TEXT NOT NULL,                     -- full JD text
    location        TEXT,
    employment_type TEXT,                              -- FULL_TIME | CONTRACT | INTERN
    min_exp_yrs     NUMERIC(4,1),
    max_exp_yrs     NUMERIC(4,1),
    education_level TEXT,                              -- HS | DIPLOMA | BACHELOR | MASTER | PHD
    status          TEXT NOT NULL DEFAULT 'DRAFT',     -- DRAFT | OPEN | CLOSED
    embedding_ref   UUID,                              -- AI service vector id for the JD
    published_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_jobs_status       ON jobs (status);
CREATE INDEX idx_jobs_recruiter_id ON jobs (recruiter_id);
CREATE INDEX idx_jobs_location     ON jobs (location);

-- ---------------------------------------------------------------------------
-- job_required_skills
-- ---------------------------------------------------------------------------
CREATE TABLE job_required_skills (
    job_id     UUID NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    skill_id   UUID NOT NULL DEFAULT gen_random_uuid(),  -- canonical taxonomy id
    skill_name TEXT NOT NULL,
    weight     NUMERIC(3,2) DEFAULT 1.0,                 -- importance, used by matching
    required   BOOLEAN DEFAULT true,                     -- must-have vs nice-to-have
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (job_id, skill_id)
);

CREATE INDEX idx_job_required_skills_job_id     ON job_required_skills (job_id);
CREATE INDEX idx_job_required_skills_skill_name ON job_required_skills (lower(skill_name));
