# Codex Prompt: Slice 02 Text Correction Base

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [slice-02-text-correction-base.md](/Users/stefan/sources/textbuddy/docs/implementation/slices/slice-02-text-correction-base.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Slice 02: Text Correction Base**.

## Globale Regeln

- Gradle mit **Groovy DSL**
- **Java 25**
- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Streaming ist grundsätzlich nur über **`SseEmitter`** zulässig
- Keine späteren Features oder Slices vorziehen

## In Scope

- `POST /api/text-correction`
- LanguageTool-Client als externer Adapter
- `TextCorrectionService`
- Problems-Panel an oder in der Editor-Insel
- Korrekturmarken im Editor
- Anwenden eines Vorschlags
- Default-Sprache `auto`

## Out of Scope

- lokales Wörterbuch
- inkrementelle Satzdiff-Logik
- Satz-Rewrite
- Wort-Synonyme
- Quick Actions
- Advisor
- Upload

## Architekturregeln

- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Controller schlank halten, Fachlogik in Service und Adapter
- Editorinteraktion in der JS-Insel umsetzen
- Keine späteren Korrekturerweiterungen vorwegnehmen

## Tests

- Unit-Tests für Mapping und Service-Logik
- MockMvc-Test für `POST /api/text-correction`
- Browser-Test für Markierung und Vorschlagsanwendung

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Stubs oder bewusste Nicht-Implementierungen
- Vorschlag für den nächsten Slice
