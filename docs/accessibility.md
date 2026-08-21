# Accessibility

Textbuddy strebt WCAG 2.2 AA an, erhebt aber ohne vollständige manuelle Prüfung keinen Konformitätsanspruch.

## Technischer Stand

- Die Hauptbereiche verwenden semantische Überschriften, Labels und Landmarken.
- Eine Skip-Link-Navigation führt zum Hauptinhalt.
- Werkzeug-Tabs und modale beziehungsweise eingeblendete Bereiche besitzen ARIA-Beziehungen.
- Statusänderungen werden über zurückhaltende Live-Regionen ausgegeben.
- Editor- und Werkzeugaktionen sind per Tastatur erreichbar; sichtbare Fokusdarstellung bleibt erhalten.
- Das nichtmodale Vorschlags-Popup erhält seinen zugänglichen Namen aus der aktuellen Textauswahl, lässt sich mit Escape schliessen und fokussiert nach tastaturbedientem Laden den ersten Vorschlag.
- Die Browser-Suite prüft Kernabläufe mit Playwright und `axe-core`.

Automatische Tests finden nicht alle Barrieren. Insbesondere Screenreader-Verständlichkeit, sinnvolle Fokusreihenfolge, Zoom/Reflow und die Bedienung komplexer Tiptap-Auswahlzustände brauchen manuelle Abnahme.

## Manuelle Abnahme pro Release

1. Gesamten Kernablauf nur mit Tastatur bedienen: Anmeldung, Schreiben, Korrektur, Quick Action, Advisor, Import und PDF-Viewer.
2. Fokus nach Öffnen, Schliessen, Fehlern und dynamischen Ergebnissen prüfen; kein Fokus darf verloren gehen.
3. Mit NVDA/Firefox oder VoiceOver/Safari Bezeichnungen, Statusmeldungen und Ergebnislisten nachvollziehen.
4. Bei 200 % und 400 % Zoom prüfen, dass Inhalt ohne horizontales Scrollen nutzbar bleibt, soweit WCAG dies verlangt.
5. Kontrast in Standard-, Hover-, Fokus-, Fehler- und Disabled-Zuständen prüfen.
6. Bewegungsreduktion mit `prefers-reduced-motion` testen.
7. Deutsch und Englisch auf fehlende oder technisch klingende Texte prüfen.

Gefundene Barrieren sollten als reproduzierbarer Test ergänzt werden, sofern sie automatisierbar sind. Das verhindert, dass dieselbe Regression erneut einzieht.
