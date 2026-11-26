---
name: review-change
description: Prüft den tatsächlichen Textbuddy-Diff unabhängig gegen den Auftrag und die aktiven Produkt- und Betriebsverträge.
---

1. Lies Auftrag, `AGENTS.md` und [simplify-change](../simplify-change/SKILL.md). Inspiziere den vollständigen staged Diff einschliesslich neuer Dateien gegen die genannte Basis. Verfolge normale Aufrufer und indirekte Build-, Konfigurations- und Ressourcenbezüge.
2. Prüfe Texttreue, halb offene UTF-16-Fundstellen, veraltete Ergebnisse, Änderungsvorschau und Fehlerverhalten entlang der betroffenen Abläufe. Prüfe OIDC, CSRF, Eingabegrenzen und Datenschutz dort, wo der Diff sie berührt. Wende `simplify-change` auf die verbleibenden Wege an.
3. Prüfe fachliche Assertions und tatsächliche Testresultate. Führe bei einem konkreten Risiko eine gezielte Gegenprobe aus, soweit sie ohne Änderungen am Arbeitsbaum möglich ist. Verwende synthetische Daten und lokale Testadapter. Arbeite lesend und unterlasse Commits und Pushes.

Nenne Basis und geprüften Diff, ausgeführte Prüfungen und Grenzen. Ordne Befunde nach Auswirkung und nenne Datei, Methode, Auslöser, Konsequenz und kleinste Korrektur. Schliesse mit einem eindeutigen Urteil zu offenen relevanten Befunden. Prüfe Korrekturen gezielt am aktualisierten Diff.
