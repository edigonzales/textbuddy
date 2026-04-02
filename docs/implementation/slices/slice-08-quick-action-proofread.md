# Slice 08: Quick Action Proofread

## Ziel

Die Quick Action „Proofread“ als separaten SSE-Endpoint und eigene UI-Aktion ergänzen.

## In Scope

- `POST /api/quick-actions/proofread/stream`
- eigene Prompt-/Service-Klasse für Proofread
- UI-Aktivierung dieser Aktion

## Out of Scope

- andere Quick Actions

## Fachliches Verhalten

- Der gesamte Text wird per LLM korrekturgelesen
- Ergebnis läuft durch dieselbe Streaming- und Diff-Pipeline wie Plain Language

## Betroffene Endpoints

- `POST /api/quick-actions/proofread/stream`

## UI / Interaktion

- Proofread-Aktion im Rewrite-Bereich aktivieren

## Services / Ports / Adapter

- Proofread-spezifischer Quick-Action-Service

## DTOs / Typen

- nutzt `QuickActionStreamRequest`

## Persistenz / Dateiquellen

- keine Persistenz

## Tests

- Unit-Test für Proofread-Service
- MockMvc-Test für Endpoint
- Browser-Test für Proofread-Aktion

## Definition of Done

- Proofread läuft isoliert und verändert keine andere Quick Action

## Handoff zum nächsten Slice

- Nächster Slice: **09 Quick Action Summarize**
