-- Application / ATS Service schema (DATA_MODELS §4) for application_db.
-- IDs are UUID. All tables carry created_at / updated_at (UTC) where relevant.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()

-- ---------------------------------------------------------------------------
-- applications
--   The join of a candidate and a job, plus its own lifecycle (current stage).
-- ---------------------------------------------------------------------------
CREATE TABLE applications (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id  UUID NOT NULL,
    job_id        UUID NOT NULL,
    current_stage TEXT NOT NULL DEFAULT 'APPLIED',   -- APPLIED|SCREENING|SHORTLISTED|INTERVIEW|OFFER|REJECTED
    source        TEXT,                              -- DIRECT | REFERRAL | IMPORT
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (candidate_id, job_id)
);

CREATE INDEX idx_applications_candidate_id ON applications (candidate_id);
CREATE INDEX idx_applications_job_id       ON applications (job_id);

-- ---------------------------------------------------------------------------
-- stage_transitions
--   Every move through the pipeline (from_stage null on the first APPLIED).
-- ---------------------------------------------------------------------------
CREATE TABLE stage_transitions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    from_stage     TEXT,                             -- null on first
    to_stage       TEXT NOT NULL,
    actor_id       UUID,                             -- who moved it (recruiter / candidate on apply)
    reason         TEXT,
    occurred_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_stage_transitions_application_id ON stage_transitions (application_id);

-- ---------------------------------------------------------------------------
-- comments
-- ---------------------------------------------------------------------------
CREATE TABLE comments (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    author_id      UUID NOT NULL,
    body           TEXT NOT NULL,
    visibility     TEXT NOT NULL DEFAULT 'INTERNAL', -- INTERNAL | SHARED
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_comments_application_id ON comments (application_id);

-- ---------------------------------------------------------------------------
-- activity_log (append-only audit trail)
-- ---------------------------------------------------------------------------
CREATE TABLE activity_log (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID,
    actor_id       UUID,
    action         TEXT NOT NULL,                    -- APPLIED | STAGE_CHANGED | COMMENTED | SCORE_UPDATED | ...
    payload        JSONB,
    occurred_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_activity_log_application_id ON activity_log (application_id);
