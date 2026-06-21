"""Skill-gap analysis and learning-roadmap generation.

Computes the set of required job skills the candidate is missing (taxonomy-
normalized) and, optionally, a learning roadmap. The roadmap uses the configured
LLM when available, otherwise a deterministic template per missing skill.
"""

from __future__ import annotations

import logging

from app.config import Settings
from app.schemas import RoadmapStep, SkillGapResponse

from .matching import skill_overlap

logger = logging.getLogger(__name__)

# Per-skill curated learning hints used by the template roadmap. Skills not
# listed get a generic but still useful default.
_RESOURCE_HINTS: dict[str, list[str]] = {
    "Kubernetes": ["Official Kubernetes docs (kubernetes.io)", "'Kubernetes Up & Running'"],
    "Docker": ["Docker getting-started guide", "Play with Docker labs"],
    "Apache Kafka": ["Confluent Kafka 101", "'Kafka: The Definitive Guide'"],
    "React": ["react.dev learn track", "Build a small SPA project"],
    "Spring Boot": ["Spring Boot guides (spring.io)", "Baeldung Spring tutorials"],
    "AWS": ["AWS Cloud Practitioner path", "AWS hands-on tutorials"],
    "Python": ["Official Python tutorial", "'Automate the Boring Stuff'"],
    "Machine Learning": ["Andrew Ng ML course", "scikit-learn user guide"],
    "PostgreSQL": ["PostgreSQL tutorial (postgresqltutorial.com)", "Use it in a project"],
    "Terraform": ["HashiCorp Terraform tutorials", "Provision a small stack"],
}

# Rough effort estimate (weeks) by skill complexity tier.
_DEFAULT_WEEKS = 4


def compute_skill_gap(
    candidate_skills: list[str],
    job_required_skills: list[str],
    *,
    generate_roadmap: bool,
    settings: Settings,
) -> SkillGapResponse:
    """Return missing/matched skills and an optional learning roadmap."""
    match = skill_overlap(candidate_skills, job_required_skills)
    roadmap = None
    if generate_roadmap and match.missing:
        roadmap = _generate_roadmap(match.missing, settings)
    return SkillGapResponse(
        missingSkills=match.missing,
        matchedSkills=match.matched,
        roadmap=roadmap,
    )


def _generate_roadmap(missing_skills: list[str], settings: Settings) -> list[RoadmapStep]:
    """Generate a roadmap via LLM when configured, else a template fallback."""
    provider = (settings.llm_provider or "template").lower()
    if provider in ("openai", "gemini"):
        try:
            from .interview import _llm_complete  # reuse the shared LLM client

            llm_roadmap = _roadmap_via_llm(missing_skills, settings, _llm_complete)
            if llm_roadmap:
                return llm_roadmap
        except Exception as exc:  # noqa: BLE001 - fail soft to template
            logger.warning("LLM roadmap generation failed (%s); using template.", exc)
    return _template_roadmap(missing_skills)


def _template_roadmap(missing_skills: list[str]) -> list[RoadmapStep]:
    """Produce a deterministic, sensible roadmap from templates."""
    steps: list[RoadmapStep] = []
    for index, skill in enumerate(missing_skills, start=1):
        resources = _RESOURCE_HINTS.get(
            skill,
            [
                f"Official {skill} documentation",
                f"A hands-on {skill} tutorial or course",
                f"Build a small project using {skill}",
            ],
        )
        steps.append(
            RoadmapStep(
                skill=skill,
                summary=(
                    f"Step {index}: Build working proficiency in {skill}. Start with "
                    f"fundamentals, then apply it in a small project to demonstrate it "
                    f"on your resume."
                ),
                resources=resources,
                estimatedWeeks=_DEFAULT_WEEKS,
            )
        )
    return steps


def _roadmap_via_llm(
    missing_skills: list[str], settings: Settings, complete_fn
) -> list[RoadmapStep] | None:
    """Ask the LLM for a roadmap; parse into structured steps (best-effort).

    Falls back to ``None`` (caller uses the template) if the response cannot be
    parsed into the expected structure.
    """
    import json

    system = (
        "You are a career coach. Given a list of skills a candidate is missing "
        "for a job, produce a concise learning roadmap. Respond ONLY with a JSON "
        "array; each item: {\"skill\": str, \"summary\": str, \"resources\": "
        "[str], \"estimatedWeeks\": int}. Do not include any other text."
    )
    user = "Missing skills: " + ", ".join(missing_skills)
    raw = complete_fn(system, user, settings)
    if not raw:
        return None
    raw = raw.strip().strip("`")
    start = raw.find("[")
    end = raw.rfind("]")
    if start == -1 or end == -1:
        return None
    try:
        data = json.loads(raw[start : end + 1])
        steps = [
            RoadmapStep(
                skill=str(item.get("skill", "")),
                summary=str(item.get("summary", "")),
                resources=[str(r) for r in item.get("resources", [])],
                estimatedWeeks=int(item.get("estimatedWeeks", _DEFAULT_WEEKS)),
            )
            for item in data
            if item.get("skill")
        ]
        return steps or None
    except (ValueError, TypeError, json.JSONDecodeError):
        return None
