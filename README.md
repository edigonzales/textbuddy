# textbuddy

Dieses Repository enthält die planungs- und umsetzungsleitenden Unterlagen für eine schrittweise Neuimplementierung von TextMate mit:

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

## Arbeitsmodus

- Die Umsetzung erfolgt **inkrementell**.
- Eine Implementierungssession bearbeitet **genau einen Slice**.
- Spätere Features dürfen nicht vorgezogen werden.
- Nach jeder Session werden nur Status und Handoff aktualisiert.
