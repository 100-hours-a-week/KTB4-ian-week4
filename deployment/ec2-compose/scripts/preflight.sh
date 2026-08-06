#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

require_root
for command_name in docker openssl sha256sum stat; do require_command "${command_name}"; done
release_env="${RELEASE_ENV:?Set RELEASE_ENV to the approved preflight environment}"

validate_release_env "${release_env}"
validate_secret_files
validate_tls_files
compose_cmd "${release_env}" config --quiet
pull_and_verify_images "${release_env}"
openssl x509 -in "${COMMUNITY_TLS_DIR}/fullchain.pem" -noout -checkend 604800
cert_key="$(openssl x509 -in "${COMMUNITY_TLS_DIR}/fullchain.pem" -pubkey -noout | openssl pkey -pubin -outform DER 2>/dev/null | sha256sum)"
private_key="$(openssl pkey -in "${COMMUNITY_TLS_DIR}/privkey.pem" -pubout -outform DER 2>/dev/null | sha256sum)"
[[ "${cert_key}" == "${private_key}" ]] || { echo "TLS certificate/key mismatch." >&2; exit 1; }

nginx_image="$(env_value "${release_env}" NGINX_IMAGE)"
docker run --rm --platform linux/amd64 --user 101:101 --read-only \
  --group-add 20001 \
  --tmpfs /tmp:rw,noexec,nosuid,nodev,size=32m,uid=101,gid=101 \
  --cap-drop ALL --security-opt no-new-privileges:true \
  --add-host backend:127.0.0.1 --add-host frontend:127.0.0.1 \
  --volume "${COMMUNITY_COMPOSE_ROOT}/nginx/nginx.prod.conf:/etc/nginx/nginx.conf:ro" \
  --volume "${COMMUNITY_TLS_DIR}:/etc/nginx/tls:ro" \
  "${nginx_image}" nginx -t

echo "Gate 2 preflight passed. No production services were changed."
