# Master-Spezifikation für die TextMate-Neuimplementierung

## Zielbild

Es soll eine neue Anwendung entstehen, die die fachlichen Kernfunktionen von TextMate in einer vereinfachten, kontrolliert inkrementellen Architektur abbildet. Die Umsetzung erfolgt nicht als Big Bang, sondern als Folge kleiner vertikaler Slices.

## Feste Architekturentscheidungen

- **Build- und Laufzeitbasis**
  - Gradle mit **Groovy DSL**
  - **Java 25** Toolchain
  - **Spring Boot 4.x**
- **Web-Stack**
  - **Spring MVC**
  - **kein WebFlux**
  - **kein `Flux`**, **kein `Mono`** in Controller- oder Service-APIs
  - Streaming ausschließlich über **SSE mit `SseEmitter`**
- **Rendering**
  - **JTE** für Layouts, Seiten und serverseitige Fragmente
  - **HTMX** für einfache servergetriebene Interaktionen und Fragmentupdates
  - **Tiptap-JavaScript-Insel** für alle editornahen High-UX-Funktionen
- **LLM und externe Dienste**
  - **Spring AI 2.x**-Linie verwenden, kompatibel zu Spring Boot 4.x
  - LanguageTool als externer HTTP-Dienst
  - Docling als externer HTTP-Dienst für Dokumentkonvertierung

## Fachliche Kernfunktionen

- Textkorrektur gegen LanguageTool
- Satz-Rewrite
- Wort-Synonyme im Kontext
- Quick Actions als Volltexttransformationen
- Advisor gegen **statische** Referenzdokumente
- Dokumentimport via Docling in den Editor

## Klare Nicht-Ziele

- Kein Big-Bang-Migrationsprojekt
- Kein Single-Endpoint-Switch für Quick Actions
- Kein reines HTMX-Frontend ohne Editor-Insel
- Kein Upload-Workflow, der Dokumente in Advisor-Regeldokumente überführt
- Keine produktive Azure/OIDC-Integration vor dem späten Polishing-Slice

## Systemgrenzen

### Serverseitig

- Layout, Seiten, Tool-Shell, Advisor-Listen, Upload-Formulare, einfache Panels
- API-Endpunkte für Fachfunktionen
- Dateibasierte Advisor-Metadaten und PDFs
- Integration zu LanguageTool, LLM und Docling

### Clientseitige Insel

- Tiptap/ProseMirror
- Undo/Redo
- Cursor/Selection-Tracking
- Satz- und Wort-Fokus
- Korrekturmarken
- Bubble-Menüs
- Rewrite-Streaming
- Diff-Ansicht
- lokales Wörterbuch
- Spiegelung des aktuellen Plaintexts in Hidden Fields / Client-State

## Öffentliche HTTP-Schnittstellen

### Page

- `GET /`
  - rendert die Hauptarbeitsfläche

### Textkorrektur

- `POST /api/text-correction`
  - Request: `{ text: string, language: string }`
  - Response: `{ original: string, blocks: CorrectionBlock[] }`

### Satz-Rewrite

- `POST /api/sentence-rewrite`
  - Request: `{ sentence: string, context: string }`
  - Response: `{ sentence: string, options: string[] }`

### Wort-Synonyme

- `POST /api/word-synonym`
  - Request: `{ word: string, context: string }`
  - Response: `{ synonyms: string[] }`

### Quick Actions

- Separate SSE-Endpoints pro Aktion:
  - `POST /api/quick-actions/plain-language/stream`
  - `POST /api/quick-actions/bullet-points/stream`
  - `POST /api/quick-actions/proofread/stream`
  - `POST /api/quick-actions/summarize/stream`
  - `POST /api/quick-actions/formality/stream`
  - `POST /api/quick-actions/social-media/stream`
  - `POST /api/quick-actions/medium/stream`
  - `POST /api/quick-actions/character-speech/stream`
  - `POST /api/quick-actions/custom/stream`
- Gemeinsamer Request-Grundtyp:
  - `{ text: string, language: string, option?: string, prompt?: string }`
- Gemeinsames SSE-Protokoll:
  - `event: chunk` mit partiellen Textteilen
  - `event: complete`
  - `event: error`

### Advisor

- `GET /api/advisor/docs`
- `GET /api/advisor/doc/{name}`
- `POST /api/advisor/validate`
  - Request: `{ text: string, docs: string[] }`
  - Response: SSE über `SseEmitter`

### Dokumentimport

- `POST /api/convert/doc`
  - Multipart-Feld `file`
  - Response: `{ html: string }`

## Domänen- und Adaptergrenzen

### Kernservices

- `TextCorrectionService`
- `SentenceRewriteService`
- `WordSynonymService`
- `QuickActionService`
- `AdvisorCatalogService`
- `AdvisorValidationService`
- `DocumentConversionService`

### Externe Adapter

- `LanguageToolClient`
- `LlmClientFacade` oder feature-spezifische LLM-Adapter
- `DoclingClient`
- `AdvisorDocumentRepository`

## Globale Umsetzungsregeln

- Ein Codex-Run implementiert **genau einen Slice**
- Spätere Slices dürfen weder teilweise noch implizit mitgebaut werden
- Gemeinsame Vorarbeit ist nur als klarer Stub oder Interface erlaubt
- Jeder Slice liefert:
  - Code
  - Tests
  - kurzen Handoff
- Jede UI-Funktion, die noch nicht implementiert ist, bleibt verborgen oder disabled

## Persistenz und Dateiquellen

- Advisor-Dokumente und Metadaten liegen dateibasiert im Projekt oder classpath-nah
- Uploads werden **nicht** persistent in den Advisor-Bestand übernommen
- Das lokale Wörterbuch ist clientseitig und browserlokal

## Qualitäts- und Testregeln

- Unit-Tests für neue Service-Logik
- MVC-/MockMvc-Tests für neue HTTP-Endpunkte
- Browser-/UI-Tests sobald sichtbare Nutzerinteraktion entsteht
- Jede Session endet mit:
  - Build grün
  - Tests grün
  - Slice-Status aktualisierbar

## Referenzen

- Analysebasis: [textmate-analysis-report.md](/Users/stefan/sources/textbuddy/docs/textmate-analysis-report.md)
- Status und Slice-Steuerung: [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)
