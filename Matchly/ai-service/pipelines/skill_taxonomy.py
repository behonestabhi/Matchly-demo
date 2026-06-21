"""Skill taxonomy: canonical skill names plus alias normalization.

Matching and skill-gap both depend on normalizing free-text skill strings to a
canonical form so that, e.g., "k8s" on a resume and "Kubernetes" in a job
description are recognized as the same skill (DATA_MODELS §10, ARCHITECTURE
§6.1). This module seeds a reasonable set of ~80 common technology skills and
their aliases and exposes helpers to canonicalize and match skills.

The taxonomy is intentionally in-code (no DB dependency) so the core matching
logic works in any environment.
"""

from __future__ import annotations

import re

# Canonical skill name -> list of aliases (lowercase comparison is used).
# Aliases need not be exhaustive; unknown skills fall back to a normalized form
# of their own text, so they still participate in overlap on exact-ish matches.
_TAXONOMY: dict[str, list[str]] = {
    # --- Languages ---
    "JavaScript": ["js", "ecmascript", "es6", "es2015", "node js"],
    "TypeScript": ["ts"],
    "Python": ["py", "python3"],
    "Java": ["java se", "core java"],
    "Kotlin": [],
    "C#": ["c sharp", "csharp", "dotnet c#"],
    "C++": ["cpp", "c plus plus"],
    "C": [],
    "Go": ["golang"],
    "Rust": [],
    "Ruby": [],
    "PHP": [],
    "Swift": [],
    "Scala": [],
    "R": [],
    "SQL": ["structured query language"],
    "Bash": ["shell", "shell scripting", "sh"],
    # --- Frontend ---
    "React": ["react js", "reactjs", "react.js"],
    "Angular": ["angular js", "angularjs"],
    "Vue.js": ["vue", "vuejs"],
    "Next.js": ["next js", "nextjs"],
    "Svelte": [],
    "HTML": ["html5"],
    "CSS": ["css3"],
    "Tailwind CSS": ["tailwind", "tailwindcss"],
    "Redux": [],
    "jQuery": ["jquery"],
    "Webpack": [],
    # --- Backend / frameworks ---
    "Node.js": ["node", "nodejs"],
    "Express.js": ["express", "expressjs"],
    "Spring": ["spring framework"],
    "Spring Boot": ["springboot"],
    "Django": [],
    "Flask": [],
    "FastAPI": ["fast api"],
    "Ruby on Rails": ["rails", "ror"],
    ".NET": ["dotnet", "dot net", "asp.net", "aspnet"],
    "GraphQL": ["graph ql"],
    "REST": ["rest api", "restful", "restful api"],
    "gRPC": ["grpc"],
    "Microservices": ["micro services", "microservice"],
    # --- Data / databases ---
    "PostgreSQL": ["postgres", "psql", "postgresql"],
    "MySQL": ["my sql"],
    "MongoDB": ["mongo", "mongo db"],
    "Redis": [],
    "Elasticsearch": ["elastic search", "elastic"],
    "Cassandra": [],
    "DynamoDB": ["dynamo db", "dynamo"],
    "Oracle": ["oracle db"],
    "SQLite": ["sql lite"],
    "Snowflake": [],
    "Apache Kafka": ["kafka"],
    "RabbitMQ": ["rabbit mq"],
    "Apache Spark": ["spark", "pyspark"],
    "Hadoop": [],
    "Airflow": ["apache airflow"],
    "pgvector": ["pg vector"],
    # --- Cloud / DevOps ---
    "AWS": ["amazon web services", "amazon aws"],
    "Azure": ["microsoft azure"],
    "Google Cloud Platform": ["gcp", "google cloud"],
    "Docker": ["docker container", "containerization"],
    "Kubernetes": ["k8s", "kube"],
    "Terraform": [],
    "Ansible": [],
    "Jenkins": [],
    "GitHub Actions": ["github action", "gh actions"],
    "GitLab CI": ["gitlab ci/cd", "gitlab-ci"],
    "CI/CD": ["ci cd", "cicd", "continuous integration"],
    "Prometheus": [],
    "Grafana": [],
    "Helm": [],
    "Nginx": ["ngnix"],
    "Linux": ["unix"],
    "Git": ["version control"],
    # --- ML / data science ---
    "Machine Learning": ["ml", "machine-learning"],
    "Deep Learning": ["dl"],
    "TensorFlow": ["tensor flow"],
    "PyTorch": ["torch", "py torch"],
    "scikit-learn": ["sklearn", "scikit learn", "sci-kit learn"],
    "Pandas": [],
    "NumPy": ["numpy"],
    "NLP": ["natural language processing"],
    "Computer Vision": ["cv", "opencv"],
    "Data Analysis": ["data analytics"],
    # --- Practices / misc ---
    "Agile": ["scrum", "agile methodology"],
    "Unit Testing": ["unit tests", "junit", "pytest", "testing"],
    "OOP": ["object oriented programming", "object-oriented"],
    "Data Structures": ["data structures and algorithms", "dsa", "algorithms"],
    "System Design": ["systems design"],
    "OAuth": ["oauth2", "oauth 2.0"],
}


def _normalize(text: str) -> str:
    """Lowercase, strip punctuation noise, and collapse whitespace.

    Keeps ``+`` and ``#`` because they are significant for skills like ``C++``
    and ``C#``; everything else non-alphanumeric becomes a space.
    """
    text = text.strip().lower()
    text = re.sub(r"[^a-z0-9+#.]+", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def _build_alias_index() -> dict[str, str]:
    """Build a normalized-alias -> canonical-name lookup table."""
    index: dict[str, str] = {}
    for canonical, aliases in _TAXONOMY.items():
        index[_normalize(canonical)] = canonical
        for alias in aliases:
            index[_normalize(alias)] = canonical
    return index


_ALIAS_INDEX: dict[str, str] = _build_alias_index()


def canonicalize(skill: str) -> str:
    """Return the canonical name for a raw skill string.

    Known aliases (e.g. ``"k8s"``) map to their canonical form
    (``"Kubernetes"``). Unknown skills are returned title-cased on their
    normalized text so that exact matches still align across inputs.
    """
    if not skill or not skill.strip():
        return ""
    norm = _normalize(skill)
    if norm in _ALIAS_INDEX:
        return _ALIAS_INDEX[norm]
    # Unknown skill: return a stable, presentable form. Preserve the original
    # casing's intent by title-casing the normalized tokens.
    return " ".join(part.capitalize() for part in norm.split(" ")).strip()


def canonical_set(skills: list[str]) -> set[str]:
    """Canonicalize a list of skills into a deduplicated set (drops blanks)."""
    result: set[str] = set()
    for skill in skills:
        canon = canonicalize(skill)
        if canon:
            result.add(canon)
    return result


def all_canonical_skills() -> list[str]:
    """Return the sorted list of canonical skills known to the taxonomy."""
    return sorted(_TAXONOMY.keys())


def taxonomy_size() -> int:
    """Return the number of canonical skills in the seeded taxonomy."""
    return len(_TAXONOMY)
