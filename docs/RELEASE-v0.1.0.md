Erste installierbare Testfassung von Sprachpilot für das Samsung Galaxy S24 Ultra ohne Root. Eigenständiges Projekt, getrennt von Javis.

## Installation

1. Unter **Assets** die Datei **Sprachpilot-0.1.0.apk** herunterladen, auf dem Handy öffnen und die Android-Installation bestätigen.
2. Im gewählten Android-TTS-Dienst eine deutsche Offline-Stimme installieren bzw. auswählen.
3. Im Flugmodus zunächst mit Kopfhörern testen, anschließend über den Lautsprecher.

Die APK enthält Vosk mit deutschem Modell und Silero-VAD. Sie hat keine Internetberechtigung. Einstellbare Sprechpausen, Knopfmodus, sichtbar gestarteter Hintergrundmodus und Stopptasten sind enthalten. Antworten sind feste lokale Testantworten; noch kein freier KI-Dialog.

## Prüfstand

Diese APK wurde auf einem öffentlichen GitHub-Actions-Standard-Runner gebaut und vor Veröffentlichung geprüft: Build, drei JUnit-Tests, Android Lint, deutscher Host-Modelltest, tatsächlicher Java-VAD-Code, APK-Signatur, finale Berechtigungen und 16-KiB-Alignment. Prüfberichte und Prüfsummen sind angehängt. Der Quellcode enthält die Modelle und Lizenztexte.

**Noch nicht auf einem echten S24 Ultra oder in einem Android-Emulator gestartet.** Echo-Unterdrückung, tatsächlich hörbare Abbruchlatenz, Mikrofon- und Hintergrundbetrieb müssen auf dem Gerät geprüft werden. Die APK ist eine mit einem Entwicklungsschlüssel signierte Testversion; keine Play-Store-Veröffentlichung. Der CI-Entwicklungsschlüssel unterscheidet sich vom früheren lokalen Testschlüssel. Eine damit bereits installierte lokale Fassung müsste vor Installation dieser Testfassung entfernt werden.

SHA-256 der veröffentlichten APK: `{{APK_SHA256}}`.

`TESTRESULTS.md` dokumentiert den ursprünglichen lokalen Build; die angehängten Prüfberichte gelten für diese auf GitHub gebaute APK. Anleitung und Gerätetestplan befinden sich im Repository.
