#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: $0 <backup-file>"
  echo "Example: $0 ~/hmc/backups/hmc-20260101_120000.sql"
  exit 1
fi

FILE="$1"
if [ ! -f "${FILE}" ]; then
  echo "ERROR: File not found: ${FILE}"
  exit 1
fi

if ! command -v docker &>/dev/null; then
  echo "ERROR: docker not found. Install from https://docker.com"
  exit 1
fi

echo "==> Dropping existing database..."
docker compose exec -T db dropdb -U hmc-user --if-exists hmc-db

echo "==> Creating fresh database..."
docker compose exec -T db createdb -U hmc-user hmc-db

echo "==> Restoring from ${FILE}..."
docker compose exec -T db psql -U hmc-user -d hmc-db < "${FILE}"

echo "Restore complete."
