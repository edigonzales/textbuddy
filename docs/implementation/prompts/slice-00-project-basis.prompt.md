# Codex Prompt: Slice 00 Project Basis

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [slice-00-project-basis.md](/Users/stefan/sources/textbuddy/docs/implementation/slices/slice-00-project-basis.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Slice 00: Project Basis**.

## Globale Regeln

- Gradle mit **Groovy DSL**
- **Java 25**
- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Streaming ist grundsätzlich nur über **`SseEmitter`** zulässig
- Keine späteren Features oder Slices vorziehen

## In Scope

- Gradle mit **Groovy DSL**
- **Java 25** Toolchain
- Spring Boot 4.x mit **Spring MVC**
- JTE-Grundintegration
- HTMX-Einbindung in der Shell
- Startseite unter `GET /`
- leere, sichtbare Arbeitsoberfläche
- `frontend/`-Arbeitsbereich für die spätere Tiptap-Insel mit Vite + TypeScript
- Stub-Interfaces und Stub-Beans für die definierten Kernservices und Adapter
- Grundtests für App-Start und `GET /`

## Out of Scope

- alle Fachfeatures
- alle API-Endpunkte außer `GET /`
- echte Editorfunktion
- Auth
- externe HTTP-Integrationen

## Architekturregeln

- **Nur Spring MVC**
- **Kein WebFlux**
- **Kein `Flux`**
- **Kein `Mono`**
- Streaming ist in späteren Slices ausschließlich über **`SseEmitter`** erlaubt
- JTE rendert die Shell
- HTMX darf nur vorbereitet, aber noch nicht fachlich verwendet werden
- Spätere Features nicht vorwegnehmen

## Tests

- Unit-/Kontexttest für Start der Anwendung
- MockMvc-Test für `GET /`
- HTML-/View-Test für die sichtbaren Platzhalter

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Stubs oder bewusste Nicht-Implementierungen
- Vorschlag für den nächsten Slice
