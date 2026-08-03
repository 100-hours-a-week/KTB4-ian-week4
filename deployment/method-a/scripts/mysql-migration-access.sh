#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"

require_root
require_command mysql

case "${1:-}" in
  grant)
    mysql --protocol=socket --batch <<'SQL'
GRANT CREATE, ALTER, DROP, INDEX, REFERENCES
  ON community.* TO 'community_app'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL
    echo "Temporary Flyway migration privileges granted."
    ;;
  revoke)
    mysql --protocol=socket --batch <<'SQL'
REVOKE IF EXISTS CREATE, ALTER, DROP, INDEX, REFERENCES
  ON community.* FROM 'community_app'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL
    echo "Temporary Flyway migration privileges revoked."
    ;;
  *)
    echo "Usage: $0 grant|revoke" >&2
    exit 1
    ;;
esac
