# Deutsche synthetische Testaufnahme

`de-synthetic.wav`: 16 kHz, Mono, PCM 16 Bit; am 2026-11-09 lokal mit Piper 1.3.0 und `de_DE-thorsten-low` erzeugt. Text in `de-reference.txt`, Dauer 5,648 Sekunden. Keine Aufnahme einer Nutzerstimme und kein persönliches Gespräch.

Quelle des Modells: https://huggingface.co/rhasspy/piper-voices/tree/main/de/de_DE/thorsten/low . Die Modellkarte nennt den Thorsten-Datensatz unter CC0: https://huggingface.co/rhasspy/piper-voices/raw/main/de/de_DE/thorsten/low/MODEL_CARD . Der Datensatz stammt von https://github.com/thorstenMueller/Thorsten-Voice . Piper-Runtime und TTS-Modell werden nicht mit der Android-App ausgeliefert.

Diese kleine saubere Aufnahme prüft die grundsätzliche Datenverarbeitung; sie ersetzt keine menschlichen Stimmen, Dialekte, Raumgeräusche oder Lautsprecher-Echos. Die 21,43 % Wortfehler in diesem einzelnen Beispiel sind kein repräsentativer Benchmark. Tokenisierung zählt „Sprachassistent“ versus „sprach assistent“ als mehrere Wortfehler.
