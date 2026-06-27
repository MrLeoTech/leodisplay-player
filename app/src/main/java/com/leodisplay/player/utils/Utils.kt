package com.leodisplay.player.utils

import android.app.Activity
import android.view.View
import android.view.WindowManager

/**
 * Utility functions for the LeoDisplay Android TV Player.
 */
object Utils {

    /**
     * Enables full-screen immersive mode hiding both navigation and status bars.
     * Implements Sticky Immersive mode to ensure bars stay hidden.
     */
    fun Activity.enableFullscreen() {
        // Keep screen on flag
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }

    /**
     * Keeps the screen awake while the player is active.
     */
    fun Activity.keepScreenOn() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * Formats a duration in milliseconds to a human-readable string.
     */
    fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return when {
            hours > 0 -> "%02d:%02d:%02d".format(hours, minutes % 60, seconds % 60)
            else -> "%02d:%02d".format(minutes, seconds % 60)
        }
    }
}
