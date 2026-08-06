#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

require_root
require_command certbot
domain="${CERTBOT_DOMAIN:-pulse.gleeze.com}"
hook="${SCRIPT_DIR}/certbot-deploy-hook.sh"

case "${CERTBOT_OPERATION:-renew}" in
  obtain)
    email="${CERTBOT_EMAIL:?Set CERTBOT_EMAIL for initial issuance}"
    certbot certonly --non-interactive --agree-tos --email "${email}" \
      --webroot --webroot-path "${COMMUNITY_DATA_ROOT}/acme" \
      --domain "${domain}" --deploy-hook "${hook}"
    ;;
  renew)
    certbot renew --non-interactive --cert-name "${domain}" \
      --webroot --webroot-path "${COMMUNITY_DATA_ROOT}/acme" \
      --deploy-hook "${hook}"
    ;;
  *)
    echo "CERTBOT_OPERATION must be obtain or renew." >&2
    exit 1
    ;;
esac
