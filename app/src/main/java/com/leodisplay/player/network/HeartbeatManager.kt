package com.leodisplay.player.network

import android.app.ActivityManager
import android.content.Context
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import com.google.gson.Gson
import com.leodisplay.player.config.Config
import com.leodisplay.player.config.PlayerState
import com.leodisplay.player.storage.StorageManager
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.NetworkInterface
import java.util.*

/**
 * Handles periodic heartbeats to the server to report player health and status.
 * Enterprise version: Reports detailed device metrics and current player state.
 */
class HeartbeatManager(private val context: Context) {

    private val storageManager = StorageManager(context)
    private val networkMonitor = NetworkMonitor(context)
    private var heartbeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient()
    private val gson = Gson()
    
    var currentState: PlayerState = PlayerState.ONLINE

    fun start() {
        if (heartbeatJob?.isActive == true) return
        
        heartbeatJob = scope.launch {
            while (isActive) {
                sendHeartbeat()
                delay(Config.HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        heartbeatJob?.cancel()
        networkMonitor.unregister()
    }

    private suspend fun sendHeartbeat() {
        try {
            val activationCode = storageManager.getOrCreateActivationCode()
            val deviceId = storageManager.getOrCreateDeviceId()
            val metrics = getDeviceMetrics(activationCode, deviceId)
            
            Log.i("LeoDisplay", "Heartbeat: $activationCode | State: $currentState | RAM: ${metrics["ram_free"]}MB")

            val json = gson.toJson(metrics)
            val body = json.toRequestBody("application/json".toMediaType())
            
            // Supabase REST API Integration
            val request = Request.Builder()
                .url("${Config.SUPABASE_URL}/rest/v1/tvs?activation_code=eq.$activationCode")
                .patch(body)
                .addHeader("apikey", Config.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${Config.SUPABASE_ANON_KEY}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("LeoDisplay", "Heartbeat sync failed: ${response.code} ${response.message}")
                }
            }
        } catch (e: Exception) {
            Log.w("LeoDisplay", "Heartbeat network failed: ${e.message}")
        }
    }

    private fun getDeviceMetrics(activationCode: String, deviceId: String): Map<String, Any> {
        val metrics = mutableMapOf<String, Any>()
        
        metrics["activation_code"] = activationCode
        metrics["device_id"] = deviceId
        metrics["device_name"] = getDeviceName()
        metrics["model"] = Build.MODEL
        metrics["manufacturer"] = Build.MANUFACTURER
        metrics["android_version"] = Build.VERSION.RELEASE
        metrics["apk_version"] = getApkVersion()
        metrics["uptime_seconds"] = SystemClock.elapsedRealtime() / 1000
        metrics["status"] = currentState.name
        metrics["internet_status"] = if (networkMonitor.isOnline.value) "online" else "offline"
        
        // RAM Metrics
        val mi = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(mi)
        metrics["ram_total"] = mi.totalMem / (1024 * 1024)
        metrics["ram_free"] = mi.availMem / (1024 * 1024)
        metrics["ram_used"] = (mi.totalMem - mi.availMem) / (1024 * 1024)

        // Storage Metrics
        val stat = StatFs(Environment.getDataDirectory().path)
        metrics["storage_total"] = (stat.blockCountLong * stat.blockSizeLong) / (1024 * 1024)
        metrics["storage_free"] = stat.availableBytes / (1024 * 1024)

        metrics["wifi_signal"] = getWifiSignal()
        metrics["ip_address"] = getIpAddress()
        metrics["battery_level"] = getBatteryLevel()
        metrics["last_heartbeat"] = System.currentTimeMillis()
        
        return metrics
    }

    private fun getDeviceName(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        } else {
            null
        } ?: "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    private fun getApkVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun getWifiSignal(): Int {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            wifiManager.connectionInfo.rssi
        } catch (e: Exception) {
            -100
        }
    }

    private fun getIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress ?: continue
                        val isIPv4 = sAddr.indexOf(':') < 0
                        if (isIPv4) return sAddr
                    }
                }
            }
        } catch (e: Exception) {}
        return "0.0.0.0"
    }

    private fun getBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}
