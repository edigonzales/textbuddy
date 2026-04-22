# GUI Refactor Mapping (Mockup -> bestehende Funktion -> technischer Hook)

## Verwendete Mockups
- High-Fidelity-Mockups: [gui-refactor-high-fidelity.html](/Users/stefan/sources/textbuddy/docs/mockups/gui-refactor-high-fidelity.html)

## Mapping
| UI-Element im Mockup | Bestehende Funktion | Technischer Hook im Code |
| --- | --- | --- |
| Action-Pills (gruppiert) | Auswahl einer Quick Action | `data-quick-action` in [home.jte](/Users/stefan/sources/textbuddy/src/main/jte/pages/home.jte), Auswahl-Logik in [quick-action-stream.ts](/Users/stefan/sources/textbuddy/frontend/src/editor/quick-action-stream.ts) |
| Konfigurationsbereich (progressiv) | Anzeigen von `option` / `prompt` je Aktion | `data-quick-action-selected-action`, `data-quick-action-config`, `data-quick-action-option`, `data-quick-action-prompt` |
| Zentraler `Anwenden`-Button | Startet den aktiven Rewrite-Stream | `data-quick-action-run`, `runQuickAction(selectedAction)` |
| Streaming-Statusbanner | Laufender SSE-Status / Fehler / Erfolg | `data-quick-action-status`, `setPanelState(...)` |
| Rewrite-Diff Vorher/Nachher | Transparente Änderungssicht | `data-rewrite-diff-before`, `data-rewrite-diff-after`, `createRewriteDiff(...)` |
| Rewrite rückgängig | Vollständiges Undo des letzten Volltext-Rewrites | `data-rewrite-diff-undo`, `completedRewrite.original` |
| Editor Toolbar (Undo/Redo + Counter) | Editor-Basisfunktionen | `data-editor-action`, `data-editor-count`, `mountEditorIsland()` |
| Dokumentimport (Dropzone + OCR) | Dateiimport inkl. OCR-Sprache | `data-document-import-*`, [document-import.ts](/Users/stefan/sources/textbuddy/frontend/src/editor/document-import.ts) |
| Advisor Trefferliste + Detail | Regelprüfung, Auswahl, Kontextanzeige | `data-advisor-result-*`, [advisor-validation.ts](/Users/stefan/sources/textbuddy/frontend/src/editor/advisor-validation.ts) |
| PDF Viewer öffnen | Navigierbarer Dokumentkontext | `data-advisor-open`, `data-advisor-result-detail-open`, [advisor-pdf-viewer.ts](/Users/stefan/sources/textbuddy/frontend/src/editor/advisor-pdf-viewer.ts) |

## Zustandsmodell (Quick Actions)
- `idle`: Aktion gewählt, bereit zum Anwenden
- `streaming`: SSE aktiv, Editor gesperrt
- `success`: Rewrite abgeschlossen, Diff sichtbar
- `error`: Fehlerstatus mit API-/SSE-Fehlermeldung

## Keine Vertragsänderungen
- Keine Änderungen an API-Endpoints oder Payload-Formaten.
- UI-Orchestrierung und DOM-Struktur wurden überarbeitet; bestehende Backend-Verträge bleiben intakt.
