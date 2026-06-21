"""Matching engine: explainable weighted match score (ARCHITECTURE §6.1).

The final score is a transparent, tunable blend rather than a single opaque
cosine number (ADR-004)::

    final = w_sem * semantic       # cosine(resume_emb, jd_emb), 0..1
          + w_skill * skillOverlap # |matched req skills| / |req skills|
          + w_exp * experienceFit  # candidate yrs vs required band
          + w_edu * educationFit   # degree-level match

Skills are normalized through the taxonomy so synonyms ("k8s" == "Kubernetes")
do not register as false gaps.
"""

from __future__ import annotations

from dataclasses import dataclass, field

import numpy as np

from .skill_taxonomy import canonical_set

# Education levels ordered from lowest to highest, with an ordinal index used to
# compute education fit. Matches DATA_MODELS levels (HS|DIPLOMA|BACHELOR|...).
_EDU_ORDER: dict[str, int] = {
    "HS": 0,
    "HIGH SCHOOL": 0,
    "DIPLOMA": 1,
    "ASSOCIATE": 1,
    "BACHELOR": 2,
    "BACHELORS": 2,
    "BSC": 2,
    "MASTER": 3,
    "MASTERS": 3,
    "MSC": 3,
    "MBA": 3,
    "PHD": 4,
    "DOCTORATE": 4,
}


def cosine_similarity(a: np.ndarray, b: np.ndarray) -> float:
    """Return cosine similarity in ``[0, 1]`` for two vectors.

    Negative cosine values (vectors pointing apart) are clamped to ``0`` because
    a "negative" similarity has no meaning as a match component.
    """
    a = np.asarray(a, dtype=np.float64).ravel()
    b = np.asarray(b, dtype=np.float64).ravel()
    if a.size == 0 or b.size == 0 or a.shape != b.shape:
        return 0.0
    denom = float(np.linalg.norm(a) * np.linalg.norm(b))
    if denom == 0.0:
        return 0.0
    sim = float(np.dot(a, b) / denom)
    # Clamp into [0, 1].
    return max(0.0, min(1.0, sim))


@dataclass
class SkillMatch:
    """Result of comparing candidate skills against required job skills."""

    overlap: float
    matched: list[str] = field(default_factory=list)
    missing: list[str] = field(default_factory=list)


def skill_overlap(candidate_skills: list[str], job_required_skills: list[str]) -> SkillMatch:
    """Compute taxonomy-normalized skill overlap.

    Overlap is ``|matched required skills| / |required skills|``. When the job
    lists no required skills, overlap is defined as ``1.0`` (nothing to miss).
    Matched and missing skill names are returned in canonical form.
    """
    required = canonical_set(job_required_skills)
    candidate = canonical_set(candidate_skills)
    if not required:
        return SkillMatch(overlap=1.0, matched=[], missing=[])
    matched = sorted(required & candidate)
    missing = sorted(required - candidate)
    overlap = len(matched) / len(required)
    return SkillMatch(overlap=overlap, matched=matched, missing=missing)


def experience_fit(
    exp_yrs: float | None,
    min_exp: float | None,
    max_exp: float | None,
) -> float:
    """Score how well a candidate's experience fits the required band, in ``[0, 1]``.

    - No band specified -> neutral ``1.0`` (no constraint to violate).
    - Within ``[min, max]`` -> ``1.0``.
    - Below ``min`` -> linear ramp from ``0`` (no experience) to ``1`` at ``min``.
    - Above ``max`` -> gentle decay (over-qualification is mildly penalized, not
      treated as a hard miss), floored at ``0.6``.
    """
    if exp_yrs is None:
        # Cannot evaluate; treat as neutral so it neither helps nor hurts.
        return 1.0 if (min_exp is None and max_exp is None) else 0.5
    exp_yrs = max(0.0, float(exp_yrs))

    lo = float(min_exp) if min_exp is not None else None
    hi = float(max_exp) if max_exp is not None else None

    if lo is None and hi is None:
        return 1.0

    if lo is not None and exp_yrs < lo:
        if lo <= 0:
            return 1.0
        return max(0.0, min(1.0, exp_yrs / lo))

    if hi is not None and exp_yrs > hi:
        # Decay 0.1 per year over the max, floored at 0.6.
        over = exp_yrs - hi
        return max(0.6, 1.0 - 0.1 * over)

    return 1.0


def _edu_rank(level: str | None) -> int | None:
    """Map an education-level string to its ordinal rank, or ``None`` if unknown."""
    if not level:
        return None
    return _EDU_ORDER.get(level.strip().upper())


def education_fit(candidate_level: str | None, job_level: str | None) -> float:
    """Score education match in ``[0, 1]``.

    - No job requirement -> ``1.0``.
    - Candidate meets or exceeds the required level -> ``1.0``.
    - Candidate below requirement -> partial credit proportional to how close
      they are (e.g. BACHELOR vs required MASTER scores higher than HS would).
    """
    job_rank = _edu_rank(job_level)
    if job_rank is None:
        return 1.0
    cand_rank = _edu_rank(candidate_level)
    if cand_rank is None:
        # Unknown candidate education against a stated requirement -> partial.
        return 0.5
    if cand_rank >= job_rank:
        return 1.0
    # Below requirement: scale by ratio of ranks (require >0 handled by +1 base).
    return max(0.0, (cand_rank + 1) / (job_rank + 1))


def weighted_score(
    semantic: float,
    skill: float,
    experience: float,
    education: float,
    *,
    w_sem: float,
    w_skill: float,
    w_exp: float,
    w_edu: float,
) -> float:
    """Combine the four components into a final score in ``[0, 1]``.

    Weights are normalized defensively so the result stays bounded even if the
    configured weights do not sum exactly to 1.0.
    """
    total_w = w_sem + w_skill + w_exp + w_edu
    if total_w <= 0:
        return 0.0
    raw = (
        w_sem * semantic
        + w_skill * skill
        + w_exp * experience
        + w_edu * education
    )
    return max(0.0, min(1.0, raw / total_w))


def round3(value: float) -> float:
    """Round to 3 decimal places (match_scores precision is NUMERIC(4,3))."""
    return round(float(value), 3)
