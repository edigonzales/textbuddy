# Konfigurationsbeispiele

Für die Distribution in Phase 05 liegen zwei Beispiele vor:

- Umgebungsvariablen: `config/examples/textbuddy.env.example`
- Properties-Datei: `config/examples/application-production.properties.example`

## Mindestkonfiguration für LLM-Provider

Folgende Werte sind im Provider-Modus zwingend:

- `textbuddy.llm.base-url`
- `textbuddy.llm.api-key`
- `textbuddy.llm.model`

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
