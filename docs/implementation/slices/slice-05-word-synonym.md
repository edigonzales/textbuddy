# Slice 05: Word Synonym

## Ziel

Synonyme für das aktuell fokussierte Wort im Satzkontext bereitstellen und direkt im Editor einsetzen.

## In Scope

- `POST /api/word-synonym`
- `WordSynonymService`
- Wortfokus in der Editor-Insel
- Bubble-Menü am Wort
- Abrufen und Anwenden von Synonymen

## Out of Scope

- Quick Actions
- Streaming
- Advisor

## Fachliches Verhalten

- Der Client erkennt das aktuell fokussierte Wort.
- Das Bubble-Menü bietet „Wort umschreiben“ an.
- Das Backend liefert Synonyme im Kontext des Satzes.
- Die gewählte Alternative ersetzt exakt die Wortrange.

## Betroffene Endpoints

- `POST /api/word-synonym`

## UI / Interaktion

- Bubble-Menü am Wort
- Synonymliste nah am Editor

## Services / Ports / Adapter

- `WordSynonymService`
- LLM-Adapter oder Spring-AI-Aufruf für strukturierte Ausgabe

## DTOs / Typen

- `WordSynonymRequest`
- `WordSynonymResponse`

## Persistenz / Dateiquellen

- keine Persistenz

## Tests

- Unit-Test für Serviceantwort
- MockMvc-Test für `POST /api/word-synonym`
- Browser-Test:
  - Wortfokus erkennen
  - Synonyme laden
  - Auswahl ersetzt Wort

## Definition of Done

- Synonymfunktion arbeitet unabhängig von Quick Actions
- Wortersatz ist positionsgenau

## Handoff zum nächsten Slice

- Nächster Slice: **06 Quick Action Infrastructure**
- Die direkte Rewrite-UX im Editor ist vollständig genug für das erste Streaming-Feature
