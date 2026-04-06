# Codex Prompt: Phase 02 Frontend-Parität und sprachliche Normalisierung

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [phase-02-frontend-paritaet-und-sprachliche-normalisierung.md](/Users/stefan/sources/textbuddy/docs/implementation/phases/phase-02-frontend-paritaet-und-sprachliche-normalisierung.md)
- [05-phase-index.md](/Users/stefan/sources/textbuddy/docs/implementation/05-phase-index.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Phase 02: Frontend-Parität und sprachliche Normalisierung**.

## Globale Regeln

- Gradle mit **Groovy DSL**
- **Java 25**
- **Nur Spring MVC**
- Editornahe Logik nur in der JS-Insel
- Keine spätere Phase vorziehen

## Sprachregel

- Verwende in allen deutschsprachigen, nutzersichtbaren Texten echte Umlaute (`ä`, `ö`, `ü`, `Ä`, `Ö`, `Ü`)
- Verwende keine ASCII-Umschreibungen wie `ae`, `oe`, `ue` in UI-Texten, Fehlermeldungen, Prompts und JTE-Texten
- Technische Kennungen, API-Feldnamen, Endpoints, Dateinamen, CSS-Klassen und Test-IDs bleiben ASCII
- Schweizer Rechtschreibung mit `ss` bleibt zulässig

## In Scope

- Umstellung deutscher UI-Texte auf echte Umlaute
- erweiterte Korrektursprachwahl
- Advisor-PDF-Viewer im Browser
- Textstatistiken und Flesch-Score
- produktionsnahe Überarbeitung der Paneltexte

## Out of Scope

- OCR
- Packaging und Release-Auslieferung
- neue Backend-Provider oder neue Produktmodule

## Architekturregeln

- JTE rendert die Shell, editornahes Verhalten bleibt in der JS-Insel
- Keine neue Server-Rendering-Logik für Tiptap-Interaktionsdetails
- Technische Kennungen bleiben ASCII, nutzersichtbare Texte verwenden Umlaute

## Tests

- Browser-Tests für PDF-Viewer, Statistik und Sprachumschaltung
- Frontend-Unit-Tests für Textstatistik
- Regressionstests für Rewrite-, Advisor- und Korrekturflows
- repräsentative Tests für deutsche UI-Texte mit Umlauten

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Punkte oder bewusste Nicht-Implementierungen
- Vorschlag für die nächste Phase
