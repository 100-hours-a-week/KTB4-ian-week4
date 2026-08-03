#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"

readonly RESTORE_ARCHIVE="${RESTORE_ARCHIVE:-}"
readonly RESTORE_CONFIRM="${RESTORE_CONFIRM:-}"

require_root
require_command mysql
require_command sha256sum

if [[ "${RESTORE_CONFIRM}" != "restore-community" ]]; then
  echo "Set RESTORE_CONFIRM=restore-community after reviewing the archive." >&2
  exit 1
fi
if [[ -z "${RESTORE_ARCHIVE}" ]]; then
  echo "Set RESTORE_ARCHIVE to an archive under ${COMMUNITY_ROOT}/backup." >&2
  exit 1
fi

archive_real_path="$(realpath -e -- "${RESTORE_ARCHIVE}")"
case "${archive_real_path}" in
  "${COMMUNITY_ROOT}"/backup/community-*.tar.gz)
    ;;
  *)
    echo "Restore archive is outside the approved backup directory." >&2
    exit 1
    ;;
esac

if tar -tzf "${archive_real_path}" |
  awk 'BEGIN{bad=0} /^\// || /(^|\/)\.\.(\/|$)/ {bad=1} END{exit bad ? 0 : 1}'; then
  echo "Restore archive contains an unsafe path." >&2
  exit 1
fi

restore_stage="$(mktemp -d /tmp/community-restore.XXXXXX)"
assert_safe_path "${restore_stage}"
trap 'rm -rf -- "${restore_stage}"' EXIT

tar -xzf "${archive_real_path}" -C "${restore_stage}"
require_file "${restore_stage}/database.sql.gz"
require_file "${restore_stage}/uploads.tar.gz"
require_file "${restore_stage}/SHA256SUMS"

(
  cd "${restore_stage}"
  sha256sum --check SHA256SUMS
)

systemctl stop community-backend.service

pre_restore_uploads="${COMMUNITY_ROOT}/uploads.pre-restore-$(timestamp)"
assert_safe_path "${pre_restore_uploads}"
mv "${COMMUNITY_ROOT}/uploads" "${pre_restore_uploads}"
install -d -o "${COMMUNITY_USER}" -g "${COMMUNITY_GROUP}" -m 0750 \
  "${COMMUNITY_ROOT}/uploads"

if ! tar -xzf "${restore_stage}/uploads.tar.gz" \
  --no-same-owner \
  -C "${COMMUNITY_ROOT}"; then
  rm -rf -- "${COMMUNITY_ROOT}/uploads"
  mv "${pre_restore_uploads}" "${COMMUNITY_ROOT}/uploads"
  exit 1
fi
chown -R "${COMMUNITY_USER}:${COMMUNITY_GROUP}" \
  "${COMMUNITY_ROOT}/uploads"
find "${COMMUNITY_ROOT}/uploads" -type d -exec chmod 0750 {} +
find "${COMMUNITY_ROOT}/uploads" -type f -exec chmod 0640 {} +

if ! gzip -dc "${restore_stage}/database.sql.gz" |
  mysql --protocol=socket community; then
  echo "Database restore failed. Uploads remain restored; inspect before retrying." >&2
  exit 1
fi

systemctl start community-backend.service
echo "Restore completed. Previous uploads remain at ${pre_restore_uploads}."
