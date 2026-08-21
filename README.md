# Textbuddy

Textbuddy ist eine serverbasierte Schreibassistenz für Korrekturen, Umformulierungen, statische Schreibregeln und Dokumentimport. Das Backend ist eine Spring-Boot-MVC-Anwendung; der Editor ist eine kleine TypeScript-/Tiptap-Insel in serverseitig gerendertem JTE-HTML.

## Voraussetzungen

- Java 25
- Node.js 20.19 oder neuer und npm
- optional: eine von Kreuzberg unterstützte lokale OCR-Laufzeit für Bild- und Scan-PDF-Texterkennung

## Lokal starten

Der ungeschützte Modus ist ausschliesslich an einer expliziten Loopback-Adresse erlaubt. Die Stub-Adapter benötigen keine externen Dienste:

```bash
./gradlew bootRun --args='--server.address=127.0.0.1 --textbuddy.auth.enabled=false --textbuddy.llm.mode=stub --textbuddy.languagetool.mode=stub --textbuddy.document.mode=stub'
```

Danach ist Textbuddy unter [http://127.0.0.1:8080](http://127.0.0.1:8080) erreichbar.

## Prüfen und bauen

```bash
./gradlew test
npm ci --prefix playwright
npx --prefix playwright playwright install chromium
npm test --prefix playwright
./gradlew clean verifyReleaseBundle installerZip
```

Das Release-Bundle liegt danach unter `build/release/`. Die CI führt Java-, Frontend-, Browser- und Accessibility-Tests sowie ein Dependency-Audit aus.

## Produktion

Authentifizierung ist standardmässig aktiv und erfordert eine OIDC-Client-Konfiguration. LLM-Zugriff benötigt einen OpenAI-kompatiblen Provider. LanguageTool und Dokumentimport können eingebettet oder über HTTP betrieben werden. Konkrete Beispiele liegen unter `config/examples/`.

Eine `.env`-Datei wird nicht automatisch geladen. Vor einem Start mit dem Beispiel muss sie explizit in die Shell übernommen werden:

```bash
set -a
source config/examples/textbuddy.env.example
set +a
java --enable-native-access=ALL-UNNAMED -jar build/libs/textbuddy.jar \
  --spring.config.additional-location=file:config/examples/application-production.properties.example
```

Weitere Dokumentation:

- [Getting Started](docs/getting-started.md)
- [Architektur](docs/architecture.md)
- [Betrieb und Konfiguration](docs/operations.md)
- [Accessibility](docs/accessibility.md)

## Sicherheitsmodell in Kürze

- `/api/**` ist im Normalbetrieb OIDC-geschützt und CSRF-gesichert.
- `textbuddy.auth.enabled=false` startet nur mit expliziter Loopback-Bindung.
- Actuator veröffentlicht ausschliesslich `/actuator/health`, ohne Komponenten oder Details.
- Provider-Antworten und API-Schlüssel werden nicht in Fehlermeldungen ausgegeben.
- Text- und Uploadgrössen werden vor der Verarbeitung begrenzt.

API-Verträge und technische Entscheidungen stehen in [docs/architecture.md](docs/architecture.md); Hinweise zu Reverse Proxy, TLS, Rate Limits und Secrets in [docs/operations.md](docs/operations.md).
