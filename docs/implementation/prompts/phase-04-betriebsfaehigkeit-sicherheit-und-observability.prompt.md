# Codex Prompt: Phase 04 Betriebsfähigkeit, Sicherheit und Observability

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [phase-04-betriebsfaehigkeit-sicherheit-und-observability.md](/Users/stefan/sources/textbuddy/docs/implementation/phases/phase-04-betriebsfaehigkeit-sicherheit-und-observability.md)
- [05-phase-index.md](/Users/stefan/sources/textbuddy/docs/implementation/05-phase-index.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Phase 04: Betriebsfähigkeit, Sicherheit und Observability**.

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

- Health-Checks und Startvalidierung
- pseudonymisierte Nutzungslogs
- rollenbasierter Advisor-Zugriff
- Retry-, Timeout- und Fehlerstrategie
- konsistente Diagnose- und Trace-Informationen

## Out of Scope

- neue Fachfunktionen
- neue Frontend-Produktfeatures
- finales Release-Packaging

## Architekturregeln

- Diagnose- und Sicherheitslogik darf die Fachschnittstellen nicht unnötig verkomplizieren
- Fehlerstrategien müssen über alle produktiven Adapter konsistent sein
- Rollenlogik für Advisor bleibt dateibasiert und dokumentenorientiert

## Tests

- Unit-Tests für Config-, Rollen- und Logginglogik
- MockMvc-Tests für Auth-, Fehler- und Health-Fälle
- Integrations-Tests für Startfehler bei fehlender Pflichtkonfiguration
- Smoke-Tests für geschützte API-Flows

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Punkte oder bewusste Nicht-Implementierungen
- Vorschlag für die nächste Phase
