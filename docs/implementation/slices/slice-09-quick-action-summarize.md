# Slice 09: Quick Action Summarize

## Ziel

Die Quick Action „Summarize“ mit festen Optionen als separaten SSE-Endpoint und eigener UI ergänzen.

## In Scope

- `POST /api/quick-actions/summarize/stream`
- Optionen:
  - `sentence`
  - `three_sentence`
  - `paragraph`
  - `page`
  - `management_summary`
- eigene Prompt-/Service-Klasse
- UI-Menü für Optionen

## Out of Scope

- andere Quick Actions

## Fachliches Verhalten

- Nutzer wählt eine Summarize-Variante
- Request enthält `option`
- Backend streamt zusammengefassten Text

## Betroffene Endpoints

- `POST /api/quick-actions/summarize/stream`

## UI / Interaktion

- Menü oder Dropdown für Summarize-Varianten

## Services / Ports / Adapter

- Summarize-spezifischer Quick-Action-Service

## DTOs / Typen

- `QuickActionStreamRequest.option` ist hier verpflichtend

## Persistenz / Dateiquellen

- keine Persistenz

## Tests

- Unit-Test pro Optionsmapping
- MockMvc-Test für Endpoint mit `option`
- Browser-Test für mindestens zwei Varianten

## Definition of Done

- Summarize funktioniert mit den definierten Optionen

## Handoff zum nächsten Slice

- Nächster Slice: **10 Quick Action Formality**
