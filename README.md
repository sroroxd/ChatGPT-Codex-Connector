# Sprachpilot · Android-Sprachprototyp

Entwickelt für das **Samsung Galaxy S24 Ultra ohne Root**. Stand: 9. November 2026. Version 0.1.0.

Der erste Meilenstein ist der überprüfbare Sprachkreislauf. Antworten sind feste lokale Testantworten mit Rücklesen des erkannten Textes. **Noch kein freier KI-Dialog.** Gedächtnis, App-Steuerung, Kamera und Cloud sind nicht implementiert.

## APK direkt herunterladen

**[Sprachpilot 0.1.0 für das S24 Ultra herunterladen](https://github.com/sroroxd/ChatGPT-Codex-Connector/releases/download/v0.1.0/Sprachpilot-0.1.0.apk)** · [Release mit Quellcode, Prüfsummen und Prüfberichten](https://github.com/sroroxd/ChatGPT-Codex-Connector/releases/tag/v0.1.0)

Auf dem Handy den Download öffnen und die Android-Installation bestätigen. Die APK ist eine Entwicklungsfassung und noch nicht auf einem echten S24 Ultra getestet. Die aktuelle SHA-256 steht im Release und in dessen `SHA256SUMS.txt`. Der GitHub-Release-Build läuft über `.github/workflows/release.yml`; die Prüfberichte sind angehängt. Sein Entwicklungsschlüssel unterscheidet sich vom früheren lokalen Build.

Dieses Repository ist das eigenständige **Sprachpilot-Projekt**. Es enthält keinen allgemeinen ChatGPT-Connector und gewährt keinen Fernzugriff auf dein Handy. Das Javis-Projekt bleibt davon getrennt. Modell-Dateien und Lizenztexte sind enthalten; die App muss keine Modelle nachladen.

## Auf dem Handy ausprobieren

1. Die APK über den Downloadlink oben herunterladen und öffnen. Die Installation aus dieser Quelle gegebenenfalls einmalig erlauben. Die APK ist eine signierte Entwicklungsfassung, kein Play-Store-Release. Root ist nicht nötig.
2. In der App **Android-Sprachausgabe öffnen**. Im ausgewählten TTS-Dienst muss eine **deutsche Offline-Stimme** installiert sein. Auf Samsung findet sich das üblicherweise unter *Einstellungen → Allgemeine Verwaltung → Text-zu-Sprache*. Die genaue Bezeichnung hängt von One UI und dem TTS-Anbieter ab. Eine eventuell nötige Sprachinstallation läuft über dessen Einstellungen; die App lädt nichts nach.
3. Flugmodus einschalten, WLAN ebenfalls ausschalten. Zunächst Kopfhörer verwenden. **Zuhören starten**, Mikrofon erlauben, die einmalige lokale Modellvorbereitung abwarten.
4. „Hallo, ich möchte heute einen Spaziergang machen“ sagen und eine kurze Pause lassen. Die App soll deutsch antworten. Während der Antwort deutlich „Stopp, ich habe noch eine Frage“ sagen. Die laufende Ausgabe soll abbrechen und die neue Eingabe erkannt werden.
5. Erst danach denselben Test über den Handy-Lautsprecher wiederholen. Falls sich die App ohne eigenes Sprechen unterbricht, ist das ein Echo-Fehler; bitte nicht als erfolgreichen Unterbrechungstest werten.
6. Den **Hintergrundmodus** nur bei Bedarf anwählen und erneut starten. Dafür verlangt diese App erlaubte Benachrichtigungen. Die dauerhafte Benachrichtigung hat eine Stopptaste. Der Modus startet nicht beim Booten oder nach einem Prozessabbruch.

Der normale Knopf startet einen Sprachzug. Das Mikrofon bleibt während dessen Antwort zum Unterbrechen aktiv. Nach vollständiger Ausgabe, beim Verlassen der App oder nach 30 Sekunden ohne Eingabe endet dieser Modus. **Alles stoppen** beendet Mikrofon und Ausgabe. Ein noch laufender nativer Verarbeitungsschritt kann kurz zum Freigeben der Ressourcen brauchen; der nächste Start wartet darauf.

**Noch nicht auf einem echten S24 Ultra getestet.** Es liegt weder ein Android-Emulatortest noch ein Nachweis für Echo-Unterdrückung, Bluetooth-Routing, Akkulaufzeit oder die hörbare Abbruchlatenz vor. Die bisher ausgeführten Prüfungen stehen in [TESTRESULTS.md](TESTRESULTS.md). Der konkrete [Gerätetestplan](docs/GERAETETEST.md) ist der nächste Schritt.

## Was enthalten ist

| Aufgabe | Umsetzung |
| --- | --- |
| Deutsche Streaming-Erkennung | Vosk Android 0.3.75; `vosk-model-small-de-0.15`, ca. 45 MB Download |
| Sprache und Sprechpause erkennen | Silero VAD 6.2.3; ONNX Runtime Android 1.31.1; 16 kHz Mono, 512 Samples je Frame |
| Unterbrechen | Drei aufeinanderfolgende Sprachframes lösen direkt `TextToSpeech.stop()` aus; die vollständige Transkription wird nicht abgewartet |
| Antwortbeginn | Einstellbare Stille von 400–1400 ms, Standard 700 ms; maximal 20 Sekunden pro Sprachsegment |
| Deutsche Stimme | Installierte Stimme des voreingestellten System-TTS-Diensts; nur als offline deklarierte und installierte deutsche Stimmen |
| Echo | `VOICE_COMMUNICATION`, Kommunikations-Audiomodus und Android `AcousticEchoCanceler`; Verfügbarkeit und Aktivierung werden angezeigt |
| Hintergrund | Aus der sichtbaren Activity gestarteter Mikrofon-Foreground-Service mit Stopptaste; kein automatischer Neustart |
| Datenschutz | Keine Internetberechtigung, keine Audiodateien, keine gespeicherten Transkripte, keine App-Telemetrie; ORT-Telemetrie explizit ausgeschaltet |

Die Systemstimme läuft in einem anderen Android-Prozess. Ihre Offline-Deklaration ist kein unabhängiges Audit des TTS-Anbieters. Der Flugmodus-Test prüft, ob die gesamte Funktion ohne Netz funktioniert. Die App gibt keinen automatischen Cloud-Fallback frei.

Android kann Mikrofonzugriff entziehen, Eingaben stummschalten oder Prozesse beenden. Die App reagiert auf Audiofokusverlust und auf vom System gemeldete Stummschaltung mit einem Sitzungsende. Samsung-Energiesparregeln bleiben wirksam; keine Akku-Ausnahme wird automatisch verlangt. Verhindert Android die Aufnahme, wird nichts mit Root oder versteckten Diensten umgangen.

## Selbst bauen

Voraussetzungen: JDK 17, Android SDK Plattform 35 und Build Tools 35.0.0. Der Wrapper verwendet Gradle 8.10.2 mit festem SHA-256; AGP 8.7.3. `ANDROID_HOME` auf das lokale SDK setzen oder `sdk.dir` in einer eigenen `local.properties` eintragen.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Die erste Entwicklungskompilierung benötigt Internet zum Laden der Build-Abhängigkeiten; die fertige App enthält ihre Modelle und benötigt es nicht. Das Projekt paketiert ausschließlich `arm64-v8a`, passend zum S24 Ultra. `compileSdk` und `targetSdk` sind bewusst 35, `minSdk` 31; die App beansprucht damit keinen Nachweis für sämtliche neueren Android-Versionen oder Store-Anforderungen. Abhängigkeiten sind fest versioniert.

Das Ergebnis liegt unter `app/build/outputs/apk/debug/app-debug.apk`. Der lokale Testschlüssel ist kein dauerhafter Veröffentlichungs-Schlüssel. Ein später mit anderem Schlüssel gebautes Paket kann die jetzige Testinstallation nicht direkt aktualisieren; vor einer Verteilung richten wir einen kontrollierten Signierprozess ein.

Host-Modelltest, separat vom Android-Build:

```sh
python3 -m venv .venv
.venv/bin/pip install -r tools/requirements-host.txt
.venv/bin/python tools/host_smoke.py tests/fixtures/de-synthetic.wav --output host-smoke.json
```

Die Testaufnahme ist synthetisch und sauber. Ihre Herkunft ist in `tests/fixtures/README.md` dokumentiert. Ein erfolgreicher Host-Test belegt keine Qualität mit deiner Stimme oder in deinem Raum.

Die [Recherche und technischen Entscheidungen](docs/ENTSCHEIDUNGEN.md) enthalten Quellen, Alternativen und die nächste Modellwahl. [Claude](docs/CLAUDE.md) ist als optionaler Entwicklungsreviewer vorbereitet, bisher ohne Installation, Login oder API-Aufrufe.
