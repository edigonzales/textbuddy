# textbuddy

Dieses Repository enthält die planungs- und umsetzungsleitenden Unterlagen sowie die lauffähige Referenzimplementierung für die schrittweise Neuimplementierung von TextMate mit:

- Gradle Groovy
- Java 25
- Spring Boot 4.x MVC
- JTE
- HTMX
- Tiptap-JavaScript-Insel
- SSE via `SseEmitter`

## Implementierungsdokumentation

Die Arbeitsgrundlage liegt unter [docs/implementation/00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md).

Wichtige Einstiegsdateien:

- [Master-Spec](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [Workflow pro Session](/Users/stefan/sources/textbuddy/docs/implementation/01-workflow.md)
- [Codex-Regeln](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [Slice-Index](/Users/stefan/sources/textbuddy/docs/implementation/03-slice-index.md)
- [Status](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

## Voraussetzungen

- Java 25
- Node.js 20+
- aktuelles `npm`

## Lokaler Entwicklungsstart

```bash
./gradlew bootRun
```

Danach ist die Anwendung unter [http://localhost:8080](http://localhost:8080) erreichbar.

Kompletter Build mit Java- und Frontend-Tests:

```bash
./gradlew test
```

## Release und Distribution (Phase 05)

Release-Bundle erzeugen:

```bash
./gradlew clean verifyReleaseBundle installerZip
```

Ergebnis:

- `build/release/textbuddy.jar`
- `build/release/textbuddy-<version>.zip`
- `build/release/docs/release/runbook-produktionsbetrieb.md`
- `build/release/docs/release/abnahme-checkliste.md`
- `build/release/config/examples/`

Standardstartpfad:

```bash
cd build/release
java -jar textbuddy.jar
```

Optionaler Installer-Start:

```bash
unzip textbuddy-<version>.zip -d textbuddy
cd textbuddy
./bin/start-textbuddy.sh
```

Weitere Details:

- [Runbook Produktionsbetrieb](/Users/stefan/sources/textbuddy/docs/release/runbook-produktionsbetrieb.md)
- [Konfigurationsbeispiele](/Users/stefan/sources/textbuddy/docs/release/konfigurationsbeispiele.md)
- [Abnahme- und Release-Checkliste](/Users/stefan/sources/textbuddy/docs/release/abnahme-checkliste.md)
- [CI- und Release-Pipeline](/Users/stefan/sources/textbuddy/docs/release/ci-und-release-pipeline.md)

## Signatur und Checksum

Im Tag-Release werden zusätzlich erzeugt:

- `textbuddy.jar.sha256`
- `textbuddy.jar.asc`
- `textbuddy.jar.sha256.asc`

Verifikation (Linux):

```bash
sha256sum -c textbuddy.jar.sha256
gpg --verify textbuddy.jar.asc textbuddy.jar
gpg --verify textbuddy.jar.sha256.asc textbuddy.jar.sha256
```

## CI und Release

Neue Workflows unter `.github/workflows/`:

- `ci.yml` für Pull Requests und Push auf `main`
- `release.yml` für Tag-Releases (`v*`) und `workflow_dispatch`

Release wird als Draft erstellt und danach manuell über GitHub veröffentlicht.

## Auth / OIDC

Auth ist standardmässig deaktiviert:

- `textbuddy.auth.enabled=false`
- Home-Seite bleibt offen
- APIs sind direkt nutzbar

OIDC-Grundintegration aktivierst du über Properties. Beispiel:

```bash
./gradlew bootRun --args='
  --textbuddy.auth.enabled=true
  --spring.security.oauth2.client.registration.demo.client-id=demo-client
  --spring.security.oauth2.client.registration.demo.client-secret=demo-secret
  --spring.security.oauth2.client.registration.demo.scope=openid,profile,email
  --spring.security.oauth2.client.registration.demo.authorization-grant-type=authorization_code
  --spring.security.oauth2.client.registration.demo.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
  --spring.security.oauth2.client.provider.demo.authorization-uri=https://issuer.example.test/oauth2/authorize
  --spring.security.oauth2.client.provider.demo.token-uri=https://issuer.example.test/oauth2/token
  --spring.security.oauth2.client.provider.demo.user-info-uri=https://issuer.example.test/userinfo
  --spring.security.oauth2.client.provider.demo.user-name-attribute=sub
  --spring.security.oauth2.client.provider.demo.jwk-set-uri=https://issuer.example.test/oauth2/jwks
'
```

## Adapter-Konfiguration

Der Standardpfad ist:

- LLM im Provider-Modus
- LanguageTool eingebettet in der JVM
- Dokumentimport eingebettet über Kreuzberg

Wichtige Properties:

- `textbuddy.llm.mode=provider|stub`
- `textbuddy.llm.base-url`
- `textbuddy.llm.api-key`
- `textbuddy.llm.model`
- `textbuddy.languagetool.mode=embedded|http|stub`
- `textbuddy.languagetool.base-url`
- `textbuddy.languagetool.ngram-path`
- `textbuddy.document.mode=kreuzberg|http|stub`
- `textbuddy.document.base-url`
- `textbuddy.document.api-key`
- `textbuddy.runtime.home`
- `textbuddy.runtime.initialize-local-resources`

## Arbeitsmodus

- Die Umsetzung erfolgt inkrementell.
- Eine Implementierungssession bearbeitet genau einen Slice oder eine Produktionsphase.
- Spätere Features dürfen nicht vorgezogen werden.
