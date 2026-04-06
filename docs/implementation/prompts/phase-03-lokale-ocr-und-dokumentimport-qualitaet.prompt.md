# Codex Prompt: Phase 03 Lokale OCR und Dokumentimport-Qualität

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [phase-03-lokale-ocr-und-dokumentimport-qualitaet.md](/Users/stefan/sources/textbuddy/docs/implementation/phases/phase-03-lokale-ocr-und-dokumentimport-qualitaet.md)
- [05-phase-index.md](/Users/stefan/sources/textbuddy/docs/implementation/05-phase-index.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Phase 03: Lokale OCR und Dokumentimport-Qualität**.

## Globale Regeln

- Gradle mit **Groovy DSL**
- **Java 25**
- **Nur Spring MVC**
- Keine spätere Phase vorziehen

## Sprachregel

- Verwende in allen deutschsprachigen, nutzersichtbaren Texten echte Umlaute (`ä`, `ö`, `ü`, `Ä`, `Ö`, `Ü`)
- Verwende keine ASCII-Umschreibungen wie `ae`, `oe`, `ue` in UI-Texten, Fehlermeldungen, Prompts und JTE-Texten
- Technische Kennungen, API-Feldnamen, Endpoints, Dateinamen, CSS-Klassen und Test-IDs bleiben ASCII
- Schweizer Rechtschreibung mit `ss` bleibt zulässig

## In Scope

- lokale OCR für Dokumentimport
- OCR-Sprachen `de`, `en`, `fr`, `it`
- Import-Nachbearbeitung für editorfreundliches HTML
- Upload-Limits, Timeouts und robuste Fehlerrückgaben
- realistische Formatfreigabe entsprechend der lokalen Runtime

## Out of Scope

- neue Quick Actions
- neue Advisor-Funktionen
- Release- und Packaging-Themen

## Architekturregeln

- Kein externer Docling-Server im Standardpfad
- Dokumentimport liefert weiter HTML für den bestehenden Editorvertrag
- Fehlerzustände müssen kontrolliert und nutzerverständlich sein

## Tests

- Integrations-Tests mit gescannten Dokumenten
- Tests für OCR-Sprachen und Fallback
- Browser-Test für OCR-Import
- Fehler- und Grenztests für große oder beschädigte Dateien

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Punkte oder bewusste Nicht-Implementierungen
- Vorschlag für die nächste Phase
