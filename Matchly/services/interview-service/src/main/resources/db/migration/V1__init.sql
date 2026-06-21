-- Interview Service schema (DATA_MODELS §6) for interview_db.
-- IDs are UUID. All tables carry created_at / updated_at (UTC).

CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()

-- ---------------------------------------------------------------------------
-- question_sets
-- ---------------------------------------------------------------------------
CREATE TABLE question_sets (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id   UUID NOT NULL,
    job_id         UUID NOT NULL,
    status         TEXT NOT NULL DEFAULT 'GENERATING',   -- GENERATING | READY | FAILED
    llm_model      TEXT,                                 -- e.g. gemini-1.5 / gpt-4o
    prompt_version TEXT,
    error_detail   TEXT,                                 -- populated when status = FAILED
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_question_sets_candidate_id ON question_sets (candidate_id);
CREATE INDEX idx_question_sets_job_id       ON question_sets (job_id);
CREATE INDEX idx_question_sets_pair         ON question_sets (candidate_id, job_id);

-- ---------------------------------------------------------------------------
-- questions
-- ---------------------------------------------------------------------------
CREATE TABLE questions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_set_id  UUID NOT NULL REFERENCES question_sets (id) ON DELETE CASCADE,
    category         TEXT NOT NULL,                      -- TECHNICAL | BEHAVIORAL | SCENARIO
    question         TEXT NOT NULL,
    suggested_answer TEXT,                               -- rubric / model answer for the recruiter
    difficulty       TEXT,                               -- EASY | MEDIUM | HARD
    ordinal          INT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_questions_question_set_id ON questions (question_set_id);
