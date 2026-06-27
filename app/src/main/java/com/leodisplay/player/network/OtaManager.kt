package com.leodisplay.player.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Handles Over-The-Air (OTA) updates for the LeoDisplay Player.
 */
class OtaManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun checkAndPerformUpdate(updateUrl: String, newVersionName: String) {
        val currentVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "0.0.0"
        }

        if (newVersionName > currentVersion) {
            Log.i("LeoDisplay", "New version available: $newVersionName (Current: $currentVersion). Starting download...")
            downloadAndInstallApk(updateUrl)
        } else {
            Log.d("LeoDisplay", "Player is up to date ($currentVersion).")
        }
    }

    private fun downloadAndInstallApk(url: String) {
        scope.launch {
            try {
                val destination = File(context.getExternalFilesDir(null), "update.apk")
                if (destination.exists()) destination.delete()

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Server returned HTTP ${connection.responseCode}")
                }

                connection.inputStream.use { input ->
                    FileOutputStream(destination).use { output ->
                        input.copyTo(output)
                    }
                }

                Log.i("LeoDisplay", "Update downloaded to ${destination.absolutePath}. Launching installer.")
                installApk(destination)
            } catch (e: Exception) {
                Log.e("LeoDisplay", "OTA Update failed: ${e.message}")
            }
        }
    }

    private fun installApk(file: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } else {
            Uri.fromFile(file)
        }
        
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
