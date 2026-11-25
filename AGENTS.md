# Arbeitsregeln für Textbuddy

## Produktgrenze

- Erhalte den Plaintext-Editor mit Undo/Redo und Zwischenablage, automatische Rechtschreib- und Grammatikprüfung mit Vorschlägen und persönlichem Wörterbuch sowie die bestehende Statistik mit deutschem Amstad-Flesch.
- Erhalte verständlicher schreiben, alle fünf Zusammenfassungsvarianten und den Advisor mit Regelauswahl, Fundstellen, Erläuterungen, Quellen und kontrollierter Korrektur. Die gemeinsame Änderungsvorschau erlaubt Annahme und Ablehnung.
- Erhalte die angebotenen Dokumentimporte einschliesslich OCR auf der unterstützten Laufzeit und den DOCX-Export von Text und Absätzen. RRB-Fachregeln, Rechtsquellenabgleich, neue Regelverwaltung und layouttreue Word-Bearbeitung liegen ausserhalb des Auftrags.

## Architektur und Grenzen

- Java 25, Spring Boot MVC und JTE liefern eine Anwendung mit TypeScript/Tiptap im Browser. Gradle Groovy und npm/Vite bauen dieselbe Anwendung. Advisor-Regeln und PDF-Quellen sind gebündelt, das Wörterbuch liegt im Browser. Der Server speichert bearbeitete Texte und Uploads nicht dauerhaft.
- Eingebettetes LanguageTool und lokales Kreuzberg prüfen und importieren Texte im Textbuddy-Prozess. Für LLM-Funktionen gibt es einen OpenAI-kompatiblen Adapter und einen kontrollierten Stub.
- Erhalte OIDC, Autorisierung, CSRF, explizite Loopback-Bindung bei ausgeschalteter Anmeldung, Text- und Uploadgrenzen sowie sichere Fehlerausgaben. Behandle die LLM-Anbindung als externe Vertrauensgrenze. Verwende synthetische Testdaten. Produktive Texte, Provider-Antwortkörper und Zugangsdaten gehören weder in Logs noch in Tests, Reviews oder Git.
- Der unveränderte Plaintext ist die Referenz für Fundstellen. HTTP-Bereiche sind halb offen und in UTF-16. Prüfe die Abbildung auf Tiptap-Positionen bei Absätzen, Leerraum und Unicode. Verwirf veraltete oder mehrdeutige Ergebnisse und begrenze Advisor-Korrekturen auf die ausgewählten Fundstellen.

## Änderungen und Prüfungen

- Prüfe Arbeitsbaum, Basis und tatsächliche Aufrufer. Bewahre fremde Änderungen. Bevorzuge nach Ockham und YAGNI Löschung, direkten Code und vorhandene Bibliotheksfunktionen. Jede Schicht braucht einen aktiven Verbraucher oder eine echte Schutz- oder Abhängigkeitsgrenze. Passe gemeinsam genutzte Verträge und alle Aufrufer zusammen an. Legacy-Aliase, auf Vorrat gebaute Erweiterungspunkte und eigene Build-, Test- oder Nachweismechanik entfallen.
- Verwende Java 25 und Node.js ab 20.19. `./dev.sh` startet lokal auf Loopback mit Stub-LLM. `./gradlew test` prüft Java und Frontend. Für Browserprüfungen nutze `npm ci --prefix playwright`, `npx --prefix playwright playwright install chromium` und `npm test --prefix playwright`. `./gradlew clean verifyReleaseBundle installerZip` prüft und erstellt die Auslieferung. Für verpflichtende reale OCR-Prüfungen setze `TEXTBUDDY_REQUIRE_OCR_TESTS=true` und verwende die OCR-Laufzeit der CI.
- Wähle die kleinste aussagekräftige Prüfung mit JUnit, Node oder Playwright. Tests prüfen reale Ergebnisse und Fehlergrenzen. Nutze den Stub und lokale Mock-HTTP-Antworten für LLM-Prüfungen. Ein übersprungener, nicht ausgeführter oder fehlgeschlagener Pflichtlauf bleibt im Bericht sichtbar.
- Wende [simplify-change](.agents/skills/simplify-change/SKILL.md) vor der Änderung und auf den fertigen Diff an. Prüfe pro Bereinigungsphase den vollständigen staged Diff, nutze [review-change](.agents/skills/review-change/SKILL.md) für einen unabhängigen, lesenden OpenCode-2.x-Review über die DeepSeek-API mit DeepSeek Flash und lass berechtigte Korrekturen gezielt nachprüfen. Bei bestätigtem Quota- oder Guthabenlimit dokumentiere `SKIPPED_QUOTA` und führe die eigene Prüfung vollständig aus.
- Committe jede abgeschlossene Phase separat. Push, Merge, Release und produktive Aufrufe brauchen einen ausdrücklichen Auftrag.
