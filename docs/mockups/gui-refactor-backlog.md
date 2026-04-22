# GUI Refactor Backlog (Editor-First)

## P0 - Layout and IA stabilization
- [x] Editor-first Reihenfolge im Hauptbereich (Toolbar -> Editor -> Rewrite/Diff -> Quick Actions -> Import)
- [x] Feature-Chip-Block aus dem Primaerfluss entfernt und als optionale Notes-Disclosure abgebildet
- [x] Quick Actions in Gruppen aufgeteilt: Ueberarbeiten, Stil & Kanal, Narrativ, Custom
- [x] Konfigurationsbereich nur fuer ausgewaehlte Aktion sichtbar
- [x] Zentrale "Anwenden"-Interaktion statt direkter Ausfuehrung pro Button

## P0 - Functional parity
- [x] Alle vorhandenen Quick-Action-Endpoints unveraendert angebunden
- [x] Optionen/Payloads fuer Summarize, Formality, Social Media, Medium, Character Speech unveraendert
- [x] Custom Prompt Validierung (max length + required) bleibt erhalten
- [x] Streaming, Error, Success, Diff und Undo bleiben im UI sichtbar
- [x] Rewrite Bubble (Wort/Satz) unveraendert funktional
- [x] Dokumentimport inklusive OCR-Sprache unveraendert angebunden

## P0 - Test migration
- [x] Playwright-Flows auf "select action -> configure -> run" umgestellt
- [x] Home MVC Assertions an neue DOM-Elemente/TestIDs angepasst
- [x] Voller GUI-Regressionstest (`editor-island.spec.ts`) erfolgreich
- [x] Voller Gradle-Testlauf erfolgreich

## P1 - Visual refinement
- [x] Reduzierte visuelle Sprache (weniger Flaechenrauschen, klarere Hierarchie)
- [x] Quick-Action-Gruppen und aktive Auswahl klar markiert
- [x] Mobile Stack fuer Quick-Action-Bereich (1-spaltig)
- [ ] Optional: finaler Accessibility-Pass (tab order, focus ring tuning, aria-live copy audit)

## P1 - Deliverables
- [x] High-Fidelity-Mockups (Desktop idle/config/streaming+diff/advisor, Mobile stacked)
- [x] Mapping Mockup -> Funktion -> technischer Hook
- [x] Renderbare Vorschau als PNG

## Akzeptanzkriterien fuer Abschluss
- [ ] Product Sign-off auf IA + visuelle Richtung
- [ ] Accessibility-Pass abgeschlossen
- [ ] Keine offenen Regressionen in Quick-Action-, Rewrite-, Advisor- und Import-Flow
