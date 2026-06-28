#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR="${HOME}/hmc/backups"
mkdir -p "${BACKUP_DIR}"

OUT="${BACKUP_DIR}/hmc-$(date +%Y%m%d_%H%M%S).sql"

if ! command -v docker &>/dev/null; then
  echo "ERROR: docker not found. Install from https://docker.com"
  exit 1
fi

echo "==> Backing up database via Docker Compose..."
docker compose exec -T my-postgres pg_dump -U hmc-user hmc-db > "${OUT}"

echo "Backup saved: ${OUT}"
