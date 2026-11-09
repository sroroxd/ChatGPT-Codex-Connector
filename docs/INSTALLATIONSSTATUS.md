# Installation und Projekttrennung

Stand: 2026-11-09.

## Projekttrennung

Sprachpilot hat unter `/workspace/sprachpilot` ein eigenes lokales Git-Repository mit dem Branch-Namen `main`. Als separates Ziel hat der Nutzer das bestehende Repository `sroroxd/ChatGPT-Codex-Connector` autorisiert. Dort werden Quellcode und die APK unter Release `v0.1.0` bereitgestellt. Es wurden keine Änderungen an einem Javis-Repository auf GitHub vorgenommen und kein zusätzliches GitHub-Repository erstellt.

Android-Paketname und Namespace bleiben `de.sprachpilot`. Die Projektregeln in `AGENTS.md` untersagen eine Vermischung mit Javis, insbesondere Remotes, Submodule und gemeinsam genutzte Signierschlüssel. Ergebnisdateien unter `artifacts/` sind von der Git-Historie ausgeschlossen.

## Tatsächlicher Installationsstatus

**Die APK wurde nicht auf dem Handy installiert.** Es ist keine bestätigte Geräteverbindung vorhanden. In der Cloud-Umgebung sind keine USB-Geräteknoten für das Handy sichtbar. Die verfügbare Plugin-Suche ergab keine passende Android-Fernsteuerungsanbindung.

Der ADB-Start wurde versucht, auch mit zusätzlicher Freigabe für sein Konfigurationsverzeichnis. Er scheitert an `Cannot mkdir '/home/agent/.android': Read-only file system`. Deshalb konnten weder Geräte aufgelistet noch `adb install` auf ein identifiziertes S24 Ultra ausgeführt werden. Eine Zustimmung im Chat schafft keine physische Geräteverbindung.

Der Installationsweg ist, die [APK aus dem GitHub-Release](https://github.com/sroroxd/ChatGPT-Codex-Connector/releases/download/v0.1.0/Sprachpilot-0.1.0.apk) auf dem S24 Ultra herunterzuladen, zu öffnen und den Android-Installationsdialog zu bestätigen. Erst danach ist der Gerätetest möglich. Die App benötigt kein Root und installiert nichts im Javis-Projekt.
