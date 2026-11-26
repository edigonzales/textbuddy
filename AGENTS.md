# Arbeitsregeln für Textbuddy

## Architektur und Textintegrität

- Java 25, Spring Boot MVC und JTE liefern eine Anwendung mit TypeScript/Tiptap. LanguageTool und Kreuzberg laufen eingebettet. Die LLM-Funktionen nutzen einen OpenAI-kompatiblen Adapter und einen kontrollierten Stub. Bewahre den bestehenden Funktionsumfang gemäss [Funktionen](docs/features.md). Erweiterungen folgen konkreten Aufträgen.
- Der unveränderte Plaintext ist die Referenz für Fundstellen. HTTP-Bereiche sind halb offen und in UTF-16. Prüfe die Abbildung auf den Editor bei Absätzen, Leerraum und Unicode. Dokumentimport und Zwischenablage setzen Plaintext in den Editor ein.
- Der Advisor verifiziert Fundstellen serverseitig und bezieht Quellen aus dem lokalen Katalog. Verwirf mehrdeutige Fundstellen. Weise verworfene oder widersprüchliche Befunde als unvollständige Prüfung aus. Der Browser verwendet nur disjunkte Ersatzbereiche und prüft den Textstand vor Vorschau und Übernahme.
- Erhalte OIDC, Autorisierung, CSRF, die explizite Loopback-Bindung ohne Anmeldung, Eingabegrenzen und sichere Fehlerausgaben. Bearbeitete Texte und Uploads werden serverseitig nicht dauerhaft gespeichert. Behandle den LLM-Provider als externe Vertrauensgrenze. Verwende synthetische Testdaten. Produktive Texte, Provider-Antwortkörper und Zugangsdaten gehören weder in Logs noch in Tests, Reviews oder Git.

## Änderungen und Prüfungen

- Prüfe Ausgangscommit, Arbeitsbaum und tatsächliche Aufrufer. Bewahre fremde Änderungen und passe Verträge mit allen Verbrauchern an. Bevorzuge nach Ockham, KISS und YAGNI Löschung, direkten Code und vorhandene Bibliotheksfunktionen. Jede Schicht braucht einen aktiven Verbraucher oder eine fachliche Schutz- oder Abhängigkeitsgrenze. Verwende die vorhandenen Build-, Test- und Review-Werkzeuge.
- Prüfe fachliche Ergebnisse und Fehlergrenzen auf der kleinsten geeigneten Ebene mit JUnit, Node oder Playwright. Führe Build und Browserprüfungen gemäss [README](README.md) aus, einschliesslich [OCR mit lokalen Sprachdaten](docs/operations.md#lokale-ocr). Nutze für LLM-Tests den Stub und lokale HTTP-Mocks. Weise fehlgeschlagene und übersprungene Pflichtprüfungen im Bericht aus.
- Wende [simplify-change](.agents/skills/simplify-change/SKILL.md) vor der Änderung und auf den vollständigen staged Diff an. Nutze für substanzielle Codeänderungen [review-change](.agents/skills/review-change/SKILL.md) mit einem unabhängigen, lesenden OpenCode-2.x-Review über die DeepSeek-API mit `deepseek/deepseek-flash`. Lass begründete Korrekturen gezielt nachprüfen. Bei bestätigtem Quota- oder Guthabenlimit dokumentiere `SKIPPED_QUOTA` und prüfe den vollständigen Diff selbst.
- Committe abgeschlossene Änderungen als in sich geschlossene Einheiten. Push, Merge, Release und produktive Aufrufe brauchen einen ausdrücklichen Auftrag.
