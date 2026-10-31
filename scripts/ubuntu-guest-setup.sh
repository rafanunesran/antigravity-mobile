#!/usr/bin/env bash
# ==============================================================================
# Script executado DENTRO do container Ubuntu ARM64 (proot-distro)
# Instala pacotes essenciais, Node.js, Python, XFCE4/TigerVNC e Google Antigravity
# ==============================================================================

set -euo pipefail

export DEBIAN_FRONTEND=noninteractive

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${CYAN}[Ubuntu Guest] Atualizando repositórios APT e pacotes do sistema...${NC}"
apt-get update -y
apt-get upgrade -y

echo -e "${CYAN}[Ubuntu Guest] Instalando ferramentas de desenvolvimento, keyring e dependências...${NC}"
apt-get install -y \
    curl \
    wget \
    git \
    ca-certificates \
    gnupg \
    lsb-release \
    build-essential \
    python3 \
    python3-pip \
    python3-venv \
    nodejs \
    npm \
    dbus-x11 \
    gnome-keyring \
    libsecret-1-0 \
    libsecret-tools \
    jq \
    unzip \
    htop \
    nano

echo -e "${CYAN}[Ubuntu Guest] Instalando ambiente gráfico leve (XFCE4 + TigerVNC) para celular/tablet...${NC}"
apt-get install -y \
    xfce4 \
    xfce4-goodies \
    xfce4-terminal \
    tigervnc-standalone-server \
    tigervnc-common \
    x11-xserver-utils

echo -e "${CYAN}[Ubuntu Guest] Instalando o Google Antigravity CLI (agy)...${NC}"
if curl -fsSL https://antigravity.google/cli/install.sh | bash; then
    echo -e "${GREEN}[Ubuntu Guest] Antigravity CLI instalado pelo script oficial!${NC}"
else
    echo -e "${YELLOW}[Ubuntu Guest] Aviso: Instalador remoto não completou automaticamente. Configurando wrapper de instalação sob demanda...${NC}"
fi

# Garante que ~/.local/bin e caminhos do Antigravity estejam no PATH
if ! grep -q "antigravity" "$HOME/.bashrc" 2>/dev/null; then
    cat >> "$HOME/.bashrc" << 'EOF'

# Antigravity Mobile PATH & Helpers
export PATH="$HOME/.local/bin:$HOME/.antigravity/bin:$PATH"
export DISPLAY="${DISPLAY:-:1}"
EOF
fi

# Configura xstartup do VNC para abrir o XFCE4
mkdir -p "$HOME/.vnc"
cat > "$HOME/.vnc/xstartup" << 'EOF'
#!/usr/bin/env bash
unset SESSION_MANAGER
unset DBUS_SESSION_BUS_ADDRESS
export DISPLAY=:1
exec dbus-launch --exit-with-session startxfce4
EOF
chmod +x "$HOME/.vnc/xstartup"

# Cria um lançador na Área de Trabalho do XFCE para o Antigravity
mkdir -p "$HOME/Desktop"
cat > "$HOME/Desktop/Antigravity.desktop" << 'EOF'
[Desktop Entry]
Version=1.0
Type=Application
Name=Google Antigravity
Comment=Iniciar Antigravity CLI / IDE no Linux Mobile
Exec=xfce4-terminal -T "Google Antigravity (agy)" -e "bash -ic 'agy || (curl -fsSL https://antigravity.google/cli/install.sh | bash && agy); exec bash'"
Icon=utilities-terminal
Terminal=false
Categories=Development;IDE;
StartupNotify=true
EOF
chmod +x "$HOME/Desktop/Antigravity.desktop"

echo -e "${GREEN}[Ubuntu Guest] Provisionamento do Ubuntu ARM64 finalizado!${NC}"
