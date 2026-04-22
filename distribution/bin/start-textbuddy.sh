#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
JAR_PATH="${ROOT_DIR}/textbuddy.jar"

if ! command -v java >/dev/null 2>&1; then
  echo "Fehler: Java wurde nicht gefunden. Bitte Java 25 oder höher installieren." >&2
  exit 1
fi

JAVA_VERSION_LINE="$(java -version 2>&1 | head -n1)"
if [[ ! "${JAVA_VERSION_LINE}" =~ ([0-9]+) ]]; then
  echo "Fehler: Java-Version konnte nicht ermittelt werden." >&2
  exit 1
fi

JAVA_MAJOR="${BASH_REMATCH[1]}"
if [ "${JAVA_MAJOR}" -lt 25 ]; then
  echo "Fehler: Gefundene Java-Version ist zu alt (${JAVA_VERSION_LINE}). Erforderlich ist Java 25 oder höher." >&2
  exit 1
fi

if [ ! -f "${JAR_PATH}" ]; then
  echo "Fehler: textbuddy.jar wurde nicht gefunden unter ${JAR_PATH}." >&2
  exit 1
fi

JAVA_OPTS_VALUE="${TEXTBUDDY_JAVA_OPTS:-}"
if [ -n "${JAVA_OPTS_VALUE}" ]; then
  # shellcheck disable=SC2206
  JAVA_OPTS_ARRAY=(${JAVA_OPTS_VALUE})
else
  JAVA_OPTS_ARRAY=()
fi

exec java "${JAVA_OPTS_ARRAY[@]}" -jar "${JAR_PATH}" "$@"
