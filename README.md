# textbuddy

Dieses Repository enthält die planungs- und umsetzungsleitenden Unterlagen und die lauffaehige Referenzimplementierung fuer eine schrittweise Neuimplementierung von TextMate mit:

- Gradle Groovy
- Java 25
- Spring Boot 4.x MVC
- JTE
- HTMX
- Tiptap-JavaScript-Insel
- SSE via `SseEmitter`

## Implementierungsdokumentation

Die eigentliche Arbeitsgrundlage liegt unter [docs/implementation/00-master-spec.md](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md).

Wichtige Einstiegsdateien:

- [Master-Spec](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [Workflow pro Session](/Users/stefan/sources/textbuddy/docs/implementation/01-workflow.md)
- [Codex-Regeln](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- [Slice-Index](/Users/stefan/sources/textbuddy/docs/implementation/03-slice-index.md)
- [Status](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

## Voraussetzungen

- Java 25
- Node.js 20+
- ein aktuelles `npm`

## Build und Start

Lokaler Standardstart ohne Auth:

```bash
./gradlew bootRun
```

Danach ist die Anwendung unter [http://localhost:8080](http://localhost:8080) erreichbar.

Kompletter Build mit Java- und Frontend-Tests:

```bash
./gradlew test
```

## Auth / OIDC

Auth ist standardmaessig deaktiviert:

- `textbuddy.auth.enabled=false`
- Home-Seite bleibt offen
- APIs sind direkt nutzbar

OIDC-Grundintegration aktivierst du ueber Properties. Beispiel:

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

Mit aktivierter Auth:

- die Home-Seite zeigt den OIDC-Status
- bestehende `/api/**`-Endpoints verlangen eine angemeldete Session
- API-Fehler liefern ein konsistentes JSON-Fehlerformat inklusive `traceId`

## Adapter-Konfiguration

Der Standardpfad von Phase 01 ist:

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

## Arbeitsmodus

- Die Umsetzung erfolgt **inkrementell**.
- Eine Implementierungssession bearbeitet **genau einen Slice**.
- Spätere Features dürfen nicht vorgezogen werden.
- Nach jeder Session werden nur Status und Handoff aktualisiert.
