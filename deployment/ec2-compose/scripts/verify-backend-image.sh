#!/usr/bin/env bash

set -Eeuo pipefail

command -v docker >/dev/null 2>&1 || { echo "Docker is required." >&2; exit 1; }
image="${IMAGE_TAG:?Set IMAGE_TAG to the backend candidate image}"

platform="$(docker image inspect --platform linux/amd64 --format '{{.Os}}/{{.Architecture}}' "${image}")"
[[ "${platform}" == linux/amd64 ]] || { echo "Expected linux/amd64, found ${platform}." >&2; exit 1; }
[[ "$(docker image inspect --format '{{json .Config.ExposedPorts}}' "${image}")" == '{"8080/tcp":{}}' ]] || {
  echo "Backend must expose only 8080." >&2
  exit 1
}

docker run --rm --platform linux/amd64 --read-only --cap-drop ALL \
  --security-opt no-new-privileges:true --entrypoint sh "${image}" -ec '
    test "$(id -u)" = 10001
    command -v java >/dev/null
    ! command -v javac >/dev/null
    test -r /app/community.jar
    test -r /app/healthcheck/HealthCheck.class
  '

for label in org.opencontainers.image.source org.opencontainers.image.revision org.opencontainers.image.version; do
  value="$(docker image inspect --format "{{index .Config.Labels \"${label}\"}}" "${image}")"
  [[ -n "${value}" && "${value}" != '<no value>' ]] || { echo "Missing OCI label: ${label}" >&2; exit 1; }
done

healthcheck="$(docker image inspect --format '{{json .Config.Healthcheck.Test}}' "${image}")"
[[ "${healthcheck}" == *HealthCheck* ]] || { echo "Backend image healthcheck is missing." >&2; exit 1; }

echo "PASS: ${image} is an amd64 UID 10001 JRE-only runtime."
