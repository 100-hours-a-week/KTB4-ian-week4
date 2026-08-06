#!/usr/bin/env bash

set -Eeuo pipefail

bootstrap_root="${BOOTSTRAP_COMPOSE_ROOT:-/opt/community/deployment/ec2-compose}"
release_root="${DATA_ROOT:-/data/community}/releases"
config_root="${CONFIG_ROOT:-/opt/community/configs}"
operation="${1:?Usage: release-runner.sh deploy|rollback}"
shift

case "${operation}" in
  deploy)
    exec "${bootstrap_root}/scripts/registry-deploy.sh" "$@"
    ;;
  rollback)
    current_env="${release_root}/current.env"
    [[ -s "${current_env}" ]] || { echo "No current release state." >&2; exit 1; }
    config_sha="$(awk -F= '$1 == "CONFIG_SHA" {print $2; exit}' "${current_env}")"
    [[ "${config_sha}" =~ ^[0-9a-f]{40}$ ]] || { echo "Invalid current CONFIG_SHA." >&2; exit 1; }
    compose_root="${config_root}/${config_sha}/deployment/ec2-compose"
    exec env COMPOSE_ROOT="${compose_root}" "${compose_root}/scripts/rollback.sh" "$@"
    ;;
  *)
    echo "Unsupported operation: ${operation}" >&2
    exit 1
    ;;
esac
