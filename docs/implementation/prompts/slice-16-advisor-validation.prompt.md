# Codex Prompt: Slice 16 Advisor Validation

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [slice-16-advisor-validation.md](/Users/stefan/sources/textbuddy/docs/implementation/slices/slice-16-advisor-validation.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Slice 16: Advisor Validation**.

## Globale Regeln

- Gradle mit **Groovy DSL**
- **Java 25**
- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Streaming ist grundsätzlich nur über **`SseEmitter`** zulässig
- Keine späteren Features oder Slices vorziehen

## In Scope

- `POST /api/advisor/validate`
- Batch-Verarbeitung gegen statische Advisor-Regeln
- SSE-Streaming für Validierungsergebnisse
- clientseitige Deduplizierung
- Ergebnisliste mit Dokumentreferenz und Navigation

## Out of Scope

- Dokumentimport
- Auth

## Architekturregeln

- **Nur Spring MVC**
- Streaming ausschließlich mit **`SseEmitter`**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Advisor arbeitet nur gegen statische Referenzdokumente
- Keine Uploads als Regeldokumente behandeln

## Tests

- Unit-Tests für Batch-Aufteilung und Deduplizierung
- MockMvc-Test für `POST /api/advisor/validate`
- Browser-Test für Ergebnisstream und Anzeige

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Stubs oder bewusste Nicht-Implementierungen
- Vorschlag für den nächsten Slice
