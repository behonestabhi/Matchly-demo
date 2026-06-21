"""Application configuration, loaded from environment via pydantic-settings.

No secrets are hard-coded; every value is overridable through the environment
(or a local ``.env`` file). See ``.env.example`` for the full list.
"""

from __future__ import annotations

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Runtime configuration for the AI service.

    Attributes are populated from environment variables (case-insensitive) and
    fall back to the defaults below, which are safe for local/offline use.
    """

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        case_sensitive=False,
    )

    # --- Service identity ---
    service_name: str = "ai-service"
    # Matching model version surfaced in score/predict responses and /models.
    match_model_version: str = "match-v1.2"

    # --- Embeddings ---
    # Logical model name reported to callers. The sentence-transformers backend
    # uses this id verbatim; the deterministic fallback reports a "-fallback" tag.
    embedding_model: str = "BAAI/bge-small-en-v1.5"
    # Vector dimensionality; must match the pgvector column (vector(384)).
    embedding_dim: int = 384

    # --- Persistence ---
    # When set, embeddings are upserted into PostgreSQL + pgvector. When unset,
    # an in-memory store is used (suitable for tests and offline runs).
    database_url: str | None = None

    # --- Matching weights (ARCHITECTURE §6.1) ---
    weight_semantic: float = 0.45
    weight_skill: float = 0.35
    weight_experience: float = 0.15
    weight_education: float = 0.05

    # --- LLM (optional) ---
    # One of: "template" (offline), "openai", "gemini".
    llm_provider: str = "template"
    openai_api_key: str | None = None
    gemini_api_key: str | None = None
    openai_model: str = "gpt-4o-mini"
    gemini_model: str = "gemini-1.5-flash"
    llm_timeout_seconds: float = 30.0

    # --- CORS ---
    # The AI service is internal-only; permissive CORS is acceptable but tunable.
    cors_allow_origins: str = "*"


@lru_cache
def get_settings() -> Settings:
    """Return a cached :class:`Settings` instance (read once per process)."""
    return Settings()
