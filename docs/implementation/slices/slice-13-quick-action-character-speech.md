# Slice 13: Quick Action Character Speech

## Ziel

Die Quick Action für direkte/indirekte Rede ergänzen.

## In Scope

- `POST /api/quick-actions/character-speech/stream`
- Optionen:
  - `direct_speech`
  - `indirect_speech`
- eigene Prompt-/Service-Klasse
- UI-Auswahl für beide Varianten

## Out of Scope

- andere Quick Actions

## Fachliches Verhalten

- Nutzer wählt direkte oder indirekte Rede
- Text wird entsprechend umgeformt und gestreamt

## Betroffene Endpoints

- `POST /api/quick-actions/character-speech/stream`

## UI / Interaktion

- Menü mit zwei Sprachtransformationsarten

## Services / Ports / Adapter

- Character-Speech-spezifischer Quick-Action-Service

## DTOs / Typen

- `QuickActionStreamRequest.option` ist verpflichtend

## Persistenz / Dateiquellen

- keine Persistenz

## Tests

- Unit-Test für Optionsvalidierung
- MockMvc-Test
- Browser-Test für beide Varianten

## Definition of Done

- Character-Speech läuft über die bestehende Infrastruktur

## Handoff zum nächsten Slice

- Nächster Slice: **14 Quick Action Custom**
