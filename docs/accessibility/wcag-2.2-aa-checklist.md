# WCAG 2.2 AA Checklist - Editor Workflow

Stand: 2026-04-19  
Scope: Home-/Editor-Oberflaeche (Quick Actions, Rewrite, Korrektur, Import, Advisor, Viewer)

## Summary
- Zielniveau: WCAG 2.2 AA
- Status: Keine offenen Blocker/High Findings fuer den Editor-Hauptworkflow
- Automatisierung: Playwright + axe fuer zentrale Zustandsbilder

## Checklist
| WCAG Kriterium | Status | Prioritaet | Nachweis im Code | Ergebnis |
| --- | --- | --- | --- | --- |
| 2.4.1 Bypass Blocks (Skip Link) | Pass | High | `src/main/jte/layout/main.jte`, `src/main/resources/static/styles/app.css` | Skip-Link springt zum `#main-content`. |
| 1.3.1 Info and Relationships (Landmarks/Labels) | Pass | High | `src/main/jte/pages/home.jte` | `aria-labelledby` fuer Hauptbereiche (Editor, Korrektur, Advisor), klare Abschnittsstruktur. |
| 3.3.2 Labels or Instructions (Forms) | Pass | High | `src/main/jte/pages/home.jte` | Dictionary-Input hat explizites Label statt Placeholder-only. |
| 2.1.1 Keyboard | Pass | High | `frontend/src/editor/document-import.ts`, `frontend/src/editor/rewrite-bubble.ts`, `frontend/src/editor/advisor-pdf-viewer.ts` | Enter/Space und Escape-Handling fuer relevante Flows. |
| 2.4.7 Focus Visible | Pass | High | `src/main/resources/static/styles/app.css`, `frontend/src/style.css` | Einheitlicher, sichtbarer Focus-Ring fuer interaktive Elemente. |
| 4.1.2 Name, Role, Value | Pass | High | `frontend/src/editor/quick-action-stream.ts`, `frontend/src/editor/document-import.ts`, `frontend/src/editor/advisor-validation.ts`, `frontend/src/editor/text-correction.ts` | `aria-disabled`, `aria-busy`, `aria-pressed` und Rollen bei dynamischen UI-Zustaenden synchronisiert. |
| 4.1.3 Status Messages | Pass | High | gleiche Dateien wie oben + `home.jte` | Live-Regionen (`role=status`/`role=alert`, `aria-live`, `aria-atomic`) fuer Loading/Success/Error. |
| 1.4.3 Contrast (Minimum) | Pass | Medium | `app.css`, `style.css`, axe checks | Farben fuer Fokus- und Statuskommunikation auf AA-geeignetes Niveau angepasst. |
| 1.4.1 Use of Color | Pass | Medium | `home.jte`, Status-Texte in TS | Zustaende werden nicht nur ueber Farbe, sondern auch ueber Text vermittelt. |
| 1.4.10 Reflow (Mobile) | Pass | Medium | `app.css`, `frontend/src/style.css`, mobile Playwright test | Gestapelte Mobile-Darstellung bleibt bedienbar. |

## Open Items (kein Blocker)
- Medium: Kein manueller Screenreader-Usability-Durchlauf (NVDA/VoiceOver) im aktuellen Pass enthalten.
- Low: Optionaler Feinschliff fuer Fokusreihenfolge im Advisor-Detail bei sehr langen Trefferlisten.

## Test Coverage
- Neue A11y-Testdatei: `playwright/tests/editor-island.a11y.spec.ts`
- Abgedeckte Zustaende:
  - Idle
  - Quick-Action Konfiguration
  - Streaming + Diff
  - Advisor Result + Viewer
  - Mobile Layout
- Zusatztests:
  - Keyboard Enter/Space im Import-Flow
  - Escape fuer Rewrite-Bubble + Fokusrueckgabe
  - Escape fuer Advisor-Viewer + Fokusrueckgabe
