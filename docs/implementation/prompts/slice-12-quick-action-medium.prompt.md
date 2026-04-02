# Codex Prompt: Slice 12 Quick Action Medium

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [slice-12-quick-action-medium.md](/Users/stefan/sources/textbuddy/docs/implementation/slices/slice-12-quick-action-medium.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Slice 12: Quick Action Medium**.

## Globale Regeln

- Gradle mit **Groovy DSL**
- **Java 25**
- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Streaming ist grundsätzlich nur über **`SseEmitter`** zulässig
- Keine späteren Features oder Slices vorziehen

## In Scope

- `POST /api/quick-actions/medium/stream`
- Medium-Option als expliziter Request-Parameter oder dedizierter Typ gemäß Slice-Spezifikation
- medium-spezifischer LLM-Service oder Prompt-Baustein
- UI-Steuerung für genau diese zusätzliche Aktion

## Out of Scope

- alle späteren Quick Actions
- Advisor
- Upload

## Architekturregeln

- **Nur Spring MVC**
- Streaming ausschließlich mit **`SseEmitter`**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Option nur für diese Aktion modellieren, nicht allgemein vorziehen

## Tests

- Unit-Tests für Medium-Service oder Prompt-Aufbereitung
- MockMvc-Test für `POST /api/quick-actions/medium/stream`
- Browser-Test für Aktion, Option und Stream-Ergebnis

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Stubs oder bewusste Nicht-Implementierungen
- Vorschlag für den nächsten Slice
