"""Skill-gap endpoint."""

from __future__ import annotations

from fastapi import APIRouter

from app.config import get_settings
from app.schemas import SkillGapRequest, SkillGapResponse
from pipelines.skillgap import compute_skill_gap

router = APIRouter(prefix="/internal", tags=["skill-gap"])


@router.post("/skill-gap", response_model=SkillGapResponse)
def skill_gap(req: SkillGapRequest) -> SkillGapResponse:
    """Return missing/matched skills and an optional learning roadmap."""
    return compute_skill_gap(
        req.candidateSkills,
        req.jobRequiredSkills,
        generate_roadmap=req.generateRoadmap,
        settings=get_settings(),
    )
