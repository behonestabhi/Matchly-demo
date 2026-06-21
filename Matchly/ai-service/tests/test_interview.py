"""Tests for the offline interview-question template generator."""

from __future__ import annotations

from app.config import Settings
from app.schemas import QuestionCounts
from pipelines.interview import generate_interview


def _settings() -> Settings:
    return Settings(llm_provider="template")


def test_template_returns_requested_counts() -> None:
    counts = QuestionCounts(technical=4, behavioral=2, scenario=3)
    res = generate_interview(
        resume_text="Experienced Java engineer.",
        job_description="Backend role using Java, Kafka, and Kubernetes.",
        skills=["Java", "Apache Kafka", "Kubernetes"],
        counts=counts,
        settings=_settings(),
    )
    by_cat = {"TECHNICAL": 0, "BEHAVIORAL": 0, "SCENARIO": 0}
    for q in res.questions:
        by_cat[q.category] += 1
    assert by_cat == {"TECHNICAL": 4, "BEHAVIORAL": 2, "SCENARIO": 3}
    assert res.llmModel == "template-v1"


def test_questions_have_all_fields() -> None:
    res = generate_interview(
        resume_text=None,
        job_description=None,
        skills=["Python", "FastAPI"],
        counts=QuestionCounts(technical=2, behavioral=1, scenario=1),
        settings=_settings(),
    )
    assert len(res.questions) == 4
    for q in res.questions:
        assert q.question
        assert q.suggestedAnswer
        assert q.difficulty in {"EASY", "MEDIUM", "HARD"}
        assert q.category in {"TECHNICAL", "BEHAVIORAL", "SCENARIO"}


def test_technical_questions_reference_skills() -> None:
    res = generate_interview(
        resume_text=None,
        job_description=None,
        skills=["Kubernetes"],
        counts=QuestionCounts(technical=1, behavioral=0, scenario=0),
        settings=_settings(),
    )
    assert len(res.questions) == 1
    assert "Kubernetes" in res.questions[0].question


def test_skills_derived_from_jd_when_none_given() -> None:
    res = generate_interview(
        resume_text=None,
        job_description="We need strong Docker and Terraform experience.",
        skills=[],
        counts=QuestionCounts(technical=2, behavioral=0, scenario=0),
        settings=_settings(),
    )
    text = " ".join(q.question for q in res.questions)
    assert "Docker" in text or "Terraform" in text


def test_zero_counts_returns_empty() -> None:
    res = generate_interview(
        resume_text=None,
        job_description=None,
        skills=["Go"],
        counts=QuestionCounts(technical=0, behavioral=0, scenario=0),
        settings=_settings(),
    )
    assert res.questions == []
