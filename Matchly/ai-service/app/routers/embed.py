"""Embedding endpoint."""

from __future__ import annotations

from fastapi import APIRouter, Request

from app.schemas import EmbedRequest, EmbedResponse

router = APIRouter(prefix="/internal", tags=["embed"])


@router.post("/embed", response_model=EmbedResponse)
def embed(req: EmbedRequest, request: Request) -> EmbedResponse:
    """Embed text for a RESUME or JOB and persist the vector, returning its ref."""
    engine = request.app.state.embedding_engine
    ref = engine.embed_and_store(req.ownerType, req.ownerId, req.text)
    return EmbedResponse(embeddingRef=ref, dim=engine.dim, model=engine.model_name)
