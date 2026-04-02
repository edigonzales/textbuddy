# Codex Prompt: Slice 15 Advisor Catalog and PDF

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [slice-15-advisor-catalog-and-pdf.md](/Users/stefan/sources/textbuddy/docs/implementation/slices/slice-15-advisor-catalog-and-pdf.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Slice 15: Advisor Catalog and PDF**.

## Globale Regeln

- Gradle mit **Groovy DSL**
- **Java 25**
- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Streaming ist grundsätzlich nur über **`SseEmitter`** zulässig
- Keine späteren Features oder Slices vorziehen

## In Scope

- `GET /api/advisor/docs`
- `GET /api/advisor/doc/{name}`
- dateibasiertes `AdvisorDocumentRepository`
- statisches Metadatenmodell
- Advisor-Panel mit **Mehrfachauswahl**
- PDF-Anzeige oder PDF-Download

## Out of Scope

- `POST /api/advisor/validate`
- Upload
- Auth

## Architekturregeln

- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Noch keine SSE-Validierung
- Advisor bleibt auf statische Referenzdokumente beschränkt
- Keine Upload-Persistenz einführen

## Tests

- Unit-Test für Repository-Laden
- MockMvc-Tests für beide GET-Endpunkte
- Browser-Test für Dokumentliste und PDF-Erreichbarkeit

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Stubs oder bewusste Nicht-Implementierungen
- Vorschlag für den nächsten Slice
