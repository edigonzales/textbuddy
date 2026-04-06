# Phase 03: Lokale OCR und Dokumentimport-Qualität

## Ziel

Der Dokumentimport soll auch für gescannte oder bildbasierte Dokumente ohne externen Docling-Server zuverlässig funktionieren. Zusätzlich wird die Import-Pipeline qualitativ und betrieblich gehärtet.

## Funktionen

- lokale OCR für gescannte PDFs und bildbasierte Dokumente
- OCR-Sprachkonfiguration für die unterstützten Hauptsprachen
- bessere HTML-Nachbearbeitung für editorfreundlichen Import
- Upload-Limits, Timeouts und robuste Fehlerrückgaben
- Formatmatrix an die lokal tatsächlich unterstützte Runtime angleichen

## In Scope

- OCR im eingebetteten Dokumentimport
- OCR-Sprachen `de`, `en`, `fr`, `it`
- Nachbearbeitung der Importergebnisse für den Editor
- Upload-Limits und Import-Timeouts
- Fehlerrückgaben für beschädigte oder zu große Dateien

## Out of Scope

- neue Quick Actions
- neue Advisor-Funktionen
- Release- und Packaging-Themen

## Fachliches Verhalten

- Gescannte PDFs sollen ohne externen Docling-Server importiert werden können.
- OCR läuft im Standardpfad lokal in derselben Anwendung.
- Die Import-Pipeline liefert weiterhin HTML zurück, weil der Editorvertrag unverändert bleibt.
- Nicht unterstützte Formate werden früh, klar und konsistent abgewiesen.
- Große oder beschädigte Dateien führen zu kontrollierten Fehlermeldungen statt zu Timeouts ohne Rückmeldung.

## Betroffene Endpoints

- `POST /api/convert/doc`
- `GET /`

## UI / Interaktion

- Upload-UI zeigt klare Fehlermeldungen bei OCR- oder Importproblemen
- Formatliste spiegelt nur real unterstützte Formate wider
- Erfolg und Fehlschlag werden im bestehenden Import-Panel transparent dargestellt

## Services / Ports / Adapter

- eingebetteter Kreuzberg-Importadapter mit OCR-Unterstützung
- HTML-Nachbearbeitung vor dem Setzen in den Editor
- Import-Grenzwert- und Validierungslogik

## Tests

- Integrations-Tests mit gescannten PDFs und bildbasierten Dokumenten
- Tests für OCR-Sprachkonfiguration und Fallback-Verhalten
- Browser-Test für OCR-Import und Editorübernahme
- Fehler- und Grenztests für große, beschädigte oder nicht unterstützte Dateien

## Definition of Done

- gescannte Dokumente funktionieren ohne externen Docling-Server
- Importfehler sind für Nutzer verständlich und technisch stabil
- die Upload-Formatliste entspricht der wirklichen lokalen Runtime

## Handoff zur nächsten Phase

- Nächste Phase: **04 Betriebsfähigkeit, Sicherheit und Observability**
