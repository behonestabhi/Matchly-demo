"""Resume parsing endpoint."""

from __future__ import annotations

from fastapi import APIRouter, HTTPException

from app.schemas import ParseRequest, ParseResponse
from pipelines.parsing import extract_text, parse_resume

router = APIRouter(prefix="/internal", tags=["parse"])


@router.post("/parse", response_model=ParseResponse)
def parse(req: ParseRequest) -> ParseResponse:
    """Extract text from a resume (text or base64 file) and parse it to JSON."""
    try:
        text = extract_text(
            text=req.text, file_base64=req.fileBase64, mime_type=req.mimeType
        )
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    return parse_resume(text)
