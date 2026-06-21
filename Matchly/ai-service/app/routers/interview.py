"""Interview-question generation endpoint."""

from __future__ import annotations

from fastapi import APIRouter

from app.config import get_settings
from app.schemas import InterviewRequest, InterviewResponse
from pipelines.interview import generate_interview

router = APIRouter(prefix="/internal", tags=["interview"])


@router.post("/interview/generate", response_model=InterviewResponse)
def interview_generate(req: InterviewRequest) -> InterviewResponse:
    """Generate categorized interview questions (LLM if configured, else template)."""
    return generate_interview(
        resume_text=req.resumeText,
        job_description=req.jobDescription,
        skills=req.skills,
        counts=req.counts,
        settings=get_settings(),
    )
