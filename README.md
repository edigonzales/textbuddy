# Textbuddy

Textbuddy ist eine Schreibassistenz mit lokaler LanguageTool-Korrektur, Richtlinien-Advisor, **Verständlicher schreiben** und fünf Zusammenfassungsvarianten. Der Editor bietet Dokumentimport mit lokaler OCR, Textstatistik und DOCX-Export. Spring Boot rendert die Oberfläche mit JTE und bindet den TypeScript-/Tiptap-Editor ein.

## Entwickeln

Voraussetzungen sind Java 25, Node.js ab 20.19 und npm. `./dev.sh` startet Textbuddy auf [127.0.0.1:8080](http://127.0.0.1:8080) mit Stub-LLM und ohne OIDC-Anmeldung. Nach Änderungen an der Anwendung das Skript neu starten.

## Prüfen und paketieren

Die reguläre Suite prüft echte OCR mit den [lokalen Sprachdaten](docs/operations.md#lokale-ocr). Playwright startet anschliessend das gebaute JAR im selben Loopback-Modus.

```bash
./gradlew clean test installerZip
npm ci --prefix playwright
npx --prefix playwright playwright install chromium
npm test --prefix playwright
```

Die Auslieferung liegt unter `build/libs/textbuddy.jar` und `build/distributions/textbuddy-*.zip`. Der Betrieb mit OIDC und einem OpenAI-kompatiblen Provider ist unter [Betrieb und Konfiguration](docs/operations.md) beschrieben.

- [Funktionen](docs/features.md)
- [Architektur](docs/architecture.md)
- [Eigene Advisor-Regelwerke erstellen](docs/advisor-authoring.md)
- [Barrierefreiheit](docs/accessibility.md)
- [Archivstand vor der Bereinigung](https://github.com/edigonzales/textbuddy/tree/archive/pre-rrb-cleanup-2026-11-25)
