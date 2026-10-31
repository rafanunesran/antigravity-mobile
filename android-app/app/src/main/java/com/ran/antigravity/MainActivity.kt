package com.ran.antigravity

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var rootLayout: FrameLayout
    private lateinit var dashboardView: LinearLayout
    private lateinit var webViewContainer: FrameLayout
    private lateinit var webView: WebView
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    // Portas locais onde o Ubuntu ARM64 expõe o Antigravity para o APK:
    // 7681 -> ttyd (Antigravity CLI direto em tela cheia com suporte a touch/teclado)
    // 6080 -> noVNC / websockify (Desktop Linux XFCE4 + Antigravity em tela cheia)
    private val URL_CLI = "http://127.0.0.1:7681"
    private val URL_DESKTOP = "http://127.0.0.1:6080/vnc.html?autoconnect=true&resize=remote"
    private val INSTALL_CMD = "pkg update -y && pkg install -y git curl && git clone https://github.com/rafanunesran/antigravity-mobile.git ~/antigravity-mobile 2>/dev/null || (cd ~/antigravity-mobile && git pull); bash ~/antigravity-mobile/install.sh"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.parseColor("#0D1117")
        window.navigationBarColor = Color.parseColor("#0D1117")

        buildUI()
        checkLinuxServices()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildUI() {
        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0D1117"))
        }

        // 1. Painel Inicial de Controle do Container Linux
        dashboardView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
        }

        val title = TextView(this).apply {
            text = "🚀 Antigravity Linux"
            textSize = 28f
            setTextColor(Color.parseColor("#58A6FF"))
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Ambiente Ubuntu ARM64 + Google Antigravity para Celular e Tablet"
            textSize = 14f
            setTextColor(Color.parseColor("#8B949E"))
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 40)
        }

        statusText = TextView(this).apply {
            text = "Verificando motor Linux local..."
            textSize = 15f
            setTextColor(Color.parseColor("#3FB950"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
        }

        val btnStartCli = createStyledButton("⚡ Abrir Antigravity (Modo Terminal Nativo)", "#238636") {
            bootLinuxAndConnect(mode = "cli", targetUrl = URL_CLI)
        }

        val btnStartGui = createStyledButton("🖥️ Abrir Antigravity Desktop (Modo Tablet / GUI)", "#1F6FEB") {
            bootLinuxAndConnect(mode = "gui", targetUrl = URL_DESKTOP)
        }

        val btnProvision = createStyledButton("📦 Instalar / Atualizar Motor Linux (1º Uso)", "#30363D") {
            provisionLinuxEngine()
        }

        dashboardView.addView(title)
        dashboardView.addView(subtitle)
        dashboardView.addView(statusText)
        dashboardView.addView(progressBar)
        dashboardView.addView(btnStartCli)
        dashboardView.addView(btnStartGui)
        dashboardView.addView(btnProvision)

        // 2. Container Tela Cheia para exibir o Antigravity rodando no Linux
        webViewContainer = FrameLayout(this).apply {
            visibility = View.GONE
        }

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
        }

        val floatingMenuBtn = Button(this).apply {
            text = "⚙️ Painel"
            textSize = 11f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#AA161B22"))
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                100
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                setMargins(16, 16, 16, 16)
            }
            layoutParams = params
            setOnClickListener {
                webViewContainer.visibility = View.GONE
                dashboardView.visibility = View.VISIBLE
                checkLinuxServices()
            }
        }

        webViewContainer.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        webViewContainer.addView(floatingMenuBtn)

        rootLayout.addView(dashboardView)
        rootLayout.addView(webViewContainer)

        setContentView(rootLayout)
    }

    private fun createStyledButton(label: String, hexColor: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 15f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor(hexColor))
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 16)
            }
            layoutParams = params
            setPadding(32, 32, 32, 32)
            setOnClickListener { onClick() }
        }
    }

    private fun checkLinuxServices() {
        executor.execute {
            val cliOnline = isPortOpen("http://127.0.0.1:7681")
            val guiOnline = isPortOpen("http://127.0.0.1:6080")
            mainHandler.post {
                when {
                    cliOnline && guiOnline -> statusText.text = "✅ Motor Linux Ativo (CLI + Desktop prontos)"
                    cliOnline -> statusText.text = "✅ Motor Linux Ativo (Modo Terminal pronto)"
                    guiOnline -> statusText.text = "✅ Motor Linux Ativo (Modo Desktop pronto)"
                    else -> statusText.text = "⏸️ Motor Linux em espera (Clique abaixo para iniciar)"
                }
            }
        }
    }

    private fun bootLinuxAndConnect(mode: String, targetUrl: String) {
        progressBar.visibility = View.VISIBLE
        statusText.text = "Iniciando container Ubuntu ARM64 e Antigravity ($mode)..."

        // Envia Intent RUN_COMMAND para o motor Termux iniciar o daemon se ainda não estiver ativo
        sendTermuxCommand("/data/data/com.termux/files/usr/bin/agy-mobile", arrayOf("app-daemon", mode))

        executor.execute {
            var connected = false
            for (attempt in 1..25) {
                if (isPortOpen(targetUrl)) {
                    connected = true
                    break
                }
                Thread.sleep(800)
            }

            mainHandler.post {
                progressBar.visibility = View.GONE
                if (connected) {
                    openEmbeddedLinuxView(targetUrl)
                } else {
                    statusText.text = "⚠️ O servidor local ainda não respondeu. Iniciando via motor Termux..."
                    launchTermuxAppFallback()
                }
            }
        }
    }

    private fun openEmbeddedLinuxView(url: String) {
        dashboardView.visibility = View.GONE
        webViewContainer.visibility = View.VISIBLE
        webView.loadUrl(url)
    }

    private fun provisionLinuxEngine() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Install Antigravity Linux", INSTALL_CMD))
        Toast.makeText(
            this,
            "Comando de instalação copiado! Cole no Termux na primeira vez.",
            Toast.LENGTH_LONG
        ).show()

        sendTermuxCommand(
            "/data/data/com.termux/files/usr/bin/bash",
            arrayOf("-c", INSTALL_CMD),
            background = false
        )
        launchTermuxAppFallback()
    }

    private fun sendTermuxCommand(executable: String, args: Array<String>, background: Boolean = true) {
        try {
            val intent = Intent().apply {
                setClassName("com.termux", "com.termux.app.RunCommandService")
                action = "com.termux.RUN_COMMAND"
                putExtra("com.termux.RUN_COMMAND_PATH", executable)
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", args)
                putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", background)
            }
            startService(intent)
        } catch (_: Exception) {
            // Se o usuário ainda não habilitou allow-external-apps, o fallback abre o Termux diretamente
        }
    }

    private fun launchTermuxAppFallback() {
        val launchIntent = packageManager.getLaunchIntentForPackage("com.termux")
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(
                this,
                "Instale o app base Termux (F-Droid) para fornecer o kernel/runtime Linux.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun isPortOpen(urlStr: String): Boolean {
        return try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 600
                readTimeout = 600
                requestMethod = "GET"
            }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..499
        } catch (_: Exception) {
            false
        }
    }

    override fun onBackPressed() {
        if (webViewContainer.visibility == View.VISIBLE) {
            webViewContainer.visibility = View.GONE
            dashboardView.visibility = View.VISIBLE
            checkLinuxServices()
        } else {
            super.onBackPressed()
        }
    }
}
