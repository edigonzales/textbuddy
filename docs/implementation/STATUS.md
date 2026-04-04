# Implementierungsstatus

## Nutzung

- Dieses Dokument wird nach jeder Codex-Session aktualisiert.
- Pro Session darf genau **ein** Slice von `pending` nach `done` wechseln.
- Optional darf genau ein nächster Slice als `ready` markiert werden.
- Wenn ein Slice blockiert ist, dokumentiere den Grund knapp im Handoff-Bereich.

## Aktueller Stand

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
| 18 | ready | Auth and Polish |

## Empfohlene Reihenfolge

- Standardstart ist **Slice 00**
- Danach strikt numerisch fortfahren, sofern kein dokumentierter Blocker besteht

## Handoff-Notizen

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
