# Phase 04: Betriebsfähigkeit, Sicherheit und Observability

## Ziel

Die Anwendung soll für den Produktionsbetrieb abgesichert, nachvollziehbar und fehlerrobust werden. Dazu gehören betriebliche Diagnosepunkte, Rollenlogik, pseudonymisierte Nutzungssignale und konsistente Fehlerstrategien.

## Funktionen

- Health-Checks für LLM, LanguageTool und Dokumentimport-Engine
- Fail-fast-Konfigurationsprüfung beim Start
- pseudonymisierte Nutzungslogs wie im Original
- rollenbasierter Zugriff auf Advisor-Dokumente
- Retry-, Timeout- und Fehlerstrategie für externe Aufrufe
- konsistente Tracing- und SSE-Protokollierung

## In Scope

- Actuator- und Health-Logik
- Startvalidierung für Pflichtkonfiguration
- Rollenlogik für Advisor-Dokumente
- pseudonymisierte Usage-Events
- Fehler- und Timeout-Strategien für produktive Adapter

## Out of Scope

- neue Fachfunktionen
- neue Frontend-Produktfeatures
- finales Release-Packaging

## Fachliches Verhalten

- Die Anwendung startet nicht in einen halbkonfigurierten Zustand.
- Fehlende Pflichtkonfiguration wird früh und eindeutig signalisiert.
- Advisor-Dokumente können abhängig von Rollen oder Freigaben gefiltert werden.
- Nutzungslogs enthalten keine direkten Personenkennungen.
- Timeouts und Retries sind pro Adapter kontrolliert statt implizit.
- API-Fehler und SSE-Fehler bleiben fachlich konsistent und nachvollziehbar.

## Betroffene Endpoints

- `GET /actuator/health`
- `GET /actuator/info`
- bestehende `/api/**`-Endpoints
- `GET /api/advisor/docs`
- `POST /api/advisor/validate`

## UI / Interaktion

- keine neue Primärfunktion, aber verbesserte Fehlerrückgaben und nachvollziehbare Zustände
- bestehende Problem-JSON- und Trace-ID-Logik wird betrieblich vervollständigt

## Services / Ports / Adapter

- LLM-Provider-Facade
- eingebettetes LanguageTool
- eingebetteter Dokumentimport
- Advisor-Katalog und Advisor-Zugriffslogik
- Security- und Logging-Konfiguration

## Tests

- Unit-Tests für Konfigurationsvalidierung, Rollenlogik und Pseudonymisierung
- MockMvc-Tests für `401`, `403`, Health-Responses und Problem-JSON
- Integrations-Tests für Startfehler bei fehlender Pflichtkonfiguration
- Smoke-Tests für geschützte API-Flows mit Auth an und aus

## Definition of Done

- Produktionskonfiguration wird beim Start validiert
- Nutzungslogs sind pseudonymisiert und fachlich nutzbar
- Rollen- und Freigabelogik für Advisor funktioniert
- zentrale Diagnosepunkte und Fehlerstrategien sind vorhanden

## Handoff zur nächsten Phase

- Nächste Phase: **05 Release, Distribution und Produktionsabnahme**
