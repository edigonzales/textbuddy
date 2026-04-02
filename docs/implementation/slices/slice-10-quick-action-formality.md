# Slice 10: Quick Action Formality

## Ziel

Die Quick Action „Formality“ mit den Varianten `formal` und `informal` ergänzen.

## In Scope

- `POST /api/quick-actions/formality/stream`
- Optionen `formal` und `informal`
- eigene Prompt-/Service-Klasse
- UI-Menü oder Toggle für beide Varianten

## Out of Scope

- andere Quick Actions

## Fachliches Verhalten

- Nutzer wählt Formalitätsstufe
- Ergebnis wird gestreamt und im Editor angewendet

## Betroffene Endpoints

- `POST /api/quick-actions/formality/stream`

## UI / Interaktion

- kleine Auswahl für Formalitätsstufe

## Services / Ports / Adapter

- Formality-spezifischer Quick-Action-Service

## DTOs / Typen

- `QuickActionStreamRequest.option` ist verpflichtend

## Persistenz / Dateiquellen

- keine Persistenz

## Tests

- Unit-Test für Optionsvalidierung
- MockMvc-Test
- Browser-Test für beide Varianten

## Definition of Done

- Formality läuft isoliert über die bestehende Rewrite-Infrastruktur

## Handoff zum nächsten Slice

- Nächster Slice: **11 Quick Action Social Media**
