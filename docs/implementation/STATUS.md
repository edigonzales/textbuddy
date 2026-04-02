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
| 07 | ready | Quick Action Bullet Points |
| 08 | pending | Quick Action Proofread |
| 09 | pending | Quick Action Summarize |
| 10 | pending | Quick Action Formality |
| 11 | pending | Quick Action Social Media |
| 12 | pending | Quick Action Medium |
| 13 | pending | Quick Action Character Speech |
| 14 | pending | Quick Action Custom |
| 15 | pending | Advisor Catalog and PDF |
| 16 | pending | Advisor Validation |
| 17 | pending | Document Import |
| 18 | pending | Auth and Polish |

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
