package com.leodisplay.player.ui.error

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.leodisplay.player.R
import com.leodisplay.player.config.Config
import com.leodisplay.player.network.NetworkMonitor
import com.leodisplay.player.ui.player.PlayerActivity
import com.leodisplay.player.utils.Utils.enableFullscreen
import kotlinx.coroutines.*

/**
 * Professional Error Screen with automatic recovery.
 */
class ErrorActivity : AppCompatActivity() {

    private lateinit var networkMonitor: NetworkMonitor
    private var countdownJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error)
        enableFullscreen()

        networkMonitor = NetworkMonitor(this)

        val tvRetry = findViewById<TextView>(R.id.tvAutoRetry)
        val btnRetry = findViewById<Button>(R.id.btnRetry)

        btnRetry.setOnClickListener {
            Log.d("LeoDisplay", "Manual retry clicked.")
            attemptReconnect()
        }

        observeNetwork()
        startCountdown(tvRetry)
    }

    override fun onResume() {
        super.onResume()
        enableFullscreen()
    }

    private fun startCountdown(textView: TextView) {
        countdownJob = lifecycleScope.launch {
            var seconds = (Config.RECONNECT_INTERVAL_MS / 1000).toInt()
            while (isActive && seconds > 0) {
                textView.text = getString(R.string.error_auto_retry, seconds)
                delay(1000)
                seconds--
                if (seconds == 0) {
                    attemptReconnect()
                    seconds = (Config.RECONNECT_INTERVAL_MS / 1000).toInt()
                }
            }
        }
    }

    private fun observeNetwork() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                networkMonitor.isOnline.collect { isOnline ->
                    if (isOnline) {
                        Log.i("LeoDisplay", "Internet Restored. Returning to Player.")
                        navigateToPlayer()
                    }
                }
            }
        }
    }

    private fun attemptReconnect() {
        networkMonitor.refresh()
        if (networkMonitor.isOnline.value) {
            navigateToPlayer()
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
        countdownJob?.cancel()
        networkMonitor.unregister()
        super.onDestroy()
    }
}
