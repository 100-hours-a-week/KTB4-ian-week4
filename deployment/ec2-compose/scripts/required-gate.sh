#!/usr/bin/env bash

set -Eeuo pipefail
(( $# > 0 )) || { echo "At least one prerequisite result is required." >&2; exit 2; }
for result in "$@"; do
  [[ "${result}" == success ]] || {
    echo "Required prerequisite result was '${result:-missing}', expected 'success'." >&2
    exit 1
  }
done
echo "All required prerequisites succeeded."
