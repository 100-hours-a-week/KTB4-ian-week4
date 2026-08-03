#!/usr/bin/env bash

set -Eeuo pipefail

readonly COMMUNITY_USER="community"
readonly COMMUNITY_GROUP="community"
readonly COMMUNITY_ROOT="/var/lib/community"
readonly BACKEND_ROOT="/opt/community/backend"
readonly FRONTEND_ROOT="/opt/community/frontend"
readonly OPERATIONS_ROOT="/opt/community/deployment/method-a"
readonly ENV_FILE="/etc/community/backend.env"

require_root() {
  if [[ "${EUID}" -ne 0 ]]; then
    echo "This script must be run as root." >&2
    exit 1
  fi
}

require_command() {
  local command_name="$1"
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "Required command not found: ${command_name}" >&2
    exit 1
  fi
}

require_file() {
  local file_path="$1"
  if [[ ! -f "${file_path}" ]]; then
    echo "Required file not found: ${file_path}" >&2
    exit 1
  fi
}

file_has_exact_metadata() {
  local file_path="$1"
  local expected_owner="$2"
  local expected_group="$3"
  local expected_mode="$4"
  local actual_metadata

  actual_metadata="$(
    stat -c '%U:%G:%a' "${file_path}" 2>/dev/null
  )" || return 1

  [[ "${actual_metadata}" == \
    "${expected_owner}:${expected_group}:${expected_mode}" ]]
}

is_valid_http_origin() {
  local origin="$1"
  local authority
  local host
  local port=""

  if [[ ! "${origin}" =~ ^https?://([^/?#[:space:]]+)$ ]]; then
    return 1
  fi
  authority="${BASH_REMATCH[1]}"

  if [[ "${authority}" =~ ^\[([0-9A-Fa-f:]+)\](:(.+))?$ ]]; then
    host="${BASH_REMATCH[1]}"
    port="${BASH_REMATCH[3]:-}"
    [[ "${host}" == *:* ]] || return 1
  elif [[ "${authority}" =~ ^([A-Za-z0-9.-]+)(:([0-9]+))?$ ]]; then
    host="${BASH_REMATCH[1]}"
    port="${BASH_REMATCH[3]:-}"
    if [[ "${host}" == .* ||
      "${host}" == *. ||
      "${host}" == -* ||
      "${host}" == *- ||
      "${host}" == *..* ]]; then
      return 1
    fi
  else
    return 1
  fi

  if [[ -n "${port}" ]]; then
    [[ "${port}" =~ ^[0-9]{1,5}$ ]] || return 1
    local port_number=$((10#${port}))
    ((port_number >= 1 && port_number <= 65535)) || return 1
  fi
}

is_valid_jwt_secret() {
  local encoded_secret="$1"
  local decoded_length
  local decode_flag

  if base64 --decode </dev/null >/dev/null 2>&1; then
    decode_flag="--decode"
  elif base64 -D </dev/null >/dev/null 2>&1; then
    decode_flag="-D"
  else
    return 1
  fi

  decoded_length="$(
    printf '%s' "${encoded_secret}" |
      base64 "${decode_flag}" 2>/dev/null |
      wc -c |
      tr -d '[:space:]'
  )" || return 1

  [[ "${decoded_length}" =~ ^[0-9]+$ ]] || return 1
  ((decoded_length >= 32))
}

assert_safe_path() {
  local candidate="$1"
  case "${candidate}" in
    /opt/community/* | /var/lib/community/* | /etc/community/* | /tmp/community-*)
      ;;
    *)
      echo "Refusing unsafe path: ${candidate}" >&2
      exit 1
      ;;
  esac
}

timestamp() {
  date -u '+%Y%m%dT%H%M%SZ'
}

is_loopback_listener_address() {
  local listener_address="$1"
  local port="$2"

  case "${listener_address}" in
    "127.0.0.1:${port}" | "[::1]:${port}" | \
      "[::ffff:127.0.0.1]:${port}")
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}
