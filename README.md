# 🚀 Antigravity Mobile (Android APK + Linux Ubuntu ARM64)

Um **Aplicativo Android (`.apk`)** nativo + motor **Linux (Ubuntu ARM64)** que roda no seu celular ou tablet e abre diretamente o **Google Antigravity** em tela cheia!

---

## 📲 Download do APK

O APK é compilado automaticamente pelo **GitHub Actions** a cada atualização do repositório:

1. Acesse a aba **[Releases](https://github.com/rafanunesran/antigravity-mobile/releases)** (ou **[Actions](https://github.com/rafanunesran/antigravity-mobile/actions)**) deste repositório.
2. Baixe o arquivo **`Antigravity-Mobile-Linux.apk`**.
3. Instale o APK no seu celular ou tablet Android.

---

## ⚙️ Como funciona a arquitetura do App?

1. **O Aplicativo Android (`Antigravity-Mobile-Linux.apk`):**
   - Possui uma interface nativa dedicada com 2 modos de visualização em tela cheia:
     - **⚡ Modo Terminal Nativo:** Abre diretamente a interface do Antigravity CLI (`agy`) renderizada dentro do próprio APK (via `ttyd` local na porta `7681`).
     - **🖥️ Modo Desktop (Tablet / GUI):** Abre a área de trabalho completa do **Ubuntu Linux (XFCE4)** com a janela do Antigravity já aberta dentro do próprio APK (via `noVNC/websockify` na porta `6080`), sem precisar de clientes VNC externos!
   - Dispara automaticamente o boot do container Linux em segundo plano via `com.termux.RUN_COMMAND`.

2. **O Motor Linux em Background (Ubuntu ARM64):**
   - Fornece a `glibc >= 2.28` oficial do Ubuntu ARM64 exigida pelo binário do **Google Antigravity**, além de Node.js, Python 3, Git e `gnome-keyring`.

---

## 🚀 Passo a Passo de Uso (Celular e Tablet)

### 1º Uso (Configuração Única do Motor Linux):
1. Tenha o app base **[Termux (F-Droid)](https://f-droid.org/en/packages/com.termux/)** instalado no aparelho (ele fornece a camada de virtualização `proot` sem exigir root).
2. Abra o seu app **Antigravity Linux (APK)** e toque no botão **"📦 Instalar / Atualizar Motor Linux (1º Uso)"** (ou cole no Termux):
   ```bash
   pkg update -y && pkg install -y git curl && git clone https://github.com/rafanunesran/antigravity-mobile.git && cd antigravity-mobile && bash install.sh
   ```

### Uso no Dia a Dia:
- Basta tocar no ícone do app **Antigravity Linux** na tela inicial do seu celular ou tablet e escolher:
  - **⚡ Abrir Antigravity (Modo Terminal Nativo)**
  - **🖥️ Abrir Antigravity Desktop (Modo Tablet / GUI)**

---

## 🗂️ Estrutura do Projeto

```text
antigravity-mobile/
├── .github/workflows/
│   └── build-apk.yml            # GitHub Actions que compila e publica o APK automaticamente
├── android-app/                 # Código-fonte completo do APK Android (Kotlin)
│   └── app/src/main/
│       ├── AndroidManifest.xml
│       └── java/com/ran/antigravity/MainActivity.kt
├── install.sh                   # Instalador do container Ubuntu ARM64 + ponte com o APK
├── bin/
│   └── agy-mobile               # Daemon que expõe o Antigravity (ttyd + noVNC) para o APK
└── scripts/
    └── ubuntu-guest-setup.sh    # Instala Antigravity, XFCE4, TigerVNC e noVNC no Ubuntu
```
