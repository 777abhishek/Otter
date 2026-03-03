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
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends analytics events to the backend.
 *
 * Transparency:
 * - Only sends if user has enabled analytics in settings
 * - Uses a random app-generated device ID (not ANDROID_ID)
 * - Includes app version and platform for debugging
 */
@Singleton
class AnalyticsService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsService: SettingsService,
    private val deviceIdManager: DeviceIdManager,
) {
    private val client = OkHttpClient()
    private val baseUrl = BuildConfig.Otter_BACKEND_BASE_URL
    private val apiKey = BuildConfig.Otter_APP_API_KEY

    suspend fun sendAnalyticsEvent(
        eventType: String,
        eventName: String,
        eventData: Map<String, Any>? = null,
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                // Check if analytics is enabled
                val settings = settingsService.getSettings().first()
                if (!settings.analyticsEnabled) {
                    return@withContext true // Silently skip
                }

                val deviceId = deviceIdManager.getOrCreateDeviceId()
                val eventDataMap = eventData as? Map<String, Any> ?: emptyMap()
                val json =
                    JSONObject().apply {
                        put("deviceId", deviceId)
                        put("eventType", eventType)
                        put("eventName", eventName)
                        put("eventData", JSONObject(eventDataMap))
                        put("appVersion", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                        put("platform", "android")
                    }

                val requestBuilder =
                    Request.Builder()
                        .url("$baseUrl/api/analytics/event")
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

    suspend fun sendAnalyticsBatch(events: List<Map<String, Any>>): Boolean =
        withContext(Dispatchers.IO) {
            try {
                // Check if analytics is enabled
                val settings = settingsService.getSettings().first()
                if (!settings.analyticsEnabled) {
                    return@withContext true // Silently skip
                }

                val deviceId = deviceIdManager.getOrCreateDeviceId()
                val jsonArray = JSONArray()
                events.forEach { event ->
                    val eventDataMap = (event["eventData"] as? Map<String, Any>) ?: emptyMap()
                    jsonArray.put(
                        JSONObject().apply {
                            put("deviceId", deviceId)
                            put("eventType", event["eventType"])
                            put("eventName", event["eventName"])
                            put("eventData", JSONObject(eventDataMap))
                            put("appVersion", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                            put("platform", "android")
                        },
                    )
                }

                val request =
                    Request.Builder()
                        .url("$baseUrl/api/analytics/batch")
                        .post(jsonArray.toString().toRequestBody("application/json".toMediaType()))
                        .apply {
                            if (apiKey.isNotBlank()) {
                                header("Authorization", "Bearer $apiKey")
                            }
                        }
                        .build()

                val response = client.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
}
