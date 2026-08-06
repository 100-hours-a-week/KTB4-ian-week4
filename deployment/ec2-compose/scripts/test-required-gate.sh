#!/usr/bin/env bash

set -Eeuo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
gate="${SCRIPT_DIR}/required-gate.sh"
"${gate}" success success success >/dev/null
for result in failure cancelled skipped ''; do
  if "${gate}" success "${result}" success >/dev/null 2>&1; then
    echo "FAIL: gate accepted '${result:-missing}'." >&2
    exit 1
  fi
done
echo "PASS: required gate rejects every non-success result."
