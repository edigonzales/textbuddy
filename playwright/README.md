# Playwright Workspace

Dieses Verzeichnis enthält die Browser- und UI-Tests für sichtbare Textbuddy-Interaktionen.

## Abdeckung

Die Suite deckt folgende Verhaltensbereiche ab:

- `editor-island.spec.ts`: Editor und Hidden Mirror, Undo/Redo, Statistik, Korrektur, lokales Wörterbuch, Advisor, Plaintext-Import, Dokumentexport sowie die sichtbaren Volltext-Aktionen mit gemeinsamem Diff-Review.
- `editor-island.a11y.spec.ts`: Accessibility und Tastaturbedienung für Editor, Prüfseitenleiste, Advisor-Befunde, Diff-Ansichten, Statistik-Popover, mobile Darstellung und Upload.

Die Tests bilden die sichtbare Oberfläche mit den Modi **Überarbeiten** und **Prüfen** ab. API-Hooks und `data-testid` sind die primären Selektoren; nicht freigeschaltete Werkzeuge müssen im DOM fehlen.

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
