package com.leodisplay.player.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.leodisplay.player.ui.splash.SplashActivity

/**
 * Automatically starts the LeoDisplay Player when the device finishes booting.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("LeoDisplay", "Boot Completed. Starting Player...")
            
            val startIntent = Intent(context, SplashActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(startIntent)
        }
    }
}
