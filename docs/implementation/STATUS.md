# Implementierungsstatus

## Nutzung

- Dieses Dokument wird nach jeder Codex-Session aktualisiert.
- Pro Session darf genau **ein** Slice von `pending` nach `done` wechseln.
- Optional darf genau ein nächster Slice als `ready` markiert werden.
- Nach Abschluss aller Slices werden die weiteren Umsetzungsstände zusätzlich über die Produktionsphasen gepflegt.
- Wenn ein Slice blockiert ist, dokumentiere den Grund knapp im Handoff-Bereich.

## Aktueller Stand der Slices

| Slice | Status | Kurzbeschreibung |
| --- | --- | --- |
| 00 | done | Project Basis |
| 01 | done | Editor Island Base |
| 02 | done | Text Correction Base |
| 03 | done | Text Correction Enhancements |
| 04 | done | Sentence Rewrite |
| 05 | done | Word Synonym |
| 06 | done | Quick Action Infrastructure |
| 07 | done | Quick Action Bullet Points |
| 08 | done | Quick Action Proofread |
| 09 | done | Quick Action Summarize |
| 10 | done | Quick Action Formality |
| 11 | done | Quick Action Social Media |
| 12 | done | Quick Action Medium |
| 13 | done | Quick Action Character Speech |
| 14 | done | Quick Action Custom |
| 15 | done | Advisor Catalog and PDF |
| 16 | done | Advisor Validation |
| 17 | done | Document Import |
| 18 | done | Auth and Polish |

## Produktionsphasen

Details und Abgrenzung der Phasen stehen im [05-phase-index.md](/Users/stefan/sources/textbuddy/docs/implementation/05-phase-index.md) sowie in den zugehörigen Phasen- und Prompt-Dokumenten.

| Phase | Status | Kurzbeschreibung |
| --- | --- | --- |
| 01 | done | Produktiver Kern: echte LLM-Integration, eingebettetes LanguageTool, eingebetteter Dokumentimport |
| 02 | done | Frontend-Parität und sprachliche Normalisierung |
| 03 | done | Lokale OCR und Dokumentimport-Qualität |
| 04 | done | Betriebsfähigkeit, Sicherheit und Observability |
| 05 | done | Release, Distribution und Produktionsabnahme |

## Empfohlene Reihenfolge

- Standardstart ist **Slice 00**
- Danach strikt numerisch fortfahren, sofern kein dokumentierter Blocker besteht
- Nach Abschluss von Slice 18 mit **Phase 01** fortfahren und danach die Phasen strikt numerisch abschließen

## Handoff-Notizen

- Post-Phase Verbesserungen umgesetzt: GitHub-Actions-Workflows `ci.yml` und `release.yml` sind ergänzt, Release-Signierung mit `textbuddy.jar.sha256`, `textbuddy.jar.asc` und `textbuddy.jar.sha256.asc` ist im Tag-Workflow automatisiert, und ein optionales ZIP-Installer-Artefakt `textbuddy-<version>.zip` mit Startskripten für Unix/Windows ist dokumentiert sowie im Build verankert.
- Post-Phase Verbesserungen Build/Test-Handoff: `./gradlew clean verifyReleaseBundle installerZip` ist grün, `./gradlew test --tests 'app.textbuddy.smoke.InstallerStartScriptSmokeTest' --tests 'app.textbuddy.smoke.JarStartupSmokeTest' --tests 'app.textbuddy.smoke.JarEndToEndSmokeTest' --tests 'app.textbuddy.smoke.JarPerformanceSmokeTest'` ist grün, und die neuen Workflow-Dateien sind YAML-valide.
- Phase 05 abgeschlossen: Das Release-Artefakt läuft als `textbuddy.jar` im Standardpfad `java -jar textbuddy.jar`, ein reproduzierbarer `releaseBundle`-Pfad liefert Jar plus Runbook/Checklisten/Konfigurationsbeispiele, lokale Laufzeitressourcen werden beim Start kontrolliert initialisiert, und die Phase-05-Abnahme ist über Jar-basierte Smoke-, End-to-End-, Lifecycle-/Recoverability- und Performance-Smoke-Tests dokumentiert.
- Phase 05 Build/Test-Handoff: `./gradlew test --tests 'app.textbuddy.config.RuntimeResourceInitializerTest' --tests 'app.textbuddy.web.health.ActuatorObservabilityMvcTest' --tests 'app.textbuddy.smoke.JarStartupSmokeTest' --tests 'app.textbuddy.smoke.JarEndToEndSmokeTest' --tests 'app.textbuddy.smoke.JarPerformanceSmokeTest'` ist grün, und `./gradlew clean releaseBundle` ist grün; das Bundle enthält `build/release/textbuddy.jar`, `docs/release/*` sowie `config/examples/*`.
- Phase 04 abgeschlossen: Die Anwendung validiert Pflichtkonfiguration und Advisor-Metadaten beim Start, liefert betriebliche Diagnose über `/actuator/health` und `/actuator/info`, protokolliert `/api/**`-Nutzung pseudonymisiert mit Trace-ID, erzwingt rollenbasierten Advisor-Zugriff über dateibasierte Dokumentfreigaben und nutzt konsistente Timeout-/Retry-/Fehlerstrategien für produktive HTTP-Adapter (LanguageTool, Docling) sowie nachvollziehbare SSE-Diagnoseprotokolle.
- Phase 04 Build/Test-Handoff: `./gradlew test` ist grün. Der Lauf enthält Unit-Tests für Konfigurationsnormalisierung, Rollen- und Pseudonymisierungslogik, MockMvc-Tests für `401`/`403`/Health/Problem-JSON, Integrations-Tests für Startfehler bei fehlender Pflichtkonfiguration sowie Smoke-Tests für geschützte API-Flows mit Auth an und aus. Nächste reguläre Phase ist **05**.
- Phase 03 abgeschlossen: Dokumentimport unterstützt lokale OCR mit den Sprachen `de`, `en`, `fr`, `it`, nutzt Runtime-gefilterte Formatfreigaben inklusive bildbasierter Formate, verarbeitet Import-HTML editorfreundlich nach, erzwingt Upload-Limits und Timeout-Logik im Standardpfad und liefert kontrollierte Problem-JSON-Fehler für große, beschädigte oder nicht verarbeitbare Dateien.
- Phase 03 Build/Test-Handoff: `./gradlew test --tests 'app.textbuddy.document.DefaultDocumentConversionServiceTest' --tests 'app.textbuddy.document.EditorFriendlyHtmlPostProcessorTest' --tests 'app.textbuddy.integration.docling.KreuzbergDoclingClientTest' --tests 'app.textbuddy.integration.docling.KreuzbergDoclingClientOcrFallbackTest' --tests 'app.textbuddy.web.document.DocumentConversionControllerMvcTest' --tests 'app.textbuddy.web.document.DocumentConversionUploadLimitsMvcTest' --tests 'app.textbuddy.web.page.HomePageMvcTest' --tests 'app.textbuddy.web.error.ErrorHandlingMvcTest'` ist grün, `./gradlew test --tests 'app.textbuddy.smoke.CoreFlowsSmokeMvcTest'` ist grün, und `npm test -- editor-island.spec.ts` unter `playwright/` ist grün. OCR-Integrationsläufe für gescannte Dokumente werden automatisch übersprungen, wenn die lokale OCR-Runtime zwar vorhanden, aber ohne betriebsbereite Sprachdaten gestartet ist.
- Phase 01 abgeschlossen: Produktive OpenAI-kompatible LLM-Adapter mit Prompt-Katalog sind aktiv, `POST /api/sentence-rewrite` akzeptiert optional `context`, Advisor-Validierung läuft über LLM-Batches, LanguageTool läuft standardmässig eingebettet, Dokumentimport standardmässig über eingebettetes Kreuzberg, und die Sprachwahl wird aus der UI an Korrektur sowie Quick Actions durchgereicht.
- Phase 01 Build/Test-Handoff: `./gradlew test --rerun-tasks --tests 'app.textbuddy.integration.llm.*' --tests 'app.textbuddy.integration.languagetool.EmbeddedLanguageToolClientTest' --tests 'app.textbuddy.integration.docling.KreuzbergDoclingClientTest' --tests 'app.textbuddy.web.sentencerewrite.SentenceRewriteControllerMvcTest' --tests 'app.textbuddy.web.quickaction.MediumQuickActionControllerMvcTest' --tests 'app.textbuddy.smoke.JarStartupSmokeTest'` ist grün; `npm test -- --grep "language selection is sent with correction requests|language selection is sent with quick action requests|word synonym uses the focused word context and replaces only that range"` unter `playwright/` ist grün. Nächste reguläre Phase ist **02**.
- Phase 02 abgeschlossen: Deutschsprachige nutzersichtbare Texte in Shell und Browserlogik nutzen echte Umlaute, die Korrektursprachwahl umfasst `auto`, `de-CH`, `fr`, `it`, `en-US`, `en-GB`, der Advisor-Bereich enthält einen eingebetteten PDF-Viewer mit Seitensteuerung, Zoom und Download, und ein neues Textstatistik-Panel liefert Zeichen, Wörter, Silben, Sätze sowie Flesch-Lesbarkeit.
- Phase 02 Build/Test-Handoff: `npm run test:unit` unter `frontend/`, `npm test -- editor-island.spec.ts` unter `playwright/` sowie `./gradlew test --tests 'app.textbuddy.web.page.HomePageMvcTest' --tests 'app.textbuddy.web.advisor.AdvisorCatalogControllerMvcTest' --tests 'app.textbuddy.web.error.ErrorHandlingMvcTest' --tests 'app.textbuddy.quickaction.FormalityQuickActionServiceTest' --tests 'app.textbuddy.quickaction.SocialMediaQuickActionServiceTest' --tests 'app.textbuddy.quickaction.CharacterSpeechQuickActionServiceTest' --tests 'app.textbuddy.quickaction.CustomQuickActionServiceTest' --tests 'app.textbuddy.quickaction.CustomQuickActionPromptTest'` sind grün. Nächste reguläre Phase ist **03**.
- Slice 00 abgeschlossen: Gradle-Groovy-Basis, Spring-Boot-4-MVC-Shell mit JTE/HTMX, Frontend-Workspace, Kernservice-/Adapter-Stubs und Basistests stehen.
- Slice 01 abgeschlossen: Die Tiptap-Insel ist in `GET /` integriert und liefert lokalen Plain-Text-Editor mit Hidden Mirror, Zeichen-/Wortzaehlern, Undo/Redo sowie den Events `editor:text-changed` und `editor:selection-changed`.
- Slice 01 Build/Test-Handoff: Frontend-Assets werden per Gradle aus `frontend/` nach Spring-Static-Resources gebaut; MockMvc prueft die Seiteneinbindung, Playwright deckt Tippen, Mirror und Undo/Redo ab.
- Slice 02 abgeschlossen: `POST /api/text-correction` liefert Volltext-Korrekturblöcke, `TextCorrectionService` mappt LanguageTool-Matches mit Default-Sprache `auto`, und die Editor-Insel markiert Probleme inklusive Problems-Panel und Suggestion-Apply.
- Slice 02 Build/Test-Handoff: `./gradlew build` ist gruen, Unit- und MockMvc-Tests decken Service und Endpoint ab, und Playwright prueft Markierung plus Anwenden eines Vorschlags im Browser.
- Slice 03 abgeschlossen: Die Editor-Insel waehlt jetzt die Korrektursprache, speichert ein lokales Woerterbuch browserlokal und prueft nur geaenderte Segmente; Punkt und Zeilenumbruch ueberspringen die Debounce-Verzoegerung.
- Slice 03 Build/Test-Handoff: Frontend-Unit-Tests decken Segment-Diff, Soforttrigger und Woerterbuchfilter ab, MockMvc bestaetigt die unveraenderte Shell/Endpoint-Nutzung, und Playwright prueft Sprache, lokales Woerterbuch sowie differenzierte Re-Checks; `./gradlew build` und `npm test` unter `playwright/` sind gruen.
- Slice 04 abgeschlossen: `POST /api/sentence-rewrite` liefert alternative Formulierungen ueber `SentenceRewriteService`, die Editor-Insel erkennt den fokussierten Satz, blendet ein Bubble-Menue ein und ersetzt nach Auswahl exakt nur dessen Range.
- Slice 04 Build/Test-Handoff: Java-Unit- und MockMvc-Tests decken Service und Rewrite-Endpoint ab, Frontend-Unit-Tests pruefen Satzfokus-Ranges, und Playwright validiert Bubble, Request-Satz und exakten Satzaustausch; `./gradlew test` und `npm test` unter `playwright/` sind gruen.
- Slice 05 abgeschlossen: `POST /api/word-synonym` liefert kontextbezogene Synonyme ueber `WordSynonymService`, die Editor-Insel kombiniert Wort- und Satzfokus in einer gemeinsamen Rewrite-Bubble, priorisiert Wortkontext und haelt Satzumformung ueber denselben Bubble-Kontext erreichbar.
- Slice 05 Build/Test-Handoff: Java-Unit- und MockMvc-Tests decken Service und Endpoint ab, Frontend-Unit-Tests pruefen Wortfokus, Satzfokus und die kombinierte Modusaufloesung, und Playwright validiert Wortmodus, Satzmodus, exakten Wortaustausch sowie Satz-Rewrite aus dem Wortkontext; `./gradlew test`, `npm run test:unit` unter `frontend/` und `npm test` unter `playwright/` sind gruen.
- Slice 06 abgeschlossen: `POST /api/quick-actions/plain-language/stream` streamt Plain-Language-Rewrites per `SseEmitter`, die Editor-Insel nutzt einen gemeinsamen SSE-POST-Client plus gemeinsamen Rewrite-Stream-Handler, und nach Abschluss erscheinen Diff-Ansicht sowie kompletter Rewrite-Undo.
- Slice 06 Build/Test-Handoff: Java-Tests decken SSE-Payloads, Home-Page-Shell und den Plain-Language-Streaming-Endpoint ab, Frontend-Unit-Tests laufen weiter unveraendert, und Playwright validiert Stream-Verarbeitung, Diff-Anzeige und kompletten Undo; `./gradlew test` und `npm test` unter `playwright/` sind gruen.
- Slice 07 abgeschlossen: `POST /api/quick-actions/bullet-points/stream` nutzt dieselbe `SseEmitter`-, Diff- und Undo-Infrastruktur wie Plain Language, waehrend die Editor-Insel genau eine weitere aktive Quick Action fuer strukturierte Stichpunkte freischaltet.
- Slice 07 Build/Test-Handoff: Unit-Tests decken den Bullet-Points-Servicepfad in `DefaultQuickActionService` ab, MockMvc prueft den neuen Streaming-Endpoint, und Playwright validiert Button, Stream, Diff und Undo fuer Bullet Points; naechster Slice ist `08 Quick Action Proofread`.
- Slice 08 abgeschlossen: `POST /api/quick-actions/proofread/stream` nutzt einen eigenen Proofread-Service mit separatem LLM-Adapter und haengt sich in dieselbe `SseEmitter`-, Diff- und Undo-Infrastruktur wie die bestehenden Volltext-Rewrites.
- Slice 08 Build/Test-Handoff: Unit-Tests decken `ProofreadQuickActionService` ab, MockMvc prueft den neuen Streaming-Endpoint, und Playwright validiert die Proofread-Aktion inklusive Stream, Diff und Undo; naechster Slice ist `09 Quick Action Summarize`.
- Slice 09 abgeschlossen: `POST /api/quick-actions/summarize/stream` nutzt einen eigenen Summarize-Service mit festem Options-Mapping fuer `sentence`, `three_sentence`, `paragraph`, `page` und `management_summary`; die Toolbar bietet dafuer ein eigenes Dropdown, waehrend Streaming, Diff und kompletter Undo unveraendert wiederverwendet werden.
- Slice 09 Build/Test-Handoff: Parameterisierte Unit-Tests decken das Options-Mapping ab, MockMvc prueft den Summarize-Endpoint inklusive Pflichtfeld `option`, und Playwright validiert mindestens zwei Summarize-Varianten im Browser; naechster Slice ist `10 Quick Action Formality`.
- Slice 10 abgeschlossen: `POST /api/quick-actions/formality/stream` nutzt einen eigenen Formality-Service mit dediziertem Request-Typ und festem Options-Mapping fuer `formal` und `informal`; die Toolbar bietet dafuer ein eigenes Dropdown, waehrend Streaming, Diff und kompletter Undo unveraendert wiederverwendet werden.
- Slice 10 Build/Test-Handoff: Unit-Tests decken Formality-Optionsvalidierung und Service-Mapping ab, MockMvc prueft den neuen Streaming-Endpoint inklusive Pflichtfeld `option`, und Playwright validiert beide Varianten im Browser; naechster Slice ist `11 Quick Action Social Media`.
- Slice 11 abgeschlossen: `POST /api/quick-actions/social-media/stream` nutzt einen eigenen Social-Media-Service mit festem Kanal-Mapping fuer `bluesky`, `instagram` und `linkedin`; die Toolbar bietet dafuer ein eigenes Dropdown, waehrend Streaming, Diff und kompletter Undo unveraendert wiederverwendet werden.
- Slice 11 Build/Test-Handoff: Unit-Tests decken Kanal-Mapping und Service-Validierung ab, MockMvc prueft den neuen Social-Media-Endpoint inklusive Pflichtfeld `option`, und Playwright validiert mindestens zwei Kanaele im Browser; `./gradlew test`, `npm test` unter `playwright/` und `./gradlew build` sind gruen. Naechster Slice ist `12 Quick Action Medium`.
- Slice 12 abgeschlossen: `POST /api/quick-actions/medium/stream` nutzt einen eigenen Medium-Service mit festem Typ-Mapping fuer `email`, `official_letter`, `presentation` und `report`; die Toolbar bietet dafuer ein eigenes Dropdown, waehrend Streaming, Diff und kompletter Undo unveraendert wiederverwendet werden.
- Slice 12 Build/Test-Handoff: Unit-Tests decken Medium-Optionsvalidierung und Service-Mapping ab, MockMvc prueft den neuen Streaming-Endpoint inklusive Pflichtfeld `option`, und Playwright validiert mindestens zwei Medium-Typen im Browser; naechster Slice ist `13 Quick Action Character Speech`.
- Slice 13 abgeschlossen: `POST /api/quick-actions/character-speech/stream` nutzt einen eigenen Character-Speech-Service mit festem Options-Mapping fuer `direct_speech` und `indirect_speech`; die Toolbar bietet dafuer ein eigenes Dropdown, waehrend Streaming, Diff und kompletter Undo unveraendert wiederverwendet werden.
- Slice 13 Build/Test-Handoff: Unit-Tests decken Character-Speech-Optionsvalidierung und Service-Mapping ab, MockMvc prueft den neuen Streaming-Endpoint inklusive Pflichtfeld `option`, und Playwright validiert beide Varianten im Browser; naechster Slice ist `14 Quick Action Custom`.
- Slice 14 abgeschlossen: `POST /api/quick-actions/custom/stream` nutzt einen eigenen Custom-Service mit dedizierter Prompt-Aufbereitung und Request-Validierung fuer das Pflichtfeld `prompt`; die Toolbar bietet dafuer ein freies Prompt-Feld, waehrend Streaming, Diff und kompletter Undo unveraendert wiederverwendet werden.
- Slice 14 Build/Test-Handoff: Unit-Tests decken Prompt-Aufbereitung, Request-Validierung und Service-Fehlerpfade ab, MockMvc prueft den neuen Streaming-Endpoint inklusive Pflichtfeld `prompt`, und Playwright validiert Custom-Prompt, Stream und Ergebnis im Browser; naechster Slice ist `15 Advisor Catalog and PDF`.
- Slice 15 abgeschlossen: `GET /api/advisor/docs` liefert einen statischen dateibasierten Advisor-Katalog aus JSON-Metadaten, `GET /api/advisor/doc/{name}` liefert die zugehoerigen Demo-PDFs inline aus, und die Home-Shell zeigt dafuer ein Advisor-Panel mit Mehrfachauswahl sowie PDF-Links.
- Slice 15 Build/Test-Handoff: Unit-Test deckt das Repository-Laden ab, MockMvc prueft beide GET-Endpunkte und die erweiterte Home-Shell, Playwright validiert Dokumentliste plus PDF-Erreichbarkeit; `./gradlew test`, `./gradlew build` und `npm test` unter `playwright/` sind gruen. Naechster Slice ist `16 Advisor Validation`.
- Slice 16 abgeschlossen: `POST /api/advisor/validate` laedt statische Dokumentregeln aus den Advisor-Metadaten, prueft sie in kleinen Batches ueber einen dedizierten LLM-Adapter und streamt Treffer als `validation`-Events per `SseEmitter`; das Advisor-Panel startet die Pruefung direkt aus der Dokumentauswahl, dedupliziert clientseitig ueber `stableKey` und zeigt eine laufende Trefferliste mit Dokument- und Seitenbezug.
- Slice 16 Build/Test-Handoff: Unit-Tests decken Batch-Aufteilung und Regel-Streaming in `DefaultAdvisorValidationService` ab, MockMvc prueft den neuen SSE-Endpoint, Frontend-Unit-Tests validieren die clientseitige Deduplizierung, und Playwright deckt Start, Event-Empfang, Deduplizierung sowie die Auswahl eines gestreamten Treffers im Browser ab; `./gradlew build` und `npm test` unter `playwright/` sind gruen. Naechster Slice ist `17 Document Import`.
- Slice 17 abgeschlossen: `POST /api/convert/doc` nimmt Multipart-Uploads entgegen, `DefaultDocumentConversionService` validiert das Dateiformat gegen einen gemeinsamen Formatkatalog, `DoclingClient` liefert HTML ueber einen konfigurierbaren Docling-v1-Adapter oder einen lokalen Stub, und die Tiptap-Insel importiert das HTML jetzt per Upload-Button oder Drag-and-Drop direkt in den Editor.
- Slice 17 Build/Test-Handoff: Unit-Tests decken Dateiformatvalidierung in `DefaultDocumentConversionService` ab, MockMvc prueft den neuen Multipart-Endpoint, die Home-Shell rendert Upload-Panel und identische Formatliste, und Playwright validiert Upload, HTML-Import, saubere Ablehnung nicht unterstuetzter Formate sowie alle bestehenden Editor-Pfade weiter; `./gradlew build` und `npm test -- editor-island.spec.ts` unter `playwright/` sind gruen. Naechster Slice ist `18 Auth and Polish`.
- Slice 18 abgeschlossen: `textbuddy.auth.enabled` schaltet eine OIDC-Grundintegration fuer bestehende `/api/**`-Endpoints zu, die Home-Shell zeigt den Auth-Zustand an, API-Fehler liefern jetzt ein konsistentes JSON-Fehlerformat mit `traceId`, HTML-Fehlerseiten werden ueber eine eigene Error-View gerendert, und Logging/Adapterkonfigurationen sind fuer lokalen Stub-Betrieb und spaetere Produktion klarer getrennt.
- Slice 18 Build/Test-Handoff: `./gradlew test` ist gruen, zusaetzliche MVC-Tests decken Auth an/aus, Fehlerseiten, Problem-JSON und einen Smoke-Test ueber Kernfluesse ab; kein weiterer regulaerer Slice vorgesehen.
