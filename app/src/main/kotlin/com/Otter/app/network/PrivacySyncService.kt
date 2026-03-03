package com.Otter.app.network

import android.content.Context
import com.Otter.app.BuildConfig
import com.Otter.app.service.SettingsService
import com.Otter.app.util.DeviceIdManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Syncs privacy settings to the backend.
 *
 * Transparency:
 * - Sends user's privacy preferences to the server
 * - Server uses these to block ingestion when disabled
 * - Called when settings change
 */
@Singleton
class PrivacySyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsService: SettingsService,
    private val deviceIdManager: DeviceIdManager,
) {
    private val client = OkHttpClient()
    private val baseUrl = BuildConfig.Otter_BACKEND_BASE_URL
    private val apiKey = BuildConfig.Otter_APP_API_KEY

    /**
     * Syncs current privacy settings to the backend.
     * Call this when the user changes privacy settings.
     */
    suspend fun syncPrivacySettings(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val settings = settingsService.getSettings().first()
                val deviceId = deviceIdManager.getOrCreateDeviceId()

                val json = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("analyticsEnabled", settings.analyticsEnabled)
                    put("crashReportingEnabled", settings.crashReportingEnabled)
                    put("dataSharingEnabled", settings.dataSharingEnabled)
                }

                val requestBuilder = Request.Builder()
                    .url("$baseUrl/api/privacy/settings")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))

                if (apiKey.isNotBlank()) {
                    requestBuilder.header("Authorization", "Bearer $apiKey")
                }

                val request = requestBuilder.build()
                val response = client.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    /**
     * Requests deletion of all data for this device.
     * Call this when the user requests data deletion.
     */
    suspend fun requestDeviceDataDeletion(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val deviceId = deviceIdManager.getOrCreateDeviceId()

                val requestBuilder = Request.Builder()
                    .delete()
                    .url("$baseUrl/api/data/$deviceId")

                if (apiKey.isNotBlank()) {
                    requestBuilder.header("Authorization", "Bearer $apiKey")
                }

                val request = requestBuilder.build()
                val response = client.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
}
