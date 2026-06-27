package com.leodisplay.player.utils

import android.content.Context
import android.util.Log
import com.leodisplay.player.config.Config
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enterprise Log Manager: Collects and uploads logs to Supabase.
 */
object LogManager {

    private const val TAG = "LeoDisplay"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var logFile: File? = null
    private val client = OkHttpClient()

    fun init(context: Context) {
        logFile = File(context.filesDir, "player.log")
        logEvent("Enterprise Player Session Started")
    }

    fun logEvent(event: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val formattedLog = "[$timestamp] $event\n"
        
        Log.i(TAG, formattedLog.trim())
        
        scope.launch {
            try {
                logFile?.let {
                    FileOutputStream(it, true).use { stream ->
                        stream.write(formattedLog.toByteArray())
                    }
                }
            } catch (e: Exception) {
                // Fallback to Logcat
            }
        }
    }

    fun uploadLogs(activationCode: String) {
        scope.launch {
            try {
                val file = logFile ?: return@launch
                if (!file.exists() || file.length() == 0L) return@launch

                val content = file.readText()
                val body = content.toRequestBody("text/plain".toMediaType())
                
                // Supabase Storage Integration (Simulated via PATCH to tvs table for simplicity in this sprint)
                // In production, use Supabase Storage API for large log files.
                val request = Request.Builder()
                    .url("${Config.SUPABASE_URL}/rest/v1/tvs?activation_code=eq.$activationCode")
                    .patch(body) // Placeholder: in reality, would be a storage upload
                    .addHeader("apikey", Config.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer ${Config.SUPABASE_ANON_KEY}")
                    .build()

                Log.d(TAG, "Uploading logs for $activationCode...")
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.i(TAG, "Logs uploaded successfully.")
                        file.delete()
                        file.createNewFile()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Log upload failed: ${e.message}")
            }
        }
    }
}
