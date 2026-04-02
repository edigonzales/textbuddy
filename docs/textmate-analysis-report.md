# Erweiterte Analyse von TextMate inkl. Spring/JTE/HTMX-Portierbarkeit

Stand: 1. April 2026

## 1. Kurzfazit

### Direkte Antworten

- **Was kann die Anwendung heute wirklich?**
  - Sie ist primär ein textzentriertes Schreibwerkzeug mit fünf Kernfunktionen:
    1. inkrementelle Rechtschreib-/Grammatikkorrektur via LanguageTool,
    2. komplette Texttransformationen als Quick Actions via LLM,
    3. Satz-Umschreiben via LLM,
    4. Wort-Synonyme im Satzkontext via LLM,
    5. regelbasierte Dokumentprüfung gegen einen **statischen** Satz von Referenzdokumenten via LLM.
- **Kann man Dokumente hochladen?**
  - Ja. Dokumente können hochgeladen bzw. per Drag & Drop importiert werden. Sie werden über Docling nach HTML konvertiert und in den Editor eingesetzt.
- **Kann man hochgeladene Dokumente “als Dokumente” analysieren lassen?**
  - Nur indirekt. Der Upload erzeugt Editorinhalt, der danach wie normaler Text korrigiert, umgeschrieben oder mit dem Advisor gegen die **vordefinierten** Referenzdokumente geprüft werden kann.
  - Es gibt **keinen** Flow, in dem ein Upload als neues Advisor-Referenzdokument gespeichert, indexiert oder in `docs/meta` oder `docs/rules` aufgenommen wird.
- **Wie gut ist eine Portierung nach Spring Boot 4.x + Spring AI 2.x Milestone + JTE + HTMX möglich?**
  - **Backend:** gut bis sehr gut portierbar.
  - **Frontend-Shell:** gut mit JTE + HTMX portierbar.
  - **High-UX-Editor:** nicht sinnvoll als reines HTMX-Frontend, aber sehr gut mit einer **Tiptap-JavaScript-Insel**.
- **Wie viel ist aus Code ableitbar und wie viel ist Raten?**
  - Der Kern der Textverarbeitung und der Workflows ist **weitgehend direkt aus Code ableitbar**.
  - Screenshot und README sind nur ergänzende, teils veraltete Evidenz.
  - Die Spring/JTE/HTMX-Einschätzung ist eine **bewusste Architekturbeurteilung**, keine im Bestand “bewiesene” Eigenschaft.

## 2. Methode und Evidenz

### Primäre Evidenzquellen

- Backend-Repo: [DCC-BS/text-mate-backend](https://github.com/DCC-BS/text-mate-backend)
- Frontend-Repo: [DCC-BS/text-mate-frontend](https://github.com/DCC-BS/text-mate-frontend)
- Backend-Einstieg: [src/text_mate_backend/app.py](https://github.com/DCC-BS/text-mate-backend/blob/main/src/text_mate_backend/app.py)
- Frontend-Einstieg: [app/pages/index.vue](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/pages/index.vue)
- Editor-/UX-Kern: [app/composables/useTextEditor.ts](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/composables/useTextEditor.ts)
- Quick-Actions-UI: [app/components/tool-panel/TextQuickActionPanel.vue](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/components/tool-panel/TextQuickActionPanel.vue)
- Advisor-UI: [app/components/tool-panel/AdvisorView.vue](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/components/tool-panel/AdvisorView.vue)
- Upload-/Konvertierungs-Flow: [app/composables/useFileConvert.ts](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/composables/useFileConvert.ts), [src/text_mate_backend/services/document_conversion_service.py](https://github.com/DCC-BS/text-mate-backend/blob/main/src/text_mate_backend/services/document_conversion_service.py)

### Konfidenzstufen

- **hoch**: direkt aus aktuellem Code ableitbar
- **mittel**: aus Code plus Tests oder Proxy-/Dummy-Schicht abgeleitet
- **niedrig**: primär aus README oder Screenshot
- **Spekulation**: bewusste Portierungs- oder Architekturannahme

### Wichtige Grenzen der Analyse

- Das Frontend nutzt mehrere externe Nuxt-Layers, etwa für Auth, Backend-Kommunikation, Health Checks, Logging und Feedback. Diese Layer liegen nicht im Repo und sind daher nur indirekt sichtbar.
- Das Backend verwendet `dcc-backend-common` und `pydantic_ai`-Abstraktionen. Der lokale Code zeigt die fachliche Struktur klar, aber nicht jede Implementierungsdetailschicht.
- Das README-Screenshot ist **schwache Evidenz** für den aktuellen UI-Stand und wirkt in mehreren Punkten veraltet.

## 3. Feature-Inventar mit Wahrheitsstatus

| Bereich | Status | Tatsächliches Verhalten | Wichtigste Evidenz | Konfidenz |
| --- | --- | --- | --- | --- |
| Inkrementelle Korrektur | implementiert | Editorinhalt wird satzweise differenziert, nur geänderte Segmente werden an LanguageTool geschickt, Treffer werden als Marks und in einem Problems-Panel angezeigt. | [text_correction.py](https://github.com/DCC-BS/text-mate-backend/blob/main/src/text_mate_backend/routers/text_correction.py), [CorrectionService.ts](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/assets/services/CorrectionService.ts) | hoch |
| Quick Actions | implementiert | Volltext-Transformation per LLM mit Streaming zurück in den Editor. | [quick_action.py](https://github.com/DCC-BS/text-mate-backend/blob/main/src/text_mate_backend/routers/quick_action.py), [quick_action_service.py](https://github.com/DCC-BS/text-mate-backend/blob/main/src/text_mate_backend/services/actions/quick_action_service.py), [TextQuickActionPanel.vue](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/components/tool-panel/TextQuickActionPanel.vue) | hoch |
| Quick Action: Plain Language | implementiert | LLM-Rewrite in Leichte Sprache. | [plain_language_agent.py](https://github.com/DCC-BS/text-mate-backend/blob/main/src/text_mate_backend/agents/agent_types/quick_actions/plain_language_agent.py) | hoch |
| Quick Action: Bullet Points | implementiert | LLM formatiert Text als Stichpunkte. | [bullet_point_agent.py](https://github.com/DCC-BS/text-mate-backend/blob/main/src/text_mate_backend/agents/agent_types/quick_actions/bullet_point_agent.py) | hoch |
| Quick Action: Summarize | implementiert | Zusammenfassung mit Varianten `sentence`, `three_sentence`, `paragraph`, `page`, `management_summary`. | [SummarizeAction.vue](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/components/tool-panel/quick-action/SummarizeAction.vue), [summarize_agent.py](https://github.com/DCC-BS/text-mate-backend/blob/main/src/text_mate_backend/agents/agent_types/quick_actions/summarize_agent.py) | hoch |
| Quick Action: Social Media | implementiert | Social-Media-Umformung für Bluesky, Instagram, LinkedIn. | [SocialMediaAction.vue](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/components/tool-panel/quick-action/SocialMediaAction.vue), [social_media_agent.py](https://github.com/DCC-BS/text-mate-backend/blob/main/src/text_mate_backend/agents/agent_types/quick_actions/social_media_agent.py) | hoch |
| Quick Action: Formality | implementiert | Umschalten zwischen formal und informal. | [FormalityAction.vue](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/components/tool-panel/quick-action/FormalityAction.vue), [formality_agent.py](https://github.com/DCC-BS/text-mate-backend/blob/main/src/text_mate_backend/agents/agent_types/quick_actions/formality_agent.py) | hoch |
| Quick Action: Medium | implementiert | Erzeugung für Medium `email`, `official_letter`, `presentation`, `report`. | [MediumAction.vue](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/components/tool-panel/quick-action/MediumAction.vue), [medium_agent.py](https://github.com/DCC-BS/text-mate-backend/blob/main/src/text_mate_backend/agents/agent_types/quick_actions/medium_agent.py) | hoch |
| Quick Action: Custom | implementiert | Freier Prompt als benutzerdefinierte Transformation. | [CustomAction.vue](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/components/tool-panel/quick-action/CustomAction.vue), [custom_agent.py](https://github.com/DCC-BS/text-mate-backend/blob/main/src/text_mate_backend/agents/agent_types/quick_actions/custom_agent.py) | hoch |
| Quick Action: Proofread | implementiert | LLM-basiertes Korrekturlesen, getrennt von LanguageTool. | [proof_read_agent.py](https://github.com/DCC-BS/text-mate-backend/blob/main/src/text_mate_backend/agents/agent_types/quick_actions/proof_read_agent.py) | hoch |
| Quick Action: Character Speech | implementiert | Umwandlung direkte/indirekte Rede. | [CharacterSpeechAction.vue](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/components/tool-panel/quick-action/CharacterSpeechAction.vue), [character_speech_agent.py](https://github.com/DCC-BS/text-mate-backend/blob/main/src/text_mate_backend/agents/agent_types/quick_actions/character_speech_agent.py) | hoch |
| Satz umschreiben | implementiert | Bubble-Menü am fokussierten Satz, 1 bis 5 Alternativen via LLM. | [sentence_rewrite.py](https://github.com/DCC-BS/text-mate-backend/blob/main/src/text_mate_backend/routers/sentence_rewrite.py), [TextRewrite.vue](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/components/text-editor/TextRewrite.vue) | hoch |
| Wort-Synonyme | implementiert | Bubble-Menü am fokussierten Wort, Synonyme im Satzkontext via LLM. | [word_synonym.py](https://github.com/DCC-BS/text-mate-backend/blob/main/src/text_mate_backend/routers/word_synonym.py), [wordSynonym.ts](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/utils/wordSynonym.ts) | hoch |
| Advisor gegen Referenzdokumente | implementiert | Prüft Text gegen statische Regeldateien aus `docs/rules`, streamt Treffer per SSE, zeigt PDF-Referenzen an. | [advisor.py](https://github.com/DCC-BS/text-mate-backend/blob/main/src/text_mate_backend/routers/advisor.py), [services/advisor.py](https://github.com/DCC-BS/text-mate-backend/blob/main/src/text_mate_backend/services/advisor.py), [AdvisorView.vue](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/components/tool-panel/AdvisorView.vue) | hoch |
| PDF-Preview der Advisor-Dokumente | implementiert | PDF aus Backend laden, im Modal mit Seitensteuerung und Zoom anzeigen. | [AdvisorPdfViewer.client.vue](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/components/tool-panel/AdvisorPdfViewer.client.vue) | hoch |
| Dokument-Upload und Konvertierung | implementiert | Upload/Dropzone, Multipart an Backend, Docling-Konvertierung zu HTML, Einsetzen in Editor. | [TextEditor.vue](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/components/TextEditor.vue), [useFileConvert.ts](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/composables/useFileConvert.ts), [convert_route.py](https://github.com/DCC-BS/text-mate-backend/blob/main/src/text_mate_backend/routers/convert_route.py) | hoch |
| Upload als neues Advisor-Dokument | nicht vorhanden | Es gibt keinen Persistenz- oder Indexierungsflow für Uploads in `docs/meta`, `docs/rules` oder `docs/*.pdf`. | [advisor.py](https://github.com/DCC-BS/text-mate-backend/blob/main/src/text_mate_backend/routers/advisor.py), [services/advisor.py](https://github.com/DCC-BS/text-mate-backend/blob/main/src/text_mate_backend/services/advisor.py) | hoch |
| User Dictionary | implementiert | Wörterbuch im Browser via IndexedDB, mit Memory-Fallback; filtert Korrekturtreffer. | [user_dictionary.query.ts](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/assets/queries/user_dictionary.query.ts), [CorrectionFetcher.ts](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/assets/services/CorrectionFetcher.ts) | hoch |
| Sprachwahl für Korrektur | implementiert | Cookie-basierte Auswahl `auto`, `de-CH`, `fr`, `it`, `en-US`, `en-GB`. | [LanguageSelect.vue](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/components/tool-panel/LanguageSelect.vue) | hoch |
| UI-Lokalisierung | teilweise/inkonsistent | UI-Lokalisierung ist auf Deutsch und Englisch ausgelegt, Textverarbeitung selbst unterstützt mehr Sprachvarianten. | [nuxt.config.ts](https://github.com/DCC-BS/text-mate-frontend/blob/main/nuxt.config.ts), [LanguageSelect.vue](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/components/tool-panel/LanguageSelect.vue) | hoch |
| Textstatistiken / Flesch | implementiert | Zeichen, Wörter, Silben, Satzlänge, Silben pro Wort, Flesch-Score. | [TextStatsView.vue](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/components/tool-panel/TextStatsView.vue), [useTextStats.ts](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/composables/useTextStats.ts) | hoch |
| Undo / Redo | implementiert | Editor-History via Tiptap, Buttons und Tests vorhanden. | [useTextEditor.ts](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/composables/useTextEditor.ts), [undoRedo.spec.ts](https://github.com/DCC-BS/text-mate-frontend/blob/main/tests/e2e/undoRedo.spec.ts) | hoch |
| Feedback-Funktion | nur README/Screenshot bzw. Layer | Im Page-Layout gibt es `FeedbackControl`, die eigentliche Implementierung liegt aber in einem externen Nuxt-Layer. | [index.vue](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/pages/index.vue), [nuxt.config.ts](https://github.com/DCC-BS/text-mate-frontend/blob/main/nuxt.config.ts) | mittel |

## 4. Was der Dokument-Upload genau ist und was nicht

### Was tatsächlich passiert

1. Der Nutzer wählt eine Datei oder zieht sie in die Dropzone.
2. Das Frontend baut `FormData` mit `file` und sendet an `/api/convert`.
3. Der Nuxt-Server leitet an FastAPI `/convert/doc` weiter.
4. Das Backend validiert Dateityp/MIME, ruft Docling `/convert/file` auf und fordert HTML an.
5. Das Ergebnis `ConversionResult(html)` kommt zurück.
6. Das Frontend setzt den HTML-Inhalt in den Tiptap-Editor.

### Technische Eigenschaften des Uploadpfads

- Die Docling-Konvertierung ist OCR-aktiviert (`do_ocr=True`).
- Konfiguriert sind OCR-Sprachen `de`, `en`, `fr`, `it`.
- Die Konvertierung ist damit nicht nur ein “Datei öffnen”-Pfad, sondern eher ein Import-/Extraktionspfad für Text aus Dokumenten.

### Was danach möglich ist

- Der importierte Inhalt kann mit der normalen Korrektur geprüft werden.
- Er kann per Quick Actions umgeschrieben werden.
- Satz- und Wortfunktionen können darauf angewendet werden.
- Der Advisor kann den importierten Text gegen die **bestehenden** Regelwerke prüfen.

### Was **nicht** passiert

- Kein Upload wird als neues Regeldokument gespeichert.
- Kein Upload landet in `docs/meta`.
- Kein Upload landet in `docs/rules`.
- Kein Upload wird später über `/advisor/doc/{name}` wieder ausgeliefert.
- Es gibt keine separate “Dokumentanalyse-Pipeline”, die ein hochgeladenes PDF als Referenzquelle für spätere Prüfläufe registriert.

### Wichtige Nuance: Textfokus statt Rich-Document-Fokus

- Das Backend liefert HTML zurück.
- Der Editor kann HTML setzen.
- Der Frontend-Zustand wird aber in `useTextEditor.ts` laufend mit `editor.getText()` synchronisiert.
- Daraus folgt:
  - Die Anwendung behandelt Importe **praktisch als Text-Extraktion in den Editor**, nicht als vollwertige strukturtreue Dokumentbearbeitung.
  - Für Produktverständnis ist das entscheidend: Upload = “Dokumenttext in den Editor holen”, nicht “Dokumentstruktur verwalten”.

## 5. Reale End-to-End-Workflows

### 5.1 Korrektur / Problems

1. Der Haupteditor lebt in [TextEditor.vue](https://github.com/DCC-BS/text-mate-frontend/blob/main/app/components/TextEditor.vue).
2. `GrammarEditor.vue` überwacht `userText`.
3. Im Tool `correction` wird die Korrektur über `TaskScheduler` verzögert ausgelöst.
4. Bei `.` oder Zeilenumbruch wird sofort ausgeführt.
5. `CorrectionService` splittet in Sätze und vergleicht mit dem letzten Stand.
6. Nur neue/geänderte Segmente werden via `CorrectionFetcher` an `/api/correct` geschickt.
7. FastAPI `/text-correction` ruft `LanguageToolService.check_text()`.
8. Treffer werden in `CorrectionBlock`s umgesetzt.
9. Frontend filtert Wörter aus dem User Dictionary heraus.
10. Die Blöcke werden über den Command Bus in Marks/Unterstreichungen und ins Problems-Panel gespiegelt.
11. Der Nutzer kann Vorschläge direkt im Popover oder im Problems-Panel anwenden.

### 5.2 Quick Actions / Rewrite mit Streaming

1. Im Rewrite-Tool wählt der Nutzer eine Aktion.
2. Das Frontend sendet `action`, `text`, `options`.
3. Das Backend mappt `Actions` auf einen spezialisierten Agenten.
4. Der Agent streamt Text aus dem LLM zurück.
5. Das Frontend bekommt einen `ReadableStream`.
6. `useTextAction()` registriert zuerst einen Diff-Ausgangspunkt.
7. `useStreamWriter()` überschreibt den Editor live mit dem Stream.
8. Danach stellt es den finalen Text so wieder her, dass eine sinnvolle Editor-History bleibt.
9. Rechts zeigt `RewriteDiffViewer.vue` Wort-Diffs, Einzel-Undo und Undo-all.

### 5.3 Satz umschreiben

1. In Rewrite-Mode markiert `useTextFocus()` den fokussierten Satz.
2. `TextRewrite.vue` zeigt ein Bubble-Menü.
3. Beim Klick auf “rewrite sentence” ruft das Frontend `/api/sentence-rewrite`.
4. Das Backend nutzt `SentenceRewriteAgent` und liefert Alternativen.
5. Die gewählte Option ersetzt exakt den Satzbereich im Editor.

### 5.4 Wort-Synonym

1. In Rewrite-Mode wird das fokussierte Wort erkannt.
2. Das Frontend sendet `word` plus Satzkontext.
3. Das Backend nutzt `WordSynonymAgent`.
4. Die gewählte Alternative ersetzt exakt den Wortbereich.

### 5.5 Advisor

1. Beim Start lädt das Frontend verfügbare Advisor-Dokumente über `/api/advisor/docs`.
2. Das Backend liest `docs/meta/*.json`, prüft Zugriffsrechte und filtert auf Dokumente, die passende Regeldateien haben.
3. Beim Check sendet das Frontend `text` und ausgewählte `docs`.
4. Das Backend filtert Regeln aus `docs/rules/*.json`.
5. Die Regeln werden in Batches von höchstens 3 Regeln verarbeitet, insgesamt höchstens 20.
6. Für jeden Batch läuft ein LLM-Check.
7. Das Backend streamt `RulesValidationContainer` per SSE.
8. Das Frontend parst das SSE-Protokoll, dedupliziert Treffer und baut eine aggregierte Ergebnisliste.
9. Klick auf eine Referenz lädt das zugehörige PDF über `/advisor/doc/{name}` und öffnet es im PDF-Modal auf der referenzierten Seite.

### Aktuell im Repo hinterlegte Advisor-Referenzdokumente

- `empfehlungen-anglizismen-maerz-2020.pdf`
- `leitfaden_geschlechtergerechte_sprache_3aufl.pdf`
- `rechtschreibleitfaden-2017.pdf`
- `schreibweisungen.pdf`
- `merkblatt_behoerdenbriefe.pdf`

Alle derzeit hinterlegten Metadaten sind im aktuellen Repo mit Zugriff `all` versehen.

### 5.6 Dokument-Import

1. Datei wird ausgewählt oder gedroppt.
2. Das Frontend zeigt während der Konvertierung einen Sperr-Overlay.
3. FastAPI `/convert/doc` ruft `DocumentConversionService`.
4. Der Service validiert MIME anhand Dateiendung.
5. Docling liefert HTML zurück.
6. Das Frontend setzt den Inhalt in Tiptap.
7. Danach greifen die normalen Text-Tools, nicht ein separater Dokumentmodus.

## 6. Abweichungen und Inkonsistenzen

### 6.1 Diagramm vs. Code

#### Bestätigt

- Die großen Funktionsblöcke aus den Diagrammen stimmen im Kern:
  - Korrektur via LanguageTool,
  - Quick Actions via LLM-Streaming,
  - Satz-/Wortfunktionen via LLM,
  - Advisor via statische Dokumente + SSE,
  - Dokumentimport via Docling.

#### Wichtige Abweichungen

- **Advisor-Dokumente sind statisch, nicht uploadbasiert.**
  - Die Diagramme können leicht den Eindruck erwecken, Upload und Advisor seien Teil eines gemeinsamen Dokument-Subsystems.
  - Im Code sind das zwei getrennte Welten:
    - Upload = temporärer Editorimport,
    - Advisor = statische Dokumentbasis unter `docs/`.
- **Advisor ist backendseitig Multi-Doc-fähig, UI-seitig aber faktisch Single-Select.**
  - Backend erwartet `docs: set[str]`.
  - `AdvisorDocSelect.vue` bindet aber `v-model="selectedDocs[0]"`, was auf eine einzelne Auswahl hinausläuft.
- **Quick Actions sind heute konkreter und anders geschnitten als ältere “rewrite text with style/audience/intent”-Vorstellungen.**
  - Die aktuelle UI bietet diskrete Aktionen und Menüs, nicht einen generischen Rewrite-Dialog mit Stil/Zielgruppe/Intention.
- **Upload liefert HTML, aber das Produkt arbeitet danach textzentriert weiter.**
  - Das ist in Diagrammen oft nicht sichtbar, im Code aber klar.

### 6.2 README vs. Code

- **README-Screenshot ist erkennbar veraltet.**
  - Screenshot zeigt u. a. Buttons wie `Simplify`, `Shorten`, `Social Media Format` und Tabs `Problems`, `Rewrite`, `Feedback`.
  - Der aktuelle Code zeigt:
    - Tool-Switch `correction`, `rewrite`, `advisor`,
    - Quick Actions wie `plain_language`, `proofread`, `medium`, `custom`, `character_speech`.
- **README-Feature “customizable style, audience, and intent” passt nicht mehr sauber.**
  - Im aktuellen Code existiert kein solcher generischer Rewrite-Formularfluss.
  - Stattdessen gibt es:
    - feste Aktionsklassen,
    - spezifische Optionsmenüs,
    - einen freien `custom`-Prompt.
- **README beschreibt das Frontend als “modern web application for advanced text editing” korrekt im Groben, aber die konkrete heutige Tool-Semantik ist feiner und anders.**

### 6.3 Screenshot vs. Code

- **Problems/Rewrite/Feedback-Ansicht im Screenshot passt nicht mehr zum aktuellen Toolmodell.**
  - Aktuell gibt es lokal analysierbar `rewrite`, `correction`, `advisor`.
  - Feedback ist im heutigen Repo eher eine zusätzliche Control aus externem Layer, nicht ein lokales Tool-Panel.
- **Quick-Action-Namen im Screenshot passen nicht zu den aktuellen Komponenten.**
- **Das Screenshot bestätigt immerhin mit niedriger Evidenz:**
  - Split-Layout Editor links / Panel rechts,
  - unterstrichene Problemstellen,
  - Quick-Actions oben,
  - seitliches Problems-Panel.

### 6.4 Frontend-Upload vs. Backend-Konvertierung

- **Frontend akzeptiert `.doc` und `.rtf`, Backend validiert diese Typen nicht.**
  - Frontend `accept`: `.txt,.doc,.docx,.pdf,.md,.html,.rtf,.pptx`
  - Backend validiert u. a. `.pdf`, `.docx`, `.pptx`, `.html`, `.md`, `.txt`, aber nicht `.doc` oder `.rtf`.
- **Backend kann mehr als die UI anbietet.**
  - Backend-MIME-Mapping enthält zusätzlich etwa `.xlsx`, `.csv` und mehrere Bildtypen.
  - Die UI exponiert diese Formate nicht.

### 6.5 Tests vs. reale Implementierung

- **Frontend-E2E-Tests laufen großteils gegen Dummy-Fetcher.**
  - Die Nuxt-API-Routen enthalten `withDummyFetcher(...)`.
  - Die Rewrite-E2E-Tests erwarten explizit Dummy-Strings wie `Action: ...`.
  - Daraus folgt: Die Tests sichern primär Frontend-Orchestrierung und UI-Flows ab, nicht das echte FastAPI-/LLM-Verhalten.
- **Backend-Testlage wirkt teilweise veraltet.**
  - `tests/test_text_rewrite_service.py` referenziert Services/Module, die in der aktuellen `src/text_mate_backend/services`-Struktur nicht mehr vorhanden sind.
  - Für die aktuelle Quick-Action-/Advisor-Struktur ist die Testabsicherung sichtbar dünn.

## 7. Wie gut das Frontend verstanden ist

### Was sich sehr gut aus dem Code ableiten lässt

- Editor-Basis ist **Tiptap 3 / ProseMirror**.
- Der Editor ist kein austauschbares Textfeld, sondern eine koordinierte Client-Engine mit:
  - eigener Selection-Logik,
  - eigenen Marks,
  - eigener History,
  - eigener Streaming-Integration,
  - eigener Hover-/Bubble-Menü-Logik.
- Die zentrale Client-Orchestrierung ist gut sichtbar:
  - `GrammarEditor.vue`
  - `useTextEditor.ts`
  - `useTextFocus.ts`
  - `useTextCorrectionMarks.ts`
  - `useTextAction.ts`
  - `useStreamWriter.ts`
  - `CorrectionService.ts`
- Die E2E-Tests bestätigen zusätzlich die erwartete UX für:
  - Problems,
  - Rewrite-Diff,
  - Textstatistiken,
  - Undo/Redo.

### Was nur mittelgut abgesichert ist

- Advisor-UX ist im Code gut lesbar, aber weniger testgestützt.
- Upload-UX ist im Code klar, aber nicht durch End-to-End-Tests im Repo abgesichert.
- Verhalten externer Nuxt-Layers ist nur teilweise sichtbar.

### Was nicht sicher aus dem Bestand ableitbar ist

- Die genaue aktuelle Produktionsoptik.
- Nichtlokale Komponenten wie Auth-Flow, Feedback-Detailverhalten, Health-Check-Layer.
- Ob das aktuelle README-Screenshot jemals 1:1 dem heutigen `main` entsprach.

## 8. Spring Boot 4.x / Spring AI 2.x / JTE / HTMX: Portierbarkeit

## 8.1 Externer Referenzrahmen

- Laut offizieller Spring-Boot-Referenz ist **Spring Boot 4.0.5** die stabile 4.x-Version auf der Referenzseite. Quelle: [Spring Boot Reference](https://docs.spring.io/spring-boot/reference/index.html)
- Die offizielle Spring-Ankündigung vom **11. Dezember 2025** sagt, dass **Spring AI 2.0.0-M1** auf **Spring Boot 4.0** und **Spring Framework 7.0** aufsetzt. Quelle: [Spring AI 2.0.0-M1 Available Now](https://spring.io/blog/2025/12/11/spring-ai-2-0-0-M1-available-now/)
- Die aktuell sichtbare Spring-AI-Referenz unter `docs.spring.io/spring-ai/reference` ist weiterhin versioniert als **1.1.4**, beschreibt aber die relevanten APIs für `ChatClient`, Tool Calling und strukturierte Ausgabe. Diese APIs dienen hier als technische Referenz, kombiniert mit der 2.0.0-M1-Ankündigung.
- Die offizielle JTE-Dokumentation sagt, dass der **`jte-spring-boot-starter-4`** mit **Spring Boot 4.x** kompatibel ist und mit **Spring WebMVC ebenso wie WebFlux** funktioniert. Quelle: [jte Spring Boot Starter 4](https://jte.gg/spring-boot-starter-4/)
- Die offizielle HTMX-Dokumentation beschreibt HTMX als browserorientierte JS-Bibliothek, die serverseitig typischerweise **HTML statt JSON** erwartet. Quelle: [HTMX Docs](https://htmx.org/docs/)
- Die offizielle HTMX-SSE-Dokumentation beschreibt die SSE-Extension als HTML-seitige EventSource-Anbindung mit DOM-Swaps. Quelle: [HTMX SSE Extension](https://htmx.org/extensions/sse/)

## 8.2 Backend-Portierung

### Gesamturteil

- **Sehr gut portierbar.**
- Der heutige Python-Backendkern ist fachlich überschaubar und für Java/Spring konzeptionell passend:
  - klar abgegrenzte Router,
  - dünne Services,
  - LLM-Aufrufe mit klaren Prompt-/Output-Verträgen,
  - Streaming nur an zwei Stellen.

### Sinnvolle Zielarchitektur

- **Spring Boot 4 MVC** ist wahrscheinlich ausreichend und pragmatischer als eine komplette WebFlux-Architektur.
- Begründung:
  - Normale JSON-Endpunkte sind trivial.
  - Spring MVC unterstützt `ResponseBodyEmitter`, `SseEmitter` und `StreamingResponseBody`. Quelle: [Spring Framework Asynchronous Requests](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html)
  - Spring MVC kann sogar reaktive Multi-Value-Streams für `text/event-stream` adaptieren. Quelle: [Spring Framework Asynchronous Requests](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html)
- **WebFlux** ist ebenfalls möglich, aber für dieses Produkt nicht zwingend erforderlich.

### Endpoint-Mapping nach Spring

| FastAPI-Funktion | Spring-Zielbild | Bewertung |
| --- | --- | --- |
| `/text-correction` | normaler `@PostMapping` mit HTTP-Client zu LanguageTool | sehr einfach |
| `/quick-action` | `@PostMapping` + Streaming-Antwort, wahlweise `StreamingResponseBody` oder `Flux<String>` | gut |
| `/sentence-rewrite` | `@PostMapping` + strukturierte Antwort als Java-Record | sehr einfach |
| `/word-synonym` | `@PostMapping` + strukturierte Antwort als Java-Record | sehr einfach |
| `/advisor/docs` | normaler `@GetMapping`, JSON aus statischen Dateien | sehr einfach |
| `/advisor/validate` | `SseEmitter` oder `Flux<ServerSentEvent<?>>` | gut |
| `/advisor/doc/{name}` | `ResponseEntity<Resource>` oder `ResourceHttpRequestHandler` | sehr einfach |
| `/convert/doc` | Multipart-Upload + HTTP-Client zu Docling | gut |

### Spring AI statt `pydantic_ai`

- **Quick Actions**
  - Gut abbildbar mit `ChatClient`.
  - Streaming ist direkt vorgesehen. Quelle: [Chat Client API](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
  - Die meisten aktuellen Python-Agenten sind nur promptgesteuerte Textgeneratoren und brauchen keine komplexe Agent-Orchestrierung.
- **Satz-/Wort-Funktionen**
  - Sehr gut für strukturierte Ausgabe geeignet.
  - `SentenceRewriteResult` und `WordSynonymResult` lassen sich als Java-Records modellieren.
  - `ChatClient.call().entity(...)` bzw. Structured Output Converter passt fachlich sehr gut. Quellen:
    - [Chat Client API](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
    - [Structured Output Converter](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html)
- **Advisor**
  - Ebenfalls gut portierbar.
  - Die heutige Python-Logik ist bereits batchorientiert und erwartet strukturierte Resultate.
  - Ein Spring-AI-Record für `RulesValidationContainer` plus batchweiser Aufruf ist nahe am Ist-Zustand.
- **Tool Calling**
  - Optional sinnvoll, aber nicht flächendeckend nötig.
  - Der größte echte Kandidat ist die heutige `medium`-Logik, die Benutzerdaten ins LLM einbezieht.
  - Für einfache Quick Actions wie Bullet Points oder Formality ist Tool Calling eher unnötiger Overhead.
  - Tool Calling ist laut Spring AI vor allem für Informationsabruf oder Aktionen sinnvoll. Quelle: [Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html)

### Auth-Portierung

- Das heutige Muster mit Azure/OIDC lässt sich grob nach Spring Security OAuth2/OIDC abbilden.
- Da das Frontend heute bereits auf externe Auth-Layer setzt und das Backend optional Auth deaktivieren kann, ist die Fachlogik nicht eng an FastAPI gekoppelt.
- Diese Portierung ist architektonisch gut machbar, wurde hier aber nicht bis auf Token-/Claim-Mapping ausdetailliert.

## 8.3 Frontend-Portierung nach JTE + HTMX

### Gesamturteil

- **JTE + HTMX für die Shell:** gut bis sehr gut.
- **JTE + HTMX ohne Editor-Insel:** unzureichend.
- **JTE + HTMX mit Tiptap-JavaScript-Insel:** sehr realistische Zielarchitektur.

### Was gut serverseitig mit JTE renderbar ist

| Bereich | Bewertung | Begründung |
| --- | --- | --- |
| Grundlayout, Navigation, Header/Footer | gut | klassische HTML-Views |
| Tool-Umschaltung | gut | serverseitige Tabs/Fragmente oder HTMX-Swaps |
| Advisor-Dokumentliste | gut | normale serverseitige HTML-Liste oder Select |
| Advisor-Ergebnisliste | gut | HTMX-Swaps oder SSE-Fragmente |
| Problems-Panel als Liste | gut | HTML-Fragmente aus serverseitigen Daten |
| Upload-Formular | gut | normales Multipart-Formular, HTMX optional |
| PDF-Metadaten / Links / Download | gut | klassisches serverseitiges HTML |
| Statische Hilfe-/Infoboxen | gut | pures JTE |

### Was mit HTMX + etwas JavaScript machbar ist

| Bereich | Bewertung | Begründung |
| --- | --- | --- |
| Advisor-Streaming | gut | HTMX-SSE passt fachlich gut |
| Panel-Refresh nach Korrekturlauf | mittel | technisch machbar, aber der Editor selbst bleibt JS-lastig |
| Upload-Erfolg -> Panel/Status aktualisieren | gut | HTMX-Fragment oder Custom Event |
| Einfache Formularinteraktionen | gut | Standard-HTMX |

### Was ohne echte JavaScript-Insel kaum sinnvoll ist

| Bereich | Bewertung | Begründung |
| --- | --- | --- |
| Tiptap-/ProseMirror-Editor | schlecht | ist selbst eine Client-Engine |
| Inline-Korrekturmarken an Text-Ranges | schlecht | benötigt clientseitige Textpositionen und Marks |
| Hover-Popover auf markierten Ranges | schlecht | Range-gebundene Editor-Interaktion |
| Satz-/Wort-Fokus im Editor | schlecht | basiert auf Selection-Updates im Editor |
| Stream direkt in den Editor schreiben | schlecht | aktueller Flow liest `ReadableStream` und mutiert Editorzustand inkrementell |
| Diff-Viewer für Rewrite | schlecht | aktueller Diff entsteht clientseitig gegen lebenden Editorzustand |
| Undo-/Redo-Integration mit Rewrite-Streaming | schlecht | aktueller Flow rekonstruiert History gezielt im Client |
| Browserlokales User Dictionary via IndexedDB | schlecht ohne JS | ist explizit clientlokal implementiert |

## 8.4 Warum eine Tiptap-JS-Insel die richtige Grenze ist

### Was in die Insel gehört

- Tiptap-/ProseMirror-Initialisierung
- History/Undo/Redo
- Selection-Tracking
- Wort-/Satz-Fokus
- Korrektur-Marks und Hover-Logik
- Bubble-Menüs
- Streaming-Anwendung in den Editor
- Rewrite-Diff-Logik
- Browserlokales Wörterbuch
- lokale Textstatistiken, wenn die Reaktionsgeschwindigkeit hoch bleiben soll

### Was aus Nuxt wegfallen kann

- Nuxt-Server-Proxy-Routen
- `apiHandler`-Wrapper
- `serviceRegistrant.ts` und Teile der clientseitigen Dependency Injection
- ein großer Teil der Nuxt-spezifischen Kompositions-/Plugin-Hülle
- einige Nuxt-UI-spezifische Wrapperkomponenten

### Wie JTE + HTMX + Insel zusammenspielen würden

- JTE rendert die Grundseite, Toolpanels, Formulare und Ergebnisfragmente.
- HTMX lädt/aktualisiert Fragmente und kann Advisor-SSE gut abbilden.
- Die Tiptap-Insel lebt in einem klar abgegrenzten DOM-Bereich.
- HTMX kann über `htmx.onLoad(...)` bzw. Load-Events die Insel initialisieren oder neu verbinden, falls Fragmente ersetzt werden. Quelle: [HTMX Docs](https://htmx.org/docs/)

### Wichtige Einschränkung zu HTMX-SSE

- HTMX-SSE ist stark für “Server schickt HTML-Fragmente in ein Panel”.
- Der heutige Rewrite-Flow ist aber **kein** reiner Fragment-Stream:
  - er streamt Rohtext,
  - schreibt ihn live in den Editor,
  - rekonstruiert danach die History,
  - berechnet clientseitig Diffs.
- Genau dafür ist eine JS-Insel deutlich passender als HTMX allein.

## 9. Empfehlung für eine Zielarchitektur

### Empfohlener Stack

- **Spring Boot 4 MVC**
- **Spring Security** für OIDC/Azure
- **Spring AI 2.x Milestone/GA-Nachfolger**, sobald für Projektpolitik akzeptabel
- **JTE** für serverseitige Seiten und Fragmente
- **HTMX 2.x** für Form-/Panel-/SSE-Interaktionen
- **Tiptap 3 als isolierte Editor-Insel**

### Empfohlene Aufteilung

- **Server-rendered**
  - Rahmenlayout
  - Tool-Switch
  - Advisor-Seitenbereich
  - Problems-Liste, soweit nicht direkt an Editor-Marks gekoppelt
  - Upload-Form und Status
  - PDF-View-Trigger
- **JS-Insel**
  - gesamter Editor
  - alle textpositionsabhängigen Interaktionen
  - Streaming-Rewrite
  - lokale Diff-/Undo-/Dictionary-Logik

### Warum das realistisch ist

- Das aktuelle Frontend ist trotz Nuxt bereits stark in klaren Client-Module zerlegt.
- Der Editor-Kern ist technisch fast schon eine eigenständige Applikation innerhalb der Seite.
- Genau diese Form eignet sich gut für eine Insel-Migration.

## 10. Was sicher ist, was unsicher ist

### Sicher aus dem Code ableitbar

- vorhandene API-Endpunkte
- fachliche Kernfeatures
- Streaming- und SSE-Pfade
- statischer Charakter des Advisor-Dokumentbestands
- Upload-als-Konvertierung, nicht Upload-als-Regeldokument
- Einsatz von Tiptap
- Clientlogik für Marks, Selection, StreamWriter, Diff, Undo/Redo, IndexedDB-Wörterbuch

### Nur mittelbar abgesichert

- exaktes Laufzeitverhalten externer Nuxt-Layers
- Produktionsverhalten aller Auth- und Feedback-Flows
- reale UI-Details dort, wo nur Screenshot oder Dummy-Tests vorliegen

### Spekulation / Architektururteil

- konkrete Entscheidung MVC vs WebFlux in einer Neuumsetzung
- exakte Spring-AI-2.x-GA-Reife zum Umsetzungszeitpunkt
- ob das Wörterbuch in der Neuumsetzung clientlokal bleiben oder serverseitig persistiert werden sollte

## 11. Quellen

### TextMate-Code

- Backend: [https://github.com/DCC-BS/text-mate-backend](https://github.com/DCC-BS/text-mate-backend)
- Frontend: [https://github.com/DCC-BS/text-mate-frontend](https://github.com/DCC-BS/text-mate-frontend)
- README-Screenshot: [Frontend README](https://github.com/DCC-BS/text-mate-frontend/blob/main/README.md)

### Offizielle Technologiequellen

- Spring Boot Reference: [https://docs.spring.io/spring-boot/reference/index.html](https://docs.spring.io/spring-boot/reference/index.html)
- Spring AI ChatClient: [https://docs.spring.io/spring-ai/reference/api/chatclient.html](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
- Spring AI Tool Calling: [https://docs.spring.io/spring-ai/reference/api/tools.html](https://docs.spring.io/spring-ai/reference/api/tools.html)
- Spring AI Structured Output: [https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html)
- Spring AI 2.0.0-M1 Announcement: [https://spring.io/blog/2025/12/11/spring-ai-2-0-0-M1-available-now/](https://spring.io/blog/2025/12/11/spring-ai-2-0-0-M1-available-now/)
- Spring Framework MVC Async / SSE: [https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html)
- Spring Framework WebFlux Return Types: [https://docs.spring.io/spring-framework/reference/web/webflux/controller/ann-methods/return-types.html](https://docs.spring.io/spring-framework/reference/web/webflux/controller/ann-methods/return-types.html)
- JTE Spring Boot Starter 4: [https://jte.gg/spring-boot-starter-4/](https://jte.gg/spring-boot-starter-4/)
- HTMX Docs: [https://htmx.org/docs/](https://htmx.org/docs/)
- HTMX SSE Extension: [https://htmx.org/extensions/sse/](https://htmx.org/extensions/sse/)
