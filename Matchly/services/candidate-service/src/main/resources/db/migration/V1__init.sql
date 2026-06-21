-- Candidate Service schema (DATA_MODELS §2) for candidate_db.
-- IDs are UUID. All tables carry created_at / updated_at (UTC).
-- Raw resume files live in S3 (here: local disk; the relative path is the s3_key).
-- The embedding vector lives in the AI service (referenced by embedding_ref).

CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()

-- ---------------------------------------------------------------------------
-- candidates
-- ---------------------------------------------------------------------------
CREATE TABLE candidates (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL,                       -- references Auth user (logical, not FK)
    headline   TEXT,
    location   TEXT,
    phone      TEXT,
    links      JSONB,                               -- {github, linkedin, portfolio}
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_candidates_user_id ON candidates (user_id);

-- ---------------------------------------------------------------------------
-- resumes
-- ---------------------------------------------------------------------------
CREATE TABLE resumes (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id  UUID NOT NULL REFERENCES candidates (id) ON DELETE CASCADE,
    s3_key        TEXT NOT NULL,
    file_name     TEXT NOT NULL,
    mime_type     TEXT NOT NULL,                    -- application/pdf | ...docx
    parse_status  TEXT NOT NULL DEFAULT 'PENDING',  -- PENDING | PARSED | FAILED
    embedding_ref UUID,                             -- id of vector row in AI service
    is_primary    BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_resumes_candidate_id ON resumes (candidate_id);

-- ---------------------------------------------------------------------------
-- parsed_resumes (structured result of parsing; populated when PARSED)
-- ---------------------------------------------------------------------------
CREATE TABLE parsed_resumes (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id     UUID NOT NULL UNIQUE REFERENCES resumes (id) ON DELETE CASCADE,
    full_name     TEXT,
    email         TEXT,
    phone         TEXT,
    total_exp_yrs NUMERIC(4,1),
    raw_text      TEXT,                             -- normalized extracted text
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- candidate_skills
-- ---------------------------------------------------------------------------
CREATE TABLE candidate_skills (
    candidate_id UUID NOT NULL REFERENCES candidates (id) ON DELETE CASCADE,
    skill_id     UUID NOT NULL DEFAULT gen_random_uuid(),  -- canonical id from skill taxonomy
    skill_name   TEXT NOT NULL,
    source       TEXT,                                     -- RESUME | SELF_DECLARED
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (candidate_id, skill_id)
);

CREATE INDEX idx_candidate_skills_candidate_id ON candidate_skills (candidate_id);

-- ---------------------------------------------------------------------------
-- experiences
-- ---------------------------------------------------------------------------
CREATE TABLE experiences (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id   UUID NOT NULL REFERENCES resumes (id) ON DELETE CASCADE,
    company     TEXT,
    title       TEXT,
    start_date  DATE,
    end_date    DATE,                               -- end null = current
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_experiences_resume_id ON experiences (resume_id);

-- ---------------------------------------------------------------------------
-- education
-- ---------------------------------------------------------------------------
CREATE TABLE education (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id   UUID NOT NULL REFERENCES resumes (id) ON DELETE CASCADE,
    institution TEXT,
    degree      TEXT,
    field       TEXT,
    level       TEXT,                               -- HS | DIPLOMA | BACHELOR | MASTER | PHD
    start_year  INT,
    end_year    INT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_education_resume_id ON education (resume_id);

-- ---------------------------------------------------------------------------
-- projects
-- ---------------------------------------------------------------------------
CREATE TABLE projects (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id   UUID NOT NULL REFERENCES resumes (id) ON DELETE CASCADE,
    name        TEXT,
    description TEXT,
    tech_stack  TEXT[],
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_projects_resume_id ON projects (resume_id);

-- ---------------------------------------------------------------------------
-- certifications
-- ---------------------------------------------------------------------------
CREATE TABLE certifications (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id  UUID NOT NULL REFERENCES resumes (id) ON DELETE CASCADE,
    name       TEXT,
    issuer     TEXT,
    issued_at  DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_certifications_resume_id ON certifications (resume_id);
