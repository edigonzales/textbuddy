# Slice 03: Text Correction Enhancements

## Ziel

Die Korrektur ergonomisch und effizient machen: Sprache wählen, lokales Wörterbuch, inkrementelle Satzprüfung und Sofortauslösung an sinnvollen Textgrenzen.

## In Scope

- Sprachauswahl für Korrektur
- browserlokales Wörterbuch
- inkrementelle Satzsegmentierung und Diffing im Client
- nur geänderte Segmente an `POST /api/text-correction`
- Sofortauslösung bei Punkt oder Zeilenumbruch

## Out of Scope

- Satz-Rewrite
- Wort-Synonyme
- Quick Actions

## Fachliches Verhalten

- Nutzer kann die Korrektursprache einstellen.
- Wörter im lokalen Wörterbuch werden nicht mehr als Problem gezeigt.
- Es werden nur neue oder veränderte Satzsegmente geprüft.
- Bei `.` oder Zeilenumbruch wird eine laufende Verzögerung übersprungen.

## Betroffene Endpoints

- weiter nur `POST /api/text-correction`

## UI / Interaktion

- Sprachwahl im Shell-Bereich
- Wörterbuch-UI zum Hinzufügen und Entfernen von Wörtern
- Problems-Panel bleibt das zentrale Korrekturpanel

## Services / Ports / Adapter

- Erweiterung der clientseitigen Correction-Logik
- lokaler Dictionary-Store
- serverseitig keine neue Adapterklasse nötig

## DTOs / Typen

- keine neuen Server-DTOs
- clientseitige Typen für Segmentstatus und Wörterbuch

## Persistenz / Dateiquellen

- Wörterbuch lokal im Browser, bevorzugt IndexedDB

## Tests

- Unit-Tests für Satzsegmentierung und Diff-Logik
- Browser-Tests:
  - Sprachwechsel wirkt auf Requests
  - Wörterbuch blendet Treffer aus
  - nur geänderte Segmente werden geprüft

## Definition of Done

- Korrektur arbeitet nicht mehr nur auf Volltextbasis
- Wörterbuch und Sprachwahl funktionieren
- Requests bleiben auf `POST /api/text-correction` begrenzt

## Handoff zum nächsten Slice

- Nächster Slice: **04 Sentence Rewrite**
- Die Korrekturerfahrung ist jetzt stabil genug für die erste LLM-Bubble-Funktion
