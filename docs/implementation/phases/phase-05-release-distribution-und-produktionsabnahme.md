# Phase 05: Release, Distribution und Produktionsabnahme

## Ziel

Die Anwendung soll als auslieferbares Artefakt abgeschlossen werden, das per `java -jar textbuddy.jar` startet und im Standardbetrieb keine externen Sidecars benötigt.

## Funktionen

- auslieferbarer `bootJar`- oder gleichwertiger Jar-Distributionspfad
- Initialisierung und Bereitstellung lokaler Laufzeitartefakte
- dokumentierter Start ohne Sidecars
- Release-Dokumentation, Konfigurationsbeispiele und Runbook
- finale Smoke-, E2E- und Abnahmetests

## In Scope

- Packaging des produktiven Artefakts
- Initialisierungslogik für lokale Ressourcen
- Start- und Betriebsdokumentation
- Abnahmekriterien und Release-Checkliste
- Performance-Smoke- und Stabilitätstests

## Out of Scope

- neue Fachfeatures
- neue UX-Features außerhalb reiner Abnahmebehebungen

## Fachliches Verhalten

- Zielartefakt ist ein Jar, das ohne LanguageTool- oder Docling-Sidecars startet.
- Notwendige lokale Ressourcen werden mit der Distribution ausgeliefert oder beim ersten Start kontrolliert bereitgestellt.
- Die Anwendung dokumentiert klar, welche Konfigurationswerte für einen produktiven LLM-Provider nötig sind.
- Start, Shutdown und Wiederanlauf sollen reproduzierbar funktionieren.

## Betroffene Endpoints

- alle produktiven Kernendpoints
- Actuator-Endpunkte für Smoke- und Release-Prüfungen

## UI / Interaktion

- keine neue Fach-UI, nur abnahmebedingte Korrekturen
- Fokus auf Startbarkeit, Stabilität und dokumentierte Bedienbarkeit

## Services / Ports / Adapter

- Build- und Packaging-Konfiguration
- Initialisierungslogik für lokale Ressourcen
- Smoke-Test- und E2E-Testpfade gegen das ausgelieferte Artefakt

## Tests

- Smoke-Test gegen das gepackte Jar
- End-to-End-Tests gegen das ausgelieferte Artefakt
- Start-, Shutdown- und Recoverability-Tests
- Performance-Smoke für Korrektur, Rewrite, Advisor und Import
- dokumentierte manuelle Abnahmekriterien

## Definition of Done

- `java -jar textbuddy.jar` ist der Standardstartpfad
- das Artefakt läuft ohne externe Sidecars
- Runbook, Konfigurationsbeispiele und Abnahme-Checkliste liegen vor
- finale Smoke- und E2E-Tests sind dokumentiert und grün

## Handoff zur nächsten Phase

- keine weitere Pflichtphase vorgesehen; danach folgen nur noch gezielte Verbesserungen oder neue Features
