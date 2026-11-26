# Eigene Advisor-Regelwerke erstellen

Textbuddy lädt Advisor-Regelwerke beim Start aus dem Anwendungsartefakt. Ein Regelwerk besteht aus genau zwei gleich benannten Dateien:

```text
src/main/resources/advisor/meta/meine-richtlinie.json
src/main/resources/advisor/docs/meine-richtlinie.pdf
```

Nach einer Änderung sind ein neuer Build und Neustart erforderlich. Es gibt bewusst kein Runtime-Verzeichnis, keinen Upload, keinen Regel-Editor, keine Rollensteuerung und keine Datenbank.

## Metadatenformat

```json
{
  "order": 10,
  "name": "meine-richtlinie",
  "title": "Meine Schreibrichtlinie",
  "summary": "Kurze Beschreibung für die Dokumentauswahl.",
  "source": "Organisation, Ausgabe 2026",
  "rules": [
    {
      "id": "klare-eindeutige-id",
      "title": "Kurzer sichtbarer Regeltitel",
      "page": 3,
      "instructions": "Prüfe präzise, wann diese Regel verletzt ist und wann nicht.",
      "message": "Erkläre den Verstoss respektvoll und konkret.",
      "suggestion": "Beschreibe eine fachlich sichere Verbesserung.",
      "matchTerms": ["problematischer Ausdruck"]
    }
  ]
}
```

`order` steuert die Katalogreihenfolge. `name` muss eindeutig sein, als sicherer Dateiname funktionieren und exakt zum PDF-Dateinamen passen. Alle Textfelder, mindestens eine Regel und mindestens ein `matchTerms`-Eintrag pro Regel sind Pflicht. Regel-IDs müssen innerhalb des Dokuments eindeutig sein; `page` beginnt bei 1.

`instructions` ist die fachliche Prüfanweisung für das LLM. Die Regeltexte `message` und `suggestion` geben Hinweise zur Begründung und zur Formulierung eines möglichen Ersatzes. `matchTerms` begrenzt Fundstellen auf konkrete Ausdrücke. Das LLM muss `matchedText` exakt aus dem Eingabetext kopieren. Der Ausschnitt muss einem Suchbegriff entsprechen, unabhängig von der Gross- und Kleinschreibung. Das ebenfalls exakt kopierte Feld `excerpt` muss im Eingabetext einmal vorkommen und `matchedText` genau einmal enthalten. Damit bestimmt der Server die Position und ordnet Dokument, Seite und PDF-Link zu.

Im Prüfresultat enthält `message` die Begründung und `replacement` nur den Text, der `matchedText` ersetzt. Ein leerer String löscht die Stelle. `null` kennzeichnet einen Befund ohne Ersatz. Der Browser erhält führenden, nachgestellten und internen Leerraum und verwendet wie im Editor üblich LF als Zeilenumbruch.

## Fachliche Review-Anforderungen

Vor einer Freigabe sollten Fachverantwortliche:

1. Quelle, Ausgabe, Seitenverweise und Nutzungsrechte prüfen.
2. Positive Fälle, erlaubte Gegenbeispiele, Gross-/Kleinschreibung, Teilwörter und Wiederholungen testen.
3. Sicherstellen, dass Vorschläge keine fachliche Bedeutung verändern oder Informationen entfernen.
4. Das Regelwerk mit realistischen Texten im Provider-Modus evaluieren; Stub-Treffer sind keine fachliche Abnahme.
5. Titel, Kurzbeschreibung und Quelle eindeutig als verbindlich, empfehlend oder Demo kennzeichnen.

Die fünf mitgelieferten Regelwerke sind projektinterne Demos mit je zwei Regeln. Sie illustrieren den technischen Ablauf, sind aber weder vollständig noch amtlich verbindlich.

## Technische Prüfung

Führe den [regulären Prüfpfad](../README.md#prüfen-und-paketieren) aus. Beim Start prüft der Katalog die PDFs, die Eindeutigkeit von Dokument- und Regel-IDs, die Seitenzahlen und die Pflichtfelder. Ergänze für neue Regeln gezielte positive und negative Beispiele, besonders für Begriffe, die auch als Teil längerer Wörter vorkommen.
