# Codex Prompt: Slice 09 Quick Action Summarize

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [slice-09-quick-action-summarize.md](/Users/stefan/sources/textbuddy/docs/implementation/slices/slice-09-quick-action-summarize.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Slice 09: Quick Action Summarize**.

## Globale Regeln

- Gradle mit **Groovy DSL**
- **Java 25**
- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Streaming ist grundsätzlich nur über **`SseEmitter`** zulässig
- Keine späteren Features oder Slices vorziehen

## In Scope

- `POST /api/quick-actions/summarize/stream`
- summarize-spezifischer LLM-Service oder Prompt-Baustein
- UI-Steuerung für genau diese zusätzliche Aktion
- Wiederverwendung der bestehenden SSE- und Diff-Infrastruktur

## Out of Scope

- alle späteren Quick Actions
- Advisor
- Upload

## Architekturregeln

- **Nur Spring MVC**
- Streaming ausschließlich mit **`SseEmitter`**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- nur die Summarize-Aktion zusätzlich freischalten

## Tests

- Unit-Tests für Summarize-Service oder Prompt-Aufbereitung
- MockMvc-Test für `POST /api/quick-actions/summarize/stream`
- Browser-Test für die neue Aktion inklusive Stream und Diff

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Stubs oder bewusste Nicht-Implementierungen
- Vorschlag für den nächsten Slice
