# Feste Codex-Regeln für alle Implementierungssessions

## Technische Defaults

- **Gradle Groovy**
- **Java 25**
- **Spring Boot 4.x**
- **Spring MVC**
- **JTE**
- **HTMX**
- **Tiptap-Insel**
- **SSE via `SseEmitter`**

## Harte Architekturregeln

- Kein WebFlux
- Kein `Flux`
- Kein `Mono`
- Keine reaktiven Controller
- Keine Quick-Action-Switch-API mit `action`-Multiplexing
- Keine Persistenz von Uploads als Advisor-Regeldokumente

## Harte Arbeitsregeln

- Implementiere **nur** den angefragten Slice
- Führe keine spätere Slice vorzeitig ein
- Lege nur minimale Stubs für spätere Arbeit an, wenn zwingend nötig
- Verstecke oder disable spätere UI-Funktionen
- Schreibe Tests für alles, was im Slice neu hinzukommt

## Mindestoutput jeder Codex-Session

- kurze Zusammenfassung des umgesetzten Slices
- Liste geänderter Dateien
- Liste der ausgeführten Tests
- offene Punkte oder bewusste Stubs
- Vorschlag für den nächsten Slice

## Qualitätsstandard

- Controller-Logik schlank halten
- Services fachlich schneiden
- Adapter für externe Systeme trennen
- Prompts nicht inline im Controller
- DTOs klar und klein halten
- Keine unnötige Generalisierung auf spätere Features

## UI-Regeln

- Serverseitige Shell mit JTE/HTMX
- Editornahe Logik nur in der JS-Insel
- Keine serverseitige Nachbildung von Tiptap-Interaktionsdetails

## Testregeln

- neue Services: Unit-Tests
- neue Endpoints: MockMvc-Tests
- sichtbare Interaktionen: Browser-/UI-Test, sobald praktikabel
