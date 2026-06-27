package com.leodisplay.player.ui.splash

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.leodisplay.player.R
import com.leodisplay.player.config.Config
import com.leodisplay.player.storage.StorageManager
import com.leodisplay.player.ui.activation.ActivationActivity
import com.leodisplay.player.ui.player.PlayerActivity
import com.leodisplay.player.utils.LogManager
import com.leodisplay.player.utils.Utils.enableFullscreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

/**
 * Splash screen displayed on app launch.
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        enableFullscreen()

        // Global Enterprise Crash Recovery
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("LeoDisplay", "FATAL CRASH in thread ${thread.name}: ${throwable.message}")
            val intent = Intent(this, SplashActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            exitProcess(1)
        }

        LogManager.init(this)
        LogManager.logEvent("Splash Started - Version 1.1 Enterprise")

        val storageManager = StorageManager(this)

        lifecycleScope.launch {
            // Ensure identity exists
            storageManager.getOrCreateActivationCode()
            storageManager.getOrCreateDeviceId()

            val isActivated = storageManager.isActivated.first()

            delay(Config.SPLASH_DURATION_MS)

            if (isActivated) {
                LogManager.logEvent("Device Activated. Launching Player.")
                navigateToPlayer()
            } else {
                LogManager.logEvent("Device Not Activated. Launching Activation Screen.")
                navigateToActivation()
            }
        }
    }

    private fun navigateToPlayer() {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToActivation() {
        val intent = Intent(this, ActivationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
