#!/usr/bin/env python3
"""Check committed model assets against the recorded provenance manifest."""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
lock = json.loads((root / "models.lock.json").read_text())
for entry in lock["files"]:
    path = root / entry["path"]
    actual = hashlib.sha256(path.read_bytes()).hexdigest()
    if actual != entry["sha256"]:
        raise SystemExit(f"SHA-256 mismatch: {entry['path']}")
print(f"PASS: {len(lock['files'])} model files match SHA-256 manifest")
