# Codex Prompt: Slice 17 Document Import

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [slice-17-document-import.md](/Users/stefan/sources/textbuddy/docs/implementation/slices/slice-17-document-import.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Slice 17: Document Import**.

## Globale Regeln

- Gradle mit **Groovy DSL**
- **Java 25**
- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Streaming ist grundsätzlich nur über **`SseEmitter`** zulässig
- Keine späteren Features oder Slices vorziehen

## In Scope

- `POST /api/convert/doc`
- `DocumentConversionService`
- `DoclingClient`
- Upload- und Drag-and-Drop-UI
- HTML-Import in die Tiptap-Insel
- saubere Angleichung unterstützter Dateiformate zwischen UI und Backend

## Out of Scope

- Upload-Persistenz als Advisor-Dokument
- Auth

## Architekturregeln

- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Upload dient nur dem Editor-Import
- Keine Übernahme in `advisor/meta`, `advisor/rules` oder `advisor/docs`

## Tests

- Unit-Tests für MIME- oder Formatvalidierung
- MockMvc-Test für `POST /api/convert/doc`
- Browser-Test für Upload, Konvertierung und Einfügen in den Editor

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Stubs oder bewusste Nicht-Implementierungen
- Vorschlag für den nächsten Slice
