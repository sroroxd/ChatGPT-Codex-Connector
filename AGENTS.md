# Eigenständiges Projekt Sprachpilot

Dieses Android-Projekt ist unabhängig vom Javis-Projekt auf GitHub.

- Projektwurzel: dieses Verzeichnis; Android applicationId und Namespace: `de.sprachpilot`.
- Eine eigene Git-Historie verwenden. Keine Javis-Remotes, Submodule, Worktrees oder gemeinsam genutzten Signierschlüssel einrichten.
- Keine Änderungen, Commits, Pushes oder Veröffentlichungen im Javis-Repository für Aufgaben dieses Projekts durchführen.
- Das autorisierte eigenständige GitHub-Repository für Sprachpilot ist `sroroxd/ChatGPT-Codex-Connector`. Nur dieses Repository für Sprachpilot-Commits und Releases verwenden; der Repository-Name ist keine Aussage über vorhandenen Handy-Fernzugriff.
- APKs und Ergebnisarchive liegen lokal unter `artifacts/` und gehören nicht in die Git-Historie.
- Root ist keine Voraussetzung. Keine Netzwerkberechtigung oder automatische Cloud-Nutzung hinzufügen. Tatsächliche Gerätetests von Host-Tests klar unterscheiden.

Prüfungen: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`, `python tools/verify_assets.py`. Die abschließende APK zusätzlich mit `tools/check_apk.py` kontrollieren.
