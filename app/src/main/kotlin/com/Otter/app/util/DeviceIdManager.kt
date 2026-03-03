package com.Otter.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.deviceIdStore: DataStore<Preferences> by preferencesDataStore(name = "device_id")

/**
 * Manages a stable, app-generated device identifier.
 *
 * Transparency note:
 * - We generate a random UUID on first run and store it locally.
 * - This is NOT the Android ANDROID_ID, advertising ID, or any hardware identifier.
 * - It is used solely to deduplicate analytics/crash reports from this install.
 * - Users can reset it by clearing app data.
 */
@Singleton
class DeviceIdManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val DEVICE_ID = stringPreferencesKey("device_id")
    }

    /**
     * Returns the stored device ID, or generates and stores a new one.
     */
    suspend fun getOrCreateDeviceId(): String {
        val stored = context.deviceIdStore.data.map { prefs ->
            prefs[Keys.DEVICE_ID]
        }.first()

        if (!stored.isNullOrBlank()) {
            return stored
        }

        val newId = UUID.randomUUID().toString()
        context.deviceIdStore.edit { prefs ->
            prefs[Keys.DEVICE_ID] = newId
        }
        return newId
    }

    /**
     * Returns the current device ID without creating a new one.
     */
    suspend fun getDeviceId(): String? {
        return context.deviceIdStore.data.map { prefs ->
            prefs[Keys.DEVICE_ID]
        }.first()
    }
}
