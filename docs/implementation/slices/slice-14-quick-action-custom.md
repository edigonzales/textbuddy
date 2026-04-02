# Slice 14: Quick Action Custom

## Ziel

Die freie benutzerdefinierte Quick Action ergänzen.

## In Scope

- `POST /api/quick-actions/custom/stream`
- freier Prompt im Request-Feld `prompt`
- eigene Prompt-/Service-Klasse
- UI-Dialog oder Drawer zur Eingabe des freien Prompts

## Out of Scope

- andere Quick Actions

## Fachliches Verhalten

- Nutzer gibt einen freien Arbeitsauftrag ein
- Der Volltext wird nach diesem Prompt transformiert

## Betroffene Endpoints

- `POST /api/quick-actions/custom/stream`

## UI / Interaktion

- Prompt-Eingabefeld im Rewrite-Bereich

## Services / Ports / Adapter

- Custom-Quick-Action-Service

## DTOs / Typen

- `QuickActionStreamRequest.prompt` ist verpflichtend

## Persistenz / Dateiquellen

- keine Persistenz

## Tests

- Unit-Test für Requestvalidierung
- MockMvc-Test
- Browser-Test für Prompt-Eingabe und Rewrite

## Definition of Done

- Freie Quick Action funktioniert separat

## Handoff zum nächsten Slice

- Nächster Slice: **15 Advisor Catalog and PDF**
