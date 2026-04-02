# Slice 11: Quick Action Social Media

## Ziel

Die Quick Action „Social Media“ mit kanalabhängigen Varianten als separaten SSE-Endpoint ergänzen.

## In Scope

- `POST /api/quick-actions/social-media/stream`
- Optionen:
  - `bluesky`
  - `instagram`
  - `linkedin`
- eigene Prompt-/Service-Klasse
- UI-Menü für Kanäle

## Out of Scope

- andere Quick Actions

## Fachliches Verhalten

- Nutzer wählt Zielkanal
- Der Text wird passend umgeformt

## Betroffene Endpoints

- `POST /api/quick-actions/social-media/stream`

## UI / Interaktion

- Kanalwahl im Rewrite-Bereich

## Services / Ports / Adapter

- Social-Media-spezifischer Quick-Action-Service

## DTOs / Typen

- `QuickActionStreamRequest.option` ist verpflichtend

## Persistenz / Dateiquellen

- keine Persistenz

## Tests

- Unit-Tests für Kanalmapping
- MockMvc-Test
- Browser-Test für mindestens zwei Kanäle

## Definition of Done

- Social-Media-Action funktioniert isoliert

## Handoff zum nächsten Slice

- Nächster Slice: **12 Quick Action Medium**
