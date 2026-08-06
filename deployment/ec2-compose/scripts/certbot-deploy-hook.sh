#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

require_root
for command_name in docker install openssl sha256sum; do require_command "${command_name}"; done
lineage="${RENEWED_LINEAGE:?Certbot must set RENEWED_LINEAGE}"
require_nonempty_file "${lineage}/fullchain.pem"
require_nonempty_file "${lineage}/privkey.pem"

openssl x509 -in "${lineage}/fullchain.pem" -noout -checkend "${TLS_MIN_VALID_SECONDS:-604800}" || {
  echo "Renewed certificate expires too soon." >&2
  exit 1
}
cert_key="$(openssl x509 -in "${lineage}/fullchain.pem" -pubkey -noout | openssl pkey -pubin -outform DER 2>/dev/null | sha256sum)"
private_key="$(openssl pkey -in "${lineage}/privkey.pem" -pubout -outform DER 2>/dev/null | sha256sum)"
[[ "${cert_key}" == "${private_key}" ]] || { echo "Certificate and private key do not match." >&2; exit 1; }

install -d -o root -g "${COMMUNITY_TLS_GID}" -m 0750 "${COMMUNITY_TLS_DIR}"
stage="$(mktemp -d "${COMMUNITY_TLS_DIR}/.renew.XXXXXX")"
backup="$(mktemp -d "${COMMUNITY_TLS_DIR}/.backup.XXXXXX")"
trap 'rm -rf -- "${stage}" "${backup}"' EXIT
install -o root -g "${COMMUNITY_TLS_GID}" -m 0640 "${lineage}/fullchain.pem" "${stage}/fullchain.pem"
install -o root -g "${COMMUNITY_TLS_GID}" -m 0640 "${lineage}/privkey.pem" "${stage}/privkey.pem"

for file in fullchain.pem privkey.pem; do
  if [[ -f "${COMMUNITY_TLS_DIR}/${file}" ]]; then
    install -o root -g "${COMMUNITY_TLS_GID}" -m 0640 "${COMMUNITY_TLS_DIR}/${file}" "${backup}/${file}"
  fi
  mv -f -- "${stage}/${file}" "${COMMUNITY_TLS_DIR}/${file}"
done

current_env="${COMMUNITY_RELEASE_ROOT}/current.env"
restore_previous_tls() {
  for file in fullchain.pem privkey.pem; do
    if [[ -f "${backup}/${file}" ]]; then
      mv -f -- "${backup}/${file}" "${COMMUNITY_TLS_DIR}/${file}"
    else
      rm -f -- "${COMMUNITY_TLS_DIR}/${file}"
    fi
  done
}

nginx_config_ok() {
  if [[ -s "${current_env}" ]]; then
    compose_root="$(env_value "${current_env}" COMPOSE_ROOT)"
    require_file "${compose_root}/compose.yaml"
    docker compose --env-file "${current_env}" --file "${compose_root}/compose.yaml" exec -T nginx nginx -t
  else
    nginx_image="${TLS_VALIDATION_IMAGE:-nginx:1.28.3-alpine3.23@sha256:0dcc88822d45581e65ae329f8be769762bf628d3b2bb7d2a077d4aa5c98b30e3}"
    require_file "${COMMUNITY_COMPOSE_ROOT}/nginx/nginx.prod.conf"
    docker run --rm --platform linux/amd64 --user 101:101 --group-add "${COMMUNITY_TLS_GID}" --read-only \
      --tmpfs /tmp:rw,noexec,nosuid,nodev,size=32m,uid=101,gid=101 \
      --cap-drop ALL --security-opt no-new-privileges:true \
      --add-host backend:127.0.0.1 --add-host frontend:127.0.0.1 \
      --volume "${COMMUNITY_COMPOSE_ROOT}/nginx/nginx.prod.conf:/etc/nginx/nginx.conf:ro" \
      --volume "${COMMUNITY_TLS_DIR}:/etc/nginx/tls:ro" \
      "${nginx_image}" nginx -t
  fi
}

if ! nginx_config_ok; then
  restore_previous_tls
  echo "New TLS files failed nginx -t; previous files restored without reload." >&2
  exit 1
fi

if [[ -s "${current_env}" ]]; then
  compose_root="$(env_value "${current_env}" COMPOSE_ROOT)"
  docker compose --env-file "${current_env}" --file "${compose_root}/compose.yaml" exec -T nginx nginx -s reload
fi

echo "TLS files replaced atomically; active edge Nginx reloaded when present."
