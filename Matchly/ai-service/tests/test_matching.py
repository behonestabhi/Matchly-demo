"""Tests for the matching engine: cosine, weighted blend, skill normalization."""

from __future__ import annotations

import numpy as np

from pipelines.matching import (
    cosine_similarity,
    education_fit,
    experience_fit,
    skill_overlap,
    weighted_score,
)
from pipelines.skill_taxonomy import canonicalize


def test_cosine_identical_vectors_is_one() -> None:
    v = np.array([1.0, 2.0, 3.0])
    assert cosine_similarity(v, v) == 1.0


def test_cosine_orthogonal_is_zero() -> None:
    a = np.array([1.0, 0.0])
    b = np.array([0.0, 1.0])
    assert cosine_similarity(a, b) == 0.0


def test_cosine_opposite_clamped_to_zero() -> None:
    a = np.array([1.0, 0.0])
    b = np.array([-1.0, 0.0])
    assert cosine_similarity(a, b) == 0.0


def test_cosine_mismatched_shapes_returns_zero() -> None:
    assert cosine_similarity(np.array([1.0, 2.0]), np.array([1.0])) == 0.0


def test_skill_normalization_aliases() -> None:
    assert canonicalize("JS") == "JavaScript"
    assert canonicalize("js") == "JavaScript"
    assert canonicalize("k8s") == "Kubernetes"
    assert canonicalize("React.js") == "React"
    assert canonicalize("postgres") == "PostgreSQL"


def test_skill_overlap_uses_taxonomy() -> None:
    candidate = ["JS", "k8s", "Docker"]
    required = ["JavaScript", "Kubernetes", "Terraform"]
    match = skill_overlap(candidate, required)
    # JS->JavaScript and k8s->Kubernetes match; Terraform is missing.
    assert match.matched == ["JavaScript", "Kubernetes"]
    assert match.missing == ["Terraform"]
    assert abs(match.overlap - (2 / 3)) < 1e-9


def test_skill_overlap_empty_required_is_full() -> None:
    match = skill_overlap(["Python"], [])
    assert match.overlap == 1.0
    assert match.missing == []


def test_experience_fit_within_band() -> None:
    assert experience_fit(4.0, 3.0, 6.0) == 1.0


def test_experience_fit_below_min_ramps() -> None:
    assert experience_fit(1.5, 3.0, 6.0) == 0.5


def test_experience_fit_over_max_decays_but_floors() -> None:
    # 4 years over max -> 1 - 0.4 = 0.6 floor.
    assert experience_fit(10.0, 3.0, 6.0) == 0.6
    assert experience_fit(50.0, 3.0, 6.0) == 0.6  # never below floor


def test_experience_fit_no_band_is_neutral() -> None:
    assert experience_fit(5.0, None, None) == 1.0


def test_education_fit_levels() -> None:
    assert education_fit("MASTER", "BACHELOR") == 1.0  # exceeds
    assert education_fit("BACHELOR", "BACHELOR") == 1.0  # meets
    assert education_fit("HS", "MASTER") < 1.0  # below
    assert education_fit("BACHELOR", None) == 1.0  # no requirement


def test_weighted_blend_matches_architecture_weights() -> None:
    # All components perfect -> final 1.0.
    final = weighted_score(
        1.0, 1.0, 1.0, 1.0, w_sem=0.45, w_skill=0.35, w_exp=0.15, w_edu=0.05
    )
    assert abs(final - 1.0) < 1e-9


def test_weighted_blend_is_component_weighted() -> None:
    final = weighted_score(
        1.0, 0.0, 0.0, 0.0, w_sem=0.45, w_skill=0.35, w_exp=0.15, w_edu=0.05
    )
    # Only semantic perfect -> equals its weight (weights sum to 1).
    assert abs(final - 0.45) < 1e-9


def test_weighted_blend_full_example() -> None:
    # semantic .95, skill .88, exp .90, edu 1.0 -> ~0.917
    final = weighted_score(
        0.95, 0.88, 0.90, 1.0, w_sem=0.45, w_skill=0.35, w_exp=0.15, w_edu=0.05
    )
    expected = 0.45 * 0.95 + 0.35 * 0.88 + 0.15 * 0.90 + 0.05 * 1.0
    assert abs(final - expected) < 1e-9
    assert 0.0 <= final <= 1.0
