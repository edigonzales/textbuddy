# Getting Started

Diese Anleitung bringt dich von einem frisch ausgecheckten Repository zu einer laufenden Textbuddy-Instanz. Danach weisst du:

- wie du Textbuddy ohne externe Dienste kennenlernst,
- wie du lokal einen echten LLM-Provider anschliesst und den Zugang prüfst,
- was der lokale Betrieb ohne Anmeldung vom geschützten OIDC-Betrieb unterscheidet.

## 1. Voraussetzungen

- Java 25
- Node.js 20.19 oder neuer und npm
- Git

Für lokale OCR von Bildern und Scan-PDFs werden zusätzlich eine von Kreuzberg unterstützte OCR-Laufzeit und passende Sprachdaten benötigt. Für den ersten Start verwenden wir stattdessen den Stub-Dokumentimport.

## 2. Repository holen

```bash
git clone <repository-url>
cd textbuddy
```

Textbuddy benötigt keine externe Datenbank. Beim ersten Build laden Gradle und npm jedoch die benötigten Abhängigkeiten herunter.

## 3. Textbuddy ohne externe Dienste kennenlernen

Starte alle externen Anbindungen zunächst im Stub-Modus:

```bash
TEXTBUDDY_AUTH_ENABLED=false \
TEXTBUDDY_LLM_MODE=stub \
TEXTBUDDY_LANGUAGETOOL_MODE=stub \
TEXTBUDDY_DOCUMENT_MODE=stub \
./gradlew bootRun --args='--server.address=127.0.0.1'
```

Öffne danach [http://127.0.0.1:8080](http://127.0.0.1:8080).

Die Werte vor `./gradlew` gelten nur für diesen Start und verändern deine Shell nicht dauerhaft:

| Einstellung | Wirkung in diesem Einstieg |
| --- | --- |
| `TEXTBUDDY_AUTH_ENABLED=false` | Textbuddy verlangt lokal keine Anmeldung. |
| `TEXTBUDDY_LLM_MODE=stub` | Quick Actions, Satzalternativen, Synonyme und Advisor verwenden reproduzierbare Demo-Antworten. |
| `TEXTBUDDY_LANGUAGETOOL_MODE=stub` | Die Korrektur erkennt nur einige fest eingebaute Testfehler. |
| `TEXTBUDDY_DOCUMENT_MODE=stub` | Textdateien werden vereinfacht importiert; für binäre Dokumente gibt es nur einen Platzhalter. |
| `server.address=127.0.0.1` | Der Server ist nur über die Loopback-Adresse des eigenen Rechners erreichbar. |

Damit kannst du die Oberfläche und die Abläufe ausprobieren. Die Ergebnisse eignen sich nicht zur Beurteilung der fachlichen Qualität von LLM, Korrektur oder Dokumentimport.

## 4. Einen echten LLM-Provider lokal testen

Beende den Stub-Start zuerst mit `Ctrl+C`. Exportiere danach die Zugangsdaten im selben Terminal:

```bash
export TEXTBUDDY_LLM_BASE_URL='https://llm.example.com/v1'
export TEXTBUDDY_LLM_API_KEY='dein-api-key'
export TEXTBUDDY_LLM_MODEL='dein-modell'
```

Starte Textbuddy nun mit dem echten LLM, aber weiterhin mit den lokalen Stub-Adaptern für Korrektur und Dokumentimport:

```bash
TEXTBUDDY_AUTH_ENABLED=false \
TEXTBUDDY_LLM_MODE=provider \
TEXTBUDDY_LANGUAGETOOL_MODE=stub \
TEXTBUDDY_DOCUMENT_MODE=stub \
./gradlew bootRun --args='--server.address=127.0.0.1'
```

Verwende hier nicht zusätzlich `--textbuddy.llm.mode=stub`: Startargumente haben Vorrang vor Umgebungsvariablen und würden den Provider-Modus wieder ausschalten.

### LLM-Einstellungen

| Umgebungsvariable | Im Provider-Modus | Standard | Bedeutung |
| --- | --- | --- | --- |
| `TEXTBUDDY_LLM_MODE` | `provider` | `provider` | Wählt den echten Provider statt der Demo-Antworten. |
| `TEXTBUDDY_LLM_BASE_URL` | erforderlich | – | Basisadresse inklusive eines Pfads wie `/v1`, aber ohne `/chat/completions`. |
| `TEXTBUDDY_LLM_API_KEY` | erforderlich | – | Zugangsschlüssel, den das Backend als Bearer-Token sendet. |
| `TEXTBUDDY_LLM_MODEL` | erforderlich | – | Exakte Modell-ID beim gewählten Provider. |
| `TEXTBUDDY_LLM_TIMEOUT` | optional | `30s` | Maximale Wartezeit für Verbindungsaufbau und Antwort. |
| `TEXTBUDDY_LLM_TEMPERATURE` | optional | `0.2` | Niedrige Werte liefern meist gleichmässigere, höhere Werte variablere Antworten; Textbuddy begrenzt den Wert auf `0` bis `2`. |

Base-URL, API-Key und Modell werden beim Start auf Vorhandensein geprüft. Der Provider wird aber erst bei der ersten LLM-Funktion kontaktiert. Ein erfolgreicher Anwendungsstart oder `GET /actuator/health` bestätigt deshalb noch nicht, dass der LLM-Zugang funktioniert.

So prüfst du den Zugang vollständig:

1. Suche im Startlog nach `LLM client: provider mode` und kontrolliere Base-URL sowie Modell.
2. Öffne Textbuddy und führe beispielsweise die Quick Action **Zusammenfassen** aus.
3. Prüfe, ob eine inhaltliche Antwort deines Providers erscheint.

Textbuddy erwartet einen synchronen, OpenAI-kompatiblen Chat-Completions-Endpunkt:

```text
POST <base-url>/chat/completions
Authorization: Bearer <api-key>
```

Die Anfrage enthält eine Modell-ID, System- und Benutzernachrichten, `temperature` sowie `stream=false`. Der Antworttext muss unter `choices[0].message.content` stehen. Provider, die nur eine eigene API, ausschliesslich die Responses API oder zusätzliche zwingende Header und Request-Felder anbieten, benötigen einen zusätzlichen Adapter.

Auch ein Provider, der den Bearer-Header nicht auswertet, benötigt derzeit einen nicht leeren Wert für `TEXTBUDDY_LLM_API_KEY`: Textbuddy prüft ihn beim Start und sendet den Header bei jeder Anfrage.

## 5. Was bedeutet Betrieb ohne Anmeldung?

> **Wichtig:** Die Textbuddy-Anmeldung und der Zugang zum LLM-Provider sind zwei getrennte Schutzebenen.

```text
Benutzer -- OIDC --> Textbuddy -- API-Key --> LLM-Provider
```

`TEXTBUDDY_AUTH_ENABLED=false` deaktiviert nur die erste Schutzebene. Textbuddy verlangt dann keine Benutzeranmeldung und kennt keine OIDC-Benutzeridentität. Der LLM-API-Key wird trotzdem benötigt und bleibt im Backend; er wird nicht an den Browser ausgeliefert.

Im lokalen Modus gilt ausserdem:

- Textbuddy startet nur mit einer expliziten Loopback-Adresse wie `127.0.0.1`.
- Alle lokalen API-Funktionen sind ohne Anmeldung erreichbar. Jeder Benutzer oder Prozess auf dem Rechner, der den Port erreicht, kann sie aufrufen.
- Weil es keine Anmeldesitzung gibt, sind auch Logout und der sitzungsbezogene CSRF-Schutz deaktiviert.
- LLM-Funktionen können eingegebenen Text an den konfigurierten Provider übertragen und dort Kosten verursachen.
- Die Loopback-Bindung begrenzt die Erreichbarkeit, ersetzt aber keine Benutzerkontrolle.

Dieser Modus ist für Entwicklung auf dem eigenen Rechner gedacht, nicht für einen Reverse Proxy, Containerzugriff, ein Team oder die Produktion.

## 6. Für Team- oder Produktionsbetrieb OIDC aktivieren

Für einen geschützten Betrieb brauchst du eine OIDC-Client-Registrierung und einen TLS-terminierenden Reverse Proxy. Baue zuerst das ausführbare JAR und kopiere die Beispielkonfiguration:

```bash
./gradlew bootJar
cp config/examples/application-production.properties.example application-production.properties
```

Setze anschliessend sowohl die OIDC- als auch die LLM-Werte. Secrets gehören nicht in Git:

```bash
export TEXTBUDDY_AUTH_ENABLED=true
export TEXTBUDDY_OIDC_ISSUER_URI='https://identity.example.com/realms/team'
export TEXTBUDDY_OIDC_CLIENT_ID='textbuddy'
export TEXTBUDDY_OIDC_CLIENT_SECRET='...'

export TEXTBUDDY_LLM_MODE=provider
export TEXTBUDDY_LLM_BASE_URL='https://llm.example.com/v1'
export TEXTBUDDY_LLM_API_KEY='dein-api-key'
export TEXTBUDDY_LLM_MODEL='dein-modell'
```

Starte danach die Anwendung:

```bash
java --enable-native-access=ALL-UNNAMED -jar build/libs/textbuddy.jar \
  --spring.config.additional-location=file:./application-production.properties
```

Eine `.env`-Datei wird von Java nicht automatisch geladen. Die Variablen müssen wie oben in der aktuellen Shell exportiert oder vom Prozessmanager beziehungsweise der Secret-Verwaltung bereitgestellt werden.

Der OIDC-Provider muss die Redirect-URI `{baseUrl}/login/oauth2/code/{registrationId}` zulassen. Reverse-Proxy-Header, TLS, Rate Limits und weitere Produktionsdetails stehen in [Betrieb und Konfiguration](operations.md).

## Häufige Probleme

### Die Anwendung startet mit deaktivierter Anmeldung nicht

Setze eine explizite Loopback-Adresse:

```text
--server.address=127.0.0.1
```

`0.0.0.0` ist bei `TEXTBUDDY_AUTH_ENABLED=false` absichtlich nicht erlaubt.

### Die Anwendung meldet fehlende LLM-Properties

Im Provider-Modus müssen Base-URL, API-Key und Modell gesetzt und im Prozess sichtbar sein. Exportiere die Variablen im selben Terminal, in dem du Textbuddy startest. Wenn du noch keinen Provider verwenden möchtest, starte ausdrücklich mit `TEXTBUDDY_LLM_MODE=stub`.

### Textbuddy liefert weiterhin Demo-Antworten

Beende den laufenden Prozess und starte Textbuddy neu. Entferne insbesondere `--textbuddy.llm.mode=stub` aus einem früheren Startbefehl, da dieser Parameter `TEXTBUDDY_LLM_MODE=provider` überstimmt. Im Provider-Modus erscheint im Log `LLM client: provider mode`.

### Der Provider liefert HTTP 401 oder 403

Der Provider lehnt den API-Key oder dessen Berechtigungen ab. Prüfe den Wert von `TEXTBUDDY_LLM_API_KEY` und ob das gewählte Modell mit diesem Zugang verwendet werden darf.

### Der Provider liefert HTTP 404

Prüfe die Base-URL. Für einen üblichen Provider ist beispielsweise `https://llm.example.com/v1` richtig; Textbuddy ruft daraus `/v1/chat/completions` auf. Hänge `/chat/completions` nicht selbst an.

### Der Provider liefert HTTP 429

Das Rate Limit oder ein Nutzungskontingent ist erreicht. Textbuddy wiederholt fehlgeschlagene Provider-Aufrufe nicht automatisch.

### Die LLM-Anfrage läuft in einen Timeout

Der Standard beträgt 30 Sekunden. Prüfe zuerst Erreichbarkeit und Provider-Auslastung. Falls längere Antworten regulär mehr Zeit benötigen, kannst du beispielsweise `TEXTBUDDY_LLM_TIMEOUT=60s` setzen und Textbuddy neu starten.

### Der Provider liefert keinen nutzbaren Antworttext

Prüfe, ob die Antwort dem Chat-Completions-Format entspricht und `choices[0].message.content` enthält. Strukturierte Funktionen wie Synonyme, Satzalternativen und Advisor benötigen darin zusätzlich das angeforderte JSON. Textbuddy gibt Provider-Antwortkörper absichtlich nicht in Fehlermeldungen aus.

### OCR funktioniert nicht

Der Stub-Dokumentimport funktioniert ohne OCR. Für echte Scan-Erkennung müssen die OCR-Laufzeit und die benötigten Sprachdaten auf dem Zielsystem installiert sein. Die CI prüft diesen Pfad separat.

## Optional: Tests und Release-Build für Mitentwickelnde

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

## Weiterführende Dokumentation

- [Architektur](architecture.md)
- [Betrieb und Konfiguration](operations.md)
- [Accessibility](accessibility.md)
