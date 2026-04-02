# Slice 01: Editor Island Base

## Ziel

Die Tiptap-JavaScript-Insel in die serverseitige Shell integrieren und eine erste lokale Bearbeitung mit Mirror, Zählern und Undo/Redo bereitstellen.

## In Scope

- Tiptap-Insel mit Plain-Text-Fokus
- Integration der gebauten Frontend-Assets in die Seite
- Hidden Mirror Field für aktuellen Plaintext
- Zeichen- und Wortzähler
- Undo/Redo
- kleine clientseitige Events:
  - `editor:text-changed`
  - `editor:selection-changed`

## Out of Scope

- Server-APIs für Textverarbeitung
- Korrekturmarken
- Bubble-Menüs
- Quick Actions
- Upload

## Fachliches Verhalten

- Nutzer kann Text im Editor eingeben.
- Der aktuelle Plaintext wird synchron in ein Hidden-Feld oder äquivalenten Mirror-State geschrieben.
- Zeichen- und Wortzähler aktualisieren sich live.
- Undo/Redo funktionieren lokal im Editor.

## Betroffene Endpoints

- keine neuen Endpoints
- bestehend: `GET /`

## UI / Interaktion

- Platzhalter-Editor aus Slice 00 wird durch die echte Insel ersetzt
- Buttons für Undo/Redo sind sichtbar
- Noch nicht implementierte Funktionsbuttons bleiben disabled oder verborgen

## Services / Ports / Adapter

- keine neuen Server-Services
- clientseitige Editor-Module für:
  - Bootstrapping
  - Mirror-Sync
  - Counter
  - Undo/Redo-Steuerung

## DTOs / Typen

- keine neuen Server-DTOs
- clientseitige Typen für Editor-State und Event-Details

## Persistenz / Dateiquellen

- keine Persistenz
- Assets aus `frontend/` werden gebaut und von Spring statisch ausgeliefert

## Tests

- MockMvc-Test: Seite bindet Editor-Insel-Container ein
- Browser-Test:
  - Editor lädt
  - Tippen aktualisiert Mirror
  - Zähler reagieren
  - Undo/Redo funktionieren

## Definition of Done

- Nutzer kann lokal im Editor arbeiten
- Mirror enthält den Plaintext
- Undo/Redo und Zähler laufen zuverlässig
- Keine Fach-API wird bereits fälschlich aufgerufen

## Handoff zum nächsten Slice

- Nächster Slice: **02 Text Correction Base**
- Der Editor ist bereit für die erste serverseitige Textverarbeitungsintegration
