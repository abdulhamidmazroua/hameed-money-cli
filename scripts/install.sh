#!/usr/bin/env bash
set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; CYAN='\033[0;36m'; NC='\033[0m'

info()  { echo -e "${CYAN}==>${NC} $1"; }
ok()    { echo -e "${GREEN}   ✓${NC} $1"; }
err()   { echo -e "${RED}   ✗${NC} $1"; exit 1; }

HMC_BIN="$HOME/.local/bin/hmc"

# ---------------------------------------------------------------
echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║        HameedMoneyCLI — Installer           ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════╝${NC}"
echo ""

# ---------------------------------------------------------------
info "Checking prerequisites..."

command -v native-image >/dev/null 2>&1 || err "GraalVM native-image not found. Install GraalVM 25+ via SDKMAN: sdk install java 25.0.2-graalce"
ok "GraalVM native-image found ($(native-image --version 2>&1 | head -1))"

# ---------------------------------------------------------------
info "Building native binary (this may take a couple of minutes)..."
./mvnw -Pnative native:compile -DskipTests -q

# ---------------------------------------------------------------
info "Installing hmc to ~/.local/bin..."
mkdir -p "$HOME/.local/bin"
cp target/hameed-money-cli "$HMC_BIN"
chmod +x "$HMC_BIN"

if [[ ":$PATH:" != *":$HOME/.local/bin:"* ]]; then
  echo ""
  info "Adding ~/.local/bin to PATH in ~/.zshrc"
  echo 'export PATH="$HOME/.local/bin:$PATH"' >> ~/.zshrc
  export PATH="$HOME/.local/bin:$PATH"
fi
ok "hmc installed"

# ---------------------------------------------------------------
info "Configuration..."
echo ""
echo "  On first launch, hmc creates ~/.hmc/config.json automatically."
echo "  Edit it to add your API keys:"
echo ""
echo "    ~/.hmc/config.json"
echo ""

# ---------------------------------------------------------------
echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║             Setup complete!                 ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════╝${NC}"
echo ""
echo "  Run:  hmc"
echo ""
echo "  Quick start:"
echo "    hmc init --name \"My Wallet\" --asset EGP --balance 5000"
echo "    hmc report nw EGP"
echo ""
