# Slice 15: Advisor Catalog and PDF

## Ziel

Die statischen Advisor-Dokumente sichtbar machen und PDF-Dateien bereitstellen, ohne bereits die eigentliche Validierung zu implementieren.

## In Scope

- `GET /api/advisor/docs`
- `GET /api/advisor/doc/{name}`
- dateibasierter `AdvisorDocumentRepository`
- Metadatenmodell für Referenzdokumente
- Advisor-Panel mit Dokumentauswahl
- PDF-Öffnung oder eingebettete Anzeige

## Out of Scope

- `POST /api/advisor/validate`
- Dokumentupload

## Fachliches Verhalten

- Der Nutzer sieht den verfügbaren statischen Dokumentkatalog.
- Dokumente können ausgewählt werden.
- Ein Dokument kann als PDF im Browser angezeigt oder heruntergeladen werden.

## Betroffene Endpoints

- `GET /api/advisor/docs`
- `GET /api/advisor/doc/{name}`

## UI / Interaktion

- Advisor-Panel mit **Mehrfachauswahl**
- PDF-Öffnung über Link, Modal oder Iframe-basierte Anzeige

## Services / Ports / Adapter

- `AdvisorCatalogService`
- `AdvisorDocumentRepository`

## DTOs / Typen

- `AdvisorDocsResponseItem`

## Persistenz / Dateiquellen

- statische JSON-Metadaten
- statische PDF-Dateien

## Tests

- Unit-Test für Repository-Laden
- MockMvc-Tests für beide GET-Endpunkte
- Browser-Test:
  - Dokumente werden angezeigt
  - PDF-Endpunkt ist erreichbar

## Definition of Done

- Advisor-Dokumente sind sichtbar und auslieferbar
- Noch keine Validierungslogik vorhanden

## Handoff zum nächsten Slice

- Nächster Slice: **16 Advisor Validation**
