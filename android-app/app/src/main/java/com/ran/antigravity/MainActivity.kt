package com.ran.antigravity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var rootLayout: FrameLayout
    private lateinit var webView: WebView
    private lateinit var topProgressBar: ProgressBar
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    companion object {
        private const val PREFS_NAME = "antigravity_cloud_prefs"
        private const val KEY_REMOTE_URL = "remote_url"
        private const val DEFAULT_CLOUD_URL =
            "https://antigravity.google.com/r/a1080bc4-c4b4-48ed-bea5-1c01174c89d5-v2"
        private const val FILE_CHOOSER_RESULT_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.parseColor("#0D1117")
        window.navigationBarColor = Color.parseColor("#0D1117")

        buildUI()
        enableImmersiveFullscreen()

        // Abre direto a máquina na nuvem como um aplicativo nativo
        webView.loadUrl(getSavedCloudUrl())
    }

    private fun getSavedCloudUrl(): String {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_REMOTE_URL, DEFAULT_CLOUD_URL) ?: DEFAULT_CLOUD_URL
    }

    private fun saveCloudUrl(url: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_REMOTE_URL, url.trim()).apply()
    }

    private fun enableImmersiveFullscreen() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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

        webView = WebView(this).apply {
            setBackgroundColor(Color.parseColor("#0D1117"))
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(false)
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                // Remove a assinatura "; wv" do WebView para permitir login Google OAuth nativo sem bloqueio 403
                val defaultUa = userAgentString ?: ""
                userAgentString = defaultUa
                    .replace("; wv", "")
                    .replace(Regex("Version/\\d+\\.\\d+\\s*"), "")
            }

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val targetUrl = request?.url?.toString() ?: return false
                    return if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
                        false // Mantém toda a navegação e login Google dentro do próprio APK
                    } else {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
                        } catch (_: Exception) {
                        }
                        true
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    CookieManager.getInstance().flush()
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress < 100) {
                        topProgressBar.visibility = View.VISIBLE
                        topProgressBar.progress = newProgress
                    } else {
                        topProgressBar.visibility = View.GONE
                    }
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    fileUploadCallback?.onReceiveValue(null)
                    fileUploadCallback = filePathCallback
                    return try {
                        val intent = fileChooserParams?.createIntent()
                            ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "*/*"
                            }
                        @Suppress("DEPRECATION")
                        startActivityForResult(intent, FILE_CHOOSER_RESULT_CODE)
                        true
                    } catch (_: Exception) {
                        fileUploadCallback = null
                        false
                    }
                }
            }
        }

        topProgressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.VISIBLE
        }

        // Botão flutuante discreto no canto superior direito (toque longo ou clique para trocar link/recarregar)
        val quickMenuPill = TextView(this).apply {
            text = "⚙️"
            textSize = 14f
            alpha = 0.35f
            setTextColor(Color.WHITE)
            setPadding(24, 16, 24, 16)
            setBackgroundColor(Color.parseColor("#33161B22"))
            setOnClickListener {
                showQuickMenu()
            }
        }

        rootLayout.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        rootLayout.addView(
            topProgressBar,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                8,
                Gravity.TOP
            )
        )

        rootLayout.addView(
            quickMenuPill,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END
            ).apply {
                setMargins(0, 24, 24, 0)
            }
        )

        setContentView(rootLayout)
    }

    private fun showQuickMenu() {
        val options = arrayOf(
            "🔄 Recarregar Sessão Atual",
            "🚀 Ir para Minha Máquina Cloud (Padrão)",
            "🌐 Painel Geral Antigravity (Trocar Sessão)",
            "✏️ Alterar Link da Sessão Cloud"
        )
        AlertDialog.Builder(this)
            .setTitle("Antigravity Cloud")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> webView.reload()
                    1 -> webView.loadUrl(getSavedCloudUrl())
                    2 -> webView.loadUrl("https://antigravity.google.com")
                    3 -> showEditUrlDialog()
                }
            }
            .show()
    }

    private fun showEditUrlDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setText(getSavedCloudUrl())
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle("Link da Sessão Antigravity")
            .setMessage("Cole o link do /remote-control da sua máquina na nuvem:")
            .setView(input)
            .setPositiveButton("Salvar e Abrir") { _, _ ->
                val newUrl = input.text.toString().trim()
                if (newUrl.startsWith("http")) {
                    saveCloudUrl(newUrl)
                    webView.loadUrl(newUrl)
                    Toast.makeText(this, "Link atualizado!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Restaurar Padrão") { _, _ ->
                saveCloudUrl(DEFAULT_CLOUD_URL)
                webView.loadUrl(DEFAULT_CLOUD_URL)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            val results = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            fileUploadCallback?.onReceiveValue(results)
            fileUploadCallback = null
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveFullscreen()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
