#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"
exec ./gradlew bootRun --args='--server.address=127.0.0.1 --textbuddy.auth.enabled=false --textbuddy.llm.mode=stub'
