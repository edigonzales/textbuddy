# CI- und Release-Pipeline

## Überblick

Die Pipeline basiert auf GitHub Actions und besteht aus zwei Workflows:

- `ci.yml`: Qualitätssicherung bei `pull_request` und `push` auf `main`
- `release.yml`: Release-Erstellung bei SemVer-Tag (`v*`) oder manuell per `workflow_dispatch`

## CI-Workflow (`ci.yml`)

Der CI-Workflow führt folgende Schritte aus:

1. Checkout des Repositories
2. Setup Java 25 (Temurin)
3. Setup Node.js 20
4. `./gradlew clean test verifyReleaseBundle installerZip`
5. bei Fehlern: Upload von Reports und Testresultaten als Artifact

Der CI-Workflow veröffentlicht keine Releases.

## Release-Workflow (`release.yml`)

### Trigger

- automatisch bei Tag-Push `v*`
- manuell via `workflow_dispatch` (Input `tag`)

### Ablauf

1. Release-Tag auf SemVer mit Prefix `v` validieren
2. Checkout auf den Release-Tag
3. Setup Java 25 und Node.js 20
4. Build der Release-Artefakte (`verifyReleaseBundle`, `installerZip`)
5. Pflichtdateien prüfen
6. GPG-Secrets validieren
7. Checksum und Signaturen erzeugen
8. Checksum und Signaturen verifizieren
9. Draft Release mit allen Assets erstellen

### Release-Assets

- `textbuddy.jar`
- `textbuddy.jar.sha256`
- `textbuddy.jar.asc`
- `textbuddy.jar.sha256.asc`
- `textbuddy-<version>.zip`

## Erforderliche GitHub-Secrets

- `GPG_PRIVATE_KEY`
- `GPG_PASSPHRASE`
- optional: `GPG_KEY_ID`

Fehlende Pflichtsecrets führen im Release-Workflow zu einem expliziten Abbruch.

## Draft-Promote

Der Release-Workflow erstellt absichtlich nur einen Draft Release. Die Veröffentlichung erfolgt manuell im GitHub-UI über "Publish release".

## Troubleshooting

### Release-Workflow bricht vor Signierung ab

- Prüfe, ob `GPG_PRIVATE_KEY` und `GPG_PASSPHRASE` gesetzt sind.
- Prüfe, ob der Tag dem Muster `vX.Y.Z` entspricht.

### Signaturprüfung schlägt fehl

- Prüfe, ob der öffentliche Schlüssel korrekt importiert wurde.
- Prüfe, ob `textbuddy.jar` nach der Signierung unverändert ist.

### Installer-Smoke schlägt fehl

- Prüfe Java-Version (mindestens 25).
- Prüfe, ob das ZIP vollständig entpackt wurde und `bin/start-textbuddy.sh` vorhanden ist.
