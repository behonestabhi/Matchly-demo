"""Matchly AI service (FastAPI).

Stateless compute service for the Matchly recruitment platform: resume parsing,
text embeddings, semantic matching, skill-gap analysis, interview-question
generation, and success prediction. HTTP-only (no Kafka); called over REST by the
Java microservices. Listens on port 8000.
"""

__version__ = "1.0.0"
