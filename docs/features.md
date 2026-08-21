# Funktionsübersicht

Textbuddy unterstützt beim Schreiben, Korrigieren, Umformulieren, Prüfen und Importieren von Texten. Diese Übersicht beschreibt die Funktionen aus Sicht der Anwendenden und zeigt, welche Anbindungen dafür benötigt werden.

## Was funktioniert womit?

| Bereich | Benötigte Anbindung |
| --- | --- |
| Editor, Undo/Redo und Textstatistik | keine externe Anbindung |
| Textkorrektur und Korrekturvorschläge | LanguageTool, lokal eingebettet oder über HTTP |
| Synonyme, Satzalternativen und Quick Actions | LLM-Provider |
| Advisor-Prüfung | LLM-Provider; die Referenzdokumente sind in Textbuddy enthalten |
| Dokumentimport | lokaler Kreuzberg-Adapter oder HTTP-Dokumentdienst |
| OCR für Bilder und Scan-PDFs | zusätzlich eine passende OCR-Laufzeit und Sprachdaten |
| Anmeldung im Team- oder Produktionsbetrieb | OIDC-Provider |

Die Adapter können zu Demonstrations- und Testzwecken auch im Stub-Modus laufen. Diese Antworten bilden keine echte fachliche Prüfung ab.

## Schreiben und gezielt umformulieren

Der zentrale Editor nimmt den Text auf, der mit den übrigen Werkzeugen bearbeitet wird.

- **Undo und Redo** machen Änderungen im Editor rückgängig oder stellen sie wieder her.
- **Synonyme** erscheinen nach einer bewussten Auswahl innerhalb genau eines Wortes und berücksichtigen dessen Satzkontext. Ein ausgewählter Vorschlag ersetzt nur dieses Wort.
- **Satz umformulieren** erscheint bei einer Auswahl innerhalb genau eines abgeschlossenen Satzes und berücksichtigt den umgebenden Absatz. Ein ausgewählter Vorschlag ersetzt nur diesen Satz.
- Zeichen- und Wortzahl werden während des Schreibens laufend aktualisiert.

Synonyme und Satzalternativen benötigen einen LLM-Provider. Tippen, eine reine Cursorbewegung, Leerraum oder eine satzübergreifende Auswahl öffnen keine Vorschläge. Escape oder ein Klick ausserhalb schliesst die Vorschläge für die aktuelle Auswahl.

## Textkorrektur und lokales Wörterbuch

Textbuddy prüft den Text nach einer kurzen Tipppause und verarbeitet danach nur die geänderten Textabschnitte erneut. Gefundene Probleme werden im Editor markiert und im Korrekturpanel mit Erläuterungen und möglichen Ersetzungen angezeigt. Ein Vorschlag kann direkt in den Text übernommen werden.

Für die Korrektur stehen diese Spracheinstellungen zur Verfügung:

- Automatische Erkennung
- Deutsch (Schweiz)
- Französisch
- Italienisch
- Englisch (USA)
- Englisch (UK)

Wörter, die absichtlich verwendet werden, können direkt aus einem Treffer oder über das Eingabefeld zum lokalen Wörterbuch hinzugefügt und später wieder entfernt werden. Das Wörterbuch liegt ausschliesslich im lokalen Browserspeicher. Es wird weder mit anderen Browsern synchronisiert noch serverseitig gesichert.

Die echte Prüfung verwendet das eingebettete LanguageTool oder einen konfigurierten LanguageTool-HTTP-Dienst. Der Stub-Modus erkennt nur wenige fest eingebaute Testfehler.

## Quick Actions für den gesamten Text

Quick Actions wenden eine gewählte Umformulierung immer auf den vollständigen Inhalt des Editors an. Die eingestellte Textsprache wird dabei berücksichtigt.

| Aktion | Wirkung und Varianten |
| --- | --- |
| **Vereinfachen** | Schreibt den Text in verständlichere, einfachere Sprache um. |
| **Stichpunkte** | Strukturiert den Inhalt als Aufzählung. |
| **Korrigieren** | Glättet den Text stilistisch und orthografisch. |
| **Zusammenfassen** | Erstellt einen Satz, drei Sätze, einen Absatz, eine Seite oder ein Management Summary. |
| **Ton ändern** | Formuliert den Text formell oder informell. |
| **Social Media** | Erstellt einen Beitrag für Bluesky, Instagram oder LinkedIn. |
| **Format anpassen** | Formt den Inhalt als E-Mail, offiziellen Brief, Präsentation oder Bericht. |
| **Rede umformen** | Wandelt den Inhalt in direkte oder indirekte Rede um. |
| **Eigener Auftrag** | Führt eine frei formulierte Anweisung für den gesamten Text aus. |

Nach einer erfolgreichen Aktion zeigt Textbuddy den Unterschied zwischen ursprünglichem und neuem Text. Die Umformulierung kann dort mit einer eigenen Rückgängig-Funktion zurückgenommen werden. Alle Quick Actions benötigen einen LLM-Provider.

## Advisor-Prüfung mit Referenzdokumenten

Der Advisor prüft den Editorinhalt gegen Regeln aus den mitgelieferten Referenzdokumenten:

1. Ein oder mehrere Referenzdokumente auswählen.
2. Die Advisor-Prüfung starten.
3. Treffer bereits während der laufenden Prüfung ansehen.
4. Einen Treffer auswählen, um Textstelle, Erläuterung, Empfehlung und Dokumentreferenz zu sehen.

Die Trefferliste wird während der Prüfung fortlaufend aktualisiert und führt identische Treffer nicht mehrfach auf. Die zugehörigen PDFs können im integrierten Viewer, in einem neuen Browser-Tab oder als Download geöffnet werden.

Die Dokumente und ihre Regeln sind Bestandteil von Textbuddy. Für die inhaltliche Advisor-Prüfung wird ein LLM-Provider benötigt.

## Dokumente importieren

Dokumente können über den Upload-Button oder per Drag-and-drop importiert werden. Textbuddy wandelt den Inhalt in editorfreundliches HTML um und **ersetzt damit den bisherigen Editorinhalt**.

Vorgesehen sind folgende Formate:

- PDF, DOCX, PPTX und XLSX
- HTML, Markdown, AsciiDoc und TXT
- PNG, JPG/JPEG und TIF/TIFF

Welche Formate die lokale Verarbeitung tatsächlich unterstützt, hängt von der installierten Kreuzberg-Laufzeit ab. Alternativ kann ein konfigurierter HTTP-Dokumentdienst verwendet werden. Im Stub-Modus werden Textformate vereinfacht verarbeitet; binäre Dokumente liefern nur einen Platzhalter.

Für Bilder und Scan-PDFs kann die OCR-Sprache Deutsch, Englisch, Französisch oder Italienisch gewählt werden. Echte Texterkennung benötigt eine funktionierende OCR-Laufzeit und die passenden Sprachdaten. Die standardmässige Uploadgrenze beträgt 20 MB und kann vom Betrieb angepasst werden.

## Textstatistik und Oberflächensprache

Die Textstatistik wird direkt im Browser berechnet und zeigt:

- Zeichen, Wörter, Silben und Sätze,
- durchschnittliche Satzlänge,
- durchschnittliche Silbenzahl pro Wort,
- Flesch-Lesbarkeitsindex mit verständlicher Einordnung.

Die Oberfläche kann zwischen Deutsch und Englisch umgeschaltet werden. Die gewählte Oberflächensprache ist unabhängig von der Textsprache für Korrekturen und LLM-Aktionen.

## Anmeldung, Daten und Grenzen

Im zentralen Betrieb schützt OIDC die Funktionen mit einer Benutzeranmeldung. Für die lokale Entwicklung kann die Anmeldung deaktiviert werden; dieser Modus ist nur über eine explizite Loopback-Adresse erlaubt. Die Auswirkungen sind im [Getting Started](getting-started.md#5-was-bedeutet-betrieb-ohne-anmeldung) beschrieben.

Für die Verarbeitung gilt:

- Bearbeitete Texte und hochgeladene Dokumente werden von Textbuddy nicht dauerhaft serverseitig gespeichert.
- Das lokale Wörterbuch verbleibt im verwendeten Browserprofil.
- Bei einem externen LLM-, LanguageTool- oder Dokumentdienst verlassen die jeweils verarbeiteten Inhalte den Textbuddy-Prozess. Datenschutz und Aufbewahrung richten sich dann auch nach diesem Dienst.
- Stub-Antworten dienen nur dazu, Oberfläche und Abläufe auszuprobieren.
- LLM-Ergebnisse können unvollständig oder fehlerhaft sein und sollten fachlich geprüft werden.

## Weiterführende Dokumentation

- [Getting Started](getting-started.md) – Installation, erster Start und LLM-Anbindung
- [Betrieb und Konfiguration](operations.md) – OIDC, Adapter, Limits und Produktion
- [Architektur](architecture.md) – technische Komponenten und HTTP-Verträge
- [Accessibility](accessibility.md) – Barrierefreiheit und manuelle Abnahme
