# Ausgeführte Prüfungen

Diese Datei dokumentiert den ursprünglichen **lokalen Build**. Eine auf GitHub Actions erzeugte Release-APK wird dort erneut geprüft und erhält einen eigenen Entwicklungsschlüssel und eine andere Prüfsumme. Für sie gelten die am GitHub-Release angehängten aktuellen Prüfberichte und `SHA256SUMS.txt`.

Stand: 2026-11-09. Ergebnis gilt für `sprachpilot-0.1.0-debug-arm64.apk`, SHA-256 `3494fb905045b58b0754ff8f2f3b66adb5fe6ff02e6c900aa368fb7fd51ee525`, 95.912.156 Bytes (91,47 MiB).

**Keine dieser Prüfungen lief auf einem echten S24 Ultra. Die Android-App wurde bisher auch nicht in einem Emulator gestartet.** Damit sind deutsche Systemstimme, Mikrofonaufnahme, Echo-Verhalten, sichtbarer Hintergrundbetrieb und akustische Unterbrechung noch nicht zur Nutzung freigeprüft.

| Prüfung | Ergebnis | Aussagegrenze |
| --- | --- | --- |
| Android-Debug-Build | Bestanden, JDK 17 / AGP 8.7.3 / Gradle 8.10.2 / SDK 35 | Kompilierung und Paketierung, kein App-Start |
| JUnit | 3 Tests, 0 Fehler | Kurze Geräuschimpulse, Sprechpause mit Zwischenrede und maximale Segmentdauer; keine Android-Lebenszyklustests |
| Android Lint | 0 Fehler, 1 Warnung | Warnung: fehlendes x86_64 für ChromeOS; absichtlich nur ARM64 für das S24 Ultra |
| Java-VAD-Integration | Bestanden | Tatsächliches `SileroVad.java` mit Desktop-ONNX-Runtime 1.31.1 und Modell 6.2.3; kein Android-JNI-Test |
| Deutscher Modell-Smoke-Test | Bestanden | Synthetische saubere Aufnahme auf x86_64; Python-Vosk 0.3.45 und ORT 1.20.1, nicht die Android-Integrationen |
| Modell-Prüfsummen | 16 Dateien stimmen überein | Prüft mitgelieferte Dateien, keine semantische Qualitätsaussage |
| Fertiges APK-Manifest | Genau 5 erwartete Berechtigungen | `INTERNET` und `ACCESS_NETWORK_STATE` nach Manifest-Merge ausdrücklich entfernt |
| APK-Signatur | Verifiziert, v2, Entwicklungsschlüssel | Kein Veröffentlichungs-/Play-Store-Schlüssel |
| Native ARM64-Bibliotheken | Alle LOAD-Segmente ≥ 16 KiB, ZIP-Alignment für 16 KiB geprüft | Keine tatsächliche Installation auf einem 16-KiB-Android-System |

Beim Lint-Lauf wurde ein zu neuer `InputStream.readAllBytes`-Aufruf für minSdk 31 entdeckt und ersetzt. Außerdem wurden Dateistream-Freigabe, Audiofokusverlust, System-Stummschaltung, Bildschirm-Inset-Abstände und Schutz gegen alte TTS-Callbacks nachgebessert. Die endgültige APK enthält diese Änderungen.

Die Paketkontrolle hat von einer Bibliothek eingeschleuste Netzwerkberechtigungen gefunden. Sie werden jetzt über Manifest-Merge-Regeln entfernt. `tools/check_apk.py` prüft die endgültigen APK-Berechtigungen, die ARM64-Ausrichtung und die Signatur. Der letzte Build lief mit bereits geladenen Abhängigkeiten im Gradle-Offline-Modus erfolgreich.

## Host-Audiotest

Die 5,648 Sekunden lange Aufnahme lautet:

> Hallo, ich möchte heute einen Spaziergang machen. Bitte erkläre mir, wie dieser Sprachassistent funktioniert.

Erkannt wurde:

> hallo ich möchte heute ein spaziergang machen bitte erkläre mir wie dieser sprach assistent funktioniert

Die Wortfehlerrate dieses winzigen synthetischen Beispiels beträgt nach einfacher Worttokenisierung 21,43 %. Unter anderem zählt das getrennte „sprach assistent“ mehrfach. Das ist kein allgemeiner Qualitätswert und kein geeigneter Grund, Vosk bereits als endgültige Gesprächserkennung festzulegen.

Stille: maximale VAD-Wahrscheinlichkeit 0,0089 bei Schwelle 0,60. Deutsche Sprache: 157 positive Frames. Im Python-Host-Lauf: VAD-P95 0,50 ms je 32-ms-Frame; ASR-Verarbeitung P95 26,34 ms; Pipeline-Echtzeitfaktor 0,098; Peak-Prozess-RSS 283,9 MiB. **Diese Werte dürfen nicht als S24-Leistung angegeben werden.** Sie enthalten weder Mikrofon und Lautsprecher noch Androids Systemstimme. Der zusätzliche Java-Test bestätigt dieselben 157 Sprachframes mit der in Android vorgesehenen ORT-Versionslinie.

## Belege und Wiederholung

- `artifacts/build-check.txt`: endgültiger Gradle-Build.
- `artifacts/unit-tests.xml`: JUnit-Ergebnis.
- `artifacts/lint.txt`: vollständige Lint-Zusammenfassung.
- `artifacts/host-smoke.json`: Host-Modelltest einschließlich Referenz und Transkript.
- `artifacts/java-vad-check.txt`: Java-VAD-Integration.
- `artifacts/apk-check.json`: Paketgröße, Prüfsumme, Berechtigungen, Signatur- und Alignment-Prüfung.
- `models.lock.json`: Modellherkunft und SHA-256 je Datei.

Die Prüfskripte sind im Quellarchiv enthalten. Die Ergebnisdateien liegen zusätzlich neben der APK im Arbeitsverzeichnis. Der nächste Test ist der [physische Gerätetest](docs/GERAETETEST.md). Erst danach kann die Eignung auf dem S24 Ultra beurteilt werden.
