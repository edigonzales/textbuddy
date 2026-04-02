# Codex Prompt: Slice 01 Editor Island Base

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [slice-01-editor-island-base.md](/Users/stefan/sources/textbuddy/docs/implementation/slices/slice-01-editor-island-base.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Slice 01: Editor Island Base**.

## Globale Regeln

- Gradle mit **Groovy DSL**
- **Java 25**
- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Streaming ist grundsätzlich nur über **`SseEmitter`** zulässig
- Keine späteren Features oder Slices vorziehen

## In Scope

- Tiptap-JavaScript-Insel im bestehenden Shell-Layout
- Plain-Text-Bearbeitung
- Undo/Redo
- Zeichen- und Wortzähler
- Hidden-Mirror-Feld für den aktuellen Text
- minimale Toolbar nur für diese Basisfunktionen
- Browser-Test für Tippen, Undo/Redo und Mirror

## Out of Scope

- Textkorrektur
- Rewrite
- Advisor
- Upload

## Architekturregeln

- Serverseitige Shell bleibt in JTE
- Editornahe Logik nur in der JS-Insel
- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Noch keine SSE-Nutzung
- Keine späteren Controls sichtbar machen

## Tests

- bestehende Servertests grün halten
- Browser-/UI-Test für Eingabe, Undo/Redo und Hidden Mirror

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Stubs oder bewusste Nicht-Implementierungen
- Vorschlag für den nächsten Slice
