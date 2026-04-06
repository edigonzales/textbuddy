# Phase 01: Produktiver Kern

## Ziel

Die vorhandenen fachlichen Kernfunktionen sollen von einer Stub-basierten Referenzimplementierung auf einen produktiven Kernpfad umgestellt werden. Die Anwendung soll danach ohne externen LanguageTool- oder Docling-Server lauffähig sein und echte LLM-Aufrufe ausführen.

## Funktionen

- produktive OpenAI-kompatible LLM-Anbindung statt der bisherigen Stub-Beans
- Portierung der produktiven Runtime-Prompts aus den Originalrepos
- produktive Quick Actions mit SSE-Streaming
- Sentence Rewrite mit optionalem Satzkontext
- Word-Synonym-Ermittlung über echten LLM-Call
- Advisor-Validierung mit LLM-Batching statt Keyword-Stub
- eingebettetes LanguageTool als Standardpfad
- eingebetteter Dokumentimport über Kreuzberg als Standardpfad
- Ende-zu-Ende-Weitergabe der ausgewählten Sprache

## In Scope

- `PlainLanguageLlmClient`
- `BulletPointsLlmClient`
- `ProofreadLlmClient`
- `SummarizeLlmClient`
- `FormalityLlmClient`
- `SocialMediaLlmClient`
- `MediumLlmClient`
- `CharacterSpeechLlmClient`
- `CustomLlmClient`
- `AdvisorValidationLlmClient`
- `LlmClientFacade`
- `LanguageToolClient`
- `DoclingClient`
- Prompt-Katalog für produktive Laufzeitprompts
- `POST /api/sentence-rewrite` mit optionalem `context`
- Sprachweitergabe aus der UI in Quick Actions und Korrektur

## Out of Scope

- OCR für gescannte Dokumente
- PDF-Viewer im Browser
- Textstatistiken
- PWA, Onboarding und volle I18n-Parität

## Fachliches Verhalten

- Alle bisherigen LLM-Stubs bleiben höchstens als explizites Test- oder lokales Fallback-Profil erhalten.
- Standardmäßig nutzt die Anwendung einen externen OpenAI-kompatiblen LLM-Provider.
- Sentence Rewrite verwendet optionalen Kontext, bleibt aber rückwärtskompatibel, wenn kein Kontext geliefert wird.
- Advisor arbeitet weiterhin gegen statische Referenzdokumente, aber die Prüfung erfolgt produktiv über LLM-Batches.
- LanguageTool läuft standardmäßig eingebettet in der JVM und nicht als externer HTTP-Dienst.
- Dokumentimport läuft standardmäßig eingebettet über Kreuzberg und nicht über einen externen Docling-Server.
- Die Sprachwahl wird für Korrektur und Quick Actions wirklich an den Server weitergegeben.

## Betroffene Endpoints

- `POST /api/text-correction`
- `POST /api/sentence-rewrite`
- `POST /api/word-synonym`
- `POST /api/quick-actions/plain-language/stream`
- `POST /api/quick-actions/bullet-points/stream`
- `POST /api/quick-actions/proofread/stream`
- `POST /api/quick-actions/summarize/stream`
- `POST /api/quick-actions/formality/stream`
- `POST /api/quick-actions/social-media/stream`
- `POST /api/quick-actions/medium/stream`
- `POST /api/quick-actions/character-speech/stream`
- `POST /api/quick-actions/custom/stream`
- `POST /api/advisor/validate`
- `POST /api/convert/doc`

## UI / Interaktion

- Quick Actions verwenden die echte Sprachwahl statt eines harten `auto`
- Rewrite-Bubble sendet Satzkontext mit
- Korrektursprachen werden auf den Originalumfang erweitert
- bestehende Shell, Diff-Ansicht und SSE-Interaktion bleiben erhalten

## Services / Ports / Adapter

- zentrale LLM-Provider-Facade
- produktive Prompt-Lade- und Prompt-Render-Logik
- eingebetteter LanguageTool-Adapter
- eingebetteter Kreuzberg-Adapter
- bestehende Fachservices bleiben fachlich geschnitten und erhalten produktive Adapter

## Konfiguration / Betrieb

- `textbuddy.llm.base-url`
- `textbuddy.llm.api-key`
- `textbuddy.llm.model`
- `textbuddy.llm.timeout`
- `textbuddy.llm.temperature`
- `textbuddy.llm.max-retries`
- `textbuddy.languagetool.mode=embedded|http|stub`
- `textbuddy.languagetool.ngram-path`
- `textbuddy.document.mode=kreuzberg|http|stub`

## Tests

- Unit-Tests für Prompt-Rendering, Parsing, Retry und LLM-Adapter
- MockMvc-Tests für alle betroffenen Endpoints
- Tests für eingebettetes LanguageTool mit `auto`, `de-CH`, `fr`, `it`, `en-US`, `en-GB`
- Integrations-Tests für Dokumentimport mit `pdf`, `docx`, `md`, `txt`
- Smoke-Test für `java -jar` ohne LanguageTool- und Docling-Sidecars

## Definition of Done

- keine produktive Kernfunktion hängt mehr an Stub-Logik
- Quick Actions, Rewrite, Synonyme, Advisor und Import laufen über echte Adapter
- LanguageTool und Dokumentimport benötigen im Standardpfad keine externen Sidecars
- Sprachwahl wird fachlich durchgängig genutzt

## Handoff zur nächsten Phase

- Nächste Phase: **02 Frontend-Parität und sprachliche Normalisierung**
