# Playwright Workspace

Dieses Verzeichnis enthält die Browser- und UI-Tests für sichtbare Textbuddy-Interaktionen.

## Abdeckung

Der Suite-Stand umfasst 37 Tests:

- `editor-island.spec.ts`: 29 funktionale E2E-Flows für Editor, Hidden Mirror, Undo/Redo, Statistik, Korrektur, lokale Wörterbuchlogik, Import, Advisor, Rewrite Bubble und Quick Actions.
- `editor-island.a11y.spec.ts`: 8 Accessibility- und Keyboard-Flows für Idle, Quick-Action-Konfiguration, Streaming + Diff, Advisor Viewer, Mobile Layout, Upload-Button, Rewrite Escape und Viewer Escape.

Die Tests bilden die neue Editor-plus-Inspector-Struktur ab. Panels für `Korrektur`, `Aktionen`, `Advisor`, `Import` und `Statistik` werden dabei über die sichtbare Tab-Leiste geöffnet; API-Hooks und `data-testid` bleiben die primären Selektoren.

## Ausführen

```bash
npm test
```

Voraussetzung: Die Spring-Boot-App wird durch die Playwright-Konfiguration gestartet. Für lokale Einzeltests kann ein Dateiname oder `--grep` ergänzt werden:

```bash
npm test -- editor-island.spec.ts
npm test -- --grep "document import rejects unsupported formats"
```

## Nicht Abgedeckt

- Manueller Screenreader-Durchlauf mit VoiceOver/NVDA.
- Echte Provider-Smokes gegen externe LLM-Endpunkte; die Browser-Suite stubbt die Netzwerkantworten deterministisch.
