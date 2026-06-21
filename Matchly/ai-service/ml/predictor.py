"""Success predictor (ARCHITECTURE §6.2): P(candidate is shortlisted).

Until labeled historical data exists, the model is trained at startup on
deterministically-generated synthetic data (cold start, ADR/§6.2) and cached for
the process lifetime. The features mirror the contract:

    yearsExperience, skillMatchCount, certificationCount, educationLevel,
    projectCount, semanticScore

If scikit-learn is unavailable, a transparent logistic heuristic is used so the
endpoint always returns a probability (fail-soft).
"""

from __future__ import annotations

import logging

import numpy as np

from app.schemas import PredictFeatures

logger = logging.getLogger(__name__)

# Deterministic seed so model + scores are reproducible across restarts.
_SEED = 42
_N_SAMPLES = 4000

# Feature order used everywhere (training, inference, heuristic).
_FEATURE_ORDER = (
    "yearsExperience",
    "skillMatchCount",
    "certificationCount",
    "educationLevel",
    "projectCount",
    "semanticScore",
)


def _features_to_vector(features: PredictFeatures) -> np.ndarray:
    """Convert a :class:`PredictFeatures` into an ordered numpy row vector."""
    return np.array(
        [
            float(features.yearsExperience),
            float(features.skillMatchCount),
            float(features.certificationCount),
            float(features.educationLevel),
            float(features.projectCount),
            float(features.semanticScore),
        ],
        dtype=np.float64,
    )


def _generate_synthetic_dataset() -> tuple[np.ndarray, np.ndarray]:
    """Generate a labeled synthetic dataset reflecting plausible hiring signal.

    The label is drawn from a logistic function of a weighted feature
    combination plus noise, so a trained classifier recovers sensible behavior:
    more experience, skill matches, certs, higher education, projects, and
    semantic score all increase the shortlist probability.
    """
    rng = np.random.default_rng(_SEED)
    n = _N_SAMPLES

    years = rng.uniform(0, 20, n)
    skill_match = rng.integers(0, 12, n).astype(float)
    certs = rng.integers(0, 6, n).astype(float)
    edu = rng.integers(0, 5, n).astype(float)  # 0..4 (HS..PHD)
    projects = rng.integers(0, 10, n).astype(float)
    semantic = rng.uniform(0, 1, n)

    x = np.column_stack([years, skill_match, certs, edu, projects, semantic])

    # Latent "true" linear signal (normalized features) -> logistic probability.
    z = (
        0.06 * years
        + 0.22 * skill_match
        + 0.15 * certs
        + 0.18 * edu
        + 0.12 * projects
        + 2.0 * semantic
        - 2.6
    )
    z += rng.normal(0, 0.5, n)  # label noise
    prob = 1.0 / (1.0 + np.exp(-z))
    y = (rng.uniform(0, 1, n) < prob).astype(int)
    return x, y


def _logistic_heuristic(features: PredictFeatures) -> float:
    """Transparent logistic fallback used when scikit-learn is unavailable."""
    vec = _features_to_vector(features)
    weights = np.array([0.06, 0.22, 0.15, 0.18, 0.12, 2.0])
    z = float(np.dot(vec, weights)) - 2.6
    return float(1.0 / (1.0 + np.exp(-z)))


class SuccessPredictor:
    """Trains (on synthetic data) and serves shortlist probabilities."""

    def __init__(self) -> None:
        self._model = None
        self._scaler = None
        self.backend = "heuristic"
        self.version = "predict-heuristic-v1"
        self.metrics: dict[str, float] = {}
        self._train()

    def _train(self) -> None:
        """Train a scikit-learn classifier on synthetic data, or fall back."""
        try:
            from sklearn.ensemble import RandomForestClassifier
            from sklearn.metrics import (
                accuracy_score,
                f1_score,
                precision_score,
                recall_score,
            )
            from sklearn.model_selection import train_test_split
            from sklearn.preprocessing import StandardScaler
        except Exception as exc:  # noqa: BLE001
            logger.warning("scikit-learn unavailable (%s); using heuristic.", exc)
            return

        x, y = _generate_synthetic_dataset()
        x_train, x_test, y_train, y_test = train_test_split(
            x, y, test_size=0.2, random_state=_SEED, stratify=y
        )
        scaler = StandardScaler().fit(x_train)
        model = RandomForestClassifier(
            n_estimators=120, max_depth=8, random_state=_SEED, n_jobs=1
        )
        model.fit(scaler.transform(x_train), y_train)

        preds = model.predict(scaler.transform(x_test))
        self.metrics = {
            "accuracy": round(float(accuracy_score(y_test, preds)), 3),
            "precision": round(float(precision_score(y_test, preds, zero_division=0)), 3),
            "recall": round(float(recall_score(y_test, preds, zero_division=0)), 3),
            "f1": round(float(f1_score(y_test, preds, zero_division=0)), 3),
        }
        self._model = model
        self._scaler = scaler
        self.backend = "random-forest"
        self.version = "predict-rf-v1"
        logger.info("Success predictor trained (RandomForest): %s", self.metrics)

    def predict(self, features: PredictFeatures) -> float:
        """Return P(shortlisted) in ``[0, 1]`` for the given features."""
        if self._model is None or self._scaler is None:
            return round(_logistic_heuristic(features), 3)
        vec = _features_to_vector(features).reshape(1, -1)
        scaled = self._scaler.transform(vec)
        prob = float(self._model.predict_proba(scaled)[0][1])
        return round(max(0.0, min(1.0, prob)), 3)


# Process-wide singleton, lazily initialized to avoid training at import time.
_predictor: SuccessPredictor | None = None


def get_predictor() -> SuccessPredictor:
    """Return the cached :class:`SuccessPredictor`, training it on first use."""
    global _predictor
    if _predictor is None:
        _predictor = SuccessPredictor()
    return _predictor
