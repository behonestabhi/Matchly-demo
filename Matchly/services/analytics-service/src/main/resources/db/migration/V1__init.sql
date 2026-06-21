-- Analytics Service read models (DATA_MODELS §7) for analytics_db.
-- Denormalized, event-sourced projections; never the source of truth.
-- IDs are UUID. Timestamps are UTC.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()

-- ---------------------------------------------------------------------------
-- funnel_daily — per (job, day) stage counts.
-- ---------------------------------------------------------------------------
CREATE TABLE funnel_daily (
    job_id      UUID NOT NULL,
    day         DATE NOT NULL,
    applied     INT NOT NULL DEFAULT 0,
    screening   INT NOT NULL DEFAULT 0,
    shortlisted INT NOT NULL DEFAULT 0,
    interview   INT NOT NULL DEFAULT 0,
    offer       INT NOT NULL DEFAULT 0,
    rejected    INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (job_id, day)
);

-- ---------------------------------------------------------------------------
-- time_to_hire — one row per application; days_to_hire filled on OFFER.
-- ---------------------------------------------------------------------------
CREATE TABLE time_to_hire (
    application_id UUID PRIMARY KEY,
    job_id         UUID NOT NULL,
    applied_at     TIMESTAMPTZ,
    hired_at       TIMESTAMPTZ,
    days_to_hire   NUMERIC(6,2),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_time_to_hire_job_id    ON time_to_hire (job_id);
CREATE INDEX idx_time_to_hire_hired_at  ON time_to_hire (hired_at);

-- ---------------------------------------------------------------------------
-- skill_demand — per (day, skill) job count from JobPosted requiredSkills.
-- skill_id may be a deterministic surrogate when no canonical id is supplied.
-- ---------------------------------------------------------------------------
CREATE TABLE skill_demand (
    day        DATE NOT NULL,
    skill_id   UUID NOT NULL,
    skill_name TEXT NOT NULL,
    job_count  INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (day, skill_id)
);

CREATE INDEX idx_skill_demand_day ON skill_demand (day);

-- ---------------------------------------------------------------------------
-- conversion_metrics — per (job, day) conversion rates.
-- ---------------------------------------------------------------------------
CREATE TABLE conversion_metrics (
    job_id         UUID NOT NULL,
    day            DATE NOT NULL,
    applications   INT NOT NULL DEFAULT 0,
    shortlist_rate NUMERIC(4,3) NOT NULL DEFAULT 0,
    offer_rate     NUMERIC(4,3) NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (job_id, day)
);

-- ---------------------------------------------------------------------------
-- processed_events — simple consumer-side idempotency (dedup by eventId).
-- NOTE (simplification): canonical design dedups in Redis (idemp:{consumer}:{eventId});
-- here we keep a small DB table for a self-contained, replay-safe consumer.
-- ---------------------------------------------------------------------------
CREATE TABLE processed_events (
    event_id     UUID PRIMARY KEY,
    event_type   TEXT,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
