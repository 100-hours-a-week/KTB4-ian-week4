#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

require_root
for command_name in curl docker install ss; do require_command "${command_name}"; done

release_env="${RELEASE_ENV:?Set RELEASE_ENV to the candidate environment}"
validate_release_env "${release_env}"
validate_secret_files
validate_local_images "${release_env}"

smoke_root="$(mktemp -d /tmp/community-smoke.XXXXXX)"
assert_safe_path "${smoke_root}"
project_name="community-smoke-$$"
smoke_env="${smoke_root}/smoke.env"

cleanup() {
  if [[ -f "${smoke_env}" ]]; then
    COMPOSE_PROJECT_NAME="${project_name}" \
      COMPOSE_OVERRIDE_FILE="${COMMUNITY_COMPOSE_ROOT}/compose.ci.yaml" \
      compose_cmd "${smoke_env}" down --volumes --remove-orphans >/dev/null 2>&1 || true
  fi
  rm -rf -- "${smoke_root}"
}
trap cleanup EXIT

port=''
for _ in {1..40}; do
  candidate_port="$((20000 + RANDOM % 20000))"
  if ! ss -H -ltn | awk '{print $4}' | grep -Eq "[:.]${candidate_port}$"; then
    port="${candidate_port}"
    break
  fi
done
[[ -n "${port}" ]] || { echo "Could not find a free smoke port." >&2; exit 1; }

install -d -o 999 -g 999 -m 0750 "${smoke_root}/data/mysql"
install -d -o 10001 -g 10001 -m 0750 "${smoke_root}/data/uploads"
install -d -o root -g root -m 0755 "${smoke_root}/acme"
install -d -o root -g "${COMMUNITY_SECRET_GID}" -m 0750 "${smoke_root}/secrets"
for secret_name in mysql-root-password mysql-app-password jwt-secret; do
  install -o root -g "${COMMUNITY_SECRET_GID}" -m 0640 \
    "${COMMUNITY_SECRETS_DIR}/${secret_name}" "${smoke_root}/secrets/${secret_name}"
done

install -o root -g root -m 0600 "${release_env}" "${smoke_env}"
set_env_value "${smoke_env}" DATA_ROOT "${smoke_root}/data"
set_env_value "${smoke_env}" SECRETS_DIR "${smoke_root}/secrets"
set_env_value "${smoke_env}" ACME_ROOT "${smoke_root}/acme"
set_env_value "${smoke_env}" COMPOSE_ROOT "${COMMUNITY_COMPOSE_ROOT}"
set_env_value "${smoke_env}" FRONTEND_ORIGIN "http://127.0.0.1:${port}"
set_env_value "${smoke_env}" COOKIE_SECURE false
set_env_value "${smoke_env}" CI_HTTP_PORT "${port}"
set_env_value "${smoke_env}" IMAGE_PULL_POLICY never

export COMPOSE_PROJECT_NAME="${project_name}"
export COMPOSE_OVERRIDE_FILE="${COMMUNITY_COMPOSE_ROOT}/compose.ci.yaml"
compose_cmd "${smoke_env}" config --quiet
compose_cmd "${smoke_env}" up --detach --remove-orphans --wait --wait-timeout 300

DATA_ROOT="${smoke_root}/data" \
SECRETS_DIR="${smoke_root}/secrets" \
COMPOSE_ROOT="${COMMUNITY_COMPOSE_ROOT}" \
COMPOSE_PROJECT_NAME="${project_name}" \
COMPOSE_OVERRIDE_FILE="${COMMUNITY_COMPOSE_ROOT}/compose.ci.yaml" \
ALLOW_LOCAL_IMAGES="${ALLOW_LOCAL_IMAGES:-0}" \
ALLOW_HTTP_ORIGIN=1 CI_MODE=1 VERIFY_PERSISTENCE=1 \
VERIFY_PUBLIC_URL="http://127.0.0.1:${port}" RELEASE_ENV="${smoke_env}" \
  "${SCRIPT_DIR}/verify.sh"

echo "Candidate digest combination passed isolated full-stack smoke."
