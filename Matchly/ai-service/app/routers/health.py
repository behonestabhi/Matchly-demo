"""Health and model-version endpoints."""

from __future__ import annotations

from fastapi import APIRouter, Request

from app import __version__
from app.config import get_settings
from app.schemas import HealthResponse, ModelInfo, ModelsResponse

router = APIRouter(prefix="/internal", tags=["health"])


@router.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    """Liveness probe: returns service status and version."""
    return HealthResponse(status="ok", version=__version__)


@router.get("/models", response_model=ModelsResponse)
def models(request: Request) -> ModelsResponse:
    """Report active model/component versions across pipelines."""
    settings = get_settings()
    engine = request.app.state.embedding_engine
    predictor = request.app.state.predictor

    return ModelsResponse(
        embedding=ModelInfo(
            name=engine.model_name,
            version=engine.model_name,
            backend=f"{engine.backend} / store={engine.store_kind}",
        ),
        matching=ModelInfo(
            name="weighted-blend",
            version=settings.match_model_version,
            backend="cosine+skill+exp+edu",
        ),
        predictor=ModelInfo(
            name="success_predictor",
            version=predictor.version,
            backend=predictor.backend,
        ),
        interview=ModelInfo(
            name="interview-generator",
            version="template-v1",
            backend=settings.llm_provider,
        ),
    )
