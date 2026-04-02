# Slice 18: Auth and Polish

## Ziel

Die Anwendung fachlich abschließen und produktionsnäher machen: OIDC-Grundintegration, Fehlerbild, Logging, Konfigurationspolitur und letzte UI-Aktivierungen.

## In Scope

- Spring-Security/OIDC-Grundintegration
- schaltbare Auth-Aktivierung über Konfiguration
- globale Fehlerbehandlung
- Logging-Politur
- letzte UI-Aufräumarbeiten
- Build- und Startdokumentation vervollständigen

## Out of Scope

- neue Fachfeatures
- zusätzliche Quick Actions

## Fachliches Verhalten

- APIs und UI lassen sich mit oder ohne aktivierte Auth-Konfiguration starten.
- Fehler werden konsistent gerendert oder als API-Fehler ausgegeben.
- Logging und Konfiguration sind für lokale Entwicklung und spätere Produktion brauchbar.

## Betroffene Endpoints

- keine neuen Pflicht-Endpoints
- bestehende Endpoints werden ggf. abgesichert

## UI / Interaktion

- letzte disabled Platzhalter entfernen, falls alle Kernfeatures vorhanden sind
- Fehlerfälle verständlich anzeigen

## Services / Ports / Adapter

- Security-Konfiguration
- Error-Handling
- Logging-/Konfigurationsschicht

## DTOs / Typen

- nur falls für Fehlerformat nötig

## Persistenz / Dateiquellen

- keine neue Persistenz

## Tests

- Security-/Config-Tests
- MockMvc-Tests für typische Fehlerfälle
- Smoke-Test über die wichtigsten Flows

## Definition of Done

- Anwendung ist funktional vollständig
- Konfiguration und Sicherheit sind in tragfähiger Grundform vorhanden
- letzte Doku- und Qualitätslücken sind geschlossen

## Handoff zum nächsten Slice

- Kein weiterer regulärer Slice vorgesehen
