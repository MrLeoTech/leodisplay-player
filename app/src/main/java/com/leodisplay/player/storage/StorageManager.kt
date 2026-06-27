package com.leodisplay.player.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "leodisplay_prefs")

/**
 * Manages persistent storage using DataStore Preferences.
 */
class StorageManager(private val context: Context) {

    companion object {
        val ACTIVATION_CODE_KEY = stringPreferencesKey("activation_code")
        val DEVICE_ID_KEY = stringPreferencesKey("device_id")
        val SERVER_URL_KEY = stringPreferencesKey("server_url")
        val IS_ACTIVATED_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("is_activated")
    }

    /** Sets the activation status. */
    suspend fun setActivated(activated: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_ACTIVATED_KEY] = activated
        }
    }

    /** Checks if the device is activated. */
    val isActivated: Flow<Boolean>
        get() = context.dataStore.data.map { prefs ->
            prefs[IS_ACTIVATED_KEY] ?: false
        }

    /**
     * Gets the stored activation code or generates a new one if it doesn't exist.
     * Activation code is a random 6-character alphanumeric string.
     */
    suspend fun getOrCreateActivationCode(): String {
        val prefs = context.dataStore.data.first()
        val existing = prefs[ACTIVATION_CODE_KEY]
        if (existing != null) return existing

        val newCode = generateRandomCode(6)
        saveActivationCode(newCode)
        return newCode
    }

    /**
     * Gets the stored device ID or generates a new one if it doesn't exist.
     */
    suspend fun getOrCreateDeviceId(): String {
        val prefs = context.dataStore.data.first()
        val existing = prefs[DEVICE_ID_KEY]
        if (existing != null) return existing

        val newId = UUID.randomUUID().toString()
        saveDeviceId(newId)
        return newId
    }

    private fun generateRandomCode(length: Int): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    /** Saves the activation code locally. */
    suspend fun saveActivationCode(code: String) {
        context.dataStore.edit { prefs ->
            prefs[ACTIVATION_CODE_KEY] = code
        }
    }

    /** Retrieves the stored activation code, or null if not set. */
    val activationCode: Flow<String?>
        get() = context.dataStore.data.map { prefs ->
            prefs[ACTIVATION_CODE_KEY]
        }

    /** Saves a custom server URL (overrides the default Config.SERVER_URL). */
    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[SERVER_URL_KEY] = url
        }
    }

    /** Retrieves the stored server URL. */
    val serverUrl: Flow<String?>
        get() = context.dataStore.data.map { prefs ->
            prefs[SERVER_URL_KEY]
        }

    /** Saves the unique device ID. */
    suspend fun saveDeviceId(id: String) {
        context.dataStore.edit { prefs ->
            prefs[DEVICE_ID_KEY] = id
        }
    }

    /** Retrieves the stored device ID. */
    val deviceId: Flow<String?>
        get() = context.dataStore.data.map { prefs ->
            prefs[DEVICE_ID_KEY]
        }
}
