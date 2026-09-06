# wordhoard-Experiment aktualisieren

Das Experiment ist vom normalen Build getrennt. Java benötigt nur die eingecheckten
Testressourcen. Python wird ausschliesslich benötigt, wenn der gepinnte Datensatz
bewusst neu erzeugt werden soll.

Verwendet wird:

- `wordhoard` `v0.1.0`;
- Commit `1bc5730e8d6e682c416c03680b7cb8c6c7ca8cd0`;
- der offizielle CSV-Release mit geprüftem SHA-256;
- die deutsche Teilmenge unter `CC-BY-SA-4.0`.

Aktualisieren:

```sh
python3 tools/wordhoard-experiment/prepare_wordhoard.py
./gradlew wordhoardExperimentTest
```

Das Skript reduziert den deutschen Datensatz deterministisch auf die für das
Experiment benötigten Felder. Geschlecht und Flexionsformen werden nicht übernommen.
Die Quelle, Prüfsummen und Transformation stehen im erzeugten Manifest.

Eine Aktualisierung auf eine andere Version ist eine fachliche Änderung. Release,
Commit, Prüfsumme, Metriken und Ergebnisbericht müssen gemeinsam überprüft werden.
