#!/usr/bin/env bash
# ==============================================================================
# Script executado DENTRO do container Ubuntu ARM64 (proot-distro)
# Instala o aplicativo oficial Google Antigravity (APT repo) + Modo Kiosk (Single App)
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

echo -e "${CYAN}[Ubuntu Guest] Instalando dependências base, gerenciador de janela Kiosk (Openbox) e noVNC...${NC}"
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
    libnss3 \
    libatk-bridge2.0-0 \
    libdrm2 \
    libxkbcommon0 \
    libgbm1 \
    libasound2t64 || apt-get install -y libasound2 \
    openbox \
    xterm \
    tigervnc-standalone-server \
    tigervnc-common \
    novnc \
    python3-websockify \
    x11-xserver-utils \
    jq \
    unzip

echo -e "${CYAN}[Ubuntu Guest] Adicionando repositório APT oficial do Google Antigravity...${NC}"
mkdir -p /etc/apt/keyrings
curl -fsSL https://us-central1-apt.pkg.dev/doc/repo-signing-key.gpg | gpg --dearmor --yes -o /etc/apt/keyrings/antigravity-repo-key.gpg
echo "deb [signed-by=/etc/apt/keyrings/antigravity-repo-key.gpg] https://us-central1-apt.pkg.dev/projects/antigravity-auto-updater-dev/ antigravity-debian main" > /etc/apt/sources.list.d/antigravity.list

apt-get update -y || true
if apt-get install -y antigravity; then
    echo -e "${GREEN}[Ubuntu Guest] Aplicativo Google Antigravity Desktop instalado via APT!${NC}"
else
    echo -e "${YELLOW}[Ubuntu Guest] Pacote 'antigravity' GUI em fallback; garantindo Antigravity CLI (agy)...${NC}"
fi

echo -e "${CYAN}[Ubuntu Guest] Instalando Google Antigravity CLI (agy)...${NC}"
curl -fsSL https://antigravity.google/cli/install.sh | bash || true

# Garante que ~/.local/bin esteja no PATH
if ! grep -q "antigravity" "$HOME/.bashrc" 2>/dev/null; then
    cat >> "$HOME/.bashrc" << 'EOF'

# Antigravity Mobile PATH & Helpers
export PATH="$HOME/.local/bin:$HOME/.antigravity/bin:$PATH"
export DISPLAY="${DISPLAY:-:1}"
EOF
fi

# Configura Openbox sem bordas de janela (Modo Kiosk / Single App em Tela Cheia)
mkdir -p "$HOME/.config/openbox"
cat > "$HOME/.config/openbox/rc.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<openbox_config xmlns="http://openbox.org/3.4/rc" xmlns:xi="http://www.w3.org/2001/XInclude">
  <applications>
    <application class="*">
      <decor>no</decor>
      <maximized>yes</maximized>
      <fullscreen>yes</fullscreen>
    </application>
  </applications>
</openbox_config>
EOF

# Configura xstartup do VNC para rodar APENAS o app do Antigravity (sem área de trabalho do Linux!)
mkdir -p "$HOME/.vnc"
cat > "$HOME/.vnc/xstartup" << 'EOF'
#!/usr/bin/env bash
unset SESSION_MANAGER
unset DBUS_SESSION_BUS_ADDRESS
export DISPLAY=:1
export PATH="$HOME/.local/bin:$HOME/.antigravity/bin:/usr/bin:/bin:$PATH"

xsetroot -solid "#0D1117"
openbox-session &

# Loop Kiosk: Mantém o aplicativo Antigravity sempre aberto em tela cheia
while true; do
    if command -v antigravity >/dev/null 2>&1; then
        antigravity --no-sandbox --disable-gpu --maximize
    elif [ -x "/usr/share/antigravity/antigravity" ]; then
        /usr/share/antigravity/antigravity --no-sandbox --disable-gpu --maximize
    else
        xterm -fullscreen -bg "#0D1117" -fg "#E6EDF3" -fa "Monospace" -fs 13 -e "bash -ic 'agy'"
    fi
    sleep 1
done
EOF
chmod +x "$HOME/.vnc/xstartup"

echo -e "${GREEN}[Ubuntu Guest]Modo Single-App do Antigravity configurado com sucesso!${NC}"
