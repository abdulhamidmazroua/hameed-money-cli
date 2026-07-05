#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR="${HOME}/hmc/backups"
mkdir -p "${BACKUP_DIR}"

OUT="${BACKUP_DIR}/hmc-$(date +%Y%m%d_%H%M%S).db"

DB_PATH="${HMC_DB_PATH:-$HOME/.hmc/hmc.db}"

if [ ! -f "$DB_PATH" ]; then
  echo "ERROR: Database not found at $DB_PATH"
  echo "Set HMC_DB_PATH or run from the directory containing hmc.db"
  exit 1
fi

echo "==> Backing up database..."
cp "$DB_PATH" "$OUT"
echo "Backup saved: ${OUT}"
