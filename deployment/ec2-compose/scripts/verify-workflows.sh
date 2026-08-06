#!/usr/bin/env bash

set -Eeuo pipefail

root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../.." && pwd)"
failures=0
while IFS= read -r use_line; do
  [[ "${use_line}" == *'uses: ./'* ]] && continue
  if [[ ! "${use_line}" =~ @[0-9a-f]{40}[[:space:]]*#[[:space:]]*v[0-9] ]]; then
    echo "Unpinned or undocumented external Action: ${use_line}" >&2
    failures=$((failures + 1))
  fi
done < <(rg -N '^[[:space:]]*-[[:space:]]+uses:' "${root}/.github/workflows")

if rg -n 'pull_request_target|:[[:space:]]*latest([[:space:]]|$)' "${root}/.github/workflows"; then
  echo "Forbidden workflow trigger or mutable image tag found." >&2
  failures=$((failures + 1))
fi
[[ "${failures}" -eq 0 ]] || exit 1
echo "PASS: external Actions use full SHAs and workflows avoid forbidden patterns."
