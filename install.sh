#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# Antigravity Mobile (Android / Tablet Linux Installer)
# Prepara o Termux + Ubuntu ARM64 (proot-distro) + XFCE4/VNC + Antigravity (agy)
# ==============================================================================

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m'

REPO_RAW_BASE="${REPO_RAW_BASE:-https://raw.githubusercontent.com/rafanunesran/antigravity-mobile/main}"
INSTALL_DIR="${HOME}/.antigravity-mobile"

log_info()  { echo -e "${CYAN}[INFO]${NC} $1"; }
log_ok()    { echo -e "${GREEN}[OK]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[AVISO]${NC} $1"; }
log_error() { echo -e "${RED}[ERRO]${NC} $1"; }

banner() {
    clear
    echo -e "${CYAN}"
    echo "  ╔═══════════════════════════════════════════════════════════╗"
    echo "  ║             🚀 ANTIGRAVITY MOBILE (LINUX)                 ║"
    echo "  ║      Ubuntu ARM64 + Antigravity CLI & Desktop no Android  ║"
    echo "  ╚═══════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

check_environment() {
    if [ ! -d "/data/data/com.termux" ] && [ -z "${TERMUX_VERSION:-}" ]; then
        log_warn "Ambiente Termux não detectado diretamente. Executando em modo compatível Linux..."
    fi
}

setup_termux_packages() {
    if command -v pkg >/dev/null 2>&1; then
        log_info "Atualizando pacotes do Termux e instalando dependências base..."
        pkg update -y && pkg upgrade -y
        pkg install -y proot-distro pulseaudio curl wget git x11-repo tur-repo
        pkg install -y termux-x11-nightly || log_warn "termux-x11-nightly opcional não instalado; VNC estará disponível."
        termux-setup-storage || true
        log_ok "Pacotes base do Termux instalados!"
    else
        log_warn "Gerenciador 'pkg' do Termux não encontrado. Pulando etapa do host Termux."
    fi
}

setup_ubuntu_distro() {
    if command -v proot-distro >/dev/null 2>&1; then
        if proot-distro list | grep -q "ubuntu.*installed"; then
            log_ok "Ubuntu (proot-distro) já está instalado."
        else
            log_info "Baixando e instalando Ubuntu ARM64 via proot-distro..."
            proot-distro install ubuntu
            log_ok "Ubuntu ARM64 instalado com sucesso!"
        fi
    else
        log_error "proot-distro não encontrado! Certifique-se de rodar este script dentro do Termux."
        exit 1
    fi
}

provision_guest() {
    log_info "Provisionando o interior do Ubuntu (Dependências, Antigravity CLI, XFCE4 e VNC)..."

    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    if [ -f "${SCRIPT_DIR}/scripts/ubuntu-guest-setup.sh" ]; then
        cp "${SCRIPT_DIR}/scripts/ubuntu-guest-setup.sh" "${INSTALL_DIR}/ubuntu-guest-setup.sh"
    else
        log_info "Baixando script de provisionamento do repositório..."
        curl -fsSL "${REPO_RAW_BASE}/scripts/ubuntu-guest-setup.sh" -o "${INSTALL_DIR}/ubuntu-guest-setup.sh"
    fi

    chmod +x "${INSTALL_DIR}/ubuntu-guest-setup.sh"

    proot-distro login ubuntu --shared-tmp -- bash -c "bash ${INSTALL_DIR}/ubuntu-guest-setup.sh"
    log_ok "Ambiente Ubuntu configurado com Antigravity!"
}

install_launcher() {
    log_info "Instalando comando 'agy-mobile' e atalhos (Termux:Widget)..."
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    BIN_DIR="${PREFIX:-/usr/local}/bin"
    mkdir -p "${BIN_DIR}"

    if [ -f "${SCRIPT_DIR}/bin/agy-mobile" ]; then
        cp "${SCRIPT_DIR}/bin/agy-mobile" "${BIN_DIR}/agy-mobile"
    else
        curl -fsSL "${REPO_RAW_BASE}/bin/agy-mobile" -o "${BIN_DIR}/agy-mobile"
    fi
    chmod +x "${BIN_DIR}/agy-mobile"

    # Atalhos para Termux:Widget na tela inicial do celular/tablet
    mkdir -p "${HOME}/.shortcuts"
    cat > "${HOME}/.shortcuts/Antigravity-CLI.sh" << 'EOF'
#!/data/data/com.termux/files/usr/bin/bash
agy-mobile cli
EOF
    cat > "${HOME}/.shortcuts/Antigravity-Desktop.sh" << 'EOF'
#!/data/data/com.termux/files/usr/bin/bash
agy-mobile gui
EOF
    chmod +x "${HOME}/.shortcuts/Antigravity-CLI.sh" "${HOME}/.shortcuts/Antigravity-Desktop.sh"

    log_ok "Atalhos criados! Digite 'agy-mobile' a qualquer momento."
}

main() {
    banner
    mkdir -p "${INSTALL_DIR}"
    check_environment
    setup_termux_packages
    setup_ubuntu_distro
    provision_guest
    install_launcher

    echo ""
    echo -e "${GREEN}═════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN} 🎉 Instalação concluída com sucesso!${NC}"
    echo -e "${GREEN}═════════════════════════════════════════════════════════════${NC}"
    echo -e "Comandos disponíveis no Termux:"
    echo -e "  ${CYAN}agy-mobile${NC}         -> Abre o painel interativo (Menu)"
    echo -e "  ${CYAN}agy-mobile cli${NC}     -> Inicia direto o Antigravity CLI (agy) no Ubuntu"
    echo -e "  ${CYAN}agy-mobile gui${NC}     -> Inicia o Desktop Linux (VNC/X11) para Tablet/Celular"
    echo -e "  ${CYAN}agy-mobile shell${NC}   -> Abre o terminal bash do Ubuntu ARM64"
    echo ""
}

main "$@"
