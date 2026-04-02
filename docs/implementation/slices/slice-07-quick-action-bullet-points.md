# Slice 07: Quick Action Bullet Points

## Ziel

Die Quick Action „Bullet Points“ als eigenen SSE-Endpoint und eigene UI-Aktion auf die bestehende Rewrite-Infrastruktur setzen.

## In Scope

- `POST /api/quick-actions/bullet-points/stream`
- eigene Prompt-/Service-Klasse für Bullet Points
- UI-Aktivierung genau für diese Aktion

## Out of Scope

- andere Quick Actions
- Infrastrukturänderungen außerhalb des Nötigen

## Fachliches Verhalten

- Volltext wird in strukturierte Stichpunkte transformiert
- Ergebnis wird über bestehende Streaming- und Diff-Logik verarbeitet

## Betroffene Endpoints

- `POST /api/quick-actions/bullet-points/stream`

## UI / Interaktion

- Bullet-Points-Aktion im Rewrite-Bereich aktivieren
- andere spätere Aktionen bleiben unverändert deaktiviert, sofern noch nicht gebaut

## Services / Ports / Adapter

- Bullet-Points-spezifischer Quick-Action-Service auf gemeinsamer Infrastruktur

## DTOs / Typen

- nutzt `QuickActionStreamRequest`
- kein eigener Request-Typ nötig

## Persistenz / Dateiquellen

- keine Persistenz

## Tests

- Unit-Test für Prompt-/Service-Aufruf
- MockMvc-Test für Bullet-Points-Endpoint
- Browser-Test für Button -> Stream -> Diff

## Definition of Done

- Bullet-Points läuft isoliert über die gemeinsame Infrastruktur

## Handoff zum nächsten Slice

- Nächster Slice: **08 Quick Action Proofread**
