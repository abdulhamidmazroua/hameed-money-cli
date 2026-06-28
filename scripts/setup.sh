#!/usr/bin/env bash
set -euo pipefail

echo "This script is superseded by ./scripts/install.sh"
echo ""
exec "$(dirname "$0")/install.sh"
