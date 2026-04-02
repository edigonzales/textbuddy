# Workflow für schrittweise Codex-Sessions

## Ziel

Diese Datei beschreibt, wie die Neuumsetzung über viele kleine Sessions organisiert wird, ohne dass Codex mehrere Features gleichzeitig anschneidet.

## Grundregel

- **Eine Session = ein Slice**

Alles andere ist verboten, außer kleine Stubs, die für den aktuellen Slice zwingend nötig sind.

## Start einer Session

Vor jeder neuen Implementierungssession werden nur diese Dateien an Codex gegeben:

- [Master-Spec](/Users/stefan/sources/textbuddy/docs/implementation/00-master-spec.md)
- [Codex-Regeln](/Users/stefan/sources/textbuddy/docs/implementation/02-codex-rules.md)
- die konkrete Slice-Datei
- die konkrete Prompt-Datei
- [STATUS.md](/Users/stefan/sources/textbuddy/docs/implementation/STATUS.md)

Nicht die gesamte Dokumentation und nicht alle Slices gleichzeitig.

## Ablauf innerhalb einer Session

1. Ziel-Slice aus `STATUS.md` auswählen
2. Slice-Datei und Prompt lesen
3. Nur die dort genannten Endpunkte, UI-Teile und Tests umsetzen
4. Alles, was später kommt, explizit auslassen
5. Am Ende:
   - geänderte Dateien nennen
   - Tests nennen
   - offene Punkte festhalten
   - nächsten sinnvollen Slice benennen

## Nach der Session

- Nur der bearbeitete Slice wird in `STATUS.md` angepasst
- Optional wird der nächste Slice als empfohlener nächster Schritt erwähnt
- Handoff-Text wird nach dem Template aus [04-session-template.md](/Users/stefan/sources/textbuddy/docs/implementation/04-session-template.md) erstellt

## Verbotene Arbeitsweise

- Mehrere Slices in einer Session
- Spätere Features „gleich mitnehmen“
- Große Infrastruktur auf Vorrat bauen
- WebFlux oder reaktive API-Typen einführen
- Ungetestete halbfertige UI-Teile stehen lassen

## Empfohlene Reihenfolge

1. Slice 00: Projektbasis
2. Slice 01: Editor-Insel-Basis
3. Slice 02 bis 05: Kerntextfunktionen
4. Slice 06 bis 14: Quick Actions einzeln
5. Slice 15 bis 17: Advisor und Dokumentimport
6. Slice 18: Auth und Polishing
