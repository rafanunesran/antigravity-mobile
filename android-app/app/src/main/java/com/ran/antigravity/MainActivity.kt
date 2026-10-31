package com.ran.antigravity

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
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
    private lateinit var splashOverlay: LinearLayout
    private lateinit var webView: WebView
    private lateinit var statusText: TextView
    private lateinit var setupButtonsContainer: LinearLayout
    private lateinit var progressBar: ProgressBar

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    // No tablet/celular, o APK se conecta diretamente à janela fullscreen do Antigravity:
    // Porta 6080 -> Janela Kiosk do Antigravity Desktop (sem barras do Linux)
    // Porta 7681 -> Antigravity nativo via WebSocket (ttyd)
    private val URL_KIOSK_APP = "http://127.0.0.1:6080/vnc.html?autoconnect=true&resize=remote&reconnect=true&show_dot=false"
    private val URL_CLI_APP = "http://127.0.0.1:7681"
    private val INSTALL_CMD = "pkg update -y && pkg install -y git curl && git clone https://github.com/rafanunesran/antigravity-mobile.git ~/antigravity-mobile 2>/dev/null || (cd ~/antigravity-mobile && git pull); bash ~/antigravity-mobile/install.sh"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.parseColor("#0D1117")
        window.navigationBarColor = Color.parseColor("#0D1117")

        buildUI()
        enableImmersiveFullscreen()

        // Ao tocar no ícone do app no Tablet/Celular, já inicia e abre direto o Antigravity!
        autoBootAntigravity()
    }

    private fun enableImmersiveFullscreen() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildUI() {
        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0D1117"))
        }

        // WebView 100% Tela Cheia que exibe exclusivamente a janela do Antigravity
        webView = WebView(this).apply {
            visibility = View.GONE
            setBackgroundColor(Color.parseColor("#0D1117"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Oculta a barra lateral do noVNC para parecer um app nativo puro do Antigravity
                    view?.evaluateJavascript(
                        """
                        (function() {
                            var style = document.createElement('style');
                            style.innerHTML = '#noVNC_control_bar, #noVNC_control_bar_anchor, #noVNC_status { display: none !important; } body { background: #0D1117 !important; margin: 0; overflow: hidden; }';
                            document.head.appendChild(style);
                        })();
                        """.trimIndent(),
                        null
                    )
                }
            }
            webChromeClient = WebChromeClient()
        }

        // Splash Screen minimalista enquanto o motor Linux sobe o Antigravity em background
        splashOverlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
            setBackgroundColor(Color.parseColor("#0D1117"))
        }

        val title = TextView(this).apply {
            text = "Antigravity"
            textSize = 32f
            setTextColor(Color.parseColor("#E6EDF3"))
            gravity = Gravity.CENTER
        }

        statusText = TextView(this).apply {
            text = "Iniciando Antigravity..."
            textSize = 15f
            setTextColor(Color.parseColor("#8B949E"))
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 32)
        }

        progressBar = ProgressBar(this).apply {
            visibility = View.VISIBLE
        }

        setupButtonsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
        }

        val btnRetry = createButton("🔄 Tentar Conectar Novamente", "#238636") {
            autoBootAntigravity()
        }

        val btnSwitchCli = createButton("⚡ Abrir em Modo Terminal (CLI)", "#1F6FEB") {
            openAntigravitySurface(URL_CLI_APP)
        }

        val btnFirstSetup = createButton("📦 Configurar Motor Linux (Apenas 1ª Vez)", "#30363D") {
            runFirstTimeSetup()
        }

        setupButtonsContainer.addView(btnRetry)
        setupButtonsContainer.addView(btnSwitchCli)
        setupButtonsContainer.addView(btnFirstSetup)

        splashOverlay.addView(title)
        splashOverlay.addView(statusText)
        splashOverlay.addView(progressBar)
        splashOverlay.addView(setupButtonsContainer)

        rootLayout.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        rootLayout.addView(
            splashOverlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(rootLayout)
    }

    private fun createButton(label: String, hexColor: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 15f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor(hexColor))
            val params = LinearLayout.LayoutParams(720, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 14, 0, 14)
            }
            layoutParams = params
            setPadding(32, 28, 32, 28)
            setOnClickListener { onClick() }
        }
    }

    private fun autoBootAntigravity() {
        progressBar.visibility = View.VISIBLE
        setupButtonsContainer.visibility = View.GONE
        statusText.text = "Iniciando Antigravity..."

        // Acorda o container Linux silenciosamente em segundo plano
        sendTermuxCommand("/data/data/com.termux/files/usr/bin/agy-mobile", arrayOf("app-daemon", "both"))

        executor.execute {
            var targetUrl: String? = null
            for (attempt in 1..20) {
                if (isPortOpen("http://127.0.0.1:6080")) {
                    targetUrl = URL_KIOSK_APP
                    break
                } else if (isPortOpen("http://127.0.0.1:7681")) {
                    targetUrl = URL_CLI_APP
                    break
                }
                Thread.sleep(600)
            }

            mainHandler.post {
                if (targetUrl != null) {
                    openAntigravitySurface(targetUrl)
                } else {
                    progressBar.visibility = View.GONE
                    statusText.text = "Motor Linux ainda não inicializado neste aparelho."
                    setupButtonsContainer.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun openAntigravitySurface(url: String) {
        splashOverlay.visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.loadUrl(url)
    }

    private fun runFirstTimeSetup() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Install Antigravity", INSTALL_CMD))
        Toast.makeText(
            this,
            "Comando copiado! Cole no Termux para concluir a instalação única.",
            Toast.LENGTH_LONG
        ).show()

        sendTermuxCommand(
            "/data/data/com.termux/files/usr/bin/bash",
            arrayOf("-c", INSTALL_CMD),
            background = false
        )

        val launchIntent = packageManager.getLaunchIntentForPackage("com.termux")
        if (launchIntent != null) {
            startActivity(launchIntent)
        }
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
        }
    }

    private fun isPortOpen(urlStr: String): Boolean {
        return try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 500
                readTimeout = 500
                requestMethod = "GET"
            }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..499
        } catch (_: Exception) {
            false
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveFullscreen()
        }
    }

    override fun onBackPressed() {
        if (webView.visibility == View.VISIBLE && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
