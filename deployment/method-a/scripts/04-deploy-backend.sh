#!/usr/bin/env bash

set -Eeuo pipefail
umask 027

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"

readonly BACKEND_ARTIFACT="${BACKEND_ARTIFACT:-/tmp/community-artifacts/community-backend.jar}"

require_root
require_command jar
require_command install
require_file "${BACKEND_ARTIFACT}"
assert_safe_path "${BACKEND_ARTIFACT}"

if [[ "${BACKEND_ARTIFACT}" != *.jar ]]; then
  echo "Backend artifact must have a .jar extension." >&2
  exit 1
fi

jar tf "${BACKEND_ARTIFACT}" >/dev/null

release_path="${BACKEND_ROOT}/releases/community-$(timestamp).jar"
assert_safe_path "${release_path}"

install -o root -g "${COMMUNITY_GROUP}" -m 0640 \
  "${BACKEND_ARTIFACT}" \
  "${release_path}"
ln -sfn "${release_path}" "${BACKEND_ROOT}/app.jar"
chown -h root:"${COMMUNITY_GROUP}" "${BACKEND_ROOT}/app.jar"

echo "Backend release installed: ${release_path}"
