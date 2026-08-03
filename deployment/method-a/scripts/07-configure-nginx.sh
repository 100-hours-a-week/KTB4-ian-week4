#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
METHOD_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"

readonly NGINX_SOURCE="${METHOD_ROOT}/nginx/community.conf"
readonly NGINX_AVAILABLE="/etc/nginx/sites-available/community.conf"
readonly NGINX_ENABLED="/etc/nginx/sites-enabled/community.conf"

require_root
require_command nginx
require_file "${NGINX_SOURCE}"

install -o root -g root -m 0644 \
  "${NGINX_SOURCE}" \
  "${NGINX_AVAILABLE}"
ln -sfn "${NGINX_AVAILABLE}" "${NGINX_ENABLED}"

if [[ -L /etc/nginx/sites-enabled/default ]]; then
  unlink /etc/nginx/sites-enabled/default
fi

nginx -t
systemctl enable nginx
systemctl reload nginx

echo "Nginx configuration installed and reloaded."
