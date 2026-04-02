# Slice 17: Document Import

## Ziel

Dokumente per Upload oder Drag & Drop an Docling senden und das zurückgelieferte HTML in den Editor übernehmen.

## In Scope

- `POST /api/convert/doc`
- `DocumentConversionService`
- `DoclingClient`
- Upload-UI und Drag-&-Drop
- HTML-Import in die Tiptap-Insel
- unterstützte Formate zwischen UI und Backend angleichen

## Out of Scope

- Persistenz von Uploads als Advisor-Dokument
- OCR-Konfiguration pro Nutzer

## Fachliches Verhalten

- Nutzer lädt unterstützte Datei hoch.
- Backend sendet die Datei an Docling.
- Docling liefert HTML.
- Der Editor übernimmt den HTML-Inhalt.
- Unterstützte Formate in UI und Backend sind identisch.

## Betroffene Endpoints

- `POST /api/convert/doc`

## UI / Interaktion

- Upload-Button
- Drag-&-Drop-Zone
- Ladezustand während Konvertierung

## Services / Ports / Adapter

- `DocumentConversionService`
- `DoclingClient`

## DTOs / Typen

- `DocumentConversionResponse`

## Persistenz / Dateiquellen

- keine Persistenz des Uploads
- temporäre Verarbeitung nur im Requestkontext

## Tests

- Unit-Test für Dateitypvalidierung
- MockMvc-Test für Multipart-Endpoint
- Browser-Test:
  - Upload funktioniert
  - Editor übernimmt den Inhalt
  - nicht unterstützte Formate werden sauber abgewiesen

## Definition of Done

- Upload und HTML-Import laufen
- UI-Formatliste und Backend-Unterstützung stimmen überein

## Handoff zum nächsten Slice

- Nächster Slice: **18 Auth and Polish**
