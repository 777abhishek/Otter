package com.Otter.app.network

import android.content.Context
import android.os.Build
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
 * Sends crash reports to the backend.
 *
 * Transparency:
 * - Only sends if user has enabled crash reporting in settings
 * - Uses a random app-generated device ID (not ANDROID_ID)
 * - Redacts potential PII (emails, tokens, auth headers, URLs with query params)
 */
@Singleton
class CrashReportService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsService: SettingsService,
    private val deviceIdManager: DeviceIdManager,
) {
    private val client = OkHttpClient()
    private val baseUrl = BuildConfig.Otter_BACKEND_BASE_URL
    private val apiKey = BuildConfig.Otter_APP_API_KEY

    /**
     * Redacts potentially sensitive information from text.
     * - Email addresses
     * - Authorization/Bearer tokens
     * - URLs with query parameters (keeps base URL)
     * - Long hex strings that could be tokens
     */
    private fun redactPii(text: String): String {
        var result = text

        // Redact email addresses
        val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        result = emailRegex.replace(result) { "[REDACTED_EMAIL]" }

        // Redact Authorization headers
        val authRegex = Regex("(Authorization|Bearer|Token)\\s*[:=]?\\s*\\S+", RegexOption.IGNORE_CASE)
        result = authRegex.replace(result) { "${it.groupValues[1]} [REDACTED]" }

        // Redact URLs with query strings (keep base)
        val urlQueryRegex = Regex("(https?://[^\\s?]+)\\?[^\\s]+")
        result = urlQueryRegex.replace(result) { "${it.groupValues[1]}?[REDACTED]" }

        // Redact long hex strings (likely tokens/keys)
        val hexRegex = Regex("\\b[a-fA-F0-9]{16,}\\b")
        result = hexRegex.replace(result) { "[REDACTED_TOKEN]" }

        return result
    }

    suspend fun sendCrashReport(
        stackTrace: String,
        errorType: String,
        errorMessage: String,
        deviceInfo: Map<String, Any>? = null,
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                // Check if crash reporting is enabled
                val settings = settingsService.getSettings().first()
                if (!settings.crashReportingEnabled) {
                    return@withContext true // Silently skip
                }

                val deviceId = deviceIdManager.getOrCreateDeviceId()

                // Redact PII from stack trace and error message
                val redactedStackTrace = redactPii(stackTrace)
                val redactedErrorMessage = redactPii(errorMessage)

                val deviceInfoJson =
                    JSONObject().apply {
                        put("manufacturer", Build.MANUFACTURER)
                        put("model", Build.MODEL)
                        put("androidVersion", Build.VERSION.RELEASE)
                        put("sdkVersion", Build.VERSION.SDK_INT)
                        deviceInfo?.forEach { (key, value) ->
                            put(key, value)
                        }
                    }

                val json =
                    JSONObject().apply {
                        put("deviceId", deviceId)
                        put("stackTrace", redactedStackTrace)
                        put("errorType", errorType)
                        put("errorMessage", redactedErrorMessage)
                        put("deviceInfo", deviceInfoJson)
                        put("appVersion", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                        put("platform", "android")
                    }

                val requestBuilder =
                    Request.Builder()
                        .url("$baseUrl/api/crash/report")
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
}
