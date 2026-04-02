# Codex Prompt: Slice 03 Text Correction Enhancements

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [slice-03-text-correction-enhancements.md](/Users/stefan/sources/textbuddy/docs/implementation/slices/slice-03-text-correction-enhancements.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Slice 03: Text Correction Enhancements**.

## Globale Regeln

- Gradle mit **Groovy DSL**
- **Java 25**
- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Streaming ist grundsätzlich nur über **`SseEmitter`** zulässig
- Keine späteren Features oder Slices vorziehen

## In Scope

- Sprachauswahl
- lokales Wörterbuch im Browser
- inkrementelle Satz- oder Segment-Diff-Logik
- verzögerte Korrektur-Auslösung
- Sofortauslösung bei Punkt oder Zeilenumbruch
- Filtering bereits bekannter Wörter aus den Korrekturtreffern

## Out of Scope

- Satz-Rewrite
- Wort-Synonyme
- Quick Actions
- Advisor
- Upload

## Architekturregeln

- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Wörterbuch bleibt clientseitig lokal
- Keine Serverpersistenz für User-Dictionary
- Keine späteren Bubble-Menüs oder Rewrite-Funktionen vorwegnehmen

## Tests

- Unit-Tests für Segment-Diff und Wörterbuchfilter
- MockMvc-Tests für unveränderten Korrektur-Endpoint
- Browser-Test für Sprache, Wörterbuch und differenzierte Re-Checks

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Stubs oder bewusste Nicht-Implementierungen
- Vorschlag für den nächsten Slice
