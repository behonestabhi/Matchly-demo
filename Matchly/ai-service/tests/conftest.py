"""Pytest bootstrap: ensure the ai-service root is importable.

Adds the parent directory (the ``ai-service`` root) to ``sys.path`` so the
``app``, ``pipelines``, and ``ml`` packages import without an editable install,
regardless of the directory pytest is invoked from.
"""

from __future__ import annotations

import sys
from pathlib import Path

_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))
