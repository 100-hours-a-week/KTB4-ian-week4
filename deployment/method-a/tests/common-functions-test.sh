#!/usr/bin/env bash

set -Eeuo pipefail

TEST_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
METHOD_ROOT="$(cd -- "${TEST_DIR}/.." && pwd)"
source "${METHOD_ROOT}/scripts/lib/common.sh"

valid_origins=(
  "http://127.0.0.1"
  "http://127.0.0.1:8080"
  "https://community.example.com"
  "https://community.example.com:8443"
  "http://[::1]:8080"
)

invalid_origins=(
  "http://127.0.0.1/"
  "https://community.example.com/path"
  "https://community.example.com?query=1"
  "https://community.example.com#fragment"
  "https://community..example.com"
  "https://community.example.com:0"
  "https://community.example.com:65536"
  "ftp://community.example.com"
)

for origin in "${valid_origins[@]}"; do
  if ! is_valid_http_origin "${origin}"; then
    echo "Expected valid origin: ${origin}" >&2
    exit 1
  fi
done

for origin in "${invalid_origins[@]}"; do
  if is_valid_http_origin "${origin}"; then
    echo "Expected invalid origin: ${origin}" >&2
    exit 1
  fi
done

valid_jwt_secret="MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
invalid_jwt_secrets=(
  "not@base64"
  "MTIz"
)

if ! is_valid_jwt_secret "${valid_jwt_secret}"; then
  echo "Expected a valid Base64 JWT secret." >&2
  exit 1
fi

for jwt_secret in "${invalid_jwt_secrets[@]}"; do
  if is_valid_jwt_secret "${jwt_secret}"; then
    echo "Expected an invalid Base64 JWT secret." >&2
    exit 1
  fi
done

valid_loopback_listeners=(
  "127.0.0.1:8080"
  "[::1]:8080"
  "[::ffff:127.0.0.1]:8080"
)

invalid_loopback_listeners=(
  "0.0.0.0:8080"
  "[::]:8080"
  "172.31.1.10:8080"
)

for listener_address in "${valid_loopback_listeners[@]}"; do
  if ! is_loopback_listener_address "${listener_address}" "8080"; then
    echo "Expected a valid loopback listener: ${listener_address}" >&2
    exit 1
  fi
done

for listener_address in "${invalid_loopback_listeners[@]}"; do
  if is_loopback_listener_address "${listener_address}" "8080"; then
    echo "Expected an invalid loopback listener: ${listener_address}" >&2
    exit 1
  fi
done

echo "Common deployment function tests passed."
