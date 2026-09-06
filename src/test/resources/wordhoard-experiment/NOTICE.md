# Herkunft und Bearbeitung der Testdaten

`lexicon.tsv.gz` ist eine bearbeitete deutsche Teilmenge des Datensatzes
[wordhoard](https://github.com/natema/wordhoard), Release `v0.1.0`, Commit
`1bc5730e8d6e682c416c03680b7cb8c6c7ca8cd0`.

Der Quelldatensatz steht unter `CC-BY-SA-4.0`. Die mitgelieferte Datenlizenz und
der ursprüngliche Hinweistext liegen neben dieser Datei. Textbuddy übernimmt nur
Lemma, Wortart, Frequenzrang, Frequenzzahl, geschätztes CEFR-Niveau und die Angabe,
ob das Niveau aus einem Anker oder einer Schätzung stammt. Geschlecht und
Flexionsformen wurden entfernt; die Zeilen wurden deterministisch nach Rang,
Lemma und Wortart sortiert und als TSV komprimiert.

Die CEFR-Angabe von wordhoard ist ein frequenzbasierter Proxy und keine amtliche
Zuordnung. Goethe-Wortlisten wurden laut wordhoard nur zur Kalibrierung verwendet
und werden in den Daten nicht weitergegeben.

Das Textkorpus stammt aus Textbuddys eigenem ZIX-Kompatibilitätsexperiment. Es
enthält keine Texte aus wordhoard oder dessen Quellen.
