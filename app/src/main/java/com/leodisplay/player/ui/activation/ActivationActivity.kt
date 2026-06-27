package com.leodisplay.player.ui.activation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.leodisplay.player.R
import com.leodisplay.player.config.Config
import com.leodisplay.player.network.NetworkMonitor
import com.leodisplay.player.storage.StorageManager
import com.leodisplay.player.ui.player.PlayerActivity
import com.leodisplay.player.utils.Utils.enableFullscreen
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

/**
 * Professional Activation Screen.
 * Displays the code and checks every 10 seconds for registration.
 */
class ActivationActivity : AppCompatActivity() {

    private lateinit var storageManager: StorageManager
    private lateinit var networkMonitor: NetworkMonitor
    private var checkJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activation)
        enableFullscreen()

        storageManager = StorageManager(this)
        networkMonitor = NetworkMonitor(this)

        val tvCode = findViewById<TextView>(R.id.tvActivationCode)
        
        lifecycleScope.launch {
            val code = storageManager.getOrCreateActivationCode()
            tvCode.text = code
            Log.i("LeoDisplay", "Activation Screen: Showing code $code")
            startActivationCheck(code)
        }
    }

    override fun onResume() {
        super.onResume()
        enableFullscreen()
    }

    private fun startActivationCheck(code: String) {
        checkJob = lifecycleScope.launch {
            while (isActive) {
                if (networkMonitor.isOnline.value) {
                    val isRegistered = checkRegistrationOnBackend(code)
                    if (isRegistered) {
                        Log.i("LeoDisplay", "Activation Successful! Navigating to Player.")
                        storageManager.setActivated(true)
                        navigateToPlayer()
                        break
                    }
                }
                delay(Config.CHECK_ACTIVATION_INTERVAL_MS)
            }
        }
    }

    private suspend fun checkRegistrationOnBackend(code: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val playerUrl = Config.buildPlayerUrl(code)
            val url = URL(playerUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val responseCode = connection.responseCode
            
            // If the player page exists (200 OK), it means the device is registered/active
            return@withContext responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            Log.v("LeoDisplay", "Waiting for activation... (HTTP ${e.message})")
            false
        }
    }

    private fun navigateToPlayer() {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        checkJob?.cancel()
        networkMonitor.unregister()
        super.onDestroy()
    }
}
