package com.leodisplay.player.config

/**
 * Configuration constants for the LeoDisplay Android TV Player.
 * All server and timing settings are centralized here for easy modification.
 */
object Config {

    /** Server base URL — change this to point to your LeoDisplay instance */
    const val SERVER_URL = "https://mrleo-display.netlify.app"

    /** Player web path appended to SERVER_URL */
    const val PLAYER_PATH = "/player"

    /**
     * Builds the full player URL with the given activation code.
     * Example: https://mrleo-display.netlify.app/player/ABC123
     */
    fun buildPlayerUrl(activationCode: String): String {
        return "$SERVER_URL$PLAYER_PATH/$activationCode"
    }

    /** Heartbeat interval in seconds (prepared for Sprint APK 2) */
    const val HEARTBEAT_INTERVAL_SECONDS = 30L

    /** Application display name */
    const val APP_NAME = "LeoDisplay"

    /** Splash screen duration in milliseconds */
    const val SPLASH_DURATION_MS = 2000L

    /** Auto-reconnect interval when server is offline (milliseconds) */
    const val RECONNECT_INTERVAL_MS = 10000L

    /** WebView cache size in bytes (10 MB) */
    const val WEBVIEW_CACHE_SIZE = 10 * 1024 * 1024

    /** User-Agent suffix appended to the default WebView UA */
    const val USER_AGENT_SUFFIX = "LeoDisplayTV/1.0"

    /** API Check Interval for activation (milliseconds) */
    const val CHECK_ACTIVATION_INTERVAL_MS = 10000L

    /** Heartbeat interval (milliseconds) */
    const val HEARTBEAT_INTERVAL_MS = 30000L

    /** Supabase Configuration */
    const val SUPABASE_URL = "https://your-project.supabase.co"
    const val SUPABASE_ANON_KEY = "your-anon-key"
}
