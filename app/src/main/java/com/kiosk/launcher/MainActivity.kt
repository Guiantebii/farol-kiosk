package com.kiosk.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
        private const val NETWORK_RELOAD_DEBOUNCE_MS = 2_000L
        private const val MIN_SPLASH_VISIBLE_MS = 400L
        private const val REVEAL_FADE_MS = 220L
    }

    private var kioskInErrorState = false
    private var lastMainFrameHttpError = false
    private var kioskReloadInProgress = false
    private var pendingKioskReveal = false
    private var kioskLoadStartedAt = 0L
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val connectivityManager: ConnectivityManager by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val reloadHandler = Handler(Looper.getMainLooper())
    private val networkReloadRunnable = Runnable {
        if (kioskInErrorState) {
            attemptKioskReload("network")
        }
    }
    private val reloadWatchdogRunnable = Runnable {
        if (!kioskInErrorState || !::binding.isInitialized) return@Runnable
        if (!hasValidatedNetwork()) {
            scheduleWatchdogRetry()
            return@Runnable
        }
        attemptKioskReload("watchdog")
        scheduleWatchdogRetry()
    }
    private val revealSplashRunnable = Runnable { performRevealWithFade() }

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
        registerNetworkRecoveryCallback()

        if (savedInstanceState == null) {
            startInitialKioskLoad()
        } else {
            binding.webView.restoreState(savedInstanceState)
            pendingKioskReveal = false
            binding.kioskLoadingSplash.visibility = View.GONE
            binding.webView.visibility = View.VISIBLE
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

        val forwardTouch = View.OnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
        binding.root.setOnTouchListener(forwardTouch)
        binding.kioskLoadingSplash.setOnTouchListener(forwardTouch)
        binding.webView.setOnTouchListener(forwardTouch)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: WebView) {
        webView.setBackgroundColor(Color.BLACK)
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                if (url == null || url.startsWith("data:") || url == "about:blank") return
                if (pendingKioskReveal && !lastMainFrameHttpError) {
                    scheduleRevealKioskContent()
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url == null || url.startsWith("data:") || url == "about:blank") return

                kioskReloadInProgress = false
                if (lastMainFrameHttpError || (kioskInErrorState && !pendingKioskReveal)) return
                if (pendingKioskReveal) {
                    scheduleRevealKioskContent()
                }
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
                lastMainFrameHttpError = true
                Log.e(
                    WEBVIEW_LOG_TAG,
                    "Erro de carregamento: ${request.url} - ${error.description}"
                )
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
            cacheMode = WebSettings.LOAD_DEFAULT
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                offscreenPreRaster = true
            }
        }
    }

    private fun startInitialKioskLoad() {
        if (!hasValidatedNetwork()) {
            enterKioskErrorState(binding.webView)
            return
        }
        loadKioskUrlWithSplash(preferences.kioskUrl)
    }

    private fun showLoadingSplash(waitingForNetwork: Boolean) {
        cancelPendingReveal()
        binding.kioskLoadingMessage.setText(
            if (waitingForNetwork) R.string.kiosk_loading_waiting_network
            else R.string.kiosk_loading_message
        )
        binding.kioskLoadingSplash.alpha = 1f
        binding.kioskLoadingSplash.visibility = View.VISIBLE
        binding.webView.visibility = View.INVISIBLE
        binding.webView.alpha = 1f
    }

    private fun loadKioskUrlWithSplash(url: String) {
        pendingKioskReveal = true
        lastMainFrameHttpError = false
        kioskReloadInProgress = true
        kioskLoadStartedAt = SystemClock.elapsedRealtime()
        showLoadingSplash(waitingForNetwork = false)
        Log.d(WEBVIEW_LOG_TAG, "Carregando: $url")
        binding.webView.loadUrl(url)
    }

    private fun scheduleRevealKioskContent() {
        if (!pendingKioskReveal || lastMainFrameHttpError) return
        reloadHandler.removeCallbacks(revealSplashRunnable)
        val elapsed = SystemClock.elapsedRealtime() - kioskLoadStartedAt
        val delay = (MIN_SPLASH_VISIBLE_MS - elapsed).coerceAtLeast(0L)
        reloadHandler.postDelayed(revealSplashRunnable, delay)
    }

    private fun cancelPendingReveal() {
        reloadHandler.removeCallbacks(revealSplashRunnable)
    }

    private fun performRevealWithFade() {
        if (!::binding.isInitialized || !pendingKioskReveal || lastMainFrameHttpError) return

        pendingKioskReveal = false
        kioskReloadInProgress = false
        if (kioskInErrorState) {
            clearErrorWatchdog()
            Log.d(WEBVIEW_LOG_TAG, "Painel carregado após recuperação")
        }

        binding.webView.visibility = View.VISIBLE
        binding.webView.alpha = 0f
        binding.webView.animate()
            .alpha(1f)
            .setDuration(REVEAL_FADE_MS)
            .start()

        binding.kioskLoadingSplash.animate()
            .alpha(0f)
            .setDuration(REVEAL_FADE_MS)
            .withEndAction {
                binding.kioskLoadingSplash.visibility = View.GONE
                binding.kioskLoadingSplash.alpha = 1f
            }
            .start()
    }

    private fun enterKioskErrorState(webView: WebView) {
        kioskInErrorState = true
        kioskReloadInProgress = false
        pendingKioskReveal = false
        cancelPendingReveal()
        webView.stopLoading()
        showLoadingSplash(waitingForNetwork = !hasValidatedNetwork())
        scheduleWatchdogRetry()
        Log.w(WEBVIEW_LOG_TAG, "Aguardando rede/URL — retentativa a cada 30s")
    }

    private fun attemptKioskReload(source: String) {
        if (!::binding.isInitialized || !kioskInErrorState) return
        if (!hasValidatedNetwork()) {
            showLoadingSplash(waitingForNetwork = !hasValidatedNetwork())
            scheduleWatchdogRetry()
            return
        }
        if (kioskReloadInProgress) return

        Log.d(WEBVIEW_LOG_TAG, "Tentando recarregar ($source): ${preferences.kioskUrl}")
        loadKioskUrlWithSplash(preferences.kioskUrl)
    }

    private fun scheduleWatchdogRetry() {
        reloadHandler.removeCallbacks(reloadWatchdogRunnable)
        reloadHandler.postDelayed(reloadWatchdogRunnable, RELOAD_INTERVAL_MS)
    }

    private fun scheduleNetworkReload() {
        reloadHandler.removeCallbacks(networkReloadRunnable)
        reloadHandler.postDelayed(networkReloadRunnable, NETWORK_RELOAD_DEBOUNCE_MS)
    }

    private fun hasValidatedNetwork(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun registerNetworkRecoveryCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                reloadHandler.post {
                    if (!kioskInErrorState || !::binding.isInitialized) return@post
                    scheduleNetworkReload()
                }
            }
        }
        networkCallback = callback
        connectivityManager.registerNetworkCallback(request, callback)
    }

    private fun clearErrorWatchdog() {
        kioskInErrorState = false
        lastMainFrameHttpError = false
        kioskReloadInProgress = false
        reloadHandler.removeCallbacks(reloadWatchdogRunnable)
        reloadHandler.removeCallbacks(networkReloadRunnable)
        cancelPendingReveal()
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
            binding.webView.onResume()
            binding.webView.resumeTimers()
            applyImmersiveMode()
        }
    }

    override fun onPause() {
        if (::binding.isInitialized) {
            binding.webView.onPause()
            binding.webView.pauseTimers()
        }
        super.onPause()
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
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (event?.repeatCount == 0) {
                    okPressCounter.registerPress()
                }
                return true
            }
        }

        if (isBlockedSystemKey(keyCode)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
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
        if (!hasValidatedNetwork()) {
            enterKioskErrorState(binding.webView)
        } else {
            loadKioskUrlWithSplash(preferences.kioskUrl)
        }
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
        networkCallback?.let { callback ->
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        }
        networkCallback = null
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
