# Funktionen

Unter **Überarbeiten** stehen **Verständlicher schreiben** und fünf Zusammenfassungen zur Verfügung. Die Zusammenfassung kann einen Satz, drei Sätze, einen Absatz, eine Seite oder eine Management Summary liefern.

LanguageTool prüft den Text nach einer kurzen Eingabepause. Unter **Prüfen** lassen sich Korrekturen übernehmen, die Textsprache ändern und der Richtlinien-Advisor öffnen. Das persönliche Wörterbuch wird im lokalen Browserspeicher geführt. Verfügbar sind automatische Spracherkennung, Deutsch (Schweiz), Französisch, Italienisch, Englisch (USA) und Englisch (UK).

## Advisor und Diff

Der Advisor prüft ausgewählte gebündelte Demo-Regelwerke und zeigt den Fortschritt. Eine unvollständige Prüfung wird im Ergebnis ausgewiesen. Jeder Befund zeigt die exakte Fundstelle, eine Begründung sowie Dokument, Seite und PDF-Link. Wenn bei der Prüfung ein konkreter Ersatz oder eine Löschung vorgeschlagen wurde, lässt sich der Befund für die Vorschau auswählen. Überlappende Alternativen bleiben sichtbar und lassen sich einzeln auswählen, sobald der bisher gewählte Vorschlag abgewählt ist. Eigene Regelwerke lassen sich als [JSON-/PDF-Paare bündeln](advisor-authoring.md).

Der Browser erstellt den Diff für die ausgewählten Advisor-Befunde lokal. Änderungen aus **Überarbeiten** nutzen dieselbe Vorschau. Änderungen lassen sich einzeln oder gesammelt annehmen und ablehnen. Die Übernahme erfolgt in einer rückgängig machbaren Editortransaktion. Fehler, Ablehnung und ein erreichtes Diff-Ressourcenlimit erhalten den Originaltext.

## Dokumente und Statistik

Der Import unterstützt PDF, DOCX, PPTX, XLSX, HTML, Markdown, AsciiDoc, TXT, PNG, JPG/JPEG und TIF/TIFF. Der extrahierte Text ersetzt den Editorinhalt und behält Absatzumbrüche. OCR verwendet die gewählte Textsprache, bei automatischer Erkennung Deutsch. Die Standard-Uploadgrenze beträgt 20 MB.

Die Werkzeugleiste bietet Undo und Redo, Kopieren, DOCX-Export und Textstatistik. Der Export schreibt jede Textzeile als Word-Absatz. Die Statistik zeigt unter anderem Wörter, Sätze und Silben. Bei **Deutsch (Schweiz)** zeigt sie den Amstad-Flesch-Wert. **Verständlicher schreiben** zeigt dann auch einen Vergleich der formalen Lesbarkeit vor und nach der Überarbeitung.

LanguageTool und Dokumentimport verarbeiten Eingaben lokal. Textbuddy speichert Text und Uploads serverseitig nicht dauerhaft. Bei LLM-Aktionen werden die dafür erforderlichen Inhalte an den konfigurierten Provider gesendet. Die gebündelten Advisor-Regeln sind projektspezifische Demos.
