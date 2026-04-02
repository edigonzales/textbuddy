# Codex Prompt: Slice 14 Quick Action Custom

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [slice-14-quick-action-custom.md](/Users/stefan/sources/textbuddy/docs/implementation/slices/slice-14-quick-action-custom.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Slice 14: Quick Action Custom**.

## Globale Regeln

- Gradle mit **Groovy DSL**
- **Java 25**
- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Streaming ist grundsätzlich nur über **`SseEmitter`** zulässig
- Keine späteren Features oder Slices vorziehen

## In Scope

- `POST /api/quick-actions/custom/stream`
- Custom-Prompt-Feld in der UI
- custom-spezifischer LLM-Service oder Prompt-Baustein
- sichere Validierung leerer oder unzulässiger Custom-Eingaben
- Wiederverwendung der bestehenden SSE- und Diff-Infrastruktur

## Out of Scope

- Advisor
- Upload
- Auth

## Architekturregeln

- **Nur Spring MVC**
- Streaming ausschließlich mit **`SseEmitter`**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Keine generische Action-Switch-API einführen
- Nur diese letzte Quick Action ergänzen

## Tests

- Unit-Tests für Custom-Request-Validierung und Prompt-Aufbereitung
- MockMvc-Test für `POST /api/quick-actions/custom/stream`
- Browser-Test für Custom-Eingabe, Stream und Ergebnis

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Stubs oder bewusste Nicht-Implementierungen
- Vorschlag für den nächsten Slice
