# Slice 02: Text Correction Base

## Ziel

Die erste funktionsfähige LanguageTool-Integration bereitstellen: kompletter Text wird geprüft, Fehler werden markiert und Vorschläge können angewendet werden.

## In Scope

- `POST /api/text-correction`
- `LanguageToolClient`
- `TextCorrectionService`
- vollständige Textprüfung gegen den gesamten Editorinhalt
- einfache debouncte Auslösung vom Client
- Korrekturmarken im Editor
- Problems-Panel mit Vorschlägen
- Anwenden eines Vorschlags

## Out of Scope

- inkrementelle Satzdiff-Logik
- Wörterbuch
- explizite Sprachumschaltung
- Bubble-Menüs für Satz/Wort

## Fachliches Verhalten

- Nach kurzer Tipp-Pause wird der aktuelle Text geprüft.
- Das Backend ruft LanguageTool auf und liefert Korrekturblöcke zurück.
- Der Client markiert Problemstellen im Editor.
- Das Problems-Panel listet Korrekturen.
- Klick auf einen Vorschlag ersetzt den betroffenen Textbereich im Editor.

## Betroffene Endpoints

- `POST /api/text-correction`

## UI / Interaktion

- Problems-Panel wird erstmals funktional
- Unterstreichungen/Marks im Editor
- Klick auf Problem oder Vorschlag navigiert bzw. ersetzt Text

## Services / Ports / Adapter

- `TextCorrectionService`
- `LanguageToolClient`
- clientseitige Correction-Bridge zwischen Editor und Endpoint

## DTOs / Typen

- `CorrectionRequest`
- `CorrectionResponse`
- `CorrectionBlock`

## Persistenz / Dateiquellen

- keine Persistenz

## Tests

- Unit-Test für Mapping LanguageTool -> `CorrectionBlock`
- MockMvc-Test für `POST /api/text-correction`
- Browser-Test:
  - Text eingeben
  - Fehler erscheinen
  - Vorschlag anwenden ersetzt Text

## Definition of Done

- Korrektur-Endpoint liefert belastbare Daten
- Fehler werden im Editor sichtbar markiert
- Vorschläge lassen sich anwenden
- Noch immer keine Wörterbuch- oder Inkrementalitätslogik

## Handoff zum nächsten Slice

- Nächster Slice: **03 Text Correction Enhancements**
- Bestehende Volltextprüfung wird dort zu einer inkrementelleren UX erweitert
