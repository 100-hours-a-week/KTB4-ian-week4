#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"

require_root
require_command curl
require_command systemctl

current_backend="$(readlink -f "${BACKEND_ROOT}/app.jar" || true)"
current_frontend="$(readlink -f "${FRONTEND_ROOT}/current" || true)"

mapfile -t backend_releases < <(
  find "${BACKEND_ROOT}/releases" -maxdepth 1 -type f -name '*.jar' \
    -printf '%T@ %p\n' |
    sort -nr |
    cut -d' ' -f2-
)
mapfile -t frontend_releases < <(
  find "${FRONTEND_ROOT}/releases" -mindepth 1 -maxdepth 1 -type d \
    -printf '%T@ %p\n' |
    sort -nr |
    cut -d' ' -f2-
)

previous_backend=""
for candidate in "${backend_releases[@]}"; do
  if [[ "${candidate}" != "${current_backend}" ]]; then
    previous_backend="${candidate}"
    break
  fi
done

previous_frontend=""
for candidate in "${frontend_releases[@]}"; do
  if [[ "${candidate}" != "${current_frontend}" ]]; then
    previous_frontend="${candidate}"
    break
  fi
done

if [[ -z "${previous_backend}" || -z "${previous_frontend}" ]]; then
  echo "A previous backend and frontend release are both required." >&2
  exit 1
fi

restore_original_releases() {
  if [[ -n "${current_backend}" ]]; then
    ln -sfn "${current_backend}" "${BACKEND_ROOT}/app.jar"
  fi
  if [[ -n "${current_frontend}" ]]; then
    ln -sfn "${current_frontend}" "${FRONTEND_ROOT}/current"
  fi
  systemctl restart community-backend.service nginx || true
}

ln -sfn "${previous_backend}" "${BACKEND_ROOT}/app.jar"
ln -sfn "${previous_frontend}" "${FRONTEND_ROOT}/current"

if ! systemctl restart community-backend.service nginx; then
  echo "Rollback restart failed; restoring original release links." >&2
  restore_original_releases
  exit 1
fi

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
  echo "Rollback target did not become ready within 60 seconds; restoring original release links." >&2
  restore_original_releases
  exit 1
fi

echo "Rollback complete."
echo "Backend: ${previous_backend}"
echo "Frontend: ${previous_frontend}"
