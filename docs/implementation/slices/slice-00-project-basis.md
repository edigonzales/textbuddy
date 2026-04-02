# Slice 00: Project Basis

## Ziel

Ein buildbares Grundgerüst für einen Spring-Boot-4-MVC-Monolithen schaffen, inklusive JTE, HTMX, Frontend-Buildpfad für die spätere Tiptap-Insel und klarer Paket- und Projektstruktur.

## In Scope

- Gradle-Projekt mit **Groovy DSL**
- **Java 25** Toolchain
- Spring Boot 4.x, Spring MVC, JTE, Actuator, Spring AI 2.x-kompatible Dependency-Vorbereitung
- Basis-Paketstruktur unter `app.textbuddy`
- `TextbuddyApplication`
- `GET /` mit JTE-Rendering
- leere, aber sichtbare Arbeitsoberfläche mit Platzhaltern
- Frontend-Arbeitsbereich für die Insel unter `frontend/` mit Vite + TypeScript
- Testgrundlage:
  - JUnit 5
  - MockMvc
  - Playwright-Verzeichnis für spätere Browser-Tests
- Interface- und Stub-Schicht für alle Kernservices
- leere Konfigurationsklassen für externe Systeme

## Out of Scope

- echte Textkorrektur
- echte Editorfunktion
- echte API-Endpunkte außer `GET /`
- Auth
- externe HTTP-Integrationen

## Fachliches Verhalten

- Die Anwendung startet lokal.
- `GET /` rendert eine Shell mit:
  - Header
  - Editor-Container-Platzhalter
  - rechtem Panel-Platzhalter
  - deaktivierten oder rein visuellen Tool-Bereichen
- Noch keine fachliche Interaktion.

## Betroffene Endpoints

- `GET /`

## UI / Interaktion

- JTE-Layout mit Hauptseite
- HTMX kann bereits eingebunden sein, wird in diesem Slice aber noch nicht produktiv genutzt
- Platzhaltertexte müssen klar machen, dass spätere Slices die Bereiche füllen

## Services / Ports / Adapter

- Interfaces und Stub-Beans für:
  - `TextCorrectionService`
  - `SentenceRewriteService`
  - `WordSynonymService`
  - `QuickActionService`
  - `AdvisorCatalogService`
  - `AdvisorValidationService`
  - `DocumentConversionService`
- Adapter-Interfaces oder Clients für:
  - `LanguageToolClient`
  - `LlmClientFacade`
  - `DoclingClient`
  - `AdvisorDocumentRepository`

## DTOs / Typen

- Lege die zukünftigen DTO-/Record-Typen als leere, aber compilebare Grundstruktur an:
  - `CorrectionRequest`, `CorrectionResponse`, `CorrectionBlock`
  - `SentenceRewriteRequest`, `SentenceRewriteResponse`
  - `WordSynonymRequest`, `WordSynonymResponse`
  - `QuickActionStreamRequest`
  - `AdvisorDocsResponseItem`, `AdvisorValidateRequest`, `AdvisorValidationEvent`
  - `DocumentConversionResponse`

## Persistenz / Dateiquellen

- Lege leere classpath-nahe Verzeichnisstruktur an für spätere Advisor-Daten:
  - `advisor/meta`
  - `advisor/rules`
  - `advisor/docs`

## Tests

- `contextLoads`
- MockMvc-Test für `GET /` mit Status 200
- einfacher View-/HTML-Test: Shell enthält Editor- und Panel-Platzhalter

## Definition of Done

- Gradle-Build läuft
- App startet
- `GET /` rendert erfolgreich
- Tests für diesen Slice sind grün
- Es gibt noch keine halbfertigen Fachfeatures

## Handoff zum nächsten Slice

- Nächster Slice: **01 Editor Island Base**
- Frontend-Buildpfad und Grundseite stehen; als Nächstes wird die Tiptap-Insel funktional gemacht
