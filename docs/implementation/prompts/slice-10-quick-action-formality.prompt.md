# Codex Prompt: Slice 10 Quick Action Formality

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [slice-10-quick-action-formality.md](/Users/stefan/sources/textbuddy/docs/implementation/slices/slice-10-quick-action-formality.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Slice 10: Quick Action Formality**.

## Globale Regeln

- Gradle mit **Groovy DSL**
- **Java 25**
- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Streaming ist grundsätzlich nur über **`SseEmitter`** zulässig
- Keine späteren Features oder Slices vorziehen

## In Scope

- `POST /api/quick-actions/formality/stream`
- Formality-Option als expliziter Request-Parameter oder dedizierter Typ gemäß Slice-Spezifikation
- formality-spezifischer LLM-Service oder Prompt-Baustein
- UI-Steuerung für genau diese zusätzliche Aktion

## Out of Scope

- alle späteren Quick Actions
- Advisor
- Upload

## Architekturregeln

- **Nur Spring MVC**
- Streaming ausschließlich mit **`SseEmitter`**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Option nur für diese Aktion modellieren, nicht vorab für andere verallgemeinern

## Tests

- Unit-Tests für Formality-Service oder Prompt-Aufbereitung
- MockMvc-Test für `POST /api/quick-actions/formality/stream`
- Browser-Test für Aktion, Option und Stream-Ergebnis

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Stubs oder bewusste Nicht-Implementierungen
- Vorschlag für den nächsten Slice
