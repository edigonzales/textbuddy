# Codex Prompt: Slice 06 Quick Action Infrastructure

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [slice-06-quick-action-infrastructure.md](/Users/stefan/sources/textbuddy/docs/implementation/slices/slice-06-quick-action-infrastructure.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Slice 06: Quick Action Infrastructure**.

## Globale Regeln

- Gradle mit **Groovy DSL**
- **Java 25**
- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Streaming ist grundsätzlich nur über **`SseEmitter`** zulässig
- Keine späteren Features oder Slices vorziehen

## In Scope

- gemeinsamer SSE-Client in der Editor-Insel
- gemeinsamer Streaming-Handler
- Diff-Grundlage für Rewrite-Ergebnisse
- `POST /api/quick-actions/plain-language/stream`
- Plain-Language-LLM-Service
- Rewrite-Bereich mit genau einem aktiven Button
- kompletter Undo des Rewrite-Ergebnisses

## Out of Scope

- alle weiteren Quick Actions
- Advisor
- Upload

## Architekturregeln

- **Nur Spring MVC**
- Streaming ausschließlich mit **`SseEmitter`**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- SSE-Eventtypen nur `chunk`, `complete`, `error`
- Keine weiteren Quick-Action-Endpoints anlegen

## Tests

- Unit-Tests für SSE-Payload-Erzeugung
- MockMvc-Test für `POST /api/quick-actions/plain-language/stream`
- Browser-Test für Stream-Verarbeitung, Diff und Undo

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Stubs oder bewusste Nicht-Implementierungen
- Vorschlag für den nächsten Slice
