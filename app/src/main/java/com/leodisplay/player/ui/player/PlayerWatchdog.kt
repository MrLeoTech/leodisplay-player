package com.leodisplay.player.ui.player

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import android.webkit.WebView
import kotlinx.coroutines.*

/**
 * Enterprise Watchdog: Monitors memory, responsiveness, and connectivity.
 */
class PlayerWatchdog(
    private val webView: WebView,
    private val onHang: () -> Unit
) {
    private val context: Context = webView.context
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var watchdogJob: Job? = null

    fun start() {
        watchdogJob = scope.launch {
            while (isActive) {
                delay(30000) // Check every 30 seconds
                performChecks()
            }
        }
    }

    fun stop() {
        watchdogJob?.cancel()
    }

    private fun performChecks() {
        checkWebView()
        checkMemory()
    }

    private fun checkWebView() {
        if (webView.url == null || webView.contentHeight == 0) {
            Log.e("LeoDisplay", "Watchdog: WebView appears empty or frozen. Triggering recovery.")
            onHang()
        }
    }

    private fun checkMemory() {
        val mi = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(mi)
        
        if (mi.lowMemory) {
            Log.w("LeoDisplay", "Watchdog: Low memory detected (${mi.availMem / 1024 / 1024}MB free). Clearing cache.")
            webView.clearCache(false)
        }
    }
}
