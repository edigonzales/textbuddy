# Konfigurationsbeispiele

Für die Distribution in Phase 05 liegen zwei Beispiele vor:

- Umgebungsvariablen: `config/examples/textbuddy.env.example`
- Properties-Datei: `config/examples/application-production.properties.example`

## Mindestkonfiguration für LLM-Provider

Folgende Werte sind im Provider-Modus zwingend:

- `textbuddy.llm.base-url`
- `textbuddy.llm.api-key`
- `textbuddy.llm.model`

Optional:

- `textbuddy.llm.health-probe-enabled=false`

Der LLM-Health-Check prüft standardmässig nur die Konfiguration. Mit `textbuddy.llm.health-probe-enabled=true` wird zusätzlich eine echte Chat-Completion gegen den konfigurierten Provider ausgeführt.

Infomaniak kann über den OpenAI-kompatiblen Endpunkt angebunden werden:

```properties
textbuddy.llm.base-url=https://api.infomaniak.com/2/ai/<project-id>/openai/v1
textbuddy.llm.model=qwen3
textbuddy.llm.api-key=<nicht-einchecken>
```

## Sidecar-freier Standardbetrieb

Der dokumentierte Standardpfad bleibt ohne externe Sidecars:

- `textbuddy.languagetool.mode=embedded`
- `textbuddy.document.mode=kreuzberg`

## Runtime-Initialisierung

Lokale Laufzeitressourcen werden über diese Properties gesteuert:

- `textbuddy.runtime.home`
- `textbuddy.runtime.initialize-local-resources`

## Installer-Skript

Das optionale Startskript im ZIP-Installer unterstützt zusätzliche JVM-Parameter über:

- `TEXTBUDDY_JAVA_OPTS`

Für Java 25 und lokale OCR-/Dokumentbibliotheken kann bei Native-Access-Warnungen dieser Startparameter nötig sein:

```bash
TEXTBUDDY_JAVA_OPTS="--enable-native-access=ALL-UNNAMED"
```

OCR setzt installierte Tesseract-Sprachdaten für die genutzten Sprachen voraus. Fehlende Sprachdaten blockieren den App-Start nicht, können aber gescannte Importe verschlechtern oder OCR-Warnungen verursachen.
