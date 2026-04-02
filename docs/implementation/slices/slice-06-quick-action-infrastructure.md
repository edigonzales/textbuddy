# Slice 06: Quick Action Infrastructure

## Ziel

Die technische Basis für streamingfähige Volltexttransformationen schaffen und nur die Aktion „Plain Language“ als erste echte SSE-Funktion anbinden.

## In Scope

- gemeinsamer SSE-Client in der Editor-Insel
- gemeinsamer Streaming-Handler
- Diff-Grundlage für Rewrite-Ergebnisse
- `POST /api/quick-actions/plain-language/stream`
- erste Rewrite-Toolbar mit **nur** Plain-Language-Aktion
- Undo des kompletten Rewrite-Ergebnisses

## Out of Scope

- weitere Quick Actions
- Advisor
- Upload

## Fachliches Verhalten

- Nutzer startet Plain-Language-Rewrite.
- Backend streamt Textteile per SSE.
- Editor wird während des Streams aktualisiert.
- Nach Abschluss ist ein Diff zwischen Alt und Neu sichtbar.
- Ein kompletter Undo stellt den Ursprungszustand wieder her.

## Betroffene Endpoints

- `POST /api/quick-actions/plain-language/stream`

## UI / Interaktion

- Rewrite-Bereich sichtbar
- nur Plain-Language-Button aktiv
- Diff-Ansicht unter oder neben dem Editor

## Services / Ports / Adapter

- gemeinsamer Quick-Action-SSE-Client
- `QuickActionService`
- Plain-Language-spezifischer LLM-Service

## DTOs / Typen

- `QuickActionStreamRequest`
- SSE-Event-Payloads:
  - `chunk`
  - `complete`
  - `error`

## Persistenz / Dateiquellen

- keine Persistenz

## Tests

- Unit-Tests für SSE-Payload-Erzeugung
- MockMvc-Test für `plain-language`-Streaming-Endpoint
- Browser-Test:
  - Stream wird verarbeitet
  - Editorinhalt ändert sich
  - Diff wird angezeigt
  - Undo stellt Ursprung her

## Definition of Done

- Streaming-Grundlage steht
- Plain-Language läuft isoliert
- weitere Quick Actions sind noch nicht aktiv

## Handoff zum nächsten Slice

- Nächster Slice: **07 Quick Action Bullet Points**
- Neue Quick Actions dürfen nur noch auf die bestehende Infrastruktur aufsetzen
