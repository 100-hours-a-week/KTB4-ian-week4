#!/usr/bin/env bash

set -Eeuo pipefail
umask 027

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
METHOD_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"

require_root
require_command find
require_command install
require_command readlink
require_command rsync
assert_safe_path "${OPERATIONS_ROOT}"

if [[ "$(readlink -f "${METHOD_ROOT}")" == \
  "$(readlink -f "${OPERATIONS_ROOT}")" ]]; then
  echo "Operations bundle is already installed at ${OPERATIONS_ROOT}."
  exit 0
fi

install -d -o root -g root -m 0755 "$(dirname -- "${OPERATIONS_ROOT}")"
install -d -o root -g root -m 0755 "${OPERATIONS_ROOT}"

rsync -a --delete \
  --chown=root:root \
  --chmod=D0755,F0644 \
  "${METHOD_ROOT}/" \
  "${OPERATIONS_ROOT}/"

find "${OPERATIONS_ROOT}" -type f -name '*.sh' -exec chmod 0755 {} +

echo "Operations bundle installed: ${OPERATIONS_ROOT}"
