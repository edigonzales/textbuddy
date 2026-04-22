# Abnahme- und Release-Checkliste

## Automatisierte Pflichtprüfungen

1. Build, Bundle und Installer
- `./gradlew clean verifyReleaseBundle installerZip`
- Erwartung: `build/release/textbuddy.jar` und `build/release/textbuddy-<version>.zip` sind vorhanden.

2. Smoke gegen gepacktes Artefakt
- `./gradlew test --tests 'app.textbuddy.smoke.JarStartupSmokeTest'`
- Erwartung: Jar startet als `textbuddy.jar`, Health und Info sind erreichbar.

3. End-to-End gegen gepacktes Artefakt ohne Sidecars
- `./gradlew test --tests 'app.textbuddy.smoke.JarEndToEndSmokeTest'`
- Erwartung: Kernflows (Korrektur, Rewrite, Quick Action, Advisor, Import) funktionieren über HTTP gegen das Jar.

4. Start-, Shutdown- und Recoverability-Prüfung
- Bestandteil von `JarStartupSmokeTest`
- Erwartung: Stop und Wiederanlauf auf demselben Port funktionieren reproduzierbar.

5. Performance-Smoke
- `./gradlew test --tests 'app.textbuddy.smoke.JarPerformanceSmokeTest'`
- Erwartung: Korrektur, Rewrite, Advisor und Import bleiben innerhalb der definierten Smoke-Grenzen.

6. CI-Workflow-Prüfung
- Trigger: `pull_request` oder Push auf `main`
- Erwartung: `ci.yml` läuft mit Java 25 und Node 20, veröffentlicht kein Release und lädt bei Fehlern Reports hoch.

7. Release-Workflow-Prüfung
- Trigger: SemVer-Tag `vX.Y.Z`
- Erwartung: `release.yml` erzeugt Draft Release mit folgenden Assets:
  - `textbuddy.jar`
  - `textbuddy.jar.sha256`
  - `textbuddy.jar.asc`
  - `textbuddy.jar.sha256.asc`
  - `textbuddy-<version>.zip`

8. Integritäts-/Signaturprüfung
- `sha256sum -c textbuddy.jar.sha256`
- `gpg --verify textbuddy.jar.asc textbuddy.jar`
- `gpg --verify textbuddy.jar.sha256.asc textbuddy.jar.sha256`
- Erwartung: alle Prüfungen grün, manipuliertes Jar fällt durch.

## Manuelle Abnahmekriterien

1. Standardstartpfad
- Aus `build/release` startet `java -jar textbuddy.jar` ohne Sidecars.

2. Installer-Startpfad
- ZIP entpacken und `bin/start-textbuddy.sh` ausführen.
- Health-Check (`/actuator/health`) liefert `UP`.

3. Java-Mindestversion im Skript
- Simulierter Java-17-Pfad führt zu klarer Fehlermeldung und Abbruch.

4. Betriebsdiagnose
- `/actuator/health` liefert `UP`.
- `/actuator/info` zeigt aktive Betriebsmodi.

5. Lokale Laufzeitressourcen
- Das Runtime-Verzeichnis enthält `config`, `logs`, `tmp`, `cache`.

6. Konfigurationsklarheit
- LLM-Pflichtwerte sind in den Konfigurationsbeispielen dokumentiert.

## Freigabekriterium

Die Release-Stufe gilt als abnahmefähig, wenn alle automatisierten Pflichtprüfungen grün sind, die manuellen Abnahmekriterien erfüllt sind und der Draft Release manuell publiziert werden kann.
