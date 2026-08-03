#!/usr/bin/env bash

set -Eeuo pipefail
umask 027

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"

readonly FRONTEND_ARTIFACT="${FRONTEND_ARTIFACT:-/tmp/community-artifacts/community-frontend.tar.gz}"

require_root
require_command tar
require_file "${FRONTEND_ARTIFACT}"
assert_safe_path "${FRONTEND_ARTIFACT}"

if tar -tzf "${FRONTEND_ARTIFACT}" |
  awk 'BEGIN{bad=0} /^\// || /(^|\/)\.\.(\/|$)/ {bad=1} END{exit bad ? 0 : 1}'; then
  echo "Frontend artifact contains an unsafe path." >&2
  exit 1
fi

release_path="${FRONTEND_ROOT}/releases/$(timestamp)"
assert_safe_path "${release_path}"
install -d -o root -g root -m 0755 "${release_path}"
tar -xzf "${FRONTEND_ARTIFACT}" \
  --no-same-owner \
  --no-same-permissions \
  -C "${release_path}"

require_file "${release_path}/index.html"
require_file "${release_path}/dist/app.js"
require_file "${release_path}/dist/app.css"

find "${release_path}" -type d -exec chmod 0755 {} +
find "${release_path}" -type f -exec chmod 0644 {} +
chown -R root:root "${release_path}"

ln -sfn "${release_path}" "${FRONTEND_ROOT}/current"
chown -h root:root "${FRONTEND_ROOT}/current"

echo "Frontend release installed: ${release_path}"
