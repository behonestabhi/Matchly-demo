"""Tests for skill-gap analysis and template roadmap generation."""

from __future__ import annotations

from app.config import Settings
from pipelines.skillgap import compute_skill_gap


def _settings() -> Settings:
    # Force the offline template path.
    return Settings(llm_provider="template")


def test_missing_skills_are_taxonomy_normalized() -> None:
    res = compute_skill_gap(
        candidate_skills=["JS", "Docker"],
        job_required_skills=["JavaScript", "Kubernetes", "k8s"],
        generate_roadmap=False,
        settings=_settings(),
    )
    # JS matches JavaScript; Kubernetes/k8s collapse to one missing skill.
    assert "JavaScript" in res.matchedSkills
    assert res.missingSkills == ["Kubernetes"]
    assert res.roadmap is None


def test_no_missing_skills() -> None:
    res = compute_skill_gap(
        candidate_skills=["Python", "Django"],
        job_required_skills=["Python"],
        generate_roadmap=True,
        settings=_settings(),
    )
    assert res.missingSkills == []
    # No missing skills -> no roadmap even when requested.
    assert res.roadmap is None


def test_template_roadmap_generated_for_missing() -> None:
    res = compute_skill_gap(
        candidate_skills=["Python"],
        job_required_skills=["Kubernetes", "Terraform"],
        generate_roadmap=True,
        settings=_settings(),
    )
    assert set(res.missingSkills) == {"Kubernetes", "Terraform"}
    assert res.roadmap is not None
    assert len(res.roadmap) == 2
    for step in res.roadmap:
        assert step.skill in {"Kubernetes", "Terraform"}
        assert step.summary
        assert step.resources
        assert step.estimatedWeeks > 0
