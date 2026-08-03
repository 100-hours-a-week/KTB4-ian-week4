#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
METHOD_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"

readonly MYSQL_CONFIG_SOURCE="${METHOD_ROOT}/mysql/community.cnf.example"
readonly MYSQL_CONFIG_TARGET="/etc/mysql/mysql.conf.d/community.cnf"

require_root
require_command mysql
require_command install
require_file "${MYSQL_CONFIG_SOURCE}"

read -r -s -p "MySQL application password: " MYSQL_APP_PASSWORD
echo
if [[ -z "${MYSQL_APP_PASSWORD}" ]]; then
  echo "Password must not be empty." >&2
  exit 1
fi
MYSQL_APP_PASSWORD_HEX="$(
  printf '%s' "${MYSQL_APP_PASSWORD}" | od -An -tx1 | tr -d '[:space:]'
)"
trap 'unset MYSQL_APP_PASSWORD MYSQL_APP_PASSWORD_HEX' EXIT
unset MYSQL_APP_PASSWORD

install -o root -g root -m 0644 \
  "${MYSQL_CONFIG_SOURCE}" \
  "${MYSQL_CONFIG_TARGET}"

systemctl restart mysql
systemctl enable mysql

mysql --protocol=socket --batch --skip-column-names <<SQL
CREATE DATABASE IF NOT EXISTS community
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
SET @app_password = CONVERT(0x${MYSQL_APP_PASSWORD_HEX} USING utf8mb4);
SET @create_user = CONCAT(
  'CREATE USER IF NOT EXISTS ''community_app''@''127.0.0.1'' IDENTIFIED BY ',
  QUOTE(@app_password)
);
PREPARE create_user_statement FROM @create_user;
EXECUTE create_user_statement;
DEALLOCATE PREPARE create_user_statement;
SET @alter_user = CONCAT(
  'ALTER USER ''community_app''@''127.0.0.1'' IDENTIFIED BY ',
  QUOTE(@app_password)
);
PREPARE alter_user_statement FROM @alter_user;
EXECUTE alter_user_statement;
DEALLOCATE PREPARE alter_user_statement;
GRANT SELECT, INSERT, UPDATE, DELETE
  ON community.* TO 'community_app'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL

unset MYSQL_APP_PASSWORD_HEX

mysql --protocol=socket --batch --skip-column-names \
  -e "SELECT @@bind_address, @@port;"

echo "MySQL database and least-privilege application user are ready."
echo "Store the same application password only in ${ENV_FILE} via sudoedit."
