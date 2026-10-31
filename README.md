# 🚀 Antigravity Mobile (Linux on Android & Tablet)

Execute um ambiente **Linux completo (Ubuntu ARM64)** no seu celular ou tablet Android para baixar, autenticar e rodar o **Google Antigravity (`agy` CLI e ambiente Desktop)** localmente, sem precisar de root!

---

## ✨ O que este projeto faz?

O Android nativo não possui a biblioteca padrão `glibc` do Linux desktop exigida pelo Google Antigravity (`glibc >= 2.28`). Este projeto automatiza toda a criação de um container **Ubuntu ARM64** sobre o **Termux (`proot-distro`)**, configurando:

1. **Ubuntu ARM64 Completo (`glibc` nativa):** Compatível com os binários oficiais do Antigravity para Linux ARM64, Node.js, Python e ferramentas de compilação.
2. **Google Antigravity (`agy`):** Instalado automaticamente via instalador oficial (`https://antigravity.google/cli/install.sh`) com suporte a keyring (`gnome-keyring` / `libsecret` / `dbus`).
3. **Modo Dual (CLI ou Desktop Gráfico):**
   - **Modo CLI (`agy-mobile cli`):** Abre diretamente a interface de terminal interativa do Antigravity (`agy`), super leve e ideal para celulares.
   - **Modo Desktop (`agy-mobile gui` / `x11`):** Inicia uma interface gráfica **XFCE4** via **TigerVNC** ou **Termux:X11**, perfeita para tablets com teclado/mouse ou modo Samsung DeX.
4. **Atalhos na Tela Inicial (`Termux:Widget`):** Scripts prontos em `~/.shortcuts` para abrir o Antigravity com 1 toque na tela inicial do Android.

---

## 📱 Pré-requisitos no Celular ou Tablet

1. Instale o **Termux** (versão oficial do **F-Droid** ou GitHub Releases — *não use a versão desatualizada da Play Store*):
   - [Baixar Termux no F-Droid](https://f-droid.org/en/packages/com.termux/)
2. *(Opcional, recomendado para Tablets / GUI)* Instale um cliente VNC ou X11:
   - **AVNC** (F-Droid / Play Store) ou **RealVNC Viewer** para conectar em `127.0.0.1:5901`.
   - Ou **Termux:X11** para aceleração gráfica direta.

---

## ⚡ Instalação Rápida (1 Comando no Termux)

Abra o **Termux** no seu celular ou tablet e cole o comando abaixo:

```bash
pkg update -y && pkg install -y git curl && git clone https://github.com/rafanunesran/antigravity-mobile.git && cd antigravity-mobile && bash install.sh
```

---

## 🎮 Como Usar

Após a instalação, você pode usar o comando `agy-mobile` de qualquer lugar no Termux:

| Comando | Descrição |
| :--- | :--- |
| `agy-mobile` | Abre o **Painel Interativo** com todas as opções numeradas |
| `agy-mobile cli` | Inicia diretamente o **Google Antigravity CLI (`agy`)** dentro do Ubuntu |
| `agy-mobile gui` | Inicia o **Desktop Linux (XFCE4 + VNC)** na porta `5901` (`127.0.0.1:5901`) |
| `agy-mobile x11` | Inicia o Desktop Linux via **Termux:X11** |
| `agy-mobile stop` | Encerra o servidor gráfico VNC |
| `agy-mobile shell` | Abre o terminal `bash` puro dentro do Ubuntu ARM64 |
| `agy-mobile update` | Atualiza o Antigravity e todos os pacotes do Ubuntu |

### 💡 Dica para Tablets (Resolução Personalizada)
Para ajustar a resolução da área de trabalho Linux para a tela do seu tablet:
```bash
AGY_RESOLUTION=2560x1600 agy-mobile gui
```

---

## 🗂️ Estrutura do Repositório

```text
antigravity-mobile/
├── install.sh                   # Instalador automatizado para o Termux (Host)
├── bin/
│   └── agy-mobile               # CLI e Menu interativo para iniciar CLI, VNC, X11 ou Shell
├── scripts/
│   └── ubuntu-guest-setup.sh    # Provisionador executado dentro do Ubuntu ARM64
└── README.md                    # Documentação completa
```

---

## 🔧 Solução de Problemas Comuns no Android (Phantom Process Killer)

No Android 12+, o sistema operacional pode limitar processos em segundo plano (erro `[Process completed (signal 9)]`).
Para desativar esse limite no seu aparelho:
1. Ative as **Opções do Desenvolvedor** no Android.
2. Ative a opção **"Desativar restrições de processos filhos"** (*Disable child process restrictions*).
