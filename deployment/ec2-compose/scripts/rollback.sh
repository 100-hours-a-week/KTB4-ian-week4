#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

require_root
require_command docker
require_command install
acquire_deploy_lock

[[ "${ROLLBACK_CONFIRM:-}" == "rollback-community" ]] || {
  echo "Set ROLLBACK_CONFIRM=rollback-community after reviewing previous.manifest." >&2
  exit 1
}

current_env="${COMMUNITY_RELEASE_ROOT}/current.env"
previous_env="${COMMUNITY_RELEASE_ROOT}/previous.env"
current_manifest="${COMMUNITY_RELEASE_ROOT}/current.manifest"
previous_manifest="${COMMUNITY_RELEASE_ROOT}/previous.manifest"

require_nonempty_file "${current_env}"
require_nonempty_file "${previous_env}"
validate_release_env "${current_env}"
validate_release_env "${previous_env}"
validate_secret_files
validate_tls_files
pull_and_verify_images "${previous_env}"
compose_cmd "${previous_env}" config --quiet

echo "WARNING: rollback does not reverse destructive Flyway migrations."
if ! compose_cmd "${previous_env}" up --detach --remove-orphans --wait --wait-timeout 300 ||
  ! RELEASE_ENV="${previous_env}" "${SCRIPT_DIR}/verify.sh"; then
  echo "Rollback candidate failed; reconverging current release." >&2
  compose_cmd "${current_env}" up --detach --remove-orphans --wait --wait-timeout 300
  RELEASE_ENV="${current_env}" "${SCRIPT_DIR}/verify.sh"
  exit 1
fi

swap_env="$(mktemp "${COMMUNITY_RELEASE_ROOT}/.rollback-env.XXXXXX")"
atomic_copy "${current_env}" "${swap_env}"
atomic_copy "${previous_env}" "${current_env}"
atomic_copy "${swap_env}" "${previous_env}"
rm -f -- "${swap_env}"

if [[ -s "${current_manifest}" && -s "${previous_manifest}" ]]; then
  swap_manifest="$(mktemp "${COMMUNITY_RELEASE_ROOT}/.rollback-manifest.XXXXXX")"
  atomic_copy "${current_manifest}" "${swap_manifest}"
  atomic_copy "${previous_manifest}" "${current_manifest}"
  atomic_copy "${swap_manifest}" "${previous_manifest}"
  rm -f -- "${swap_manifest}"
fi

echo "Rollback verified and state swapped; database and uploads were retained."
