# Codex Prompt: Slice 04 Sentence Rewrite

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [slice-04-sentence-rewrite.md](/Users/stefan/sources/textbuddy/docs/implementation/slices/slice-04-sentence-rewrite.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Slice 04: Sentence Rewrite**.

## Globale Regeln

- Gradle mit **Groovy DSL**
- **Java 25**
- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Streaming ist grundsätzlich nur über **`SseEmitter`** zulässig
- Keine späteren Features oder Slices vorziehen

## In Scope

- `POST /api/sentence-rewrite`
- `SentenceRewriteService`
- LLM-Adapter für Alternativsätze
- Satzfokus in der Editor-Insel
- Bubble-Menü für den fokussierten Satz
- Einsetzen einer gewählten Alternative

## Out of Scope

- Wort-Synonyme
- Quick Actions
- Advisor
- Upload

## Architekturregeln

- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- strukturierte Antwort, kein Streaming in diesem Slice
- Bubble-Menü nur für Sätze
- Keine Wortfunktionen oder Rewrite-Toolbar vorwegnehmen

## Tests

- Unit-Tests für Sentence-Rewrite-Service
- MockMvc-Test für `POST /api/sentence-rewrite`
- Browser-Test für Satzfokus, Optionen und Ersetzung

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Stubs oder bewusste Nicht-Implementierungen
- Vorschlag für den nächsten Slice
