@echo off
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "ROOT_DIR=%SCRIPT_DIR%.."
set "JAR_PATH=%ROOT_DIR%\textbuddy.jar"

where java >nul 2>&1
if errorlevel 1 (
  echo Fehler: Java wurde nicht gefunden. Bitte Java 25 oder höher installieren.
  exit /b 1
)

set "JAVA_VERSION_LINE="
for /f "tokens=*" %%A in ('java -version 2^>^&1') do (
  if not defined JAVA_VERSION_LINE set "JAVA_VERSION_LINE=%%A"
)

set "JAVA_VERSION_RAW="
for /f "tokens=3 delims= " %%V in ('echo %JAVA_VERSION_LINE% ^| findstr /i "version"') do (
  set "JAVA_VERSION_RAW=%%~V"
)

set "JAVA_VERSION_RAW=%JAVA_VERSION_RAW:"=%"
for /f "tokens=1 delims=." %%V in ("%JAVA_VERSION_RAW%") do set "JAVA_MAJOR=%%V"

if not defined JAVA_MAJOR (
  echo Fehler: Java-Version konnte nicht ermittelt werden.
  exit /b 1
)

if %JAVA_MAJOR% LSS 25 (
  echo Fehler: Gefundene Java-Version ist zu alt (%JAVA_VERSION_LINE%). Erforderlich ist Java 25 oder höher.
  exit /b 1
)

if not exist "%JAR_PATH%" (
  echo Fehler: textbuddy.jar wurde nicht gefunden unter %JAR_PATH%.
  exit /b 1
)

java %TEXTBUDDY_JAVA_OPTS% -jar "%JAR_PATH%" %*
