# GUI Refactor Backlog (Editor-First)

## P0 - Layout and IA stabilization
- [x] Schlanke App-Bar mit Produktname, Auth-/Betriebsstatus und UI-Sprache
- [x] Editor-first Hauptbereich: Toolbar, Editorfläche, Rewrite Bubble, Diff und sekundäre Notes
- [x] Rechter Inspector mit Tabs für Korrektur, Aktionen, Advisor, Import und Statistik
- [x] Mobile Darstellung stapelt Editor und Inspector; die Tabs bleiben horizontal bedienbar
- [x] Feature-Chip-Block aus dem Primärfluss entfernt und als optionale Notes-Disclosure abgebildet
- [x] Quick Actions in Gruppen aufgeteilt: Überarbeiten, Stil & Kanal, Narrativ, Eigener Auftrag
- [x] Konfigurationsbereich nur für ausgewählte Aktion sichtbar
- [x] Zentrale "Anwenden"-Interaktion statt direkter Ausführung pro Button

## P0 - Functional parity
- [x] Alle vorhandenen Quick-Action-Endpoints unverändert angebunden
- [x] Optionen/Payloads für Zusammenfassen, Ton ändern, Social Media, Format anpassen und Rede umformen unverändert
- [x] Custom Prompt Validierung (max length + required) bleibt erhalten
- [x] Streaming, Error, Success, Diff und Undo bleiben im UI sichtbar
- [x] Rewrite Bubble (Wort/Satz) unverändert funktional
- [x] Dokumentimport inklusive OCR-Sprache unverändert angebunden
- [x] Korrektur, Advisor, Import und Statistik sind über Inspector-Tabs erreichbar, ohne API-Verträge zu verändern

## P0 - Test migration
- [x] Playwright-Flows auf "select action -> configure -> run" umgestellt
- [x] Home MVC Assertions an neue DOM-Elemente/TestIDs angepasst
- [x] Voller GUI-Regressionstest (`npm test` im Playwright-Workspace) erfolgreich
- [x] Voller Gradle-Testlauf erfolgreich

## P1 - Visual refinement
- [x] Reduzierte visuelle Sprache (weniger Flächenrauschen, klarere Hierarchie)
- [x] Quick-Action-Gruppen und aktive Auswahl klar markiert
- [x] Mobile Stack für Editor und Inspector
- [x] Accessibility-Pass für kritische/serious axe Findings und zentrale Keyboard-Flows
- [ ] Optional: manueller Screenreader-Pass mit VoiceOver/NVDA

## P1 - Deliverables
- [x] High-Fidelity-Mockups (Desktop idle/config/streaming+diff/advisor, Mobile stacked)
- [x] Mapping Mockup -> Funktion -> technischer Hook
- [x] Renderbare Vorschau als PNG

## Akzeptanzkriterien für Abschluss
- [ ] Product Sign-off auf IA + visuelle Richtung
- [x] Automatisierter Accessibility-Pass abgeschlossen
- [x] Keine offenen Regressionen in Quick-Action-, Rewrite-, Advisor- und Import-Flow
