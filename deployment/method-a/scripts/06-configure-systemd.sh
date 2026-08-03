#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
METHOD_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"

readonly SERVICE_SOURCE="${METHOD_ROOT}/systemd/community-backend.service"
readonly SERVICE_TARGET="/etc/systemd/system/community-backend.service"

require_root
require_command systemctl
require_command stat
require_file "${SERVICE_SOURCE}"
require_file "${ENV_FILE}"

if ! file_has_exact_metadata \
  "${ENV_FILE}" \
  root \
  "${COMMUNITY_GROUP}" \
  640; then
  echo "${ENV_FILE} must be owned by root:${COMMUNITY_GROUP} with mode 640." >&2
  exit 1
fi

install -o root -g root -m 0644 \
  "${SERVICE_SOURCE}" \
  "${SERVICE_TARGET}"
systemctl daemon-reload
systemctl enable community-backend.service

echo "systemd unit installed. Start it only after artifacts and secrets are ready."
