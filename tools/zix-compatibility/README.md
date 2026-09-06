# ZIX-Referenzdaten aktualisieren

Der normale Build benötigt nur Java und die eingecheckten Testressourcen. Python ist ausschliesslich nötig, wenn die Referenzdaten bewusst gegen die offizielle ZIX-Implementierung neu erzeugt werden.

Verwendet wird unverändert:

- ZIX `0.2.1`;
- Commit `3cd7e7e9fd0937e1c41e2bf0e040950172ab3a6e`;
- spaCy-Modell `de_core_news_sm 3.8.0`;
- die am Commit eingecheckte `uv.lock`.

Beispiel mit installiertem [uv](https://docs.astral.sh/uv/):

```sh
git clone https://github.com/machinelearningZH/zix_understandability-index.git /tmp/zix-understandability
git -C /tmp/zix-understandability checkout 3cd7e7e9fd0937e1c41e2bf0e040950172ab3a6e
uv sync --project /tmp/zix-understandability --frozen
uv run --project /tmp/zix-understandability \
  python tools/zix-compatibility/generate_reference.py \
  --zix-checkout /tmp/zix-understandability
```

Das Skript verweigert einen anderen Commit. Es exportiert die offiziellen Golden-Werte sowie CEFR-Vokabular, Wortfrequenzen und Modellparameter in einfache JSON-/TSV-Ressourcen. Danach müssen `./gradlew zixCompatibilityTest` und die Änderungen an den Ressourcen geprüft werden.

Das Korpus in `corpus.json` ist für Textbuddy selbst verfasst. Es enthält keine Texte aus dem ZIX-Trainings- oder Evaluationsmaterial.
