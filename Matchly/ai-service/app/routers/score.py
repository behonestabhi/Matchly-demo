"""Match-scoring endpoint (ARCHITECTURE §6.1)."""

from __future__ import annotations

from fastapi import APIRouter, Request

from app.config import get_settings
from app.schemas import ScoreRequest, ScoreResponse
from pipelines.matching import (
    cosine_similarity,
    education_fit,
    experience_fit,
    round3,
    skill_overlap,
    weighted_score,
)

router = APIRouter(prefix="/internal", tags=["score"])


@router.post("/score", response_model=ScoreResponse)
def score(req: ScoreRequest, request: Request) -> ScoreResponse:
    """Compute the explainable weighted match score and its breakdown.

    Semantic similarity is computed from embedding refs if both are provided and
    resolvable; otherwise from on-the-fly embeddings of the supplied texts. If
    neither is available, the semantic component is treated as neutral (0.5) so
    the structured components still drive a meaningful score.
    """
    settings = get_settings()
    engine = request.app.state.embedding_engine

    semantic = _resolve_semantic(req, engine)

    skills = skill_overlap(req.candidateSkills, req.jobRequiredSkills)
    exp = experience_fit(req.expYrs, req.minExpYrs, req.maxExpYrs)
    edu = education_fit(req.candidateEduLevel, req.jobEduLevel)

    final = weighted_score(
        semantic,
        skills.overlap,
        exp,
        edu,
        w_sem=settings.weight_semantic,
        w_skill=settings.weight_skill,
        w_exp=settings.weight_experience,
        w_edu=settings.weight_education,
    )

    return ScoreResponse(
        finalScore=round3(final),
        semantic=round3(semantic),
        skillOverlap=round3(skills.overlap),
        experienceFit=round3(exp),
        educationFit=round3(edu),
        matchedSkills=skills.matched,
        missingSkills=skills.missing,
        modelVersion=settings.match_model_version,
    )


def _resolve_semantic(req: ScoreRequest, engine) -> float:
    """Resolve the semantic-similarity component from refs or text, else neutral."""
    cand_vec = None
    job_vec = None

    if req.candidateEmbeddingRef:
        cand_vec = engine.get_vector(req.candidateEmbeddingRef)
    if req.jobEmbeddingRef:
        job_vec = engine.get_vector(req.jobEmbeddingRef)

    if cand_vec is None and req.candidateText:
        cand_vec = engine.encode(req.candidateText)
    if job_vec is None and req.jobText:
        job_vec = engine.encode(req.jobText)

    if cand_vec is None or job_vec is None:
        # Neutral when we cannot establish semantic similarity.
        return 0.5
    return cosine_similarity(cand_vec, job_vec)
