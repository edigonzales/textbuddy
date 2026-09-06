# Funktionsübersicht

Der Textbuddy-MVP verwendet eine breite, TextMate-artige Arbeitsfläche. Farben, Typografie, Abstände und Zustandsfarben stammen weiterhin aus dem Textbuddy-Designsystem.

## Sichtbare MVP-Werkzeuge

Textbuddy startet bei jedem Aufruf im Modus **Überarbeiten**. In der normalen Oberfläche sind genau drei Werkzeuge freigeschaltet:

- **Korrektur** prüft den vollständigen Text nach einer kurzen Eingabepause mit genau einer abbrechbaren Anfrage.
- **Verständlicher schreiben** bearbeitet den vollständigen Text.
- **Zusammenfassen** bietet die Varianten ein Satz, drei Sätze, ein Absatz, eine Seite und Management Summary.

Die vorhandenen Backend-Funktionen und HTTP-Schnittstellen für Advisor, Synonyme, Satzvarianten, Aufzählungen, Proofread, Formalität, Social Media, Medium, Figurenrede und eigene Aktionen bleiben bewusst erhalten. Zugehörige, nicht erreichbare Frontend-Module und versteckte DOM-Bereiche werden dagegen nicht mit ausgeliefert.

## Überarbeiten

Im Startmodus füllt der Editor die verfügbare Breite bis maximal etwa 72 rem aus. Das Ribbon enthält die Zusammenfassungsvarianten und **Verständlicher schreiben**. Korrekturen laufen auch in diesem Modus im Hintergrund; vorhandene Befunde werden als Zähler bei **Prüfen** und als Markierungen im Text sichtbar, öffnen jedoch keine Seitenleiste.

Ein Klick auf eine Korrekturmarkierung wechselt direkt zu **Prüfen**, öffnet die zugehörigen Ergebnisse und fokussiert den Befund.

## Prüfen und persönliches Wörterbuch

Der Modus **Prüfen** bündelt im Ribbon:

- den aktuellen Prüfstatus,
- die gemeinsame Text- und Korrektursprache,
- **Ergebnisse (n)** zum Öffnen oder Schliessen der Ergebnisleiste,
- einen Wiederholungsbutton, falls die Prüfung fehlgeschlagen ist.

Die Ergebnisleiste existiert nur bei mindestens einem Befund. Auf dem Desktop liegt sie ohne Kartenabstand rechts neben dem Editor; mobil öffnet sie als Slideover. Bei null Befunden verschwindet sie vollständig. Vorschläge können direkt übernommen werden.

Das zunächst eingeklappte persönliche Wörterbuch liegt ausschliesslich im lokalen Browserspeicher. Es wird weder synchronisiert noch serverseitig gespeichert.

Verfügbare Textsprachen sind automatische Erkennung, Deutsch (Schweiz), Französisch, Italienisch, Englisch (USA) und Englisch (UK).

## Transformationen prüfen

**Verständlicher schreiben** und **Zusammenfassen** verändern den Editor nicht sofort. Textbuddy zeigt das Ergebnis zuerst in einer vollbreiten Diff-Prüfung:

- Standardmässig erscheint ein Inline-Diff; eine Zweispaltenansicht ist umschaltbar.
- Mehrere Änderungen können einzeln angenommen oder abgelehnt werden.
- Bis zu insgesamt 10.000 Eingabezeichen wird ein Wort-Diff berechnet. Darüber zeigt Textbuddy einen einzigen Dokumentblock zum globalen Annehmen oder Ablehnen, damit die Oberfläche responsiv bleibt.
- **Alle annehmen**, **Alle ablehnen**, **Erneut ausführen** und **Abbrechen** bearbeiten den gesamten Review-Ablauf.
- Erst wenn alle Änderungen entschieden sind, wird das aufgelöste Ergebnis in einer einzigen rückgängig machbaren Editortransaktion übernommen.
- Abbrechen, vollständiges Ablehnen, Fehler und ungültige leere Antworten lassen den Originaltext unverändert.

Bei einem unveränderten Resultat erscheint **Keine Änderungen gefunden**. Nach einer Übernahme startet die automatische Korrektur erneut.

Ist als Textsprache ausdrücklich **Deutsch (Schweiz)** gewählt, zeigt **Verständlicher schreiben** zusätzlich die Amstad-Flesch-Lesbarkeit des Originals und des vollständigen Vorschlags samt Differenz. Hat der erste Entwurf mindestens 20 Wörter und einen Wert unter 60, fordert Textbuddy genau eine gezielte Nachbesserung mit Original, Entwurf und Messwert an. Angezeigt wird der Entwurf mit dem höheren Flesch-Wert; bei Gleichstand oder einer fehlgeschlagenen Nachbesserung bleibt der erste Entwurf. Der Vergleich bleibt während einzelner Diff-Entscheidungen unverändert.

Diese Nachbesserung kann Latenz und LLM-Kosten der Aktion ungefähr verdoppeln. Sie bewertet weiterhin nur die formale Lesbarkeit, nicht CEFR, Faktenvollständigkeit, Inhalt oder allgemeine Verständlichkeit. Der Diff-Review bleibt deshalb verbindlich.

## Werkzeugleiste und Dokumente

Die kompakte Werkzeugleiste am unteren Editorrand enthält:

- Undo und Redo,
- Upload sowie Drag-and-drop auf die ganze Editorfläche,
- Kopieren in die Zwischenablage,
- clientseitigen DOCX-Download als `textbuddy-YYYY-MM-DD.docx`,
- Zeichen- und Wortzahl mit einem Popover für Silben, Sätze und Durchschnittswerte. Die deutsche Amstad-Flesch-Lesbarkeit erscheint nur bei ausdrücklich gewähltem **Deutsch (Schweiz)**; bei automatischer oder einer anderen Sprachwahl wird sie ausgeblendet.

Beim Import ersetzt der konvertierte Dokumentinhalt den bisherigen Editorinhalt. Unterstützte Eingaben sind PDF, DOCX, PPTX, XLSX, HTML, Markdown, AsciiDoc, TXT, PNG, JPG/JPEG und TIF/TIFF. Die OCR-Sprache folgt der Textsprache; bei automatischer Erkennung wird Deutsch verwendet. Die Standardgrenze beträgt 20 MB und kann im Betrieb konfiguriert werden.

Der Editor ist absichtlich ein Plaintext-Editor: Import behält Text und Absatzumbrüche, aber keine Schrift-, Listen- oder sonstige Dokumentformatierung. Auch KI-Ergebnisse werden als Plaintext eingesetzt. Der DOCX-Export bildet diesen Text wortgetreu ab; Markdown-Zeichen erzeugen keine unbeabsichtigte Formatierung. Das Datum im Dateinamen folgt dem lokalen Datum des Browsers.

## Anbindungen, Daten und Grenzen

| Bereich | Benötigte Anbindung |
| --- | --- |
| Editor, Undo/Redo, Statistik, Kopieren und DOCX | keine externe Anbindung |
| Korrektur und Vorschläge | LanguageTool, eingebettet oder über HTTP |
| Verständlicher schreiben und Zusammenfassen | LLM-Provider |
| Dokumentimport | lokaler Kreuzberg-Adapter oder HTTP-Dokumentdienst |
| OCR | passende OCR-Laufzeit und Sprachdaten |
| zentraler Betrieb | OIDC-Provider |

Für Entwicklung und Tests stehen Stub-Adapter zur Verfügung; ihre Antworten sind keine fachliche Prüfung. Text und Uploads werden von Textbuddy nicht dauerhaft serverseitig gespeichert. Bei externen LLM-, Korrektur- oder Dokumentdiensten verlassen die verarbeiteten Inhalte den Textbuddy-Prozess. LLM-Ergebnisse sollten fachlich geprüft werden.

## Weiterführende Dokumentation

- [Getting Started](getting-started.md)
- [Betrieb und Konfiguration](operations.md)
- [Architektur](architecture.md)
- [Accessibility](accessibility.md)
