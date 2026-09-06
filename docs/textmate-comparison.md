# Funktionsvergleich: Text-Mate und Textbuddy

Stand: 6. September 2026

## Quellen und Abgrenzung

Verglichen wurden die Implementierung, Konfiguration, Tests und README-Dateien der folgenden Stände:

- [Text-Mate Frontend, Commit `9009178`](https://github.com/DCC-BS/text-mate-frontend/tree/9009178858dcfc9600bf31c5edf816a11080bff6), 1. September 2026
- [Text-Mate Backend, Commit `97f9361`](https://github.com/DCC-BS/text-mate-backend/tree/97f93611a19c78f71b88b8e9d1d1d2a09bd149db), 28. August 2026
- Textbuddy, Stand dieser Dokumentation, 6. September 2026

Die Analyse unterscheidet bewusst zwischen:

- **sichtbar**: in der normalen Benutzeroberfläche erreichbar;
- **Backend**: als Fachlogik und HTTP-Schnittstelle vorhanden, aber nicht zwingend im Frontend exponiert;
- **dormant**: Code ist vorhanden, aber nicht in den aktiven UI-Fluss eingebunden.

Die beiden fremden Repositories wurden statisch analysiert. Es wurde keine produktive Text-Mate-Installation gegen reale LLM-, Azure- oder Docling-Dienste ausgeführt.

## Kurzfazit

Text-Mate ist das funktional breitere Produkt. Besonders der Dokument-Advisor, die gemessene Vereinfachung, die kontextbezogenen Wort- und Satzalternativen sowie konfigurierbare Benutzeraktionen sind vollständig oder weitgehend bis ins UI geführt.

Textbuddy verfolgt bewusst einen engeren Ansatz: vier sichtbare, vollständige Abläufe und möglichst wenig Frontend-Komplexität. Der neue Advisor-Demo-Schnitt schliesst den Weg von gebündelten Regelwerken über Prüfung und Quellenbezug bis zum kontrollierten Diff. Textbuddy besitzt ausserdem eine echte kontinuierliche LanguageTool-Korrektur, ein integriertes lokales Wörterbuch, einen breiteren lokalen Import mit OCR, einen gegen grosse Eingaben abgesicherten Diff und ein auf eine eigenständig installierbare Anwendung ausgerichtetes Release-Verfahren.

Ein isolierter Vergleich von LanguageTool mit ZIX 0.2.1 zeigt zwar eine sehr gute Rang- und Richtungsübereinstimmung, aber keine numerische oder CEFR-Kompatibilität. Das Experiment ist deshalb nicht produktiv integriert; Details stehen im [ZIX-Kompatibilitätsbericht](zix-compatibility.md).

Die Projekte sind daher nicht einfach unterschiedliche Ausbaustufen desselben Produkts:

- **Text-Mate optimiert auf Funktionsbreite und einen zentral betriebenen KI-Dienst.**
- **Textbuddy optimiert auf einen kleinen, nachvollziehbaren Funktionskern und flexible lokale beziehungsweise externe Adapter.**

## Funktionalitäten von Text-Mate

### Editor und Dokumente

- Responsives Ribbon mit den Bereichen **Transformieren** und **Prüfen**.
- Tiptap-Editor mit Plaintext als Austauschmodell und einer Grenze von 100.000 Zeichen.
- Undo, Redo, Text löschen und Kopieren in die Zwischenablage.
- Upload und Drag-and-drop mit Docling-Konvertierung.
- Im Datei-Dialog: TXT, DOC, DOCX, PDF, Markdown, HTML, RTF und PPTX.
- Das Backend akzeptiert TXT, DOCX, PDF, Markdown, HTML und PPTX sowie zusätzlich AsciiDoc, XLSX, CSV und mehrere Bildformate.
- DOC und RTF stehen zwar im Datei-Dialog, fehlen aber im Backend-MIME-Katalog und sind im analysierten Stand daher keine durchgängigen Importfunktionen.
- DOCX-Export im Browser über einen Markdown-zu-DOCX-Konverter.
- Deutsche und englische Benutzeroberfläche; die gewählte Locale wird persistiert.
- Desktop- und mobile Bedienoberflächen für die wesentlichen Aktionen.

Der Editor ist trotz Tiptap kein allgemeiner Rich-Text-Editor. Die zentrale Zustandsübergabe ist Plaintext; unterstützt werden primär Absätze, harte Zeilenumbrüche und interne Markierungen.

### Transformationen

Text-Mate exponiert folgende Aktionen im Frontend:

- **Zusammenfassen**: ein Satz, drei Sätze, ein Absatz, eine Seite oder Management Summary.
- **Aufzählung erstellen**.
- **Einfache Sprache**.
- **Kürzen**.
- **Korrekturlesen** durch das LLM.
- **Formalität**: formell oder informell.
- **Medium**: E-Mail, Behördenbrief, Präsentation oder Bericht.
- **Social Media**: Bluesky, Instagram oder LinkedIn.
- **Figurenrede**: direkte oder indirekte Rede.
- **Eigene Anweisung** als freier Prompt.
- **Administrierte Benutzeraktionen**, die aus Markdown-Dateien geladen und anhand von Azure-Rollen freigeschaltet werden.

Mit Ausnahme von **Einfache Sprache** werden diese Aktionen als Textstream vom Backend geliefert.

### Gemessene Vereinfachung

Die einfache Sprache ist in Text-Mate keine einzelne Quick Action mehr, sondern eine eigene Pipeline:

- automatische Spracherkennung für Deutsch, Englisch, Französisch und Italienisch;
- ZIX und CEFR für Deutsch;
- Flesch Reading Ease mit CEFR-Abbildung für Englisch;
- LIX für Französisch;
- Gulpease für Italienisch;
- Messung vor und nach der Umformulierung;
- maximal zwei Umformulierungsversuche;
- bis 10.000 Zeichen Verarbeitung als Gesamtdokument;
- darüber Aufteilung in zusammenhängende Einheiten mit maximal vier parallelen LLM-Aufrufen;
- Fortschrittsereignisse und abschließendes Gesamtergebnis als NDJSON-Stream;
- Anzeige von nicht ausreichend vereinfachten Passagen und Navigation zwischen diesen Passagen;
- eigenes Evaluationskorpus mit Fällen für Vereinfachung und Faktenerhalt.

Damit besitzt Text-Mate für **Einfache Sprache** einen deutlich stärkeren fachlichen Qualitätsregelkreis als Textbuddy.

### Kontextbezogene Bearbeitung

- Wort unter dem Cursor auswählen und ein bis fünf kontextbezogene Synonyme anfordern.
- Satz unter dem Cursor auswählen und alternative Formulierungen anfordern.
- Vorschlag direkt an der betroffenen Stelle einsetzen.
- Eigene Notizen und Antworten an Textstellen anlegen, bearbeiten und löschen.

Die Notizen existieren nur im aktuellen Frontend-Zustand; eine persistente Zusammenarbeit oder serverseitige Speicherung ist nicht erkennbar.

### Diff-Prüfung

- Inline- und Zweispaltenansicht.
- Einzelne Änderungen annehmen oder ablehnen.
- Alle Änderungen annehmen oder ablehnen.
- Aktion erneut ausführen.
- Übernahme als ein rückgängig machbarer Editor-Schritt.
- Gesonderte Zustände für unveränderte und leere Ergebnisse.
- Bei Vereinfachungen Vergleich der Lesbarkeitswerte vor und nach der Bearbeitung.

Der Wort-Diff besitzt aktuell keine Größenbegrenzung. Außerdem enthalten seine Hunk-Schlüssel entfernte und hinzugefügte Textteile. Textbuddy ist an dieser Stelle robuster.

### Dokument-Advisor

- Auswahl von bis zu fünf Regelwerks-Sammlungen.
- Zwei mitgelieferte Sammlungen mit insgesamt 65 Regeln aus fünf Quelldokumenten; pro Lauf werden höchstens 60 Regeln geprüft.
- Gestreamte Prüfung in Regelbatches mit Fortschrittsanzeige.
- Markierungen und Ergebniskarten direkt am Text.
- Begründung und Verbesserungsvorschlag pro Verletzung.
- Auswahl **korrigieren** oder **überspringen** pro Befund.
- Gemeinsame LLM-Korrektur der ausgewählten Befunde als Textstream.
- Anzeige des zugehörigen PDF-Dokuments auf der referenzierten Seite.
- Eigene Notizen und Antworten an Befunden.
- Eigenes Advisor-Evaluationskorpus.

### Statistik und Lesbarkeit

- Zeichen, Wörter und Silben.
- Durchschnittliche Satzlänge und durchschnittliche Silbenzahl pro Wort.
- erkannte Textsprache;
- sprachabhängiger Lesbarkeitswert, Band und – sofern fachlich ableitbar – CEFR-Niveau.

### Betrieb und Produkt-Shell

- Azure-AD-Authentifizierung oder ungeschützter Entwicklungsmodus.
- Rollenbasierte Dokumente und Benutzeraktionen.
- Nutzungsereignisse für Backend-Aktionen.
- Health Probes, zentraler vLLM-Dienst, Docling-Dienst und Docker-Deployment.
- Dummy-Modus für Frontend-Entwicklung ohne Backend.
- Onboarding, Changelog, Disclaimer, Systemstatus, Feedback und App-Wechsler über die DCC-Komponenten.

## Funktionalitäten von Textbuddy

### Sichtbare Schreibwerkzeuge

In der normalen Oberfläche sind bewusst vier vollständige Abläufe sichtbar:

1. **Korrektur**
   - automatische Prüfung des vollständigen Textes nach 350 Millisekunden Eingabepause;
   - genau ein abbrechbarer Request je Prüfzyklus;
   - Schutz vor verspäteten Antworten;
   - LanguageTool eingebettet, über HTTP oder als Stub;
   - automatische Sprache sowie Deutsch (Schweiz), Französisch, Italienisch, Englisch (USA) und Englisch (UK);
   - Markierungen im Text, Ergebnisleiste und bis zu drei Ersatzvorschläge;
   - direktes Übernehmen eines Vorschlags.
2. **Verständlicher schreiben**
   - eine LLM-Transformation des vollständigen Textes;
   - Schweizer Regeln für verständliche Sprache sind Teil des Prompts;
   - Ergebnis wird vor der Übernahme als Diff gezeigt.
3. **Zusammenfassen**
   - ein Satz, drei Sätze, ein Absatz, eine Seite oder Management Summary;
   - Ergebnis wird vor der Übernahme als Diff gezeigt.
4. **Richtlinien-Advisor**
   - Auswahl von bis zu fünf gebündelten, projektinternen Demo-Regelwerken;
   - zehn Regeln in seriellen Dreierbatches mit SSE-Fortschritt;
   - exakte Textmarkierung, Begründung, Vorschlag und PDF-Quelle;
   - **Korrigieren** oder **Überspringen** pro Befund;
   - ein atomarer LLM-Fix und Prüfung im gemeinsamen Diff.

### Persönliches Wörterbuch

- Direkt mit der LanguageTool-Ergebnisliste verbunden.
- Wörter können manuell oder aus einem Korrekturbefund übernommen werden.
- Wörter werden lokal im Browser gespeichert.
- Passende LanguageTool-Befunde werden unmittelbar ausgeblendet.
- Keine Synchronisation und keine Übertragung an das Backend.

Im Gegensatz dazu enthält Text-Mate zwar eine IndexedDB-Implementierung und eine Wörterbuch-Komponente, die Komponente wird derzeit aber nirgends gerendert und beeinflusst keine Prüfung.

### Diff-Prüfung

- Inline- und Zweispaltenansicht.
- Änderungen einzeln oder gesammelt annehmen und ablehnen.
- Erneut ausführen und abbrechen.
- Übernahme als ein Undo-Schritt.
- Wort-Diff nur bis insgesamt 10.000 Eingabezeichen.
- Bei größeren Texten genau ein Dokument-Hunk statt eines blockierenden Wort-Diffs.
- Kurze numerische Hunk-IDs ohne Nutztext.

### Editor, Import und Export

- Plaintext als verbindliches Inhaltsmodell.
- Undo, Redo, Kopieren und Textstatistik.
- Upload und Drag-and-drop.
- PDF, DOCX, PPTX, XLSX, HTML, Markdown, AsciiDoc, TXT, PNG, JPG/JPEG und TIF/TIFF.
- Dokumentkonvertierung lokal mit Kreuzberg, über HTTP oder als Stub.
- OCR-Sprache wird aus der gewählten Textsprache abgeleitet; im lokalen Adapter ist ein einmaliger Sprach-Fallback möglich.
- Importierte Formatierung wird auf Text und Absatzumbrüche reduziert.
- DOCX-Export behandelt Markdown-Zeichen als literalen Text und verwendet das lokale Browserdatum.
- Zeichen, Wörter, Silben, Sätze und Durchschnittswerte. Bei ausdrücklich gewähltem Deutsch (Schweiz) wird zusätzlich die deutsche Amstad-Flesch-Lesbarkeit clientseitig berechnet und bei **Verständlicher schreiben** vor und nach der Bearbeitung verglichen. Sie ist keine CEFR- oder umfassende Verständlichkeitsbewertung.

### Vorhandene Backend-Funktionen ohne sichtbares Frontend

- Aufzählungen.
- LLM-Korrekturlesen.
- Formalität.
- Social-Media-Texte.
- Anpassung an E-Mail, Behördenbrief, Präsentation oder Bericht.
- Direkte und indirekte Figurenrede.
- Freie eigene Anweisung.
- Kontextbezogene Synonyme.
- Satzalternativen.

Der sichtbare Textbuddy-Advisor enthält fünf einzeln auswählbare Demo-Dokumente mit derzeit je zwei Regeln, also insgesamt zehn Regeln. Eigene Regelwerke können als JSON-/PDF-Paar gebündelt werden. Laufzeit-Upload, Rollenmodell, Notizen und Persistenz sind bewusst nicht vorhanden.

### Betrieb

- Eine Spring-Boot-Anwendung mit serverseitigem HTML und kleiner TypeScript-/Tiptap-Insel.
- OIDC im zentralen Betrieb; alle nicht ausdrücklich öffentlichen Routen sind geschützt.
- Ungeschützter Modus nur bei expliziter Loopback-Bindung.
- OpenAI-kompatibler LLM-Provider oder Stub.
- Eingebettetes oder externes LanguageTool.
- Lokale oder externe Dokumentkonvertierung.
- Keine dauerhafte serverseitige Textspeicherung.
- Eigenständiges JAR und Installer-ZIP mit Startskripten.
- Release-Prüfung mit Java-, Frontend-, Browser-, Accessibility-, OCR- und Installer-Smoke-Tests sowie signierten Artefakten.

## Funktionsmapping

| Funktion | Text-Mate | Textbuddy UI | Textbuddy Backend | Einordnung |
| --- | --- | --- | --- | --- |
| Plaintext-Editor | sichtbar | sichtbar | – | Fachlich vergleichbar; beide verwenden Tiptap, aber Plaintext als Austauschmodell. |
| Undo, Redo, Kopieren | sichtbar | sichtbar | – | Vergleichbar. |
| Text löschen | sichtbar | nur manuell im Editor | – | Kleine Komfortdifferenz zugunsten Text-Mate. |
| Kontinuierliche Rechtschreib- und Grammatikprüfung | – | sichtbar | LanguageTool | Klare Stärke von Textbuddy; Text-Mate hat keinen entsprechenden Prüfzyklus. |
| Persönliches Wörterbuch | dormant, ohne Wirkung auf Prüfungen | sichtbar und integriert | – | Textbuddy ist funktional weiter. |
| Verständlicher schreiben | sichtbar | sichtbar | sichtbar | Textbuddy zeigt für explizit gewähltes Deutsch einen einfachen Flesch-Vorher-/Nachher-Vergleich. Text-Mate bleibt durch mehrsprachige Messung, CEFR, Retry, Chunking und Qualitätsanzeige deutlich weiter. |
| Zusammenfassen | sichtbar, fünf Varianten | sichtbar, fünf Varianten | sichtbar | Gleicher Funktionsumfang; Text-Mate streamt, Textbuddy antwortet atomar. |
| Aufzählungen | sichtbar | – | vorhanden | Bewusste UI-Lücke in Textbuddy. |
| Kürzen | sichtbar | – | – | Echte funktionale Lücke in Textbuddy. |
| LLM-Korrekturlesen | sichtbar | – | vorhanden | Textbuddy setzt sichtbar stattdessen auf LanguageTool. |
| Formalität | sichtbar | – | vorhanden | Bewusste UI-Lücke in Textbuddy. |
| Medium | sichtbar | – | vorhanden | Gleiche vier Backend-Varianten; nur Text-Mate exponiert sie. |
| Social Media | sichtbar | – | vorhanden | Text-Mate bietet Bluesky, Instagram und LinkedIn im UI. |
| Figurenrede | sichtbar | – | vorhanden | Backend-Parität, aber nur Text-Mate exponiert die Funktion. |
| Freie eigene Anweisung | sichtbar | – | vorhanden | Bewusste UI-Lücke in Textbuddy. |
| Rollenbasierte, administrierte Aktionen | sichtbar | – | – | Nur Text-Mate. |
| Kontextbezogene Synonyme | sichtbar am Wort | – | vorhanden | Backend-Parität; Text-Mate hat den vollständigen UI-Fluss. |
| Satzalternativen | sichtbar am Satz | – | vorhanden | Backend-Parität; Text-Mate hat den vollständigen UI-Fluss. |
| Diff-Review | sichtbar | sichtbar | – | Funktionsumfang ähnlich. Textbuddy ist bei großen Texten und Hunk-IDs robuster. |
| Streaming von KI-Text | sichtbar | – | – | Text-Mate zeigt Ergebnisse und Fortschritt früher; Textbuddy bleibt einfacher. |
| Advisor-Prüfung | sichtbar | sichtbar | sichtbar | Beide sind Ende-zu-Ende nutzbar; Text-Mate besitzt 65 statt zehn Regeln und ein Evaluationskorpus. |
| Advisor-Korrektur | sichtbar, gestreamt | sichtbar, atomar mit Diff | sichtbar | Fachlich vergleichbar; Textbuddy verwendet den vorhandenen begrenzten Diff und bleibt technisch einfacher. |
| Advisor-PDF auf Quellseite | eingebettet | neuer Tab auf referenzierter Seite | PDF-Auslieferung | Text-Mate integriert den Viewer stärker; Textbuddy vermeidet einen eigenen PDF-Zustand. |
| Notizen an Textstellen/Befunden | sichtbar, nur Sitzung | – | – | Nur Text-Mate; keine Kollaborationspersistenz. |
| Zeichen-, Wort- und Satzstatistik | sichtbar | sichtbar | – | Vergleichbar bei Grundwerten. |
| Sprachabhängige Lesbarkeit und CEFR | sichtbar | teilweise: deutsche Amstad-Flesch-Lesbarkeit bei `de-CH`, ohne CEFR | – | Die deutsche Kernlücke ist reduziert; Text-Mate deckt zusätzlich automatische Erkennung, EN/FR/IT und CEFR ab. |
| Spracherkennung | sichtbar für Analyse/Vereinfachung | Auswahl oder LanguageTool-Automatik | LanguageTool | Text-Mate ist für Lesbarkeit fachlich breiter; Textbuddy hat mehr explizite Korrekturvarianten. |
| Dokumentimport | sichtbar | sichtbar | sichtbar | Beide breit. Textbuddy exponiert XLSX und Bild-OCR direkt; Text-Mate unterstützt CSV/BMP/GIF/WebP im Backend. Die Text-Mate-Picker-Einträge DOC/RTF passen nicht zum Backend. |
| OCR | automatisch mit DE/EN/FR/IT | sprachgesteuert | Kreuzberg oder externer Dienst | Textbuddy erlaubt eine gezieltere Sprachwahl und lokalen Betrieb. |
| DOCX-Export | sichtbar, Markdown wird interpretiert, UTC-Datum | sichtbar, literaler Text, lokales Datum | – | Textbuddy entspricht konsequenter dem Plaintext-Modell. |
| Deutsche und englische UI | sichtbar, umschaltbar und persistiert | abhängig von Request-Locale | – | Text-Mate bietet die vollständigere Endnutzersteuerung. |
| Azure/OIDC-Authentifizierung | Azure AD | OIDC | OIDC | Funktional vergleichbar, architektonisch unterschiedlich. |
| Onboarding, Feedback, Systemstatus | sichtbar | – | – | Produkt-Shell von Text-Mate ist weiter ausgebaut. |
| Eingebetteter Offline-Betrieb | – | sichtbar nutzbar | LanguageTool und Dokumentimport lokal | Klare Stärke von Textbuddy. Ein reales LLM bleibt optional extern. |
| Installierbares Einzelartefakt | Docker-Stack | JAR und Installer-ZIP | – | Textbuddy ist für einen einfachen Einzelhost-Betrieb weiter. |

## Wo Text-Mate weiter ist

1. **Einfache Sprache mit messbarer Zielerreichung**
   Text-Mate misst sprachabhängig, versucht schwierige Einheiten höchstens einmal erneut und zeigt verbleibende problematische Passagen. Textbuddy führt lediglich eine einzelne, promptbasierte Transformation aus.

2. **Advisor-Inhalt, Rollen und Evaluation**
   Beide Projekte verbinden Dokumentauswahl, Prüfung, Textmarkierung, Quelle und Korrektur. Text-Mate ist mit 65 Regeln, rollenbasierten Sammlungen, Notizen, eingebettetem PDF und eigenem Evaluationskorpus deutlich weiter. Textbuddy zeigt mit zehn projektinternen Regeln bewusst nur einen kleinen, vollständigen Demo-Schnitt.

3. **Exponierte Transformationsbreite**
   Die meisten in beiden Backends vorhandenen Aktionen sind in Text-Mate auf Desktop und Mobil nutzbar. Textbuddy zeigt sie absichtlich nicht.

4. **Kontextaktionen im Editor**
   Synonyme und Satzalternativen sind bei Text-Mate direkt an Wort beziehungsweise Satz angeschlossen.

5. **Mehrsprachige Lesbarkeit**
   ZIX/CEFR, englische Flesch-Auswertung, LIX und Gulpease sind serverseitig gekapselt, begrenzt und getestet.

6. **Fachliche Evaluation**
   Text-Mate enthält eigene Advisor- und Simplify-Evaluationsfälle. Das ist für LLM-Funktionen aussagekräftiger als reine Vertragstests.

## Wo Textbuddy weiter ist

1. **Deterministische laufende Textkorrektur**
   Textbuddy verbindet LanguageTool, Markierungen, Vorschläge, Sprachvarianten und Wörterbuch in einem vollständigen Benutzerfluss. Text-Mates Aktion **Korrekturlesen** ist eine LLM-Gesamttransformation und kein Ersatz dafür.

2. **Einfachheit und kontrollierter UI-Umfang**
   Nur tatsächlich freigegebene Werkzeuge werden ausgeliefert. Text-Mate besitzt erheblich mehr Komponenten, Composables, Zustände und indirekte Kommandos.

3. **Robustheit bei großen Diffs**
   Textbuddy begrenzt die synchrone Wort-Diff-Berechnung und hält Nutztext aus DOM-Schlüsseln heraus. Text-Mate besitzt beide Schutzmaßnahmen derzeit nicht.

4. **Lokaler und austauschbarer Betrieb**
   LanguageTool und Dokumentextraktion können eingebettet laufen. Für Entwicklung existieren gezielte Stub-Adapter. Text-Mate setzt für den vollständigen Betrieb auf Backend, vLLM und Docling als getrennte Dienste.

5. **Konsequentes Plaintext-Verhalten**
   Import, KI-Ergebnisse und Export folgen demselben Modell. Markdown-Zeichen werden beim DOCX-Export nicht unbeabsichtigt als Formatierung interpretiert.

6. **Release- und Accessibility-Absicherung**
   Textbuddy prüft das installierbare Artefakt, Startskripte, OCR, Browserfunktion und Accessibility im Release-Workflow. In den analysierten Text-Mate-Repositories sind weniger betriebliche Ende-zu-Ende- und keine dedizierten Accessibility-Tests sichtbar.

## Lücken und sinnvolle nächste Schritte

### Für Textbuddy

| Priorität | Vorschlag | Begründung |
| --- | --- | --- |
| mittel | **Lexikalischen Proxy nur mit breiterer fachlicher Validierung prüfen** | ZIX ist nicht numerisch kompatibel. Das ergänzende wordhoard-Experiment erkennt auf dem kleinen Korpus 19 von 20 Vereinfachungen, kennt aber im Median nur 82,4 % der Inhaltswörter und nur zwei von zehn geprüften Schweizer Verwaltungsbegriffen. Vor einer Integration braucht es ein unabhängig bewertetes Domänenkorpus. |
| mittel | **Advisor erst fachlich, dann mengenmässig ausbauen** | Der vertikale Schnitt ist vollständig. Als Nächstes lohnen echte organisationsspezifische Regeln und ein bewertetes Korpus mehr als zusätzliche Workflow-Infrastruktur. |
| mittel | **Lesbarkeit nur bei echtem Bedarf mehrsprachig erweitern** | Der deutsche Vorher-/Nachher-Vergleich ist vorhanden. EN/FR/IT und CEFR rechtfertigen zusätzliche Formeln und Erkennung erst bei entsprechendem Nutzungsbedarf. |
| niedrig | **Streaming nur bei nachgewiesenem Warteproblem** | Für zwei sichtbare LLM-Aktionen rechtfertigt einfaches Request/Response häufig die geringere Komplexität. |
| niedrig | **Kürzen nur bei echtem Produktbedarf ergänzen** | Es ist die einzige wesentliche Text-Mate-Quick-Action, die auch im Textbuddy-Backend fehlt. |

Nach Ockham sollte Textbuddy nicht versuchen, die gesamte Text-Mate-Oberfläche nachzubauen. Mit dem deutschen Vorher-/Nachher-Wert ist die naheliegende kleine Messlücke geschlossen. Der zwischenzeitlich erprobte automatische zweite LLM-Versuch wurde wieder entfernt. ZIX- und wordhoard-Experiment bleiben bewusst im Testcode; eine produktive Proxy-Metrik oder ein erneuter Qualitätsversuch braucht zuerst zusätzliche fachliche Evidenz.

### Für Text-Mate

- Wort-Diffs ebenfalls oberhalb einer festen Eingabegröße auf einen Dokument-Hunk reduzieren.
- Kurze technische Hunk-IDs verwenden und keinen Nutztext in Schlüsseln ablegen.
- Das persönliche Wörterbuch entweder in den aktiven UI-Fluss und eine konkrete Prüfung integrieren oder Komponente, Query und README-Aussage entfernen.
- Die README-Aussage zu frei wählbaren Stilen, Zielgruppen und Zielen an die tatsächlich exponierten spezialisierten Aktionen anpassen.
- Importformate in README, Datei-Picker und Backend-Katalog konsistent machen; DOC/RTF derzeit entweder implementieren oder aus dem Picker entfernen.
- **Einfache Sprache** in der Backend-Dokumentation als `/simplify`-Pipeline beschreiben; die alte `plain_language`-Quick-Action ist im Dienst ausdrücklich stillgelegt.
- Die vielfach referenzierten, aber im Repository fehlenden Dateien `docs/simplify_redesign.md`, `docs/simplify_rules_audit.md` und `docs/advisor_redesign.md` einchecken oder die Verweise durch vorhandene Dokumentation ersetzen.

## Dokumentationsabgleich

### Text-Mate

Die README-Dateien geben einen guten Überblick, sind aber nicht vollständig deckungsgleich mit dem Code:

- Das Frontend nennt ein persönliches Wörterbuch als Funktion. Die Komponente und IndexedDB-Query existieren, aber die Komponente wird nicht gerendert und das Wörterbuch beeinflusst keine andere Funktion.
- Die beschriebenen generischen Stile **Simple, Professional, Casual, Academic, Technical**, Zielgruppen und Ziele sind im aktuellen Ribbon nicht als solche auffindbar. Sichtbar sind stattdessen die spezialisierten Aktionen aus der obigen Liste.
- Die Importbeschreibung nennt nur DOCX und TXT, während Picker und Backend wesentlich mehr Formate akzeptieren.
- Die Backend-README führt **Plain Language** noch unter Quick Actions. Der Quick-Action-Service schließt diese Aktion ausdrücklich aus und verweist auf `/simplify`.
- Zahlreiche Quelltexte verweisen auf drei nicht vorhandene Design-Dokumente. Dadurch sind wichtige fachliche Entscheidungen nur noch über Kommentare und Tests rekonstruierbar.
- Ein Frontend-Kommentar bezeichnet den Advisor-Stream noch als SSE, obwohl Frontend und Backend NDJSON-Zeilen verarbeiten.

### Textbuddy

README, Funktionsübersicht und Architektur passen zum analysierten Stand:

- vier sichtbare vollständige Abläufe einschliesslich Advisor;
- zusätzliche Funktionen nur im Backend;
- Plaintext als verbindliches Modell;
- ein Request je Korrekturzyklus;
- 10.000-Zeichen-Grenze für Wort-Diffs;
- Advisor mit zehn projektinternen Demo-Regeln, höchstens vier seriellen Validierungsaufrufen und einem atomaren Fix-Aufruf;
- keine automatischen Provider-Retries, abgesehen vom dokumentierten lokalen OCR-Sprachfallback.

Weiterführend: [Funktionsübersicht](features.md), [Architektur](architecture.md) und [Betrieb](operations.md).
