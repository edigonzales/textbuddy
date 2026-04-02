# Slice 16: Advisor Validation

## Ziel

Die eigentliche Advisor-Regelprüfung mit SSE auf Basis statischer Dokumentregeln ergänzen.

## In Scope

- `POST /api/advisor/validate`
- `AdvisorValidationService`
- SSE über `SseEmitter`
- Batch-Verarbeitung von Regeln
- clientseitige oder serverseitige Deduplizierung, hier festgelegt auf **clientseitige Deduplizierung**
- Ergebnisliste im Advisor-Panel

## Out of Scope

- Upload als Advisor-Dokument
- Auth

## Fachliches Verhalten

- Nutzer wählt Dokumente und startet die Prüfung.
- Das Backend lädt Regeln für die gewählten Dokumente.
- Regeln werden in kleinen Batches per LLM geprüft.
- Ergebnisse werden als SSE-Events `validation` gestreamt.
- Der Client dedupliziert Treffer anhand eines stabilen Schlüssels.

## Betroffene Endpoints

- `POST /api/advisor/validate`

## UI / Interaktion

- laufende Ergebnisliste
- Statusanzeige während des Streams
- Auswahl einer Regel zeigt Dokument- und Seitenbezug an

## Services / Ports / Adapter

- `AdvisorValidationService`
- LLM-Adapter
- `AdvisorDocumentRepository`

## DTOs / Typen

- `AdvisorValidateRequest`
- `AdvisorValidationEvent`

## Persistenz / Dateiquellen

- statische Regeldateien
- statische PDF-/Metadatendateien

## Tests

- Unit-Tests für Regelbatching
- MockMvc-Test für SSE-Endpoint
- Browser-Test:
  - Validierung startet
  - Events kommen an
  - Deduplizierung funktioniert

## Definition of Done

- Advisor kann Texte gegen statische Dokumentregeln prüfen
- Streaming läuft über `SseEmitter`

## Handoff zum nächsten Slice

- Nächster Slice: **17 Document Import**
