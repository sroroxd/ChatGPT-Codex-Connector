#!/usr/bin/env python3
"""Package already-verified CI outputs without including signing keys or credentials."""
import hashlib
import json
from pathlib import Path
import zipfile

root = Path(__file__).resolve().parents[1]
artifacts = root / "artifacts"
report_names = ("build-check.txt", "unit-tests.xml", "lint.txt", "host-smoke.json",
                "java-vad-check.txt", "apk-check.json")
apk = artifacts / "Sprachpilot-0.1.0.apk"
report = json.loads((artifacts / "apk-check.json").read_text())
digest = hashlib.sha256(apk.read_bytes()).hexdigest()
assert report["sha256"] == digest
assert report["signature_verified"] and not report["internet_permission"]
with zipfile.ZipFile(artifacts / "Sprachpilot-0.1.0-pruefberichte.zip", "w", zipfile.ZIP_DEFLATED) as archive:
    for name in report_names:
        archive.write(artifacts / name, name)
files = [artifacts / name for name in ("Sprachpilot-0.1.0.apk", "Sprachpilot-0.1.0-source.zip",
                                     "Sprachpilot-0.1.0-pruefberichte.zip")]
(artifacts / "SHA256SUMS.txt").write_text("".join(
    hashlib.sha256(path.read_bytes()).hexdigest() + "  " + path.name + "\n" for path in files))
notes = (root / "docs/RELEASE-v0.1.0.md").read_text().replace("{{APK_SHA256}}", digest)
(artifacts / "RELEASE-NOTES.md").write_text(notes)
print("Release packaged; APK SHA-256:", digest)
