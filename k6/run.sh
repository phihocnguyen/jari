#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K6_IMAGE="${K6_IMAGE:-grafana/k6:0.57.0}"
ENV_FILE="${SCRIPT_DIR}/.env"
TARGET="${1:-benchmark.js}"
shift || true

ENV_ARGS=()
if [[ -f "$ENV_FILE" ]]; then
  ENV_ARGS=(--env-file "$ENV_FILE")
fi

exec docker run --rm -i \
  "${ENV_ARGS[@]}" \
  --network host \
  -v "${SCRIPT_DIR}:/scripts:ro" \
  -w /scripts \
  "$K6_IMAGE" \
  run "/scripts/${TARGET}" "$@"
