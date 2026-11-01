package com.kiosk.launcher

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kiosk.launcher.databinding.ActivityMainBinding
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RELOAD_KIOSK = "extra_reload_kiosk"
        private const val WEBVIEW_LOG_TAG = "KioskWebView"
        private const val RELOAD_INTERVAL_MS = 30_000L
        private const val ERROR_PLACEHOLDER_HTML =
            "<html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>" +
                "</head><body style=\"margin:0;background:#000000;\"></body></html>"
    }

    private var kioskInErrorState = false
    private var lastMainFrameHttpError = false
    private val reloadHandler = Handler(Looper.getMainLooper())
    private val reloadWatchdogRunnable = object : Runnable {
        override fun run() {
            if (!kioskInErrorState || !::binding.isInitialized) return
            Log.d(WEBVIEW_LOG_TAG, "Watchdog: tentando recarregar ${preferences.kioskUrl}")
            lastMainFrameHttpError = false
            binding.webView.loadUrl(preferences.kioskUrl)
            reloadHandler.postDelayed(this, RELOAD_INTERVAL_MS)
        }
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferences: KioskPreferences
    private lateinit var gestureDetector: GestureDetectorCompat

    private var pinDialog: AlertDialog? = null

    private val okPressCounter = RapidPressCounter { showPinDialog() }
    private val backPressCounter = RapidPressCounter { showPinDialog() }
    private val homeIntentCounter = RapidPressCounter { showPinDialog() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = KioskPreferences(this)
        if (!preferences.isUrlConfigured()) {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        applyImmersiveMode()
        setupBackHandling()
        setupPinGestures()
        setupWebView(binding.webView)

        if (savedInstanceState == null) {
            binding.webView.loadUrl(preferences.kioskUrl)
        } else {
            binding.webView.restoreState(savedInstanceState)
        }

        if (intent.getBooleanExtra(EXTRA_RELOAD_KIOSK, false)) {
            reloadKioskUrl()
            intent.removeExtra(EXTRA_RELOAD_KIOSK)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_RELOAD_KIOSK, false)) {
            reloadKioskUrl()
            intent.removeExtra(EXTRA_RELOAD_KIOSK)
            return
        }
        homeIntentCounter.registerPress()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPinGestures() {
        val edgePx = (80 * resources.displayMetrics.density).toInt()
        val minSwipePx = (120 * resources.displayMetrics.density).toInt()

        gestureDetector = GestureDetectorCompat(
            this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (e1 == null) return false
                    val startedAtLeftEdge = e1.x <= edgePx
                    val swipeRight = e2.x - e1.x >= minSwipePx
                    val mostlyHorizontal = abs(velocityX) > abs(velocityY)
                    if (startedAtLeftEdge && swipeRight && mostlyHorizontal && velocityX > 400) {
                        showPinDialog()
                        return true
                    }
                    return false
                }
            }
        )

        binding.webView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: WebView) {
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url == null || url.startsWith("data:")) return
                if (kioskInErrorState && !lastMainFrameHttpError) {
                    clearErrorWatchdog()
                    Log.d(WEBVIEW_LOG_TAG, "Watchdog: página carregada com sucesso")
                }
                lastMainFrameHttpError = false
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                view?.loadUrl(url)
                return true
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null) {
                    view?.loadUrl(url)
                    return true
                }
                return false
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request == null || error == null || !request.isForMainFrame) return
                Log.e(
                    WEBVIEW_LOG_TAG,
                    "Erro de carregamento: ${request.url} - ${error.description}"
                )
                lastMainFrameHttpError = false
                view?.let { enterKioskErrorState(it) }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request == null || errorResponse == null || !request.isForMainFrame) return
                Log.e(
                    WEBVIEW_LOG_TAG,
                    "Erro HTTP: ${request.url} - Status: ${errorResponse.statusCode}"
                )
                if (errorResponse.statusCode >= 400) {
                    lastMainFrameHttpError = true
                    view?.let { enterKioskErrorState(it) }
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let { msg ->
                    val text = "${msg.message()} -- line ${msg.lineNumber()} @ ${msg.sourceId()}"
                    when (msg.messageLevel()) {
                        ConsoleMessage.MessageLevel.ERROR ->
                            Log.e(WEBVIEW_LOG_TAG, text)
                        ConsoleMessage.MessageLevel.WARNING ->
                            Log.w(WEBVIEW_LOG_TAG, text)
                        else ->
                            Log.d(WEBVIEW_LOG_TAG, text)
                    }
                }
                return true
            }
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
    }

    private fun enterKioskErrorState(webView: WebView) {
        if (kioskInErrorState) return
        kioskInErrorState = true
        webView.loadDataWithBaseURL(
            null,
            ERROR_PLACEHOLDER_HTML,
            "text/html",
            Charsets.UTF_8.name(),
            null
        )
        reloadHandler.removeCallbacks(reloadWatchdogRunnable)
        reloadHandler.postDelayed(reloadWatchdogRunnable, RELOAD_INTERVAL_MS)
        Log.w(WEBVIEW_LOG_TAG, "Watchdog: estado de erro — retentativa a cada 30s")
    }

    private fun clearErrorWatchdog() {
        kioskInErrorState = false
        lastMainFrameHttpError = false
        reloadHandler.removeCallbacks(reloadWatchdogRunnable)
    }

    private fun applyImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            applyImmersiveMode()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && ::binding.isInitialized) {
            applyImmersiveMode()
        }
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    backPressCounter.registerPress()
                }
            }
        )
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                backPressCounter.registerPress()
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                okPressCounter.registerPress()
                return true
            }
        }

        if (isBlockedSystemKey(keyCode)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK ||
            keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
            keyCode == KeyEvent.KEYCODE_ENTER ||
            keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
        ) {
            return true
        }
        if (isBlockedSystemKey(keyCode)) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun isBlockedSystemKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_SEARCH,
            KeyEvent.KEYCODE_ASSIST,
            KeyEvent.KEYCODE_SETTINGS,
            KeyEvent.KEYCODE_TV,
            KeyEvent.KEYCODE_TV_INPUT,
            KeyEvent.KEYCODE_GUIDE,
            KeyEvent.KEYCODE_INFO,
            KeyEvent.KEYCODE_CAPTIONS -> true
            else -> false
        }
    }

    private fun showPinDialog() {
        if (pinDialog?.isShowing == true) return

        if (!preferences.isPinConfigured()) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return
        }

        pinDialog = PinUnlockDialog.show(this, preferences) {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        pinDialog?.setOnDismissListener { pinDialog = null }
        pinDialog?.show()
    }

    private fun reloadKioskUrl() {
        if (!::binding.isInitialized) return
        clearErrorWatchdog()
        binding.webView.loadUrl(preferences.kioskUrl)
        okPressCounter.reset()
        backPressCounter.reset()
        homeIntentCounter.reset()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::binding.isInitialized) {
            binding.webView.saveState(outState)
        }
    }

    override fun onDestroy() {
        clearErrorWatchdog()
        pinDialog?.dismiss()
        if (::binding.isInitialized) {
            binding.webView.apply {
                loadUrl("about:blank")
                stopLoading()
                clearHistory()
                removeAllViews()
                destroy()
            }
        }
        super.onDestroy()
    }
}
