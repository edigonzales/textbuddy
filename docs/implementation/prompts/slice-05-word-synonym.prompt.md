# Codex Prompt: Slice 05 Word Synonym

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [slice-05-word-synonym.md](/Users/stefan/sources/textbuddy/docs/implementation/slices/slice-05-word-synonym.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Slice 05: Word Synonym**.

## Globale Regeln

- Gradle mit **Groovy DSL**
- **Java 25**
- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Streaming ist grundsätzlich nur über **`SseEmitter`** zulässig
- Keine späteren Features oder Slices vorziehen

## In Scope

- `POST /api/word-synonym`
- `WordSynonymService`
- LLM-Adapter für kontextbezogene Synonyme
- Wortfokus in der Editor-Insel
- Bubble-Menü für das fokussierte Wort
- Ersetzen des Wortes durch ein gewähltes Synonym

## Out of Scope

- weitere Rewrite-Arten
- Quick Actions
- Advisor
- Upload

## Architekturregeln

- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Bubble-Menü nur für Wörter
- Noch keine SSE-Quick-Actions

## Tests

- Unit-Tests für Word-Synonym-Service
- MockMvc-Test für `POST /api/word-synonym`
- Browser-Test für Wortfokus, Synonymliste und Ersetzung

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Stubs oder bewusste Nicht-Implementierungen
- Vorschlag für den nächsten Slice
