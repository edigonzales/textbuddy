# ZIX-Kompatibilität mit LanguageTool

Stand: 6. September 2026

## Ergebnis

Der isolierte Java-Prototyp ist **kein numerisch kompatibler Ersatz für ZIX**. Nach den vorab festgelegten Regeln lautet das Urteil **Textbuddy-Proxy-Kandidat**:

| Messgrösse | Ergebnis | ZIX-kompatibel | Proxy-Kandidat |
| --- | ---: | ---: | ---: |
| mittlere absolute ZIX-Abweichung | 1.5351 | höchstens 0.5 | – |
| 95%-Quantil der ZIX-Abweichung | 3.6026 | höchstens 1.0 | – |
| identische CEFR-Bänder | 65.0 % | mindestens 90 % | – |
| richtige Richtung | 100.0 % | mindestens 95 % | mindestens 90 % |
| Spearman-Korrelation | 0.9519 | mindestens 0.95 | mindestens 0.90 |
| starke Umkehrungen | 0 | 0 | 0 |

Damit darf eine spätere LanguageTool-basierte Integration **weder „ZIX“ heissen noch CEFR ausgeben**. Die Rangfolge und die Richtung der Vereinfachungen sind auf diesem kleinen Korpus vielversprechend. Für eine produktive Bezeichnung wie „Textbuddy-Verständlichkeitswert“ braucht es dennoch ein grösseres, fachlich geprüftes Korpus. Dieser Schritt integriert bewusst noch keinen Scorer in Anwendung, API oder Oberfläche.

## Referenz und Aufbau

Die Golden-Daten stammen aus der offiziellen Python-Implementierung:

- Repository: [machinelearningZH/zix_understandability-index](https://github.com/machinelearningZH/zix_understandability-index)
- ZIX-Version `0.2.1`
- Commit `3cd7e7e9fd0937e1c41e2bf0e040950172ab3a6e`
- spaCy-Modell `de_core_news_sm 3.8.0`
- Abhängigkeiten aus der am Commit vorhandenen `uv.lock`

Das selbst verfasste Korpus enthält 20 Original-/Vereinfachungs-Paare mit je mindestens 20 Wörtern. Abgedeckt werden Verwaltungssprache, Satzaufteilung, Komposita, Aufzählungen, Zahlen und Eigennamen. Weitere zwölf Fälle prüfen kurze Texte, `ss`/`ß`, Listen, Kennzahlen und ungewöhnliche Namen. Texte unter 20 Wörtern werden ausgewiesen, fliessen aber nicht in das Urteil ein.

Der Java-Prototyp liegt ausschliesslich im Testcode. Er verwendet LanguageTool 6.7 mit `SwissGerman`, die exportierten Wortlisten und einfache Java-Arithmetik für StandardScaler und Ridge-Modell. Lemma ist jeweils das erste nicht leere LanguageTool-Lemma; andernfalls wird die kleingeschriebene, NFC-normalisierte Wortform verwendet. `ß` wird nicht in `ss` umgewandelt.

## Abweichungen der Merkmale

| Merkmal | MAE | 95%-Quantil | Maximum | Hauptursache |
| --- | ---: | ---: | ---: | --- |
| mittlere Satzlänge | 0.4830 | 2.2500 | 5.7500 | LanguageTool und spaCy ziehen teilweise andere Satz- und Token-Grenzen. |
| RIX | 0.1143 | 0.6667 | 1.5000 | Satzgrenzen und die Tokenisierung von Komposita, Zahlen und Eigennamen unterscheiden sich. |
| A1-Anteil | 0.0433 | 0.1000 | 0.1100 | Lemma-Auswahl und Zahlenerkennung verändern Treffer und Nenner. |
| A2-Anteil | 0.0405 | 0.0957 | 0.1482 | Lemma-Auswahl und Zahlenerkennung verändern Treffer und Nenner. |
| B1-Anteil | 0.0489 | 0.1243 | 0.1423 | Lemma-Auswahl und Zahlenerkennung verändern Treffer und Nenner. |
| Common-Word-Score | 0.5117 | 1.0624 | 1.8829 | Lemma-Auswahl und Zahlenerkennung verändern Frequenzsumme und Nenner. |

Der Regressor verstärkt kleine Vokabular- und Frequenzabweichungen. Deshalb sind 35 % der CEFR-Bänder verschieden, obwohl RIX und mittlere Satzlänge meist nahe beieinanderliegen. Besonders sichtbar wird die Grenze bei Zahlen, ungewöhnlichen Namen und Sätzen, die spaCy, aber nicht LanguageTool an einem Semikolon oder Eigennamen trennt.

## Paarrichtung

Ein offizielles ZIX-Delta unter 0.5 gilt als unentschieden. Von 20 Paaren waren 19 entscheidbar; bei allen 19 zeigte Java dieselbe Verbesserungs- oder Verschlechterungsrichtung. Eine starke Umkehrung – offizielles Delta mindestens `+1.0`, Java höchstens `−0.5` – trat nicht auf.

Das Paar `admin-permit` blieb mit einem offiziellen Delta von `+0.4654` unentschieden. Beim Paar `names-meeting` war die Richtung zwar gleich, das Java-Delta mit `+0.0172` gegenüber offiziell `+4.4939` aber praktisch ohne Trennwirkung. Das ist ein wichtiger Grund, noch keine produktive Qualitätsaussage daraus abzuleiten.

## CEFR-Grenzübertritte

Bei den 40 wertungsrelevanten Texten wichen 14 CEFR-Bänder ab:

| Text-ID | offiziell | Java | absolute ZIX-Abweichung |
| --- | --- | --- | ---: |
| `admin-permit-simplified` | B2 | C1 | 1.2041 |
| `admin-tax-simplified` | B1 | B2 | 1.1472 |
| `admin-consultation-simplified` | B1 | B2 | 1.1648 |
| `sentences-maintenance-original` | B2 | C1 | 2.7033 |
| `sentences-move-simplified` | A2 | B2 | 5.4834 |
| `sentences-decision-simplified` | B2 | C2 | 2.2820 |
| `sentences-grant-original` | B2 | C1 | 1.3942 |
| `compound-waste-simplified` | B1 | B2 | 0.8377 |
| `compound-privacy-simplified` | B2 | C1 | 0.9311 |
| `compound-building-simplified` | A2 | B1 | 1.8092 |
| `compound-refund-original` | C1 | C2 | 1.9608 |
| `list-emergency-original` | B2 | C1 | 3.3240 |
| `list-emergency-simplified` | A1 | A2 | 3.6026 |
| `numbers-construction-simplified` | B1 | B2 | 3.2521 |

Kurze Diagnosefälle fallen erwartungsgemäss stärker auseinander. Der Kennzahlenfall mit Semikolon erreicht beispielsweise eine absolute Abweichung von `10.7472`; die beiden Schreibweisen mit `ss` und `ß` weichen unterschiedlich stark ab. Diese Fälle bestätigen, dass CEFR aus dem Java-Prototyp fachlich nicht vertretbar wäre.

## Reproduzieren

Der normale Build braucht kein Python. Er verwendet nur Java und die eingecheckten JSON-/TSV-Ressourcen:

```sh
./gradlew zixCompatibilityTest
```

Die Aufgabe läuft in einem eigenen JVM-Prozess und gehört weder zu `test` noch zu `check`. Sie schreibt den vollständigen maschinenlesbaren Bericht nach `build/reports/zix-compatibility/report.json` und den Markdown-Bericht nach `build/reports/zix-compatibility/report.md`.

Python wird nur zum bewussten Aktualisieren der Referenzdaten benötigt. Die gepinnten Befehle, das Korpus und der Generator liegen unter `tools/zix-compatibility/`. Die abgeleiteten Daten stehen wie ZIX unter der mitgelieferten MIT-Lizenz; das Korpus selbst stammt aus Textbuddy.

## Konsequenz für Schritt 3

Schritt 3 wird nicht automatisch ausgelöst. Vor einer produktiven Proxy-Integration wären mindestens ein grösseres unabhängiges Korpus, fachlich markierte schwierige Passagen und eine Prüfung der schwachen Kategorien nötig. Ohne diese zusätzliche Evidenz bleibt die bestehende deutsche Amstad-Flesch-Anzeige die einzige produktive Lesbarkeitsmetrik.
