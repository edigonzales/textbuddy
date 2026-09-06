# Betrieb und Konfiguration

## Produktionsstart

Empfohlen ist ein einzelner Textbuddy-Prozess hinter einem TLS-terminierenden Reverse Proxy:

```bash
java --enable-native-access=ALL-UNNAMED -jar textbuddy.jar \
  --spring.config.additional-location=file:/etc/textbuddy/application.properties
```

Die Datei `config/examples/application-production.properties.example` zeigt die notwendige OIDC- und Adapterkonfiguration. Secrets gehören in die Secret-Verwaltung der Laufzeitumgebung, nicht ins Repository oder Image.

Für Umgebungsvariablen kann `config/examples/textbuddy.env.example` als Vorlage dienen. Java lädt `.env` nicht selbst; die Variablen müssen durch den Prozessmanager gesetzt oder vor dem Start explizit mit `source` exportiert werden.

## Pflichtentscheidungen

### Authentifizierung

`textbuddy.auth.enabled=true` ist der Standard. Zusätzlich muss mindestens eine `spring.security.oauth2.client.registration.*`-Konfiguration vorhanden sein. Hinter einem Proxy muss dieser `X-Forwarded-Proto`, `X-Forwarded-Host` und `X-Forwarded-For` vertrauenswürdig neu setzen; die Beispielkonfiguration aktiviert Spring Forwarded Headers.

Der Entwicklungsmodus ist absichtlich eng:

```properties
server.address=127.0.0.1
textbuddy.auth.enabled=false
```

Ohne explizite Loopback-Adresse verweigert die Anwendung diesen Start. Diese Einstellung ist nicht für Container- oder Team-Betrieb vorgesehen.

### Adapter

- `textbuddy.llm.mode=provider|stub`: Produktion verwendet normalerweise `provider` und verlangt Base-URL, API-Key und Modell-ID.
- `textbuddy.languagetool.mode=embedded|http|stub`: `embedded` benötigt keinen Sidecar; ein optionaler N-Gram-Pfad verbessert bestimmte Regeln.
- `textbuddy.document.mode=kreuzberg|http|stub`: `kreuzberg` verarbeitet lokal. OCR benötigt eine auf der Zielplattform funktionierende Kreuzberg-OCR-Laufzeit und passende Sprachdaten.

Stub-Modi dienen lokaler Entwicklung und reproduzierbaren Tests, nicht fachlicher Abnahme.

## Grenzen und Timeouts

| Einstellung | Standard | Bedeutung |
| --- | ---: | --- |
| `textbuddy.input.max-text-length` | `50000` | maximale Zeichen pro Textanfrage |
| `textbuddy.input.max-prompt-length` | `2000` | maximale Zeichen eines eigenen Auftrags |
| `textbuddy.document.max-upload-size` | `20MB` | Multipart- und fachliche Uploadgrenze |
| `textbuddy.llm.timeout` | `30s` | Timeout einer LLM-Anfrage |
| `textbuddy.languagetool.timeout` | `10s` | Timeout im HTTP-Modus |
| `textbuddy.document.timeout` | `45s` | Timeout der Dokumentverarbeitung |

Textbuddy wiederholt fehlgeschlagene Provider- oder Netzwerkaufrufe nicht automatisch. Retries, falls fachlich gewünscht, müssen bewusst ausserhalb der Anwendung mit Idempotenz- und Kostenbetrachtung entworfen werden. Nur lokale OCR darf bei einem sprachbezogenen OCR-Fehler einmal mit der Standardsprache wiederholen.

Eine Advisor-Prüfung verarbeitet maximal fünf Dokumente und zwanzig Regeln. Beim aktuellen Katalog mit zehn Regeln sind das vier serielle LLM-Aufrufe in Dreierbatches; das SSE-Timeout wird aus der maximalen Batchzahl und dem LLM-Timeout abgeleitet. Das Anwenden ausgewählter Befunde verursacht genau einen zusätzlichen atomaren LLM-Aufruf. Die Summe der angezeigten Vorschläge ist durch `textbuddy.input.max-prompt-length` begrenzt.

## Reverse Proxy

Der Proxy sollte:

- TLS erzwingen und HTTP auf HTTPS umleiten,
- Request- und Uploadgrössen mindestens so streng wie Textbuddy begrenzen,
- Rate Limits pro Benutzer beziehungsweise Quellnetz setzen,
- eigene `X-Forwarded-*`-Header setzen und eingehende gleichnamige Header entfernen,
- den SSE-Endpoint `/api/advisor/validate` nicht puffern,
- für normale Requests ein Timeout oberhalb der konfigurierten Adapter-Timeouts verwenden.

## Monitoring und Logs

`GET /actuator/health` ist der einzige veröffentlichte Actuator-Endpoint. Er liefert absichtlich weder Komponenten noch interne Details und verlangt im zentralen, OIDC-geschützten Betrieb ebenfalls eine Anmeldung. Für Liveness genügt HTTP 200; Readiness externer Provider wird nicht durch Probe-Aufrufe simuliert.

Jede Anfrage erhält eine `X-Trace-ID`. Eine gültige eingehende ID darf maximal 64 Zeichen aus Buchstaben, Ziffern, Punkt, Unterstrich oder Bindestrich enthalten; andere Werte werden ersetzt. Provider-Antwortkörper und Secrets erscheinen nicht in kontrollierten Fehlern.

## Daten und Wiederherstellung

Textbuddy hält keine serverseitigen Nutzdaten dauerhaft. Zu sichern sind daher nur:

- Deployment- und Secret-Konfiguration,
- das veröffentlichte JAR beziehungsweise Installer-ZIP,
- gegebenenfalls externe LanguageTool-N-Gram-Daten.

Das browserlokale Wörterbuch kann nicht serverseitig wiederhergestellt werden. Advisor-Dokumente sind Bestandteil des JARs und werden durch ein neues Release aktualisiert.

Die ausgelieferten Advisor-Regelwerke sind projektinterne Demos. Für eigene Regelwerke muss ein fachlich geprüftes JSON-/PDF-Paar gemäss [Advisor-Regelwerke erstellen](advisor-authoring.md) gebaut und die Anwendung neu gestartet werden.

## Release-Prüfung

```bash
./gradlew clean test verifyReleaseBundle installerZip
npm ci --prefix playwright
npx --prefix playwright playwright install chromium
npm test --prefix playwright
```

Vor einem Release zusätzlich manuell prüfen: Anmeldung/Abmeldung am echten OIDC-Provider, Proxy-Header und Redirect-URI, LLM-Datenschutzfreigabe, OCR auf der Zielplattform sowie Upload- und Rate Limits.
