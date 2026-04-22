# Runbook: Produktionsbetrieb ohne Sidecars

## Ziel

Dieses Runbook beschreibt den Standardstartpfad und die Release-Artefakte nach Phase 05. Das Primärartefakt ist `textbuddy.jar` und startet über `java -jar textbuddy.jar`.

## Voraussetzungen

- Java 25 (`java --version`)
- Schreibrechte für das Runtime-Verzeichnis (Standard: `~/.textbuddy`)
- gesetzte LLM-Provider-Konfiguration für den produktiven Betrieb

## Artefakte bauen

```bash
./gradlew clean verifyReleaseBundle installerZip
```

Ergebnis:

- Jar: `build/release/textbuddy.jar`
- Signatur-/Checksum-Dateien (im Release-Workflow erzeugt): `*.sha256`, `*.asc`
- ZIP-Installer: `build/release/textbuddy-<version>.zip`
- Runbook und Checklisten: `build/release/docs/release/`
- Konfigurationsbeispiele: `build/release/config/examples/`

## Konfiguration

Es gibt zwei vorbereitete Beispiele:

- `config/examples/textbuddy.env.example`
- `config/examples/application-production.properties.example`

Für den produktiven Start sind mindestens diese Werte erforderlich:

- `textbuddy.llm.mode=provider`
- `textbuddy.llm.base-url`
- `textbuddy.llm.api-key`
- `textbuddy.llm.model`

Standardbetrieb ohne Sidecars:

- `textbuddy.languagetool.mode=embedded`
- `textbuddy.document.mode=kreuzberg`

## Startvarianten

### Variante A: Direktes Jar

```bash
cd build/release
java -jar textbuddy.jar
```

### Variante B: ZIP-Installer mit Startskript

```bash
unzip textbuddy-<version>.zip -d textbuddy
cd textbuddy
./bin/start-textbuddy.sh
```

Optionale JVM-Parameter über Umgebungsvariable:

```bash
TEXTBUDDY_JAVA_OPTS="-Xms256m -Xmx1g" ./bin/start-textbuddy.sh --server.port=8080
```

## Laufzeitressourcen

Beim Start werden lokale Laufzeitressourcen kontrolliert initialisiert, falls `textbuddy.runtime.initialize-local-resources=true` gesetzt ist (Standard):

- `<runtime>/config`
- `<runtime>/logs`
- `<runtime>/tmp`
- `<runtime>/cache`

Das Basisverzeichnis wird über `textbuddy.runtime.home` gesteuert. Ohne Wert wird `~/.textbuddy` verwendet.

## Smoke-Prüfungen nach Start

```bash
curl -fsS http://127.0.0.1:8080/actuator/health
curl -fsS http://127.0.0.1:8080/actuator/info
```

Erwartung:

- Health-Status `UP`
- Info enthält aktive Modi (`llmMode`, `languageToolMode`, `documentImportMode`)

## Signatur- und Checksum-Verifikation

### Linux

```bash
sha256sum -c textbuddy.jar.sha256
gpg --verify textbuddy.jar.asc textbuddy.jar
gpg --verify textbuddy.jar.sha256.asc textbuddy.jar.sha256
```

### macOS

```bash
shasum -a 256 -c textbuddy.jar.sha256
gpg --verify textbuddy.jar.asc textbuddy.jar
gpg --verify textbuddy.jar.sha256.asc textbuddy.jar.sha256
```

## Shutdown und Wiederanlauf

1. Prozess regulär beenden (`Ctrl+C` oder Signal `TERM`).
2. Erneut mit demselben Befehl starten (`java -jar textbuddy.jar` oder Startskript).
3. Health erneut prüfen (`/actuator/health`).

## Wiederherstellung bei Startfehlern

1. LLM-Pflichtkonfiguration prüfen (`base-url`, `api-key`, `model`).
2. Schreibrechte auf `textbuddy.runtime.home` prüfen.
3. Freien HTTP-Port prüfen (`server.port`).
4. Java-Version prüfen (mindestens 25).
5. Bei signierten Artefakten Checksum und Signatur erneut verifizieren.
