# Codex Prompt: Phase 01 Produktiver Kern

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [phase-01-produktiver-kern.md](/Users/stefan/sources/textbuddy/docs/implementation/phases/phase-01-produktiver-kern.md)
- [05-phase-index.md](/Users/stefan/sources/textbuddy/docs/implementation/05-phase-index.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Phase 01: Produktiver Kern**.

## Globale Regeln

- Gradle mit **Groovy DSL**
- **Java 25**
- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Streaming ist grundsätzlich nur über **`SseEmitter`** zulässig
- Keine spätere Phase vorziehen

## Sprachregel

- Verwende in allen deutschsprachigen, nutzersichtbaren Texten echte Umlaute (`ä`, `ö`, `ü`, `Ä`, `Ö`, `Ü`)
- Verwende keine ASCII-Umschreibungen wie `ae`, `oe`, `ue` in UI-Texten, Fehlermeldungen, Prompts und JTE-Texten
- Technische Kennungen, API-Feldnamen, Endpoints, Dateinamen, CSS-Klassen und Test-IDs bleiben ASCII
- Schweizer Rechtschreibung mit `ss` bleibt zulässig

## In Scope

- produktive LLM-Konfiguration und Provider-Facade
- Portierung der produktiven Runtime-Prompts
- Ersetzung aller produktiven LLM-Stubs
- eingebettetes LanguageTool
- eingebetteter Dokumentimport über Kreuzberg
- Sprachwahl Ende-zu-Ende
- `POST /api/sentence-rewrite` mit optionalem `context`
- produktive Advisor-Validierung über LLM-Batches

## Out of Scope

- OCR für gescannte Dokumente
- PDF-Viewer im Browser
- Flesch-Statistik
- PWA, Onboarding und volle I18n-Parität

## Architekturregeln

- **Nur Spring MVC**
- Streaming ausschließlich mit **`SseEmitter`**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- LanguageTool und Dokumentimport laufen im Standardpfad ohne externe Sidecars
- Stub-Modi bleiben höchstens als explizite Test- oder lokale Fallback-Profile erhalten
- Prompts gehören nicht inline in Controller oder Stub-Konfiguration

## Tests

- Unit-Tests für Prompt-Rendering, Parsing, Retry und LLM-Adapter
- MockMvc-Tests für alle betroffenen Endpoints
- Integrations-Tests für eingebettetes LanguageTool
- Import-Tests für `pdf`, `docx`, `md`, `txt`
- Smoke-Test für `java -jar` ohne LanguageTool- und Docling-Sidecars

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Punkte oder bewusste Nicht-Implementierungen
- Vorschlag für die nächste Phase
