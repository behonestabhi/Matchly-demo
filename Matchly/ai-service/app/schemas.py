"""Pydantic request/response models for the AI service API.

These mirror the contracts in ``docs/API_CONTRACTS.md`` ("AI Service (internal)")
and the field shapes in ``docs/DATA_MODELS.md``. All models use pydantic v2.
"""

from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, Field

OwnerType = Literal["RESUME", "JOB"]
Difficulty = Literal["EASY", "MEDIUM", "HARD"]
QuestionCategory = Literal["TECHNICAL", "BEHAVIORAL", "SCENARIO"]
EducationLevel = Literal["HS", "DIPLOMA", "BACHELOR", "MASTER", "PHD"]


# --------------------------------------------------------------------------- #
# Health / models
# --------------------------------------------------------------------------- #
class HealthResponse(BaseModel):
    """Liveness response."""

    status: str = "ok"
    version: str


class ModelInfo(BaseModel):
    """Description of one active model/component version."""

    name: str
    version: str
    backend: str


class ModelsResponse(BaseModel):
    """Active model versions across pipelines."""

    embedding: ModelInfo
    matching: ModelInfo
    predictor: ModelInfo
    interview: ModelInfo


# --------------------------------------------------------------------------- #
# Parsing
# --------------------------------------------------------------------------- #
class ParseRequest(BaseModel):
    """Resume parse input: raw text OR a base64-encoded file."""

    text: str | None = Field(default=None, description="Raw resume text.")
    fileBase64: str | None = Field(
        default=None, description="Base64-encoded PDF or DOCX file."
    )
    mimeType: str | None = Field(
        default=None,
        description="MIME type of the file (application/pdf, ...wordprocessingml).",
    )


class Experience(BaseModel):
    """A single work-experience entry."""

    company: str | None = None
    title: str | None = None
    startDate: str | None = None
    endDate: str | None = None
    description: str | None = None


class Education(BaseModel):
    """A single education entry."""

    institution: str | None = None
    degree: str | None = None
    field: str | None = None
    level: str | None = None
    startYear: int | None = None
    endYear: int | None = None


class Project(BaseModel):
    """A single project entry."""

    name: str | None = None
    description: str | None = None
    techStack: list[str] = Field(default_factory=list)


class Certification(BaseModel):
    """A single certification entry."""

    name: str | None = None
    issuer: str | None = None
    issuedAt: str | None = None


class ParseResponse(BaseModel):
    """Structured parse result."""

    fullName: str | None = None
    email: str | None = None
    phone: str | None = None
    totalExpYrs: float | None = None
    skills: list[str] = Field(default_factory=list)
    experiences: list[Experience] = Field(default_factory=list)
    education: list[Education] = Field(default_factory=list)
    projects: list[Project] = Field(default_factory=list)
    certifications: list[Certification] = Field(default_factory=list)
    rawText: str = ""


# --------------------------------------------------------------------------- #
# Embeddings
# --------------------------------------------------------------------------- #
class EmbedRequest(BaseModel):
    """Embed input: owner identity + text to embed."""

    ownerType: OwnerType
    ownerId: str
    text: str


class EmbedResponse(BaseModel):
    """Embed result: a reference to the persisted vector."""

    embeddingRef: str
    dim: int
    model: str


# --------------------------------------------------------------------------- #
# Scoring
# --------------------------------------------------------------------------- #
class ScoreRequest(BaseModel):
    """Match-score input.

    Either provide texts (``candidateText``/``jobText``) for on-the-fly
    embedding, or pre-computed references (``candidateEmbeddingRef``/
    ``jobEmbeddingRef``). Skills and experience/education hints drive the
    explainable components.
    """

    candidateText: str | None = None
    jobText: str | None = None
    candidateEmbeddingRef: str | None = None
    jobEmbeddingRef: str | None = None
    candidateSkills: list[str] = Field(default_factory=list)
    jobRequiredSkills: list[str] = Field(default_factory=list)
    expYrs: float | None = None
    minExpYrs: float | None = None
    maxExpYrs: float | None = None
    candidateEduLevel: str | None = None
    jobEduLevel: str | None = None


class ScoreResponse(BaseModel):
    """Match-score breakdown (ARCHITECTURE §6.1)."""

    finalScore: float
    semantic: float
    skillOverlap: float
    experienceFit: float
    educationFit: float
    matchedSkills: list[str] = Field(default_factory=list)
    missingSkills: list[str] = Field(default_factory=list)
    modelVersion: str


# --------------------------------------------------------------------------- #
# Skill gap
# --------------------------------------------------------------------------- #
class SkillGapRequest(BaseModel):
    """Skill-gap input."""

    candidateSkills: list[str] = Field(default_factory=list)
    jobRequiredSkills: list[str] = Field(default_factory=list)
    generateRoadmap: bool = False


class RoadmapStep(BaseModel):
    """One step in a learning roadmap."""

    skill: str
    summary: str
    resources: list[str] = Field(default_factory=list)
    estimatedWeeks: int


class SkillGapResponse(BaseModel):
    """Skill-gap result with an optional learning roadmap."""

    missingSkills: list[str] = Field(default_factory=list)
    matchedSkills: list[str] = Field(default_factory=list)
    roadmap: list[RoadmapStep] | None = None


# --------------------------------------------------------------------------- #
# Interview generation
# --------------------------------------------------------------------------- #
class QuestionCounts(BaseModel):
    """Requested number of questions per category."""

    technical: int = 3
    behavioral: int = 2
    scenario: int = 2


class InterviewRequest(BaseModel):
    """Interview-question generation input."""

    resumeText: str | None = None
    jobDescription: str | None = None
    skills: list[str] = Field(default_factory=list)
    counts: QuestionCounts = Field(default_factory=QuestionCounts)


class InterviewQuestion(BaseModel):
    """A single generated interview question."""

    category: QuestionCategory
    question: str
    suggestedAnswer: str
    difficulty: Difficulty


class InterviewResponse(BaseModel):
    """Generated, categorized interview questions."""

    questions: list[InterviewQuestion] = Field(default_factory=list)
    llmModel: str


# --------------------------------------------------------------------------- #
# Prediction
# --------------------------------------------------------------------------- #
class PredictFeatures(BaseModel):
    """Feature vector for the success predictor (ARCHITECTURE §6.2)."""

    yearsExperience: float = 0.0
    skillMatchCount: int = 0
    certificationCount: int = 0
    educationLevel: int = 0
    projectCount: int = 0
    semanticScore: float = 0.0


class PredictRequest(BaseModel):
    """Success-prediction input."""

    features: PredictFeatures


class PredictResponse(BaseModel):
    """Success-prediction output."""

    probability: float
    modelVersion: str
