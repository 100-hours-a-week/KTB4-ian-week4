#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

require_root
require_command docker
require_command install
require_file "${COMMUNITY_COMPOSE_FILE}"
acquire_deploy_lock

release_env="${RELEASE_ENV:-${COMMUNITY_COMPOSE_ROOT}/.env}"
release_manifest="${RELEASE_MANIFEST:-${COMMUNITY_COMPOSE_ROOT}/release-manifest}"
current_env="${COMMUNITY_RELEASE_ROOT}/current.env"
previous_env="${COMMUNITY_RELEASE_ROOT}/previous.env"
current_manifest="${COMMUNITY_RELEASE_ROOT}/current.manifest"
previous_manifest="${COMMUNITY_RELEASE_ROOT}/previous.manifest"

validate_release_env "${release_env}"
validate_release_manifest "${release_manifest}"
validate_secret_files
validate_tls_files
validate_local_images "${release_env}"
compose_cmd "${release_env}" config --quiet

reconverge_current() {
  if [[ -s "${current_env}" ]]; then
    echo "Candidate failed; reconverging the prior current release." >&2
    compose_cmd "${current_env}" up --detach --remove-orphans --wait --wait-timeout 300
    RELEASE_ENV="${current_env}" "${SCRIPT_DIR}/verify.sh"
    echo "Prior current release is healthy again." >&2
  else
    compose_cmd "${release_env}" down --remove-orphans || true
    echo "No current release exists. Restore the pre-cutover host Nginx manually." >&2
  fi
}

echo "Starting the validated candidate digest combination."
if ! compose_cmd "${release_env}" up --detach --remove-orphans --wait --wait-timeout 300; then
  compose_cmd "${release_env}" ps --all || true
  reconverge_current
  exit 1
fi

if ! RELEASE_ENV="${release_env}" "${SCRIPT_DIR}/verify.sh"; then
  reconverge_current
  exit 1
fi

if [[ -s "${current_env}" ]]; then
  atomic_copy "${current_env}" "${previous_env}"
fi
if [[ -s "${current_manifest}" ]]; then
  atomic_copy "${current_manifest}" "${previous_manifest}"
fi
atomic_copy "${release_env}" "${current_env}"
atomic_copy "${release_manifest}" "${current_manifest}"

echo "Deployment verified and release state promoted atomically."
