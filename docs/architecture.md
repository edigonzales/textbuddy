# Architektur

## Aufbau

Textbuddy ist eine einzelne Spring-Boot-Anwendung für einen zentral betriebenen Team-Server. Es gibt keine Datenbank und keinen verteilten Anwendungszustand. Diese Begrenzung ist Absicht: Deployment, Fehlersuche und Weiterentwicklung sollen ohne zusätzliche Infrastruktur verständlich bleiben.

```mermaid
flowchart LR
    Browser["Browser: JTE + Tiptap"] -->|"JSON / Multipart / SSE"| App["Textbuddy Spring Boot"]
    App --> LLM["OpenAI-kompatibles LLM"]
    App --> LT["LanguageTool eingebettet"]
    App --> Doc["Kreuzberg lokal"]
    App --> Advisor["Advisor-Metadaten und PDFs im JAR"]
```

- `web`: HTTP-Verträge, Eingabegrenzen, Fehlerabbildung und Startseite.
- Fachpakete wie `quickaction`, `textcorrection`, `advisor` und `document`: kleine Services und unveränderliche Request-/Response-Typen.
- `integration`: die eingebettete LanguageTool-Komponente und die LLM-Anbindung. `TextCorrectionService` nutzt `EmbeddedLanguageToolClient` direkt. Alle LLM-Funktionen verwenden `TextbuddyLlmClient`.
- `config`: das Konfigurationsaggregat `TextbuddyProperties` und die Auswahl der LLM-Implementierung.
- `frontend`: eine TypeScript-/Tiptap-Insel; keine zweite eigenständige Webanwendung.

## HTTP-Verträge

| Endpoint | Format | Zweck |
| --- | --- | --- |
| `POST /api/text-correction` | JSON | Rechtschreib- und Stilhinweise |
| `POST /api/quick-actions/plain-language`, `POST /api/quick-actions/summarize` | JSON | Volltext-Überarbeitung oder Zusammenfassung |
| `GET /api/advisor/docs` | JSON | Statischer Advisor-Katalog |
| `GET /api/advisor/doc/{name}` | PDF | Referenzdokument für einen neuen Browser-Tab |
| `POST /api/advisor/validate` | SSE | Regelprüfung mit Befunden, Batchfortschritt und Abschlussereignis |
| `POST /api/convert/doc` | Multipart/JSON | Extrahierten Dokumenttext als `{"text": "..."}` liefern |

Die Advisor-Prüfung verarbeitet Regeln in Dreierbatches. Das SSE-Abschlussereignis `complete` enthält `incomplete: true`, wenn eine Regel- oder Befundgrenze Ergebnisse begrenzt oder Befunde ungültig, mehrdeutig oder widersprüchlich sind. Eine ungültige Provider-Antwort führt zu einem Fehler. Ein Stream ohne gültiges Abschlussereignis gilt als fehlgeschlagen.

LanguageTool und Advisor liefern halb offene UTF-16-Bereiche im unveränderten Request-Text. Der Advisor verifiziert die vom Provider kopierten Fundstellen anhand des exakten Kontexts und ergänzt die Quellen aus dem lokalen Katalog. Mit jedem Befund liefert der Server den konkreten Ersatztext, einen leeren String für eine Löschung oder `null` für einen Befund ohne ausführbare Änderung. Der Browser prüft die ausgewählten Bereiche und ihre Originalausschnitte und erstellt daraus lokal die gemeinsame Vorschau. Beim Öffnen und Übernehmen prüft er den ursprünglichen Textstand. Die angenommenen Änderungen bilden eine Undo-Transaktion.

Der Dokumentservice prüft den Upload und ruft `KreuzbergDocumentExtractor` direkt auf. Kreuzberg liefert Text und Absatzumbrüche bei deaktiviertem Extraktionscache. Der Editor speichert Text in Absätzen mit optionalen manuellen Zeilenumbrüchen. Bei der Positionsabbildung sind Absatzgrenzen und manuelle Umbrüche jeweils ein LF. Die Zwischenablage verwendet verfügbaren Plaintext. DOCX exportiert jede Editorzeile als Absatz.

## Zustand und Daten

- Advisor-Metadaten und PDFs werden beim Start einmal aus dem Klassenpfad geladen. Alle angemeldeten Benutzer sehen denselben Katalog.
- Die mitgelieferten Advisor-Dokumente sind projektinterne Demo-Regelwerke. Eigene Regelwerke werden als geprüftes JSON-/PDF-Paar in das Artefakt gebaut; Runtime-Uploads und Persistenz existieren nicht.
- Das persönliche Wörterbuch liegt ausschliesslich in `localStorage` des Browsers. Es gibt keine Synchronisierung oder Serversicherung.
- Bearbeitete Texte und hochgeladene Dokumente werden nicht dauerhaft gespeichert.
- Provider- und Netzwerkadapter führen keine automatischen Wiederholungen aus. Einzige Ausnahme ist der lokale OCR-Adapter: Scheitert eine explizit gewählte Nicht-Standardsprache an einem OCR-Fehler, versucht er die Erkennung einmal mit der Standardsprache.

## Sicherheitsgrenzen

Im Produktionsmodus sind nur Login, Fehlerseite und statische Assets öffentlich; alle übrigen Routen verlangen eine OIDC-Anmeldung. Zustandsändernde Browseranfragen tragen ein CSRF-Token. Ein ungeschützter Start ist nur bei expliziter Loopback-Bindung möglich. Upload- und Textgrössen werden vor teurer Verarbeitung geprüft. CSP und `X-Frame-Options: SAMEORIGIN` erlauben eigene PDF-Ressourcen, aber keine fremde Einbettung.

Externe Provider sind eine eigene Datenschutzgrenze: Für LLM-Funktionen erforderliche Texte verlassen den Textbuddy-Prozess. Die Entscheidung dafür und die Aufbewahrungsregeln des Providers gehören zur Betriebskonfiguration.
