# Abnahme auf dem Samsung Galaxy S24 Ultra

**Status: vollständig offen. Nicht als bereits durchgeführt lesen.** Alle Grenzwerte sind Ziele, keine gemessenen Eigenschaften der APK. Root ist nicht erforderlich. Android-/One-UI-Version und Modellnummer aus den Einstellungen notieren.

## Erster Durchlauf ohne Entwicklungswerkzeuge

1. APK installieren, lokale deutsche Stimme im voreingestellten TTS-Dienst prüfen. Flugmodus an, WLAN aus. App starten, Mikrofonfreigabe zunächst **ablehnen**: Aufnahme darf nicht starten. Danach bewusst erlauben.
2. Ohne Hintergrundmodus starten. Nach der Modellvorbereitung „Hallo, ich möchte heute einen Spaziergang machen“ sagen. Erkannten Text mit dem Gesagten vergleichen. Antwort muss deutsch und ohne Netz funktionieren.
3. Während der Antwort „Stopp, ich habe noch eine Frage“ sagen. Alte Antwort darf nach dem Abbruch nicht fortgesetzt werden. Neue Eingabe sollte einschließlich des ersten Wortes erkannt werden.
4. Zunächst mit USB-/Kabelkopfhörern testen, danach ohne Kopfhörer. Bluetooth gesondert prüfen, weil dessen Aufnahmeprofil und Latenz abweichen können. Bei Freisprechen muss die eigene Stimme der App ignoriert werden.
5. **Alles stoppen** drücken. Androids Mikrofonanzeige muss nach der Freigabe verschwinden. Wenn sie bleibt, App über Androids aktive Apps beenden und den Fehler notieren. Freigabeziel: unter einer Sekunde auf diesem Gerät.
6. Im normalen Modus App verlassen: Aufnahme und Ausgabe enden. Ohne Eingabe 30 Sekunden warten: Sitzung endet. Während des Sprechens Stoppen drücken, schnell erneut starten, Bildschirm drehen: kein Absturz und keine alte Antwort danach.
7. Hintergrundmodus aktivieren und sichtbar starten. Benachrichtigungsfreigabe zuerst verweigern: dieser Modus muss mit Erklärung blockiert werden. Danach erlauben. Home drücken, Bildschirm zwei Minuten sperren, erneut sprechen. Mit Stopptaste der Benachrichtigung beenden. Kein Neustart nach Wegwischen aus der Übersicht oder nach einem Neustart des Handys.
8. Während der Aufnahme den Android-Mikrofonschalter ausschalten bzw. die Berechtigung entziehen. Das System kann die Aufnahme zunächst stummschalten; sobald Android das meldet, soll die Sitzung enden. Nach erneutem Erlauben ist ein neuer manueller Start nötig. Mit Anruf und anderer Audio-App Fokusverlust prüfen.

Wenn eine lokale Stimme fehlt, muss eine verständliche Meldung erscheinen. Die App darf nicht still auf einen Netzwerkdienst wechseln. Die externe TTS-Einstellungsseite kann selbst Netz benötigen, um eine Stimme erstmals zu installieren; der eigentliche Test beginnt danach im Flugmodus.

## Reproduzierbare Messreihe

Pro Audioweg mindestens 20 normale Eingaben, 20 Unterbrechungen und 20 Antworten ohne eigenes Dazwischensprechen. Abstand und Lautstärke festhalten. Standardpause zuerst 700 ms, danach 400/1000/1400 ms testen. Ein Teil der Sätze soll kurze Denkpausen, Eigennamen, Zahlen und Umgangssprache enthalten. Die minimale Erkennungsqualität wird anhand derselben persönlichen Sätze später gegen Whisper verglichen.

| Eigenschaft | Messverfahren | Ziel für nächste Freigabe |
| --- | --- | --- |
| Offline-Funktion | Gesamten Durchlauf mit Flugmodus und ausgeschaltetem WLAN | Keine Cloud erforderlich |
| Tatsächlicher Ausgabeabbruch | Zweites Gerät zeichnet Stimme und Lautsprecher gemeinsam auf; vom hörbaren Einsatz der Person bis zum Ende der alten Ausgabe messen | P95 unter 300 ms anstreben |
| Fehlunterbrechungen durch Echo | 20 Antworten vollständig abspielen, selbst schweigen | 0/20 im ruhigen Raum |
| Sprechpausen | Satz mit 300-ms-Pause und danach weiterreden; dann längere Abschlussstille | Keine vorzeitige Antwort bei kurzen Pausen |
| Erkennung | Gesprochenen Text als Referenz separat notieren; Wortfehler und Absichtstreffer zählen | Kriterien nach persönlicher Basismessung festlegen |
| Stopp | Stopptaste drücken und Mikrofonindikator beobachten | Mikrofonfreigabe unter 1 s |
| Hintergrund | Gesperrter Bildschirm, Standard-Akkueinstellungen, anschließend Energiesparen separat | Mindestens 15 Minuten nachvollziehbarer Betrieb |
| Energie/Temperatur | 15 Minuten gemischte Nutzung, Akkustand und Erwärmung davor/danach festhalten | Kein instabiles Verhalten; Zahlen dokumentieren |

Die App exportiert auf ausdrücklichen Knopfdruck eine JSON-Diagnose ohne Gespräch und Audio. `stop_call_ms` misst nur den Binder-/API-Aufruf, `stop_callback_ms` einen TTS-Callback; weder Wert misst die letzte hörbare Silbe. `-1` bedeutet noch nicht gemessen. Die drei positiven VAD-Frames sind eine logische Bedingung, keine garantierte akustische Reaktionszeit. Die Aufzeichnung mit einem zweiten Gerät ist für den Nachweis nötig und bleibt unter deiner Kontrolle.

Ergebnisse in `tests/device-results-template.csv` oder als kurze Notiz festhalten. Erst nach diesen Tests entscheiden wir über Echo-Nachbesserungen, Vosk versus Whisper und ein lokales Sprachmodell. Bei Echo-Fehlern nicht durch künstlich langen Aufnahmeausschluss während der Antwort „reparieren“: Das würde die gewünschte Unterbrechung verhindern.

## Optional per ADB

USB-Debugging nur bei Bedarf aktivieren und den eigenen Entwicklungsrechner ausdrücklich auf dem Handy autorisieren. Kein Root, keine offenen ADB-Netzwerkports nötig.

```sh
adb devices -l
adb install -r artifacts/sprachpilot-0.1.0-debug-arm64.apk
adb shell dumpsys meminfo de.sprachpilot
```

Speicher vor und während des Hintergrundtests erfassen. Logcat nicht pauschal vollständig teilen; fremde Apps können dort private Informationen hinterlassen. Die exportierte App-Diagnose ist die bevorzugte erste Rückmeldung.
