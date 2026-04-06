# Phase 02: Frontend-Parität und sprachliche Normalisierung

## Ziel

Die bestehende JTE-/Tiptap-Oberfläche soll in den wichtigsten Bedien- und Informationsflächen näher an das Original herangeführt werden. Gleichzeitig werden deutsche nutzersichtbare Texte auf echte Umlaute umgestellt.

## Funktionen

- Umstellung deutscher UI-Texte, Statusmeldungen und Fehlermeldungen auf echte Umlaute
- Korrektursprachen auf den Originalumfang erweitern
- eingebetteter Advisor-PDF-Viewer mit Seitensteuerung, Zoom und Download
- Textstatistiken inklusive Flesch-Score
- produktionsnahe Überarbeitung der Panel- und Hilfetexte

## In Scope

- JTE-Texte und nutzersichtbare Browsertexte
- Korrektursprachwahl
- Advisor-PDF-Anzeige im Browser
- Textstatistik-Panel
- deutsche Fehlermeldungen und Statusmeldungen in UI und Browserlogik

## Out of Scope

- OCR
- Packaging und Release-Auslieferung
- neue Backend-Provider oder neue Produktmodule

## Fachliches Verhalten

- Deutsche UI-Texte verwenden künftig echte Umlaute.
- Technische Kennungen, API-Feldnamen, Dateinamen, CSS-Klassen und Test-IDs bleiben ASCII.
- Die Sprachwahl bietet dieselben wesentlichen Optionen wie das Original: `auto`, `de-CH`, `fr`, `it`, `en-US`, `en-GB`.
- Advisor-Dokumente können im Browser angezeigt, gezoomt, heruntergeladen und auf die relevante Seite geöffnet werden.
- Textstatistiken zeigen mindestens Zeichen, Wörter, Silben, durchschnittliche Satzlänge, durchschnittliche Silben pro Wort und Flesch-Score.

## Betroffene Endpoints

- `GET /`
- `GET /api/advisor/docs`
- `GET /api/advisor/doc/{name}`
- bestehende Korrektur- und Rewrite-Endpoints bleiben unverändert

## UI / Interaktion

- Korrekturpanel mit erweiterter Sprachwahl
- Advisor-Treffer mit PDF-Modal oder PDF-Viewer
- Statistikbereich mit Flesch-Visualisierung oder gleichwertiger Darstellung
- überarbeitete deutsche Paneltexte und Statusmeldungen

## Services / Ports / Adapter

- bestehende Browserlogik für Korrektur, Advisor und Rewrite
- PDF-Viewer-Komponente in der JS-Insel
- Textstatistik-Logik im Frontend

## Tests

- Browser-Tests für PDF-Anzeige, Seitenwechsel, Zoom und Download
- Frontend-Unit-Tests für Textstatistik und Flesch-Berechnung
- Regressionstests für Sprachwahl und bestehende Rewrite-/Advisor-Flows
- DOM- oder Snapshot-Tests für repräsentative deutsche UI-Texte mit Umlauten

## Definition of Done

- deutsche nutzersichtbare Texte verwenden echte Umlaute
- die Sprachwahl ist auf den geplanten Umfang erweitert
- Advisor-PDFs lassen sich im Browser komfortabel öffnen
- Textstatistiken stehen wieder produktiv zur Verfügung

## Handoff zur nächsten Phase

- Nächste Phase: **03 Lokale OCR und Dokumentimport-Qualität**
