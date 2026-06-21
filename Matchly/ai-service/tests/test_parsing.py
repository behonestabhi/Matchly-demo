"""Tests for resume parsing from inline text (no binary file required)."""

from __future__ import annotations

from pipelines.parsing import extract_text, parse_resume

_SAMPLE = """\
Jane Q. Developer
jane.developer@example.com | +1 (415) 555-0199 | San Francisco, CA

Summary
Senior backend engineer with 6 years of experience building distributed systems.

Skills
Java, Spring Boot, Apache Kafka, PostgreSQL, Docker, k8s, JS

Experience
Senior Software Engineer at Acme Corp
2019 - Present
Led a team building event-driven microservices with Kafka and Spring Boot.

Software Engineer at Globex
2017 - 2019
Built REST APIs in Java.

Education
B.Sc. in Computer Science
State University
2013 - 2017

Projects
Matchly - AI recruitment platform
Built a matching engine in Python with FastAPI.

Certifications
AWS Certified Solutions Architect - Amazon Web Services
"""


def test_extract_text_passthrough() -> None:
    assert extract_text(text="hello world") == "hello world"


def test_parse_contact_fields() -> None:
    res = parse_resume(_SAMPLE)
    assert res.email == "jane.developer@example.com"
    assert res.phone is not None and "555" in res.phone
    assert res.fullName is not None and "Jane" in res.fullName


def test_parse_skills_normalized_via_taxonomy() -> None:
    res = parse_resume(_SAMPLE)
    skills = set(res.skills)
    # Aliases canonicalized.
    assert "Kubernetes" in skills  # from "k8s"
    assert "JavaScript" in skills  # from "JS"
    assert "Apache Kafka" in skills  # from "Kafka"
    assert "Spring Boot" in skills
    assert "PostgreSQL" in skills
    assert "Java" in skills


def test_parse_experience_and_education() -> None:
    res = parse_resume(_SAMPLE)
    assert len(res.experiences) >= 1
    titles = " ".join((e.title or "") for e in res.experiences)
    assert "Engineer" in titles
    assert len(res.education) >= 1
    assert any(e.level == "BACHELOR" for e in res.education)


def test_parse_total_experience_from_statement() -> None:
    res = parse_resume(_SAMPLE)
    # "6 years of experience" stated explicitly.
    assert res.totalExpYrs == 6.0


def test_parse_certifications_and_projects() -> None:
    res = parse_resume(_SAMPLE)
    assert any("AWS" in (c.name or "") for c in res.certifications)
    assert any("Matchly" in (p.name or "") for p in res.projects)


def test_parse_raw_text_preserved() -> None:
    res = parse_resume(_SAMPLE)
    assert res.rawText == _SAMPLE
