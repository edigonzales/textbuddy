# Slice 12: Quick Action Medium

## Ziel

Die Quick Action „Medium“ mit festen Ausgabearten ergänzen.

## In Scope

- `POST /api/quick-actions/medium/stream`
- Optionen:
  - `email`
  - `official_letter`
  - `presentation`
  - `report`
- eigene Prompt-/Service-Klasse
- UI-Auswahl für Medium-Typ

## Out of Scope

- andere Quick Actions
- produktive Auth-Integration

## Fachliches Verhalten

- Nutzer wählt Ausgabeart
- LLM erzeugt entsprechend strukturierten Volltext

## Betroffene Endpoints

- `POST /api/quick-actions/medium/stream`

## UI / Interaktion

- Dropdown oder Menü für die vier Medium-Typen

## Services / Ports / Adapter

- Medium-spezifischer Quick-Action-Service

## DTOs / Typen

- `QuickActionStreamRequest.option` ist verpflichtend

## Persistenz / Dateiquellen

- keine Persistenz

## Tests

- Unit-Tests für Optionsvalidierung
- MockMvc-Test
- Browser-Test für mindestens zwei Medium-Typen

## Definition of Done

- Medium-Action funktioniert isoliert

## Handoff zum nächsten Slice

- Nächster Slice: **13 Quick Action Character Speech**
