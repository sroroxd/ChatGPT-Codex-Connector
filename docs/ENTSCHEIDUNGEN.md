# Technische Entscheidungen und Recherche

Stand: 2026-11-09. Quellen sind Dokumentation und Modellkarten, keine Messungen auf dem Handy des Nutzers.

## Spracherkennung

**Erste Messbasis: Vosk 0.3.75 + small-de-0.15.** Das Modell ist klein, Apache-2.0-lizenziert, für Android vorgesehen und kann ohne Server fortlaufend erkennen. Das erleichtert einen überprüfbaren Sprachprototyp mit vollständiger Kontrolle über denselben Mikrofonstrom für ASR und VAD. Wir verwenden nicht Androids allgemeinen SpeechRecognizer mit einem bloßen Offline-Wunsch, dessen tatsächlicher Dienst vom Gerät abhängt.

Die Herstellerliste nennt beim kleinen deutschen Modell 13,75 % Wortfehler auf Tuda-de und 30,67 % bei Podcasts. Das sind fremde Benchmarks mit anderen Bedingungen. Sie zeigen bereits Grenzen bei freier Sprache. Unser einzelner synthetischer Host-Test ist ebenfalls keine Qualitätsfreigabe. Vosk bleibt **vorläufige Funktionsbasis**, nicht die endgültige Entscheidung für natürliches freies Diktieren.

**Nächster A/B-Kandidat: whisper.cpp mit multilingualem Whisper base, bei Bedarf small und Quantisierung.** Beide laufen lokal; MIT-Lizenz. Kein `.en`-Modell für Deutsch. Höherer Speicherbedarf und Verarbeitung längerer Abschnitte können Antwortlatenz und Energie erhöhen; auf dem S24 müssen wir prüfen, ob bessere Erkennung den Aufwand rechtfertigt. sherpa-onnx ist eine weitere Runtime-Option für ONNX-ASR und VAD, würde aktuell aber eine zweite native Integration hinzufügen, ohne dass bereits eine bessere deutsche Messung auf diesem Handy vorliegt.

Quellen: [Vosk API](https://github.com/alphacep/vosk-api), [Modelle und Lizenzen](https://alphacephei.com/vosk/models), [aktuelle Android-Artefakte](https://repo.maven.apache.org/maven2/com/alphacephei/vosk-android/maven-metadata.xml), [whisper.cpp](https://github.com/ggml-org/whisper.cpp), [sherpa-onnx VAD](https://k2-fsa.github.io/sherpa/onnx/vad/silero-vad.html).

## Sprechpause und Unterbrechen

**Silero 6.2.3**, MIT-Lizenz, arbeitet auf 16-kHz-PCM. Das mitgelieferte ONNX-Modell wurde auf seine Ein- und Ausgaben geprüft; der unveränderte Java-Wrapper wurde mit Desktop-ORT 1.31.1 ausgeführt. Für jeden 512-Sample-Frame werden 64 Kontextsamples und der rekurrente Zustand übernommen.

Bei drei positiven Frames wird die Ausgabe gestoppt. Das entspricht 96 ms positiver VAD-Entscheidungen; Modellreaktion, Audiopuffer und TTS-Ausgabe kommen dazu. **Es gibt deshalb noch kein Versprechen von 96 ms hörbarer Latenz.** Die Sprechpause wird anhand der Samples gezählt und ist einstellbar. Kurze Erkennungslücken setzen den Stillstandszähler zurück, neue Eingaben invalidieren ausstehende Antworten über eine fortlaufende Generation und eine gemeinsame Ausgabesperre. Alte TTS-Callbacks dürfen einen neueren Sprachzug nicht abschließen.

Quelle: [Silero 6.2.3](https://github.com/snakers4/silero-vad/releases/tag/v6.2.3). Lizenz und Modell sind in den Assets enthalten.

## Stimme und Echo

Die erste Fassung nutzt eine installierte deutsche Offline-Systemstimme. Das spart einen zweiten TTS-Modellstapel und nutzt die auf dem Gerät schon vorhandenen Stimmen. Fehlt eine passende Stimme im gewählten TTS-Dienst, endet der Start mit einer erklärenden Meldung. Ein Download wird nicht automatisch ausgelöst.

Für AEC nutzt die App den Kommunikations-Aufnahmepfad und hängt den Android-Effekt an genau die AudioRecord-Sitzung. Die Ausgabe trägt Kommunikations-Audioattribute. Wenn keine externe Kommunikationsroute aktiv ist, wird der eingebaute Lautsprecher angefragt. Audiofokus und Route werden beim Ende freigegeben. **Ein aktivierter AEC-Effekt beweist nicht, dass das Echo ausreichend entfernt wird.** Wir testen Kopfhörer und Lautsprecher getrennt. WebRTC AEC3 wäre eine mögliche Nachbesserung, braucht aber eine korrekt ausgerichtete Referenz des tatsächlich abgespielten Audios; einfach nur eine Bibliothek einzubauen reicht nicht. Das ist bisher nicht implementiert.

Spätere Alternative bei unpassender Systemstimme: lokales Piper mit einzeln geprüfter deutscher Modelllizenz. Runtime- und Stimmenlizenzen müssen getrennt betrachtet werden; insbesondere aktuelle Piper-Runtimes sind nicht automatisch MIT. Das jetzt verwendete Piper 1.3.0 diente ausschließlich zur Erzeugung einer Host-Testaufnahme und wird nicht in der App ausgeliefert.

Quellen: [Voice API](https://developer.android.com/reference/android/speech/tts/Voice), [TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech), [Android Audio-Vorverarbeitung](https://source.android.com/docs/core/audio/implement-pre-processing).

## Kleine lokale Sprachmodelle

Noch keines wird mit der App ausgeliefert oder auf dem S24 als praktikabel bestätigt. Aktuelle Kandidaten für **Textbetrieb** über llama.cpp:

| Kandidat | Rolle im nächsten Vergleich | Lizenz |
| --- | --- | --- |
| Qwen3.5 0.8B, 4-bit GGUF | Kleine, schnelle Untergrenze; deutsche Befolgung und verlässliche strukturierte Befehle genau testen | Apache-2.0 |
| Qwen3.5 2B, 4-bit GGUF | Bevorzugter Qualitätskandidat, falls Speicher, Temperatur und Latenz genügen | Apache-2.0 |
| Qwen3 1.7B, 4-bit GGUF | Vergleich mit etablierter Transformer-Architektur bei Integrationsproblemen | Apache-2.0 |

Die endgültige Quantisierung wird mit Prüfsumme festgeschrieben. Erste Kontextgrenze: 2048 Tokens, kurze Antworten. Kein Bildprojektor oder dauerhaftes Vision-Modell im Hintergrund. CPU zuerst als reproduzierbare Basis; OpenCL/Vulkan/Hexagon erst nach Vergleich derselben Prompts und Modellversion. Die NPU ist nicht automatisch schneller oder qualitativ gleichwertig. Für Snapdragon 8 Gen 3 existieren passende Ansätze, aber fremde S23-, S25- oder Referenzgerätewerte gelten nicht als Messung des S24 Ultra.

Messgrößen: Zeit bis zum ersten Antworttext, Zeit bis zur ersten Sprachausgabe, Tokens/s, PSS/RSS, Energie über 15 Minuten, thermische Drosselung sowie deutsche Befehls- und Gesprächsqualität. Der Sprachprototyp muss während der Generierung weiter unterbrechbar bleiben. Ein kleineres Modell gewinnt nur bei ausreichender Qualität; riskante Aktionen werden nicht allein durch freie Modelltexte ausgeführt.

Quellen: [Qwen3.5 0.8B](https://huggingface.co/Qwen/Qwen3.5-0.8B), [Qualcomm Qwen3.5 2B](https://huggingface.co/qualcomm/Qwen3.5-2B), [Qwen3 und Lizenzen](https://qwenlm.github.io/blog/qwen3), [llama.cpp](https://github.com/ggml-org/llama.cpp), [Android-Artefakte](https://github.com/ggml-org/llama.cpp/releases). Hersteller- und Community-Benchmarks dienen nur der Kandidatenauswahl.

## Android und spätere Fähigkeiten

Ein Mikrofon-Foreground-Service wird aus der sichtbaren App nach Laufzeitfreigabe gestartet. Manifest: `RECORD_AUDIO`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, optional sichtbare Benachrichtigungen und Audioeinstellungen. Kein Boot-Receiver, keine Accessibility-Freigabe, keine Kamera, keine Hintergrund-Starttricks.

Android 12+ beschränkt Dienststarts aus dem Hintergrund; Android 14+ prüft zusätzlich die Mikrofonberechtigung beim Dienststart. Ein Gerät ohne Root kann diese Regeln einhalten, aber keine uneingeschränkte Daueraktivität garantieren.

Quellen: [Diensttypen](https://developer.android.com/develop/background-work/services/fgs/service-types), [Startbeschränkungen](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start), [Änderungen bis Android 16+](https://developer.android.com/develop/background-work/services/fgs/changes).

Nach bestandenen Sprachtests ist die Reihenfolge:

1. Lokales Textmodell hinter austauschbarer Antwortschnittstelle, Abbruch und begrenzte Laufzeit weiter erhalten.
2. Persönliches Gedächtnis nur auf ausdrücklichen Wunsch: einzeln ansehen, bearbeiten, löschen und exportieren; Herkunft und Aufbewahrung sichtbar. Lokale Datenbank und Keystore prüfen.
3. Wissenssuche mit Quellen; App-Aktionen zunächst über dokumentierte Android-Intents und App-Schnittstellen, mit Vorschau und Bestätigung folgenreicher Schritte.
4. „Sehen und Erklären“ über ausdrücklich gestartete Kamera- oder Bildschirmfreigabe. Keine dauerhafte Bildschirmaufnahme. Accessibility nur falls später konkret erforderlich und vom Nutzer bewusst aktiviert.
5. Kontrollierbarer Aufgabenplaner, Erinnerungen nach Android-Alarmregeln und separat gekoppelte Geräte. Kein Versprechen beliebiger Aktionen in fremden Apps ohne deren Unterstützung.

Cloud-Unterstützung kommt erst als einzeln freigegebene Anfrage mit sichtbarem Anbieter, Datenumfang und möglichem Preis dazu. Kein automatisches Hochladen von Mikrofon, Gedächtnis oder Bildschirm. Nachrichten bleiben nachrangig.
