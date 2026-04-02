# Codex Prompt: Slice 18 Auth and Polish

Nutze als verbindliche Quellen:

- [00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [02-codex-rules.md](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [slice-18-auth-and-polish.md](/Users/stefan/sources/textbuddy/docs/implementation/slices/slice-18-auth-and-polish.md)
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Implementiere **nur Slice 18: Auth and Polish**.

## Globale Regeln

- Gradle mit **Groovy DSL**
- **Java 25**
- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Streaming ist grundsätzlich nur über **`SseEmitter`** zulässig
- Keine späteren Features oder Slices vorziehen

## In Scope

- Spring Security/OIDC-Grundintegration
- App-Konfigurationspolitur
- Fehlerseiten und Logging-Verbesserungen
- Produktionshärtung für bereits vorhandene Features

## Out of Scope

- neue Fachfeatures
- neue API-Endpunkte außerhalb notwendiger Sicherheits- oder Fehlerseitenintegration

## Architekturregeln

- **Nur Spring MVC**
- **Kein WebFlux**, **kein `Flux`**, **kein `Mono`**
- Bestehende Fachfeatures nicht neu schneiden
- Keine nachträgliche Großrefaktorierung ohne direkten Nutzen für diesen Slice

## Tests

- Unit-Tests für neue Sicherheits- oder Konfigurationslogik
- MockMvc-Tests für Security-Verhalten, soweit sinnvoll
- Browser-Test nur, wenn Login- oder Fehlerseiten sichtbar betroffen sind

## Abschlussformat

Gib am Ende aus:

- kurze Zusammenfassung
- Liste geänderter Dateien
- Liste ausgeführter Tests
- offene Stubs oder bewusste Nicht-Implementierungen
- Vorschlag für den nächsten Schritt nach Abschluss aller Slices
