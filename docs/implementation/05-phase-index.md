# Phasen-Index zur Produktionsreife

Nach Abschluss der Slices 00 bis 18 folgt die Produktionsreife nicht mehr in kleinen Vertikalslices, sondern in wenigen größeren Umsetzungsphasen. Jede Phase hat ein eigenes Phasendokument und eine eigene Prompt-Datei im Stil der bisherigen Slice-Dokumente.

| Phase | Titel | Hauptziel | Kernfunktionen | Abhängigkeiten | Done-Kriterium |
| --- | --- | --- | --- | --- | --- |
| 01 | Produktiver Kern | Stubs durch echte Kernintegration ersetzen | LLM-Provider, Prompt-Katalog, eingebettetes LanguageTool, eingebetteter Dokumentimport, Advisor-LLM, Sprachweitergabe | abgeschlossene Slices 00-18 | Kernfunktionen laufen produktiv ohne LanguageTool-/Docling-Sidecars |
| 02 | Frontend-Parität und sprachliche Normalisierung | Zentrale UX-Lücken schließen und UI-Texte bereinigen | Umlaute in UI-Texten, erweiterte Sprachwahl, PDF-Viewer, Textstatistiken | 01 | Kern-UX liegt näher am Original und deutsche UI-Texte verwenden echte Umlaute |
| 03 | Lokale OCR und Dokumentimport-Qualität | Gescannte Dokumente lokal importieren und Importqualität härten | OCR, Import-Nachbearbeitung, Upload-Limits, robuste Formatmatrix | 01 | Gescannte Dokumente funktionieren ohne externen Docling-Server |
| 04 | Betriebsfähigkeit, Sicherheit und Observability | Produktionsbetrieb absichern | Health-Checks, Startvalidierung, Rollenlogik, pseudonymisierte Nutzungslogs, Timeouts, Retry | 01-03 | System ist betrieblich nachvollziehbar, abgesichert und fehlertolerant |
| 05 | Release, Distribution und Produktionsabnahme | Auslieferung als `java -jar textbuddy.jar` abschließen | Packaging, Initialisierung lokaler Assets, Runbook, Abnahme- und Smoke-Tests | 01-04 | Ausgeliefertes Artefakt läuft ohne Sidecars und ist abnahmefähig |

## Zugehörige Dokumente

- [Phase 01 Produktiver Kern](/Users/stefan/sources/textbuddy/docs/implementation/phases/phase-01-produktiver-kern.md)
- [Phase 02 Frontend-Parität und sprachliche Normalisierung](/Users/stefan/sources/textbuddy/docs/implementation/phases/phase-02-frontend-paritaet-und-sprachliche-normalisierung.md)
- [Phase 03 Lokale OCR und Dokumentimport-Qualität](/Users/stefan/sources/textbuddy/docs/implementation/phases/phase-03-lokale-ocr-und-dokumentimport-qualitaet.md)
- [Phase 04 Betriebsfähigkeit, Sicherheit und Observability](/Users/stefan/sources/textbuddy/docs/implementation/phases/phase-04-betriebsfaehigkeit-sicherheit-und-observability.md)
- [Phase 05 Release, Distribution und Produktionsabnahme](/Users/stefan/sources/textbuddy/docs/implementation/phases/phase-05-release-distribution-und-produktionsabnahme.md)

- [Prompt Phase 01](/Users/stefan/sources/textbuddy/docs/implementation/prompts/phase-01-produktiver-kern.prompt.md)
- [Prompt Phase 02](/Users/stefan/sources/textbuddy/docs/implementation/prompts/phase-02-frontend-paritaet-und-sprachliche-normalisierung.prompt.md)
- [Prompt Phase 03](/Users/stefan/sources/textbuddy/docs/implementation/prompts/phase-03-lokale-ocr-und-dokumentimport-qualitaet.prompt.md)
- [Prompt Phase 04](/Users/stefan/sources/textbuddy/docs/implementation/prompts/phase-04-betriebsfaehigkeit-sicherheit-und-observability.prompt.md)
- [Prompt Phase 05](/Users/stefan/sources/textbuddy/docs/implementation/prompts/phase-05-release-distribution-und-produktionsabnahme.prompt.md)
