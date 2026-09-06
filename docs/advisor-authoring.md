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

`instructions` ist die fachliche Prüfanweisung für das LLM. `message` und `suggestion` sind kontrollierte Rückfälle und helfen dem Stub. `matchTerms` begrenzt Fundstellen auf konkrete Ausdrücke: Das LLM muss `matchedText` exakt aus dem Eingabetext kopieren und dieser Text muss – ohne Beachtung der Gross-/Kleinschreibung – einem Suchbegriff entsprechen. Dadurch kann der Server Positionen selbst bestimmen und Fix-Anfragen gegen die kanonische Regel prüfen.

## Fachliche Review-Anforderungen

Vor einer Freigabe sollten Fachverantwortliche:

1. Quelle, Ausgabe, Seitenverweise und Nutzungsrechte prüfen.
2. Positive Fälle, erlaubte Gegenbeispiele, Gross-/Kleinschreibung, Teilwörter und Wiederholungen testen.
3. Sicherstellen, dass Vorschläge keine fachliche Bedeutung verändern oder Informationen entfernen.
4. Das Regelwerk mit realistischen Texten im Provider-Modus evaluieren; Stub-Treffer sind keine fachliche Abnahme.
5. Titel, Kurzbeschreibung und Quelle eindeutig als verbindlich, empfehlend oder Demo kennzeichnen.

Die fünf mitgelieferten Regelwerke sind projektinterne Demos mit je zwei Regeln. Sie illustrieren den technischen Ablauf, sind aber weder vollständig noch amtlich verbindlich.

## Technische Prüfung

```bash
./gradlew test
npm test --prefix playwright
```

Die Katalogtests erkennen unter anderem fehlende PDFs, doppelte Dokument- oder Regel-IDs, ungültige Seitenzahlen und leere Pflichtfelder. Ergänze für neue Regeln gezielte positive und negative Beispiele, besonders für Begriffe, die auch als Teil längerer Wörter vorkommen.
