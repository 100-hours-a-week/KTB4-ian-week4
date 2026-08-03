#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"

require_root
require_command install
require_command useradd

if ! getent group "${COMMUNITY_GROUP}" >/dev/null; then
  groupadd --system "${COMMUNITY_GROUP}"
fi

if ! id "${COMMUNITY_USER}" >/dev/null 2>&1; then
  useradd \
    --system \
    --gid "${COMMUNITY_GROUP}" \
    --home-dir "${COMMUNITY_ROOT}" \
    --shell /usr/sbin/nologin \
    "${COMMUNITY_USER}"
fi

install -d -o root -g "${COMMUNITY_GROUP}" -m 0750 /etc/community
install -d -o root -g root -m 0755 /opt/community
install -d -o root -g "${COMMUNITY_GROUP}" -m 0750 "${BACKEND_ROOT}"
install -d -o root -g "${COMMUNITY_GROUP}" -m 0750 "${BACKEND_ROOT}/releases"
install -d -o root -g root -m 0755 "${FRONTEND_ROOT}"
install -d -o root -g root -m 0755 "${FRONTEND_ROOT}/releases"

install -d -o "${COMMUNITY_USER}" -g "${COMMUNITY_GROUP}" -m 0750 \
  "${COMMUNITY_ROOT}"
install -d -o "${COMMUNITY_USER}" -g "${COMMUNITY_GROUP}" -m 0750 \
  "${COMMUNITY_ROOT}/uploads"
install -d -o root -g "${COMMUNITY_GROUP}" -m 0750 \
  "${COMMUNITY_ROOT}/backup"
install -d -o root -g "${COMMUNITY_GROUP}" -m 0750 \
  "${COMMUNITY_ROOT}/evidence"

if [[ ! -e "${ENV_FILE}" ]]; then
  install -o root -g "${COMMUNITY_GROUP}" -m 0640 /dev/null "${ENV_FILE}"
else
  chown root:"${COMMUNITY_GROUP}" "${ENV_FILE}"
  chmod 0640 "${ENV_FILE}"
fi

echo "Service user and directories are ready."
echo "Populate ${ENV_FILE} with sudoedit before starting the backend."
