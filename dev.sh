#!/usr/bin/env bash

set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$project_dir"

dev_pids=()

stop_dev_processes() {
    local exit_code=$?

    trap - EXIT INT TERM
    set +e

    for dev_pid in "${dev_pids[@]}"; do
        if kill -0 "$dev_pid" 2>/dev/null; then
            kill "$dev_pid" 2>/dev/null
        fi
    done

    for dev_pid in "${dev_pids[@]}"; do
        wait "$dev_pid" 2>/dev/null
    done

    exit "$exit_code"
}

trap stop_dev_processes EXIT
trap 'exit 130' INT TERM

./gradlew classes

npm run watch --prefix frontend &
dev_pids+=("$!")

./gradlew bootRun --args='--server.address=127.0.0.1 --textbuddy.auth.enabled=false --textbuddy.llm.mode=stub --textbuddy.languagetool.mode=stub --textbuddy.document.mode=stub' &
dev_pids+=("$!")

while kill -0 "${dev_pids[0]}" 2>/dev/null && kill -0 "${dev_pids[1]}" 2>/dev/null; do
    sleep 1
done

exit_code=0
for dev_pid in "${dev_pids[@]}"; do
    if ! kill -0 "$dev_pid" 2>/dev/null; then
        wait "$dev_pid" || exit_code=$?
        break
    fi
done

exit "$exit_code"
