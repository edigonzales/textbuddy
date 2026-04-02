# Slice 04: Sentence Rewrite

## Ziel

Alternative Formulierungen für den aktuell fokussierten Satz bereitstellen und direkt im Editor anwenden können.

## In Scope

- `POST /api/sentence-rewrite`
- `SentenceRewriteService`
- Satzfokus in der Editor-Insel
- Bubble-Menü am Satz
- Abrufen und Anwenden von Alternativsätzen

## Out of Scope

- Wort-Synonyme
- Quick Actions
- Streaming

## Fachliches Verhalten

- Der Client erkennt den aktuell fokussierten Satz.
- Das Bubble-Menü bietet „Satz umschreiben“ an.
- Der Endpoint liefert Alternativen.
- Eine gewählte Alternative ersetzt exakt den fokussierten Satzbereich.

## Betroffene Endpoints

- `POST /api/sentence-rewrite`

## UI / Interaktion

- Bubble-Menü erscheint in Rewrite-Kontexten am Satz
- Optionen werden im Menü oder zugehörigem Overlay angezeigt

## Services / Ports / Adapter

- `SentenceRewriteService`
- LLM-Adapter oder Spring-AI-Aufruf für strukturierte Ausgabe

## DTOs / Typen

- `SentenceRewriteRequest`
- `SentenceRewriteResponse`

## Persistenz / Dateiquellen

- keine Persistenz

## Tests

- Unit-Test für Service-/Antwortsabbildung
- MockMvc-Test für `POST /api/sentence-rewrite`
- Browser-Test:
  - Satzfokus erkennen
  - Alternativen laden
  - Auswahl ersetzt Satz

## Definition of Done

- Satzalternativen funktionieren unabhängig von Quick Actions
- Ersatz erfolgt exakt auf Satzrange

## Handoff zum nächsten Slice

- Nächster Slice: **05 Word Synonym**
- Bubble-Mechanik und kontextbezogener Rewrite-Pfad stehen
