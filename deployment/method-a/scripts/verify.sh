#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"

require_root
require_command curl
require_command ss
require_command stat
require_file "${ENV_FILE}"

failures=0

pass() {
  echo "PASS: $1"
}

fail() {
  echo "FAIL: $1" >&2
  failures=$((failures + 1))
}

for service_name in mysql community-backend nginx; do
  if systemctl is-active --quiet "${service_name}"; then
    pass "${service_name} is active"
  else
    fail "${service_name} is not active"
  fi
done

if nginx -t >/dev/null 2>&1; then
  pass "Nginx configuration syntax"
else
  fail "Nginx configuration syntax"
fi

listener_is_loopback_only() {
  local port="$1"
  local listener_address
  local listener_found=false

  while IFS= read -r listener_address; do
    listener_found=true
    if ! is_loopback_listener_address "${listener_address}" "${port}"; then
      return 1
    fi
  done < <(
    ss -ltnH |
      awk -v port="${port}" '$4 ~ (":" port "$") { print $4 }'
  )

  [[ "${listener_found}" == true ]]
}

if listener_is_loopback_only 8080; then
  pass "Spring Boot listens only on loopback port 8080"
else
  fail "Spring Boot listener is missing or externally bound"
fi

if listener_is_loopback_only 3306; then
  pass "MySQL listens only on loopback port 3306"
else
  fail "MySQL listener is missing or externally bound"
fi

if curl --fail --silent --show-error \
  --output /dev/null \
  http://127.0.0.1/healthz; then
  pass "Nginx health endpoint"
else
  fail "Nginx health endpoint"
fi

h2_status="$(
  curl --silent --output /dev/null --write-out '%{http_code}' \
    http://127.0.0.1:8080/h2-console
)"
if [[ "${h2_status}" == "200" || "${h2_status}" == "302" ]]; then
  fail "H2 Console appears accessible in the AWS profile"
else
  pass "H2 Console is not accessible"
fi

for required_key in \
  SPRING_PROFILES_ACTIVE \
  DB_URL \
  DB_USERNAME \
  DB_PASSWORD \
  JWT_SECRET \
  FRONTEND_ORIGIN; do
  if grep -Eq "^${required_key}=.+" "${ENV_FILE}"; then
    pass "${required_key}=SET"
  else
    fail "${required_key}=MISSING"
  fi
done

if file_has_exact_metadata \
  "${ENV_FILE}" \
  root \
  "${COMMUNITY_GROUP}" \
  640; then
  pass "Environment file ownership and permissions"
else
  fail "Environment file must be root:community with mode 640"
fi

if find "${COMMUNITY_ROOT}/uploads" -perm -0002 -print -quit |
  grep -q .; then
  fail "Uploads contain world-writable paths"
else
  pass "Uploads are not world-writable"
fi

if [[ "${failures}" -ne 0 ]]; then
  echo "Verification failed: ${failures} control(s)." >&2
  exit 1
fi

echo "Local EC2 verification controls passed."
echo "Security Group, EBS encryption, and IMDSv2 still require Console evidence."
