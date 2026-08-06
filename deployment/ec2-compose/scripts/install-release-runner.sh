#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
target_root="${BOOTSTRAP_COMPOSE_ROOT:-/opt/community/deployment/ec2-compose}"

[[ "${EUID}" -eq 0 ]] || { echo "This script must be run as root." >&2; exit 1; }
install -d -o root -g root -m 0755 "${target_root}" /opt/community/bin
cp -a "${SOURCE_ROOT}/." "${target_root}/"
install -o root -g root -m 0755 "${target_root}/scripts/release-runner.sh" /opt/community/bin/community-release

echo "Installed the fixed SSM release runner at /opt/community/bin/community-release."
