"""Success-prediction endpoint (ARCHITECTURE §6.2)."""

from __future__ import annotations

from fastapi import APIRouter

from app.schemas import PredictRequest, PredictResponse
from ml.predictor import get_predictor

router = APIRouter(prefix="/internal", tags=["predict"])


@router.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest) -> PredictResponse:
    """Return P(candidate is shortlisted) and the serving model version."""
    predictor = get_predictor()
    probability = predictor.predict(req.features)
    return PredictResponse(probability=probability, modelVersion=predictor.version)
