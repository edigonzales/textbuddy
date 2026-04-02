# Slice-Index

| Slice | Titel | Hauptziel | Endpoints | UI-Bereich | Abhängigkeiten | Done-Kriterium |
| --- | --- | --- | --- | --- | --- | --- |
| 00 | Project Basis | Spring-Boot-MVC-Grundgerüst | `GET /` | leere Shell | keine | App startet, Basis-Tests grün |
| 01 | Editor Island Base | Tiptap-Basis mit Mirror und Undo/Redo | `GET /` | Editor-Insel | 00 | Tippen, Zähler, Undo/Redo |
| 02 | Text Correction Base | erste LanguageTool-Korrektur | `POST /api/text-correction` | Korrekturmarken + Problems | 01 | Fehler markieren und anwenden |
| 03 | Text Correction Enhancements | Sprache, Wörterbuch, inkrementelle Prüfung | `POST /api/text-correction` | Korrektur-UX | 02 | nur geänderte Segmente prüfen |
| 04 | Sentence Rewrite | Satzalternativen im Kontext | `POST /api/sentence-rewrite` | Bubble-Menü Satz | 01 | Satz ersetzen |
| 05 | Word Synonym | Synonyme im Satzkontext | `POST /api/word-synonym` | Bubble-Menü Wort | 01 | Wort ersetzen |
| 06 | Quick Action Infrastructure | SSE-Streaming und Diff-Basis | `POST /api/quick-actions/plain-language/stream` | Rewrite-Streaming | 01 | Live-Stream + Diff + Undo |
| 07 | Quick Action Bullet Points | Bullet-Point-Transformation | `POST /api/quick-actions/bullet-points/stream` | Rewrite-Toolbar | 06 | Aktion läuft isoliert |
| 08 | Quick Action Proofread | LLM-Proofread | `POST /api/quick-actions/proofread/stream` | Rewrite-Toolbar | 06 | Aktion läuft isoliert |
| 09 | Quick Action Summarize | Zusammenfassungen mit Optionen | `POST /api/quick-actions/summarize/stream` | Rewrite-Toolbar | 06 | Aktion läuft isoliert |
| 10 | Quick Action Formality | formal/informal | `POST /api/quick-actions/formality/stream` | Rewrite-Toolbar | 06 | Aktion läuft isoliert |
| 11 | Quick Action Social Media | social media Varianten | `POST /api/quick-actions/social-media/stream` | Rewrite-Toolbar | 06 | Aktion läuft isoliert |
| 12 | Quick Action Medium | medium-spezifische Erzeugung | `POST /api/quick-actions/medium/stream` | Rewrite-Toolbar | 06 | Aktion läuft isoliert |
| 13 | Quick Action Character Speech | direkte/indirekte Rede | `POST /api/quick-actions/character-speech/stream` | Rewrite-Toolbar | 06 | Aktion läuft isoliert |
| 14 | Quick Action Custom | freier Benutzerprompt | `POST /api/quick-actions/custom/stream` | Rewrite-Toolbar | 06 | Aktion läuft isoliert |
| 15 | Advisor Catalog and PDF | Dokumentliste und PDF-Auslieferung | `GET /api/advisor/docs`, `GET /api/advisor/doc/{name}` | Advisor-Auswahl | 00 | Docs laden, PDF öffnen |
| 16 | Advisor Validation | SSE-Regelprüfung | `POST /api/advisor/validate` | Advisor-Ergebnisse | 15 | Batches und Deduplizierung |
| 17 | Document Import | Docling-Import in Editor | `POST /api/convert/doc` | Upload/Drag&Drop | 01 | HTML-Import in Editor |
| 18 | Auth and Polish | OIDC, Errors, Logging, Finalisierung | diverse | übergreifend | 00-17 | produktionsnahe Abrundung |
