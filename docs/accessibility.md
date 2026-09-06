# Accessibility

Textbuddy strebt WCAG 2.2 AA an, erhebt aber ohne vollständige manuelle Prüfung keinen Konformitätsanspruch.

## Tastaturführung im MVP

- Der Skip-Link führt direkt zur Arbeitsfläche.
- **Überarbeiten** und **Prüfen** sind als Tabs umgesetzt. Linke und rechte Pfeiltaste wechseln zwischen ihnen.
- Alle Ribbon- und Editoraktionen sind mit der Tastatur erreichbar und besitzen zugängliche Bezeichnungen.
- Korrekturmarkierungen sind fokussierbar. Ihre Aktivierung wechselt zu **Prüfen**, öffnet die Ergebnisleiste und fokussiert den zugehörigen Befund.
- Auf Mobilgeräten hält die geöffnete Korrekturleiste den Fokus. Escape schliesst sie und gibt den Fokus an den Auslöser zurück.
- Das Statistik-Popover lässt sich mit Escape schliessen; der Fokus kehrt zum Zähler zurück.
- Diff-Blöcke können einzeln oder global angenommen und abgelehnt werden. Der deutsche Flesch-Vergleich nennt Vorher-, Nachher- und Differenzwert als Text und ist nicht allein über Farbe vermittelt. Moduswechsel und weitere mutierende Aktionen sind während Verarbeitung und Review deaktiviert.
- Status-, Erfolgs- und Fehlermeldungen werden über Live-Regions angekündigt. Das gilt auch für die optionale zweite Phase „Lesbarkeit wird weiter verbessert …“.

Nicht freigeschaltete Werkzeuge werden im Frontend gar nicht gerendert. Damit entstehen weder unsichtbare Fokusziele noch parallele DOM-Verträge für dieselbe Funktion.

## Automatische Abdeckung

Die Browser-Suite prüft mit Playwright und `axe-core`:

- die leere Editorarbeitsfläche,
- die geöffnete Korrekturleiste,
- laufende deutsche Nachbesserung sowie Inline- und Zweispalten-Diff einschliesslich Flesch-Vergleich,
- das Statistik-Popover mit eingeblendeter deutscher Flesch-Bewertung,
- die mobile Ribbon- und Slideover-Nutzung.

Unit- und Browser-Tests decken ausserdem Fokus-Rückgabe, Escape, fehlenden horizontalen Überlauf, die Abwesenheit nicht freigeschalteter Werkzeuge und die dynamische Ergebnisleiste ab.

## Manuelle Abnahme pro Release

1. Mit Tastatur schreiben, korrigieren, Modus wechseln, Ergebnisse schliessen und wieder öffnen.
2. Transformation starten und alle Einzel- und Globalentscheidungen im Inline- sowie Zweispalten-Diff bedienen.
3. Upload, Drag-and-drop, Kopieren, DOCX-Download und Statistik prüfen.
4. Mit NVDA/Firefox oder VoiceOver/Safari Namen, Statusmeldungen, Ergebnisliste und Review nachvollziehen.
5. Bei 200 % und 400 % Zoom sowie auf 390 px Breite Reflow und horizontalen Überlauf prüfen.
6. Standard-, Hover-, Fokus-, Fehler- und Disabled-Zustände auf Kontrast prüfen.
7. Bewegungsreduktion mit `prefers-reduced-motion` und beide Oberflächensprachen prüfen.

Automatische Tests ersetzen keine Prüfung der Screenreader-Verständlichkeit, sinnvollen Fokusreihenfolge und komplexen Tiptap-Auswahlzustände. Reproduzierbare Barrieren sollten als Regressionstest ergänzt werden.
