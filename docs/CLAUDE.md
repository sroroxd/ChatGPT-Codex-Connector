# Claude als zusätzlicher Entwicklungsassistent

Prüfstand 2026-11-09: Die Plugin-Suche in dieser Sitzung lieferte keine passende Claude-/Anthropic-Anbindung. `claude` ist auf dem Entwicklungsrechner nicht installiert; die Umgebungsübersicht meldet keine eingerichteten Anthropic-Zugangsdaten. Es wurde nichts angemeldet, kein API-Aufruf durchgeführt und kein neuer kostenpflichtiger Zugang eingerichtet.

Technisch praktikabel wäre der unveränderte **Claude Code** als zweiter Reviewer desselben lokalen Android-Projekts. Nach bestätigtem Zugang kann er Tests ausführen, die Audiositzung auf Rennen prüfen und Änderungen vorschlagen. Dafür ist keine Claude-Integration in die Handy-App nötig. Die Projektdatei `CLAUDE.md` enthält einen konkreten Reviewauftrag; Arbeit und Teststand sind damit bereits übergebbar.

Laut offizieller Dokumentation gibt es Anmeldung über berechtigte bestehende Claude-Abonnements sowie über die Claude Console/API und unterstützte Cloud-Provider. API-Nutzung wird nach Verbrauch abgerechnet. Zusätzliche Nutzungsguthaben und Abonnementgrenzen sind getrennt zu betrachten. Die bloße Existenz eines Accounts ist deshalb kein Nachweis kostenfreier Nutzung. Ein allgemeiner kostenloser API-Zugang wird nicht vorausgesetzt.

Vor einer Einrichtung muss geklärt sein, welchen vorhandenen Zugang der Nutzer einsetzen möchte und ob dabei zusätzliche Kosten erlaubt sind. Schlüssel gehören in die Umgebungs-Secrets, nicht in Chat oder Repository. Keine fremden Claude-Tokens aus dieser Umgebung übernehmen, keine Anmeldung umgehen und keinen Abo-Abschluss oder automatische Guthabenaufladung auslösen.

Alternativ lässt sich der vorbereitete Reviewauftrag mit dem Quellarchiv manuell in einem bereits vorhandenen Claude-Chat verwenden. Auch das übermittelt Code an einen externen Anbieter; persönliche Audioaufnahmen, Diagnoseexporte und spätere Gedächtnisinhalte sollen dabei nicht automatisch geteilt werden.

Quellen: [Authentifizierung](https://code.claude.com/docs/en/authentication), [Kosten](https://code.claude.com/docs/en/costs), [Nutzungsbedingungen und eigene Zugangsdaten](https://code.claude.com/docs/en/legal-and-compliance). Konditionen können sich ändern; vor tatsächlicher Einrichtung erneut am konkreten Zugang prüfen.
