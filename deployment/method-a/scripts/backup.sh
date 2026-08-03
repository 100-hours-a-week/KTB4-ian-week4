#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"

readonly BACKUP_ROOT="${COMMUNITY_ROOT}/backup"

require_root
require_command mysqldump
require_command sha256sum

backup_id="$(timestamp)"
stage_path="${BACKUP_ROOT}/.stage-${backup_id}"
archive_path="${BACKUP_ROOT}/community-${backup_id}.tar.gz"
assert_safe_path "${stage_path}"
assert_safe_path "${archive_path}"

cleanup() {
  if [[ -d "${stage_path}" ]]; then
    rm -rf -- "${stage_path}"
  fi
}
trap cleanup EXIT

install -d -o root -g root -m 0700 "${stage_path}"

mysqldump \
  --protocol=socket \
  --single-transaction \
  --no-tablespaces \
  community |
  gzip -9 >"${stage_path}/database.sql.gz"

tar -czf "${stage_path}/uploads.tar.gz" \
  -C "${COMMUNITY_ROOT}" \
  uploads

(
  cd "${stage_path}"
  sha256sum database.sql.gz uploads.tar.gz >SHA256SUMS
)

tar -czf "${archive_path}" -C "${stage_path}" \
  database.sql.gz uploads.tar.gz SHA256SUMS
chmod 0600 "${archive_path}"

echo "Backup created: ${archive_path}"
echo "The archive may contain personal data. Do not add it to Git or evidence."
