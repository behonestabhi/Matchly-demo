"""Resume parsing pipeline: text extraction + structured field extraction.

Text extraction:
  - PDF via PyMuPDF (``fitz``) if available, else ``pdfplumber``.
  - DOCX via ``python-docx``.
  - Plain text is used directly.
All heavy/optional libraries are imported lazily so the module imports cleanly
with only light dependencies installed.

Structured parsing:
  - Regex + a skill dictionary (the taxonomy) extract name, contacts, skills,
    experience, education, projects, and certifications.
  - spaCy is OPTIONAL: if importable it is used to improve person-name detection;
    otherwise a heuristic regex fallback is used. Either way the parser returns a
    complete structured result.
"""

from __future__ import annotations

import base64
import binascii
import logging
import re
from datetime import datetime, timezone

from app.schemas import (
    Certification,
    Education,
    Experience,
    ParseResponse,
    Project,
)

from .skill_taxonomy import _TAXONOMY, _normalize, canonicalize

logger = logging.getLogger(__name__)

_EMAIL_RE = re.compile(r"[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}")
_PHONE_RE = re.compile(
    r"(?:(?:\+?\d{1,3}[\s.\-]?)?(?:\(?\d{2,4}\)?[\s.\-]?)?\d{3,4}[\s.\-]?\d{3,4})"
)
_YEAR_RE = re.compile(r"\b(19|20)\d{2}\b")

# Section headers we recognize (case-insensitive, line-leading).
_SECTION_ALIASES: dict[str, str] = {
    "skills": "skills",
    "technical skills": "skills",
    "core competencies": "skills",
    "technologies": "skills",
    "experience": "experience",
    "work experience": "experience",
    "professional experience": "experience",
    "employment": "experience",
    "education": "education",
    "academic background": "education",
    "projects": "projects",
    "personal projects": "projects",
    "certifications": "certifications",
    "certificates": "certifications",
    "licenses": "certifications",
    "summary": "summary",
    "objective": "summary",
    "profile": "summary",
}

_EDU_DEGREE_LEVEL: list[tuple[re.Pattern[str], str]] = [
    (re.compile(r"\bph\.?d\b|\bdoctor(ate)?\b", re.I), "PHD"),
    (re.compile(r"\bm\.?\s?b\.?a\b", re.I), "MASTER"),
    (re.compile(r"\bmaster|\bm\.?\s?(sc|s|eng|tech|a)\b|\bm\.?tech\b", re.I), "MASTER"),
    (re.compile(r"\bbachelor|\bb\.?\s?(sc|s|eng|tech|a|e)\b|\bb\.?tech\b", re.I), "BACHELOR"),
    (re.compile(r"\bdiploma|\bassociate\b", re.I), "DIPLOMA"),
    (re.compile(r"\bhigh school|\bsecondary\b|\bsslc\b|\bhsc\b", re.I), "HS"),
]


# --------------------------------------------------------------------------- #
# Text extraction
# --------------------------------------------------------------------------- #
def _extract_pdf(data: bytes) -> str:
    """Extract text from PDF bytes using PyMuPDF, falling back to pdfplumber."""
    try:
        import fitz  # type: ignore  # PyMuPDF

        with fitz.open(stream=data, filetype="pdf") as doc:
            return "\n".join(page.get_text() for page in doc)
    except Exception as exc:  # noqa: BLE001
        logger.debug("PyMuPDF unavailable/failed (%s); trying pdfplumber.", exc)
    try:
        import io

        import pdfplumber  # type: ignore

        with pdfplumber.open(io.BytesIO(data)) as pdf:
            return "\n".join((page.extract_text() or "") for page in pdf.pages)
    except Exception as exc:  # noqa: BLE001
        raise ValueError(f"Could not extract PDF text: {exc}") from exc


def _extract_docx(data: bytes) -> str:
    """Extract text from DOCX bytes using python-docx."""
    try:
        import io

        from docx import Document  # type: ignore

        document = Document(io.BytesIO(data))
        return "\n".join(p.text for p in document.paragraphs)
    except Exception as exc:  # noqa: BLE001
        raise ValueError(f"Could not extract DOCX text: {exc}") from exc


def extract_text(
    *,
    text: str | None = None,
    file_base64: str | None = None,
    mime_type: str | None = None,
) -> str:
    """Resolve raw resume text from either inline text or a base64 file.

    Raises:
        ValueError: if neither input is provided or extraction fails.
    """
    if text and text.strip():
        return text
    if not file_base64:
        raise ValueError("Either 'text' or 'fileBase64' must be provided.")

    try:
        data = base64.b64decode(file_base64, validate=False)
    except (binascii.Error, ValueError) as exc:
        raise ValueError(f"Invalid base64 file content: {exc}") from exc

    mime = (mime_type or "").lower()
    if "pdf" in mime or data[:5] == b"%PDF-":
        return _extract_pdf(data)
    if "word" in mime or "docx" in mime or data[:2] == b"PK":
        return _extract_docx(data)
    # Last resort: treat as UTF-8 text.
    return data.decode("utf-8", errors="ignore")


# --------------------------------------------------------------------------- #
# Structured parsing helpers
# --------------------------------------------------------------------------- #
def _split_sections(text: str) -> dict[str, str]:
    """Split raw text into recognized sections keyed by canonical section name.

    Lines that look like a known section header start a new section. Text before
    the first header is stored under ``"_header"`` (typically name/contact info).
    """
    sections: dict[str, list[str]] = {"_header": []}
    current = "_header"
    for raw_line in text.splitlines():
        line = raw_line.strip()
        key = _section_key(line)
        if key is not None:
            current = key
            sections.setdefault(current, [])
            continue
        sections.setdefault(current, []).append(raw_line)
    return {k: "\n".join(v).strip() for k, v in sections.items()}


def _section_key(line: str) -> str | None:
    """Return the canonical section name if ``line`` is a section header."""
    if not line or len(line) > 40:
        return None
    normalized = re.sub(r"[^a-z\s]", "", line.lower()).strip()
    return _SECTION_ALIASES.get(normalized)


def _extract_name(header: str, full_text: str) -> str | None:
    """Best-effort person-name extraction (spaCy if available, else heuristic)."""
    # Try spaCy NER first (optional dependency).
    name = _spacy_person_name(full_text[:1000])
    if name:
        return name
    # Heuristic: first non-empty header line that is not a contact detail and
    # looks like a name (1-4 capitalized-ish words, no digits/@).
    for raw in header.splitlines():
        line = raw.strip()
        if not line:
            continue
        if "@" in line or any(ch.isdigit() for ch in line):
            continue
        words = line.split()
        if 1 <= len(words) <= 4 and all(len(w) >= 2 for w in words):
            if line.lower() not in _SECTION_ALIASES:
                return line
    return None


def _spacy_person_name(text: str) -> str | None:
    """Use spaCy to detect a PERSON entity, if spaCy and a model are available."""
    try:
        import spacy  # type: ignore

        try:
            nlp = spacy.load("en_core_web_sm")
        except Exception:  # noqa: BLE001 - model not installed
            return None
        doc = nlp(text)
        for ent in doc.ents:
            if ent.label_ == "PERSON":
                return ent.text.strip()
    except Exception:  # noqa: BLE001 - spaCy not installed
        return None
    return None


def _extract_skills(text: str, skills_section: str) -> list[str]:
    """Extract skills via the taxonomy dictionary over the whole text.

    Each canonical skill and its aliases are searched as whole tokens. The skills
    section (if present) is weighted by being scanned the same way; presence
    anywhere in the document counts. Returns canonical names, de-duplicated and
    ordered by first appearance.
    """
    haystack = _normalize(f"{skills_section}\n{text}")
    found: list[str] = []
    seen: set[str] = set()
    for canonical, aliases in _TAXONOMY.items():
        terms = [canonical, *aliases]
        for term in terms:
            norm_term = _normalize(term)
            if not norm_term:
                continue
            # Whole-token match (handles "c++"/"c#" because '+'/'#' are kept).
            pattern = r"(?<![a-z0-9+#])" + re.escape(norm_term) + r"(?![a-z0-9+#])"
            if re.search(pattern, haystack):
                if canonical not in seen:
                    seen.add(canonical)
                    found.append(canonical)
                break
    return found


def _extract_experiences(section: str) -> list[Experience]:
    """Extract coarse work-experience entries from the experience section."""
    if not section.strip():
        return []
    experiences: list[Experience] = []
    # Split on blank lines into blocks; each block is one role (heuristic).
    blocks = [b.strip() for b in re.split(r"\n\s*\n", section) if b.strip()]
    for block in blocks[:15]:
        lines = [ln.strip() for ln in block.splitlines() if ln.strip()]
        if not lines:
            continue
        header = lines[0]
        title, company = _split_title_company(header)
        start_date, end_date = _extract_date_range(block)
        description = " ".join(lines[1:])[:500] or None
        experiences.append(
            Experience(
                company=company,
                title=title,
                startDate=start_date,
                endDate=end_date,
                description=description,
            )
        )
    return experiences


def _split_title_company(header: str) -> tuple[str | None, str | None]:
    """Split a header line like 'Engineer at Acme' / 'Engineer, Acme'."""
    for sep in (" at ", " @ ", " - ", " | ", ", "):
        if sep in header:
            left, right = header.split(sep, 1)
            return left.strip() or None, right.strip() or None
    return header.strip() or None, None


def _extract_date_range(text: str) -> tuple[str | None, str | None]:
    """Extract a (start, end) year range from a block of text."""
    years = _YEAR_RE.findall(text)
    full = re.findall(r"\b(?:19|20)\d{2}\b", text)
    if re.search(r"\bpresent|\bcurrent\b", text, re.I) and full:
        return full[0], "Present"
    if len(full) >= 2:
        return full[0], full[1]
    if len(full) == 1:
        return full[0], None
    return None, None


def _extract_education(section: str) -> list[Education]:
    """Extract education entries from the education section."""
    if not section.strip():
        return []
    entries: list[Education] = []
    blocks = [b.strip() for b in re.split(r"\n\s*\n|\n", section) if b.strip()]
    for block in blocks[:10]:
        level = _degree_level(block)
        if level is None and not re.search(
            r"university|college|institute|school", block, re.I
        ):
            continue
        years = re.findall(r"\b(?:19|20)\d{2}\b", block)
        start_year = int(years[0]) if years else None
        end_year = int(years[1]) if len(years) >= 2 else None
        institution = _find_institution(block)
        entries.append(
            Education(
                institution=institution,
                degree=block.split("\n")[0].strip()[:120] or None,
                field=None,
                level=level,
                startYear=start_year,
                endYear=end_year,
            )
        )
    return entries


def _degree_level(text: str) -> str | None:
    """Map degree text to a canonical level (HS|DIPLOMA|BACHELOR|MASTER|PHD)."""
    for pattern, level in _EDU_DEGREE_LEVEL:
        if pattern.search(text):
            return level
    return None


def _find_institution(text: str) -> str | None:
    """Find a university/college/institute name within a block."""
    match = re.search(
        r"([A-Z][\w&.\- ]*(?:University|College|Institute|School)[\w&.\- ]*)", text
    )
    return match.group(1).strip() if match else None


def _extract_projects(section: str) -> list[Project]:
    """Extract project entries from the projects section."""
    if not section.strip():
        return []
    projects: list[Project] = []
    blocks = [b.strip() for b in re.split(r"\n\s*\n|\n", section) if b.strip()]
    for block in blocks[:10]:
        lines = [ln.strip() for ln in block.splitlines() if ln.strip()]
        name = lines[0].split(":")[0].split("-")[0].strip()[:120] if lines else None
        description = block[:400]
        tech = _extract_skills(block, "")
        projects.append(Project(name=name or None, description=description, techStack=tech))
    return projects


def _extract_certifications(section: str) -> list[Certification]:
    """Extract certification entries from the certifications section."""
    if not section.strip():
        return []
    certs: list[Certification] = []
    for raw in section.splitlines():
        line = raw.strip(" -*\t")
        if not line:
            continue
        issuer = None
        for sep in (" - ", " by ", ", ", " | "):
            if sep in line:
                left, right = line.split(sep, 1)
                line, issuer = left.strip(), right.strip()
                break
        certs.append(Certification(name=line[:160] or None, issuer=issuer, issuedAt=None))
    return certs[:15]


def _estimate_total_experience(
    experiences: list[Experience], full_text: str
) -> float | None:
    """Estimate total years of experience.

    Prefers an explicit statement ("5+ years of experience"); otherwise sums the
    spans of parsed experience entries (treating 'Present' as the current year).
    """
    match = re.search(
        r"(\d{1,2}(?:\.\d)?)\s*\+?\s*years?(?:\s+of)?\s+(?:experience|exp)",
        full_text,
        re.I,
    )
    if match:
        try:
            return float(match.group(1))
        except ValueError:
            pass

    now_year = datetime.now(timezone.utc).year
    total = 0.0
    counted = False
    for exp in experiences:
        try:
            start = int(exp.startDate) if exp.startDate and exp.startDate.isdigit() else None
        except (ValueError, AttributeError):
            start = None
        if start is None:
            continue
        if exp.endDate and exp.endDate.lower() in ("present", "current"):
            end = now_year
        else:
            try:
                end = int(exp.endDate) if exp.endDate and exp.endDate.isdigit() else start
            except (ValueError, AttributeError):
                end = start
        if end >= start:
            total += end - start
            counted = True
    return round(total, 1) if counted else None


# --------------------------------------------------------------------------- #
# Public API
# --------------------------------------------------------------------------- #
def parse_resume(text: str) -> ParseResponse:
    """Parse raw resume ``text`` into a structured :class:`ParseResponse`."""
    text = text or ""
    sections = _split_sections(text)
    header = sections.get("_header", "")

    email_match = _EMAIL_RE.search(text)
    email = email_match.group(0) if email_match else None

    phone = _find_phone(text)
    full_name = _extract_name(header, text)

    skills = _extract_skills(text, sections.get("skills", ""))
    experiences = _extract_experiences(sections.get("experience", ""))
    education = _extract_education(sections.get("education", ""))
    projects = _extract_projects(sections.get("projects", ""))
    certifications = _extract_certifications(sections.get("certifications", ""))
    total_exp = _estimate_total_experience(experiences, text)

    return ParseResponse(
        fullName=full_name,
        email=email,
        phone=phone,
        totalExpYrs=total_exp,
        skills=skills,
        experiences=experiences,
        education=education,
        projects=projects,
        certifications=certifications,
        rawText=text,
    )


def _find_phone(text: str) -> str | None:
    """Find the most plausible phone number in the text.

    Filters out short numeric noise and standalone year ranges by requiring at
    least 8 digits in the matched candidate.
    """
    for candidate in _PHONE_RE.findall(text):
        digits = re.sub(r"\D", "", candidate)
        if 8 <= len(digits) <= 15:
            return candidate.strip()
    return None
