# Betrieb und Konfiguration

## Produktionsstart

Das Installer-ZIP enthält `bin/start-textbuddy.sh` und `bin/start-textbuddy.cmd` für Java 25. Beide starten das mitgelieferte JAR und übernehmen die angegebenen Argumente. Das JAR lässt sich auch direkt starten.

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

### LLM und lokale Dienste

`textbuddy.llm.mode=provider` verwendet einen OpenAI-kompatiblen Provider und verlangt Base-URL, API-Key und Modell-ID. `stub` liefert für Entwicklung und Tests reproduzierbare Demo-Antworten. LanguageTool und Kreuzberg arbeiten im Anwendungsprozess.

### Lokale OCR

Kreuzberg verwendet Tesseract. `TESSDATA_PREFIX` muss auf den Ordner mit `deu.traineddata`, `eng.traineddata`, `fra.traineddata` und `ita.traineddata` zeigen. Die regulären Tests prüfen Text aus einem echten Scanbild und einem gescannten PDF.

Unter Ubuntu richtet die CI die Laufzeit so ein:

```bash
sudo apt-get install tesseract-ocr tesseract-ocr-deu tesseract-ocr-eng tesseract-ocr-fra tesseract-ocr-ita
export TESSDATA_PREFIX="$(dirname "$(dpkg -L tesseract-ocr-eng | grep '/eng.traineddata$')")"
```

Auf macOS (Apple Silicon) wurde die mit Kreuzberg gelieferte Laufzeit mit den vier [Tesseract-Sprachdateien aus tessdata_fast](https://github.com/tesseract-ocr/tessdata_fast/tree/4.1.0) geprüft. Lege die Dateien gemeinsam in einem Ordner ab und setze `TESSDATA_PREFIX` auf dessen absoluten Pfad. Die OCR-Laufzeit wurde auf Ubuntu x86-64 und macOS Apple Silicon geprüft.

## Grenzen und Timeouts

| Einstellung | Standard | Bedeutung |
| --- | ---: | --- |
| `textbuddy.input.max-text-length` | `50000` | maximale Zeichen pro Textanfrage |
| `textbuddy.document.max-upload-size` | `20MB` | Multipart- und fachliche Uploadgrenze |
| `textbuddy.llm.timeout` | `30s` | Timeout einer LLM-Anfrage |
| `textbuddy.document.timeout` | `45s` | Timeout der Dokumentverarbeitung |

Nur lokale OCR wiederholt bei einem sprachbezogenen OCR-Fehler einmal mit der Standardsprache.

Eine Advisor-Prüfung verarbeitet maximal fünf Dokumente und zwanzig Regeln und liefert höchstens zwanzig Befunde. Beim aktuellen Katalog mit zehn Regeln sind das vier serielle LLM-Aufrufe in Dreierbatches. Das SSE-Timeout leitet sich aus der maximalen Batchzahl und dem LLM-Timeout ab. Die Prüfung liefert bereits die Ersatztexte für die lokale Vorschau und Übernahme im Browser.

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
- das veröffentlichte JAR beziehungsweise Installer-ZIP.

Das browserlokale Wörterbuch kann nicht serverseitig wiederhergestellt werden. Advisor-Dokumente sind Bestandteil des JARs und werden durch ein neues Release aktualisiert.

Die ausgelieferten Advisor-Regelwerke sind projektinterne Demos. Für eigene Regelwerke muss ein fachlich geprüftes JSON-/PDF-Paar gemäss [Advisor-Regelwerke erstellen](advisor-authoring.md) gebaut und die Anwendung neu gestartet werden.
