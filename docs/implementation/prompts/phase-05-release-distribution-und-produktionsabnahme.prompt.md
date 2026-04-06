# Codex Prompt: Phase 05 Release, Distribution und Produktionsabnahme

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [phase-05-release-distribution-und-produktionsabnahme.md](/Users/stefan/sources/textbuddy/docs/implementation/phases/phase-05-release-distribution-und-produktionsabnahme.md)
- [05-phase-index.md](/Users/stefan/sources/textbuddy/docs/implementation/05-phase-index.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Phase 05: Release, Distribution und Produktionsabnahme**.

## Globale Regeln

- Gradle mit **Groovy DSL**
- **Java 25**
- Zielartefakt ist `java -jar textbuddy.jar`
- Keine spätere Phase vorziehen

## Sprachregel

- Verwende in allen deutschsprachigen, nutzersichtbaren Texten echte Umlaute (`ä`, `ö`, `ü`, `Ä`, `Ö`, `Ü`)
- Verwende keine ASCII-Umschreibungen wie `ae`, `oe`, `ue` in UI-Texten, Fehlermeldungen, Prompts und JTE-Texten
- Technische Kennungen, API-Feldnamen, Endpoints, Dateinamen, CSS-Klassen und Test-IDs bleiben ASCII
- Schweizer Rechtschreibung mit `ss` bleibt zulässig

## In Scope

- lauffähiges Produktionsartefakt
- Initialisierung lokaler Laufzeitressourcen
- Runbook und Konfigurationsbeispiele
- finale Smoke-, E2E- und Abnahmetests

## Out of Scope

- neue Fachfeatures
- zusätzliche UX-Funktionen außerhalb reiner Abnahmebehebungen

## Architekturregeln

- Standardbetrieb erfolgt ohne externe Sidecars
- notwendige lokale Ressourcen werden mit der Distribution ausgeliefert oder kontrolliert bereitgestellt
- Release-Artefakt und Runbook müssen denselben Standardstartpfad beschreiben

## Tests

- Smoke-Tests gegen das gepackte Artefakt
- E2E-Tests ohne Sidecars
- Start-, Shutdown- und Recoverability-Tests
- Performance-Smoke-Tests
- dokumentierte Abnahmekriterien

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Punkte oder bewusste Nicht-Implementierungen
- Vorschlag für die nächsten Verbesserungen nach Abschluss der Pflichtphasen
