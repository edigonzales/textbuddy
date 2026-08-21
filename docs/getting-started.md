# Getting Started

Diese Anleitung bringt dich von einem frisch ausgecheckten Repository zu einer laufenden Textbuddy-Instanz. Für den ersten Start verwenden wir Stub-Adapter. Danach kannst du einen beliebigen OpenAI-kompatiblen LLM-Provider anschliessen.

## 1. Voraussetzungen

- Java 25
- Node.js 20.19 oder neuer und npm
- Git

Für lokale OCR von Bildern und Scan-PDFs werden zusätzlich eine von Kreuzberg unterstützte OCR-Laufzeit und passende Sprachdaten benötigt. Für diesen Einstieg verwenden wir zunächst den Stub-Dokumentimport.

## 2. Repository prüfen

```bash
git clone <repository-url>
cd textbuddy
```

Der reine Backend-Start benötigt keine externe Datenbank und keinen separaten LanguageTool-, Dokument- oder LLM-Dienst.

## 3. Lokal ohne externe Dienste starten

```bash
./gradlew bootRun --args='--server.address=127.0.0.1 --textbuddy.auth.enabled=false --textbuddy.llm.mode=stub --textbuddy.languagetool.mode=stub --textbuddy.document.mode=stub'
```

Öffne danach [http://127.0.0.1:8080](http://127.0.0.1:8080).

In diesem Modus kannst du die Oberfläche, Korrekturabläufe, Quick Actions, Advisor-Ansicht und den Dokumentimport testen. Die Antworten des LLM sind bewusst einfache Stub-Antworten und keine fachliche Qualitätsabnahme.

Der ungeschützte Modus ist absichtlich nur an `127.0.0.1` erlaubt. Für einen Team- oder Produktionsbetrieb muss OIDC aktiviert werden.

## 4. Einen OpenAI-kompatiblen LLM anschliessen

Textbuddy verwendet den OpenAI-kompatiblen Chat-Completions-Vertrag. Der Provider muss daher ungefähr diesen Endpoint anbieten:

```text
POST <base-url>/chat/completions
Authorization: Bearer <api-key>
```

Die wichtigsten Konfigurationswerte sind:

```bash
export TEXTBUDDY_LLM_MODE=provider
export TEXTBUDDY_LLM_BASE_URL=https://llm.example.com/v1
export TEXTBUDDY_LLM_API_KEY='dein-api-key'
export TEXTBUDDY_LLM_MODEL='dein-modell'
```

Die Base-URL enthält normalerweise bereits den API-Pfad wie `/v1`, aber nicht `/chat/completions`. Textbuddy ergänzt diesen letzten Pfad selbst.

Unterstützt werden synchrone Antworten mit `choices[0].message.content`. Provider, die ausschliesslich eine eigene API, die Responses API oder zwingend spezielle Header und Request-Felder verlangen, benötigen einen zusätzlichen Adapter.

## 5. Betrieb mit OIDC

Für einen geschützten Betrieb brauchst du eine OIDC-Client-Registrierung. Kopiere die Beispielkonfiguration und ersetze insbesondere Issuer, Client-ID und Secret:

```bash
cp config/examples/application-production.properties.example application-production.properties
```

Secrets gehören nicht in Git. Setze sie über die Umgebung oder eine Secret-Verwaltung:

```bash
export TEXTBUDDY_AUTH_ENABLED=true
export TEXTBUDDY_OIDC_ISSUER_URI='https://identity.example.com/realms/team'
export TEXTBUDDY_OIDC_CLIENT_ID='textbuddy'
export TEXTBUDDY_OIDC_CLIENT_SECRET='...'
```

Starte danach mit einer externen Konfigurationsdatei:

```bash
java --enable-native-access=ALL-UNNAMED -jar build/libs/textbuddy.jar \
  --spring.config.additional-location=file:./application-production.properties
```

Der OIDC-Provider muss die Redirect-URI
`{baseUrl}/login/oauth2/code/{registrationId}` zulassen. Hinter einem Reverse Proxy müssen die `X-Forwarded-*`-Header korrekt gesetzt werden; Details stehen in [Betrieb und Konfiguration](operations.md).

## 6. Tests und Frontend prüfen

```bash
./gradlew test
npm ci --prefix playwright
npx --prefix playwright playwright install chromium
npm test --prefix playwright
```

Für einen Release-Build:

```bash
./gradlew clean verifyReleaseBundle installerZip
```

## Häufige Probleme

### Die Anwendung startet mit `auth.enabled=false` nicht

Setze eine explizite Loopback-Adresse:

```text
--server.address=127.0.0.1
```

### Der Provider liefert HTTP 404

Prüfe, ob die Base-URL korrekt endet. Für einen üblichen Provider ist beispielsweise `https://llm.example.com/v1` richtig; Textbuddy ruft daraus `/v1/chat/completions` auf.

### Der Provider liefert keinen Antworttext

Prüfe, ob die Antwort dem Chat-Completions-Format entspricht und `choices[0].message.content` enthält. API-Schlüssel, Modellname und Provider-Logs solltest du ebenfalls kontrollieren. Textbuddy gibt Provider-Antwortkörper absichtlich nicht in Fehlermeldungen aus.

### OCR funktioniert nicht

Der Stub-Dokumentimport funktioniert ohne OCR. Für echte Scan-Erkennung müssen die OCR-Laufzeit und die benötigten Sprachdaten auf dem Zielsystem installiert sein. Die CI prüft diesen Pfad separat.

## Weiterführende Dokumentation

- [Architektur](architecture.md)
- [Betrieb und Konfiguration](operations.md)
- [Accessibility](accessibility.md)
