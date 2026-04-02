# Codex Prompt: Slice 11 Quick Action Social Media

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [slice-11-quick-action-social-media.md](/Users/stefan/sources/textbuddy/docs/implementation/slices/slice-11-quick-action-social-media.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Slice 11: Quick Action Social Media**.

## Globale Regeln

- Gradle mit **Groovy DSL**
- **Java 25**
- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Streaming ist grundsätzlich nur über **`SseEmitter`** zulässig
- Keine späteren Features oder Slices vorziehen

## In Scope

- `POST /api/quick-actions/social-media/stream`
- social-media-spezifischer LLM-Service oder Prompt-Baustein
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
- nur die Social-Media-Aktion zusätzlich freischalten

## Tests

- Unit-Tests für Social-Media-Service oder Prompt-Aufbereitung
- MockMvc-Test für `POST /api/quick-actions/social-media/stream`
- Browser-Test für die neue Aktion inklusive Stream und Diff

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Stubs oder bewusste Nicht-Implementierungen
- Vorschlag für den nächsten Slice
