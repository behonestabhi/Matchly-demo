"""FastAPI application entry point for the Matchly AI service.

Wires routers, CORS, and exception handlers, and initializes shared compute
state (embedding engine, success predictor) on startup. HTTP-only; no Kafka.
Run with: ``uvicorn app.main:app --host 0.0.0.0 --port 8000``.
"""

from __future__ import annotations

import logging
from contextlib import asynccontextmanager
from datetime import datetime, timezone

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app import __version__
from app.config import get_settings
from app.routers import (
    embed,
    health,
    interview,
    parse,
    predict,
    score,
    skillgap,
)
from ml.predictor import get_predictor
from pipelines.embeddings import EmbeddingEngine

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)
logger = logging.getLogger("ai-service")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Initialize shared, expensive compute objects once per process.

    The embedding engine (model + vector store) and the success predictor
    (trained on synthetic data) are created on startup and stored on
    ``app.state`` so request handlers reuse them without re-initialization.
    """
    settings = get_settings()
    logger.info("Starting %s v%s", settings.service_name, __version__)

    app.state.embedding_engine = EmbeddingEngine(settings)
    app.state.predictor = get_predictor()
    logger.info(
        "Embedding backend=%s store=%s | predictor backend=%s",
        app.state.embedding_engine.backend,
        app.state.embedding_engine.store_kind,
        app.state.predictor.backend,
    )
    yield
    logger.info("Shutting down %s", settings.service_name)


def create_app() -> FastAPI:
    """Construct and configure the FastAPI application."""
    settings = get_settings()
    app = FastAPI(
        title="Matchly AI Service",
        description="Stateless AI compute: parsing, embeddings, matching, "
        "skill-gap, interview generation, success prediction.",
        version=__version__,
        lifespan=lifespan,
    )

    origins = [o.strip() for o in settings.cors_allow_origins.split(",") if o.strip()]
    app.add_middleware(
        CORSMiddleware,
        allow_origins=origins or ["*"],
        allow_credentials=False,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    for module in (health, parse, embed, score, skillgap, interview, predict):
        app.include_router(module.router)

    _register_exception_handlers(app)
    return app


def _register_exception_handlers(app: FastAPI) -> None:
    """Register handlers that emit the platform's RFC 7807-ish error shape."""

    @app.exception_handler(ValueError)
    async def _value_error_handler(request: Request, exc: ValueError) -> JSONResponse:
        return _problem(request, 422, "Unprocessable input", str(exc))

    @app.exception_handler(Exception)
    async def _unhandled_handler(request: Request, exc: Exception) -> JSONResponse:
        logger.exception("Unhandled error on %s: %s", request.url.path, exc)
        return _problem(
            request, 500, "Internal error", "An unexpected error occurred."
        )


def _problem(request: Request, status: int, title: str, detail: str) -> JSONResponse:
    """Build an RFC 7807-style problem response (matches API_CONTRACTS)."""
    correlation_id = request.headers.get("X-Correlation-Id")
    return JSONResponse(
        status_code=status,
        content={
            "type": "about:blank",
            "title": title,
            "status": status,
            "detail": detail,
            "correlationId": correlation_id,
            "timestamp": datetime.now(timezone.utc).isoformat(),
        },
    )


app = create_app()
