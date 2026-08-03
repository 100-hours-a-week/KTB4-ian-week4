#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"

require_root
require_command base64

read -r -p "Frontend origin (http://IP or https://domain): " FRONTEND_ORIGIN
if ! is_valid_http_origin "${FRONTEND_ORIGIN}"; then
  echo "Frontend origin must contain only scheme, host, and optional port." >&2
  exit 1
fi

read -r -s -p "MySQL application password: " DB_PASSWORD_VALUE
echo
read -r -s -p "Base64 JWT signing secret (decoded length at least 32 bytes): " JWT_SECRET_VALUE
echo

if [[ "${#DB_PASSWORD_VALUE}" -lt 16 ]]; then
  echo "MySQL password must be at least 16 characters." >&2
  exit 1
fi
if [[ "${DB_PASSWORD_VALUE}" =~ [[:space:]\'\"\\] ]] ||
  [[ "${JWT_SECRET_VALUE}" =~ [[:space:]\'\"\\] ]]; then
  echo "Secrets must not contain whitespace, quotes, or backslashes." >&2
  exit 1
fi
if ! is_valid_jwt_secret "${JWT_SECRET_VALUE}"; then
  echo "JWT signing secret must be valid Base64 decoding to at least 32 bytes." >&2
  exit 1
fi

COOKIE_SECURE_VALUE=true
if [[ "${FRONTEND_ORIGIN}" == http://* ]]; then
  COOKIE_SECURE_VALUE=false
  echo "WARN: Secure cookies are disabled for temporary HTTP validation." >&2
fi

temporary_env="$(mktemp /tmp/community-backend-env.XXXXXX)"
assert_safe_path "${temporary_env}"
cleanup() {
  unset DB_PASSWORD_VALUE JWT_SECRET_VALUE
  if [[ -f "${temporary_env}" ]]; then
    rm -f -- "${temporary_env}"
  fi
}
trap cleanup EXIT

{
  echo "SPRING_PROFILES_ACTIVE=aws"
  echo "DB_URL=jdbc:mysql://127.0.0.1:3306/community?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul"
  echo "DB_USERNAME=community_app"
  printf 'DB_PASSWORD=%s\n' "${DB_PASSWORD_VALUE}"
  printf 'JWT_SECRET=%s\n' "${JWT_SECRET_VALUE}"
  printf 'FRONTEND_ORIGIN=%s\n' "${FRONTEND_ORIGIN}"
  printf 'COOKIE_SECURE=%s\n' "${COOKIE_SECURE_VALUE}"
  echo "DB_POOL_MAX_SIZE=10"
  echo "DB_POOL_MIN_IDLE=2"
} >"${temporary_env}"

install -o root -g "${COMMUNITY_GROUP}" -m 0640 \
  "${temporary_env}" \
  "${ENV_FILE}"

unset DB_PASSWORD_VALUE JWT_SECRET_VALUE
echo "Backend environment file installed without printing secret values."
