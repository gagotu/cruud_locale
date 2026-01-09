#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if command -v xdg-user-dir >/dev/null 2>&1; then
  DOCS="$(xdg-user-dir DOCUMENTS)"
else
  DOCS="${HOME}/Documents"
fi

if [ -z "${DOCS}" ]; then
  echo "Cannot resolve Documents folder. Set HOST_DOCUMENTS_DIR manually." >&2
  exit 1
fi
if [ ! -d "${DOCS}" ]; then
  echo "Resolved Documents folder does not exist: ${DOCS}" >&2
  exit 1
fi

export HOST_DOCUMENTS_DIR="${DOCS}"
echo "HOST_DOCUMENTS_DIR resolved to: ${DOCS}"

docker compose --env-file .env up -d "$@"
