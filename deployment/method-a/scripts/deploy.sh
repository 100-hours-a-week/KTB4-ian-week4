#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"

require_root
require_command curl
require_file "${ENV_FILE}"

migration_access_granted=false
cleanup() {
  if [[ "${migration_access_granted}" == true ]]; then
    "${SCRIPT_DIR}/mysql-migration-access.sh" revoke || true
  fi
}
trap cleanup EXIT

"${SCRIPT_DIR}/install-operations.sh"
"${SCRIPT_DIR}/04-deploy-backend.sh"
"${SCRIPT_DIR}/05-deploy-frontend.sh"
"${SCRIPT_DIR}/06-configure-systemd.sh"
"${SCRIPT_DIR}/07-configure-nginx.sh"

"${SCRIPT_DIR}/mysql-migration-access.sh" grant
migration_access_granted=true
systemctl restart community-backend.service
systemctl restart nginx

backend_ready=false
for _ in {1..30}; do
  if curl --fail --silent \
    --output /dev/null \
    http://127.0.0.1:8080/api/csrf; then
    backend_ready=true
    break
  fi
  sleep 2
done
if [[ "${backend_ready}" != true ]]; then
  echo "Backend did not become ready within 60 seconds." >&2
  exit 1
fi

"${SCRIPT_DIR}/mysql-migration-access.sh" revoke
migration_access_granted=false

echo "Deployment applied. Run verify.sh and inspect service logs."
