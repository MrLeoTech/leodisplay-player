package com.leodisplay.player.network

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.leodisplay.player.config.Config
import com.leodisplay.player.storage.StorageManager
import com.leodisplay.player.ui.player.PlayerActivity
import com.leodisplay.player.ui.splash.SplashActivity
import com.leodisplay.player.utils.LogManager
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.system.exitProcess

/**
 * Enterprise Remote Command Manager: Polling-based Realtime Integration.
 */
class RemoteCommandManager(
    private val context: Context,
    private val onReload: () -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val storageManager = StorageManager(context)
    private val otaManager = OtaManager(context)
    private val client = OkHttpClient()
    private val gson = Gson()
    private var commandJob: Job? = null

    fun start() {
        if (commandJob?.isActive == true) return
        
        commandJob = scope.launch {
            while (isActive) {
                try {
                    val activationCode = storageManager.getOrCreateActivationCode()
                    checkForCommands(activationCode)
                } catch (e: Exception) {
                    Log.e("LeoDisplay", "Command polling error: ${e.message}")
                }
                delay(5000) // Poll every 5 seconds for "near-realtime"
            }
        }
    }

    fun stop() {
        commandJob?.cancel()
    }

    private suspend fun checkForCommands(activationCode: String) {
        val request = Request.Builder()
            .url("${Config.SUPABASE_URL}/rest/v1/tvs?activation_code=eq.$activationCode&select=last_command")
            .get()
            .addHeader("apikey", Config.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${Config.SUPABASE_ANON_KEY}")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val listType = object : TypeToken<List<Map<String, String>>>() {}.type
                val data: List<Map<String, String>> = gson.fromJson(body, listType)
                
                if (data.isNotEmpty()) {
                    val lastCommand = data[0]["last_command"]
                    if (!lastCommand.isNullOrEmpty()) {
                        executeCommand(lastCommand)
                        clearCommandOnServer(activationCode)
                    }
                }
            }
        }
    }

    fun executeCommand(command: String) {
        LogManager.logEvent("Remote Command Received: $command")
        
        // Command validation & Confirmation
        val validCommands = listOf(
            "restart_player", "reload_playlist", "clear_cache", 
            "maintenance_mode", "reboot_device", "ota_update", 
            "capture_screenshot", "upload_logs"
        )
        
        if (!validCommands.contains(command.lowercase())) {
            Log.w("LeoDisplay", "Invalid Remote Command: $command")
            return
        }

        when (command.lowercase()) {
            "restart_player" -> restartPlayer()
            "reload_playlist" -> onReload()
            "clear_cache" -> clearCache()
            "reboot_device" -> rebootApp()
            "ota_update" -> LogManager.logEvent("OTA update requested via command.")
            "capture_screenshot" -> captureAndUploadScreenshot()
            "upload_logs" -> LogManager.uploadLogs(runBlocking { storageManager.getOrCreateActivationCode() })
            "maintenance_mode" -> LogManager.logEvent("Entering Maintenance Mode.")
            else -> Log.w("LeoDisplay", "Validated command with no implementation: $command")
        }
    }

    private fun restartPlayer() {
        val intent = Intent(context, PlayerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }

    private fun clearCache() {
        scope.launch(Dispatchers.Main) {
            val dummyWebView = android.webkit.WebView(context)
            dummyWebView.clearCache(true)
            LogManager.logEvent("WebView Cache Cleared.")
            onReload()
        }
    }

    private fun rebootApp() {
        val intent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
        exitProcess(0)
    }

    private fun captureAndUploadScreenshot() {
        if (context is PlayerActivity) {
            val base64 = context.captureScreenshot()
            uploadScreenshot(base64)
        }
    }

    private fun uploadScreenshot(base64: String) {
        scope.launch {
            try {
                val code = storageManager.getOrCreateActivationCode()
                val json = gson.toJson(mapOf("last_screenshot" to base64))
                val body = json.toRequestBody("application/json".toMediaType())
                
                val request = Request.Builder()
                    .url("${Config.SUPABASE_URL}/rest/v1/tvs?activation_code=eq.$code")
                    .patch(body)
                    .addHeader("apikey", Config.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer ${Config.SUPABASE_ANON_KEY}")
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        LogManager.logEvent("Screenshot uploaded successfully.")
                    }
                }
            } catch (e: Exception) {
                Log.e("LeoDisplay", "Screenshot upload failed: ${e.message}")
            }
        }
    }

    private suspend fun clearCommandOnServer(activationCode: String) {
        try {
            val json = gson.toJson(mapOf("last_command" to null))
            val body = json.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("${Config.SUPABASE_URL}/rest/v1/tvs?activation_code=eq.$activationCode")
                .patch(body)
                .addHeader("apikey", Config.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${Config.SUPABASE_ANON_KEY}")
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { }
        } catch (e: Exception) {}
    }
}
