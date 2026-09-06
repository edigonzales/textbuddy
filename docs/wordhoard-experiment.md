# Lexikalische Schwierigkeit mit wordhoard und LanguageTool

## Kurzurteil

`wordhoard` ist für Textbuddy ein **brauchbares zusätzliches lexikalisches Signal**, aber noch keine produktionsreife Verständlichkeits- oder CEFR-Metrik.

Auf dem vorhandenen, selbst verfassten Textbuddy-Korpus erkennt der Frequenzrang 19 von 20 Vereinfachungen als lexikalisch leichter. Der Median der bekannten Inhaltswörter beträgt jedoch nur 82,4 %. Von zehn gezielt geprüften Schweizer Verwaltungsbegriffen sind nur `Gesuch` und `Bewilligung` enthalten. Das Resultat lautet deshalb bewusst **robustes lexikalisches Signal im Testkorpus**, nicht allgemeine Validierung.

Es gibt keine Änderung an Anwendung, API oder Oberfläche. Das Experiment liegt ausschliesslich im Testcode und läuft weder mit `test` noch mit `check`. DAFlex wird nicht bezogen oder vorausgesetzt und bleibt ausserhalb dieses Experiments, solange kein verwendbarer Datendownload vorliegt.

## Daten und Reproduzierbarkeit

- wordhoard `v0.1.0`, Commit `1bc5730e8d6e682c416c03680b7cb8c6c7ca8cd0`;
- offizieller CSV-Release mit fest hinterlegter SHA-256-Prüfsumme;
- 69’877 deutsche Zeilen unter `CC-BY-SA-4.0`;
- deterministisch reduzierte Testressource mit Lemma, Wortart, Frequenzrang, Frequenzzahl, CEFR-Proxy und dessen Quelle;
- LanguageTool 6.7 mit `SwissGerman` für Token, Lemma und Wortart;
- dasselbe Textbuddy-Korpus wie beim ZIX-Vergleich: 20 Original-/Vereinfachungspaare und 12 diagnostische Randfälle.

Die Datenlizenz, der ursprüngliche Hinweistext, ein Transformationshinweis und ein Manifest mit beiden Prüfsummen werden mitgeliefert. Beim Kleinschreiben und der NFC-Normalisierung entstehen 597 Schlüssel-Kollisionen, zum Beispiel `Mann` und `mann`. Das Experiment verwendet in diesem Fall deterministisch den häufigeren Eintrag und weist die Kollisionen im JSON-Bericht aus.

Die Testdaten lassen sich bewusst aktualisieren mit:

```sh
python3 tools/wordhoard-experiment/prepare_wordhoard.py
./gradlew wordhoardExperimentTest
```

Der vollständige maschinenlesbare Bericht entsteht unter `build/reports/wordhoard-experiment/report.json`, die generierte Markdown-Fassung daneben als `report.md`. Python ist nur für eine bewusste Aktualisierung der eingecheckten Ressource nötig; der Test selbst benötigt ausschliesslich Java.

## Messung

Es werden nur Inhaltswörter ausgewertet. Zahlen und Satzzeichen bleiben draussen. Von LanguageTool erkannte Eigennamen werden ebenfalls ausgeschlossen. Der Lookup verwendet zuerst Lemma und Wortart, danach klar ausgewiesene Lemma- beziehungsweise Oberflächen-Fallbacks.

Der primäre Richtungswert ist der Mittelwert von `log10(Frequenzrang)`. Unbekannte Inhaltswörter erhalten den Rang 69’878. Ein kleinerer Wert steht damit für allgemein häufigeren Wortschatz. Zusätzlich werden ausgewiesen:

- Anteil bekannter und unbekannter Inhaltswörter;
- Anteil seltener Wörter mit einem Rang über 20’000;
- medianer Rang der bekannten Wörter;
- geschätzte Abdeckung bis B1 und B2;
- problematische unbekannte oder seltene Lemmata;
- Fallbacks zwischen LanguageTool- und wordhoard-Wortarten.

Eine Paardifferenz unter `0.02` gilt als unentschieden. Die pragmatischen Schwellen wurden vor dem ersten vollständigen Messlauf festgelegt:

| Urteil | Richtung | Verschlechterungen | Median bekannte Wörter |
| --- | ---: | ---: | ---: |
| robustes Signal im Testkorpus | mindestens 80 % | höchstens 10 % | mindestens 80 % |
| begrenztes Diagnosesignal | mindestens 65 % | – | mindestens 65 % |

Diese Grenzen dienen einer reproduzierbaren technischen Entscheidung. Sie ersetzen keine wissenschaftliche Kalibrierung.

## Resultate

| Messgrösse | Ergebnis |
| --- | ---: |
| Paare lexikalisch leichter | 19 von 20 / 95,0 % |
| Paare lexikalisch schwerer | 1 von 20 / 5,0 % |
| Median bekannte Inhaltswörter | 82,4 % |
| Median unbekannte Inhaltswörter | 17,6 % |
| Lemma-Treffer mit Wortart-Fallback | 49 von 571 / 8,6 % |
| Treffer mit Oberflächen-Fallback | 3 von 571 / 0,5 % |
| Richtung wie offizieller ZIX bei entscheidbaren Paaren | 18 von 19 / 94,7 % |

Die einzige Umkehrung ist `sentences-decision`: Die vereinfachte Fassung erhält wegen Wortschatzabdeckung und Frequenzrang einen geringfügig schlechteren Wert (`−0.1120`). Das ist ein nützlicher Warnfall: Satzaufteilung kann die allgemeine Verständlichkeit erhöhen, ohne dass ein rein lexikalischer Scorer dies erkennt. Knapp jeder elfte ausgewertete Token benötigt ausserdem einen Wortart-Fallback. Die Abbildung zwischen den LanguageTool-Tags und den Universal-POS-Daten ist also relevant und muss sichtbar bleiben.

Die `ss`- und `ß`-Diagnose ergibt denselben Wert. Listen und Zahlen bleiben messbar; Zahlen werden wie vorgesehen ausgeschlossen. Bei den Eigennamen-Diagnosen erkennt LanguageTool jedoch keinen Namen zuverlässig als Eigennamen. Sieben beziehungsweise fünf Namen erscheinen deshalb als unbekannte Inhaltswörter und verschlechtern den Wert. Eine produktive Lösung müsste diesen Fehler begrenzen.

## Schweizer Verwaltungssprache

| Begriff | wordhoard | Rang | CEFR-Proxy | LanguageTool-Lemma |
| --- | --- | ---: | --- | --- |
| Gesuch | bekannt | 16’517 | C1, geschätzt | `gesuch` |
| Bewilligung | bekannt | 29’082 | C2, geschätzt | `bewilligung` |
| Rechtsmittelbelehrung | unbekannt | – | – | nicht erkannt |
| Veranlagungsverfügung | unbekannt | – | – | `veranlagungsverfügung` |
| subsidiär | unbekannt | – | – | `subsidiär` |
| Vernehmlassungsverfahren | unbekannt | – | – | nicht erkannt |
| Baubewilligung | unbekannt | – | – | `baubewilligung` |
| Katasterschätzung | unbekannt | – | – | `katasterschätzung` |
| Grundstückgewinnsteuer | unbekannt | – | – | `grundstückgewinnsteuer` |
| zuhanden | unbekannt | – | – | nicht erkannt |

Die Lücke ist fachlich wichtig. Ein unbekanntes Verwaltungskompositum kann tatsächlich schwierig sein, kann aber ebenso ein unvermeidbarer und für die Zielgruppe bekannter Fachbegriff sein. Der pauschale Maximalrang ist daher als Diagnose brauchbar, als Qualitätsurteil zu grob.

## Empfehlung

wordhoard sollte vorerst isoliert bleiben. Für einen nächsten Schritt ist kein weiterer technischer Scorer nötig, sondern ein unabhängig zusammengestelltes und manuell bewertetes Schweizer Verwaltungskorpus. Es sollte schwierige Wörter, zulässige Fachbegriffe, Eigennamen, Komposita und echte Vereinfachungen markieren.

Erst wenn die Richtung dort stabil bleibt, wäre eine kleine produktive Unterdimension vertretbar, zum Beispiel:

> Häufigkeit des Wortschatzes: eher anspruchsvoll
>
> 18 % der Inhaltswörter sind selten oder unbekannt. Besonders auffällig: …

Die Oberfläche sollte weder `ZIX` noch ein CEFR-Niveau für den Gesamttext behaupten. Ein einzelner „Verständlichkeitswert“ würde Lesbarkeit, Syntax, Kohärenz und fachliche Richtigkeit vermischen und die vorhandene Evidenz überdehnen.
