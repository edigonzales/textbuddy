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
| 02 | ready | Text Correction Base |
| 03 | pending | Text Correction Enhancements |
| 04 | pending | Sentence Rewrite |
| 05 | pending | Word Synonym |
| 06 | pending | Quick Action Infrastructure |
| 07 | pending | Quick Action Bullet Points |
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
