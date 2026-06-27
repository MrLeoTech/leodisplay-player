package com.leodisplay.player.utils

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.leodisplay.player.config.Config

/**
 * Optimized WebView configuration for professional Digital Signage (24/7).
 */
object WebViewHelper {

    @SuppressLint("SetJavaScriptEnabled")
    fun configure(webView: WebView) {
        // Enable Cookies & Third-party cookies for cross-origin assets
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true)
        }

        webView.settings.apply {
            // Essential for Player Web App
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            
            // Media support
            mediaPlaybackRequiresUserGesture = false
            
            // Layout & Viewport
            loadWithOverviewMode = true
            useWideViewPort = true
            
            // Performance & Rendering
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            cacheMode = WebSettings.LOAD_DEFAULT
            
            // Local Storage & File Access
            allowFileAccess = true
            allowContentAccess = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
            }

            // TV Controls (Disabled for Signage)
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false

            // Network Security (Mixed Content)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            // User Agent Identification
            userAgentString = "${userAgentString} ${Config.USER_AGENT_SUFFIX}"
        }

        // Advanced Hardware Acceleration
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
        
        // Visual polish
        webView.setBackgroundColor(android.graphics.Color.BLACK)
        
        // Remove scrollbars
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
    }
}
