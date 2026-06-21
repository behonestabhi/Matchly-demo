"""Embedding engine and vector store.

Embedding backend:
  - ``sentence-transformers`` is used when importable (the production backend,
    e.g. ``BAAI/bge-small-en-v1.5``).
  - Otherwise a deterministic, dependency-light hashing embedding is used so the
    service always works (tests, offline, CI). The fallback is L2-normalized and
    stable for a given input, so cosine similarity remains meaningful.

Vector store:
  - When ``DATABASE_URL`` is set, vectors are upserted into PostgreSQL +
    pgvector (DATA_MODELS §8). The driver (psycopg) is imported lazily; if it is
    unavailable the store falls back to in-memory and logs a warning.
  - Otherwise an in-memory dict is used, keyed by a generated ``embeddingRef``.

The store is keyed by ``(owner_type, owner_id, model)`` for upsert semantics and
also indexed by ``embeddingRef`` for fast lookup during scoring.
"""

from __future__ import annotations

import hashlib
import logging
import threading
import uuid

import numpy as np

from app.config import Settings

logger = logging.getLogger(__name__)


# --------------------------------------------------------------------------- #
# Embedding backends
# --------------------------------------------------------------------------- #
class _HashingEmbedder:
    """Deterministic hashing embedder used when sentence-transformers is absent.

    Produces a stable, L2-normalized float vector of the configured dimension by
    hashing token n-grams into buckets (a lightweight feature-hashing / "hashing
    trick" approach). It is not semantically as strong as a transformer, but it
    is deterministic and yields sensible cosine similarities for overlapping
    text, which is sufficient for offline/test use.
    """

    def __init__(self, dim: int) -> None:
        self.dim = dim
        self.model_name = f"hashing-{dim}-fallback"

    @staticmethod
    def _tokens(text: str) -> list[str]:
        cleaned = "".join(c.lower() if c.isalnum() else " " for c in text)
        words = [w for w in cleaned.split() if w]
        # Include unigrams and bigrams to capture a little word order.
        grams = list(words)
        grams += [f"{a}_{b}" for a, b in zip(words, words[1:])]
        return grams

    def encode(self, text: str) -> np.ndarray:
        vec = np.zeros(self.dim, dtype=np.float64)
        for token in self._tokens(text):
            digest = hashlib.md5(token.encode("utf-8")).digest()
            idx = int.from_bytes(digest[:4], "little") % self.dim
            sign = 1.0 if digest[4] & 1 else -1.0
            vec[idx] += sign
        norm = float(np.linalg.norm(vec))
        if norm > 0:
            vec /= norm
        return vec


class _SentenceTransformerEmbedder:
    """Wrapper around a lazily-loaded sentence-transformers model."""

    def __init__(self, model_name: str, model_obj: object, dim: int) -> None:
        self.model_name = model_name
        self._model = model_obj
        self.dim = dim

    def encode(self, text: str) -> np.ndarray:
        vec = self._model.encode(text, normalize_embeddings=True)  # type: ignore[attr-defined]
        return np.asarray(vec, dtype=np.float64).ravel()


def _build_embedder(settings: Settings):
    """Return the best available embedder, preferring sentence-transformers."""
    try:
        from sentence_transformers import SentenceTransformer  # type: ignore

        model = SentenceTransformer(settings.embedding_model)
        dim = int(model.get_sentence_embedding_dimension())
        logger.info("Embedding backend: sentence-transformers (%s, dim=%d)",
                    settings.embedding_model, dim)
        return _SentenceTransformerEmbedder(settings.embedding_model, model, dim)
    except Exception as exc:  # noqa: BLE001 - any import/load failure -> fallback
        logger.warning(
            "sentence-transformers unavailable (%s); using hashing fallback.", exc
        )
        return _HashingEmbedder(settings.embedding_dim)


# --------------------------------------------------------------------------- #
# Vector stores
# --------------------------------------------------------------------------- #
class _InMemoryStore:
    """Thread-safe in-memory vector store keyed by embeddingRef."""

    def __init__(self) -> None:
        self._by_ref: dict[str, np.ndarray] = {}
        self._by_owner: dict[tuple[str, str, str], str] = {}
        self._lock = threading.Lock()

    def upsert(self, owner_type: str, owner_id: str, model: str, vector: np.ndarray) -> str:
        key = (owner_type, owner_id, model)
        with self._lock:
            ref = self._by_owner.get(key) or str(uuid.uuid4())
            self._by_owner[key] = ref
            self._by_ref[ref] = vector
        return ref

    def get(self, ref: str) -> np.ndarray | None:
        with self._lock:
            return self._by_ref.get(ref)


class _PgVectorStore:
    """PostgreSQL + pgvector store. Imports psycopg lazily."""

    def __init__(self, database_url: str) -> None:
        import psycopg  # type: ignore  # noqa: F401 - validate availability

        self._database_url = database_url
        self._psycopg = __import__("psycopg")
        self._ensure_schema()

    def _connect(self):
        return self._psycopg.connect(self._database_url)

    def _ensure_schema(self) -> None:
        with self._connect() as conn, conn.cursor() as cur:
            cur.execute("CREATE EXTENSION IF NOT EXISTS vector;")
            cur.execute(
                """
                CREATE TABLE IF NOT EXISTS embeddings (
                    id          UUID PRIMARY KEY,
                    owner_type  TEXT NOT NULL,
                    owner_id    UUID NOT NULL,
                    model       TEXT NOT NULL,
                    vector      vector NOT NULL,
                    UNIQUE (owner_type, owner_id, model)
                );
                """
            )
            conn.commit()

    @staticmethod
    def _to_literal(vector: np.ndarray) -> str:
        return "[" + ",".join(f"{x:.8f}" for x in vector.tolist()) + "]"

    def upsert(self, owner_type: str, owner_id: str, model: str, vector: np.ndarray) -> str:
        ref = str(uuid.uuid4())
        literal = self._to_literal(vector)
        with self._connect() as conn, conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO embeddings (id, owner_type, owner_id, model, vector)
                VALUES (%s, %s, %s, %s, %s)
                ON CONFLICT (owner_type, owner_id, model)
                DO UPDATE SET vector = EXCLUDED.vector
                RETURNING id;
                """,
                (ref, owner_type, owner_id, model, literal),
            )
            row = cur.fetchone()
            conn.commit()
            return str(row[0]) if row else ref

    def get(self, ref: str) -> np.ndarray | None:
        with self._connect() as conn, conn.cursor() as cur:
            cur.execute("SELECT vector FROM embeddings WHERE id = %s;", (ref,))
            row = cur.fetchone()
        if not row or row[0] is None:
            return None
        raw = row[0]
        if isinstance(raw, str):
            raw = raw.strip().lstrip("[").rstrip("]")
            values = [float(x) for x in raw.split(",") if x.strip()]
        else:
            values = [float(x) for x in raw]
        return np.asarray(values, dtype=np.float64)


# --------------------------------------------------------------------------- #
# Engine
# --------------------------------------------------------------------------- #
class EmbeddingEngine:
    """Embeds text and persists vectors, abstracting backend and store choice."""

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._embedder = _build_embedder(settings)
        self._store = self._build_store(settings)

    @staticmethod
    def _build_store(settings: Settings):
        if settings.database_url:
            try:
                store = _PgVectorStore(settings.database_url)
                logger.info("Vector store: PostgreSQL + pgvector.")
                return store
            except Exception as exc:  # noqa: BLE001
                logger.warning(
                    "pgvector store unavailable (%s); using in-memory store.", exc
                )
        logger.info("Vector store: in-memory.")
        return _InMemoryStore()

    @property
    def dim(self) -> int:
        """Dimensionality of vectors produced by the active backend."""
        return self._embedder.dim

    @property
    def model_name(self) -> str:
        """Name of the active embedding backend/model."""
        return self._embedder.model_name

    @property
    def backend(self) -> str:
        """Backend kind: ``sentence-transformers`` or ``hashing-fallback``."""
        return (
            "sentence-transformers"
            if isinstance(self._embedder, _SentenceTransformerEmbedder)
            else "hashing-fallback"
        )

    @property
    def store_kind(self) -> str:
        """Active store kind: ``pgvector`` or ``in-memory``."""
        return "pgvector" if isinstance(self._store, _PgVectorStore) else "in-memory"

    def encode(self, text: str) -> np.ndarray:
        """Return the embedding vector for ``text``."""
        return self._embedder.encode(text or "")

    def embed_and_store(self, owner_type: str, owner_id: str, text: str) -> str:
        """Embed ``text`` and upsert it; return the resulting ``embeddingRef``."""
        vector = self.encode(text)
        return self._store.upsert(owner_type, owner_id, self.model_name, vector)

    def get_vector(self, ref: str) -> np.ndarray | None:
        """Look up a previously stored vector by reference."""
        return self._store.get(ref)
