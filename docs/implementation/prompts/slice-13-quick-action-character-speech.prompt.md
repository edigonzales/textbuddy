# Codex Prompt: Slice 13 Quick Action Character Speech

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [slice-13-quick-action-character-speech.md](/Users/stefan/sources/textbuddy/docs/implementation/slices/slice-13-quick-action-character-speech.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Slice 13: Quick Action Character Speech**.

## Globale Regeln

- Gradle mit **Groovy DSL**
- **Java 25**
- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Streaming ist grundsätzlich nur über **`SseEmitter`** zulässig
- Keine späteren Features oder Slices vorziehen

## In Scope

- `POST /api/quick-actions/character-speech/stream`
- character-speech-spezifischer LLM-Service oder Prompt-Baustein
- UI-Steuerung für genau diese zusätzliche Aktion
- Wiederverwendung der bestehenden SSE- und Diff-Infrastruktur

## Out of Scope

- spätere Custom-Quick-Action
- Advisor
- Upload

## Architekturregeln

- **Nur Spring MVC**
- Streaming ausschließlich mit **`SseEmitter`**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- nur die Character-Speech-Aktion zusätzlich freischalten

## Tests

- Unit-Tests für Character-Speech-Service oder Prompt-Aufbereitung
- MockMvc-Test für `POST /api/quick-actions/character-speech/stream`
- Browser-Test für die neue Aktion inklusive Stream und Diff

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Stubs oder bewusste Nicht-Implementierungen
- Vorschlag für den nächsten Slice
