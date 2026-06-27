package com.leodisplay.player.ui.player

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.leodisplay.player.R
import com.leodisplay.player.config.Config
import com.leodisplay.player.config.PlayerState
import com.leodisplay.player.network.HeartbeatManager
import com.leodisplay.player.network.NetworkMonitor
import com.leodisplay.player.network.RemoteCommandManager
import com.leodisplay.player.storage.StorageManager
import com.leodisplay.player.ui.error.ErrorActivity
import com.leodisplay.player.utils.LogManager
import com.leodisplay.player.utils.Utils.enableFullscreen
import com.leodisplay.player.utils.WebViewHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * Enterprise Player Activity: Fully integrated with Supabase Realtime/Admin.
 */
class PlayerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LeoDisplay"
    }

    private lateinit var webView: WebView
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var storageManager: StorageManager
    private lateinit var heartbeatManager: HeartbeatManager
    private lateinit var remoteCommandManager: RemoteCommandManager
    private lateinit var watchdog: PlayerWatchdog
    private var isErrorVisible = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        enableFullscreen()

        LogManager.init(this)
        LogManager.logEvent("Player Activity Initializing...")

        networkMonitor = NetworkMonitor(this)
        storageManager = StorageManager(this)
        heartbeatManager = HeartbeatManager(this)
        heartbeatManager.currentState = PlayerState.ONLINE
        
        remoteCommandManager = RemoteCommandManager(this) {
            LogManager.logEvent("Reloading Player via command.")
            loadPlayerUrl()
        }

        webView = findViewById(R.id.webView)
        WebViewHelper.configure(webView)
        setupWebViewClient()
        setupWebChromeClient()

        watchdog = PlayerWatchdog(webView) {
            LogManager.logEvent("Watchdog triggered recovery.")
            loadPlayerUrl()
        }

        observeNetwork()
        loadPlayerUrl()
        
        heartbeatManager.start()
        remoteCommandManager.start()
        watchdog.start()
        
        LogManager.logEvent("LeoDisplay Enterprise Player Started.")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableFullscreen()
        }
    }

    private fun observeNetwork() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                networkMonitor.isOnline.collect { isOnline ->
                    Log.d(TAG, "Connectivity: $isOnline")
                    if (!isOnline && !isErrorVisible) {
                        heartbeatManager.currentState = PlayerState.OFFLINE
                        showErrorScreen()
                    } else if (isOnline) {
                        heartbeatManager.currentState = PlayerState.ONLINE
                    }
                }
            }
        }
    }

    private fun setupWebViewClient() {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d(TAG, "Loading URL: $url")
                isErrorVisible = false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.i(TAG, "Player Loaded Successfully.")
                isErrorVisible = false
                heartbeatManager.currentState = PlayerState.ONLINE
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    LogManager.logEvent("WebView Main Frame Error: ${error?.errorCode}")
                    heartbeatManager.currentState = PlayerState.ERROR
                    showErrorScreen()
                }
            }
        }
    }

    private fun setupWebChromeClient() {
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                super.onShowCustomView(view, callback)
                Log.v(TAG, "Custom View requested (Video Fullscreen)")
            }
        }
    }

    private fun loadPlayerUrl() {
        lifecycleScope.launch {
            val code = storageManager.getOrCreateActivationCode()
            val url = Config.buildPlayerUrl(code)
            LogManager.logEvent("Loading Player URL: $url")
            webView.loadUrl(url)
        }
    }

    private fun showErrorScreen() {
        if (isErrorVisible) return
        isErrorVisible = true
        LogManager.logEvent("Navigating to Recovery Screen.")
        val intent = Intent(this, ErrorActivity::class.java)
        startActivity(intent)
    }

    /**
     * Captures the current screen and returns it as a Base64 string.
     * Used for remote monitoring.
     */
    fun captureScreenshot(): String {
        val bitmap = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        webView.draw(canvas)
        
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> true // Lockdown
            else -> {
                webView.dispatchKeyEvent(event)
                super.onKeyDown(keyCode, event)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.resumeTimers()
        enableFullscreen()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        webView.pauseTimers()
    }

    override fun onDestroy() {
        heartbeatManager.stop()
        remoteCommandManager.stop()
        watchdog.stop()
        networkMonitor.unregister()
        webView.destroy()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // No-op for Kiosk
    }
}
