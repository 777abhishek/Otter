package com.Otter.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.Otter.app.OtterApplication
import com.Otter.app.R
import com.Otter.app.util.DynamicIconProvider
import com.Otter.app.util.PreferenceUtil.getInt
import com.Otter.app.util.PreferenceUtil.updateLong
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object UpdateUtil {
    private const val TAG = "UpdateUtil"

    private const val UPDATES_CHANNEL_ID = "updates_channel"
    private const val UPDATES_CHANNEL_NAME = "Updates"
    private const val NOTIFICATION_ID_UPDATES = 1301

    private fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun ensureUpdatesChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = nm.getNotificationChannel(UPDATES_CHANNEL_ID)
            if (existing == null) {
                val channel =
                    NotificationChannel(
                        UPDATES_CHANNEL_ID,
                        UPDATES_CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_LOW,
                    ).apply {
                        description = "Shows update progress and status"
                        setShowBadge(false)
                        setSound(null, null)
                        enableVibration(false)
                    }
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun postUpdateNotification(
        context: Context,
        title: String,
        text: String,
        ongoing: Boolean,
    ) {
        if (!canPostNotifications(context)) return
        ensureUpdatesChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification =
            NotificationCompat.Builder(context, UPDATES_CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_light)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(ongoing)
                .setOnlyAlertOnce(true)
                .setProgress(0, 0, ongoing)
                .build()
        kotlin.runCatching {
            nm.notify(NOTIFICATION_ID_UPDATES, notification)
        }
    }

    private fun cancelUpdateNotification(context: Context) {
        if (!canPostNotifications(context)) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        kotlin.runCatching {
            nm.cancel(NOTIFICATION_ID_UPDATES)
        }
    }

    fun postUpdateAvailableNotification(
        title: String,
        text: String,
    ) {
        val context = OtterApplication.appContext
        postUpdateNotification(context, title = title, text = text, ongoing = false)
    }

    suspend fun updateYtDlpWithNotification(): YoutubeDL.UpdateStatus? {
        val context = OtterApplication.appContext
        postUpdateNotification(context, "Updating yt-dlp", "Checking for updates…", ongoing = true)
        return runCatching {
            val status = updateYtDlp()
            val (title, text) =
                when (status) {
                    YoutubeDL.UpdateStatus.DONE -> "yt-dlp updated" to "Update completed"
                    YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> "yt-dlp" to "Already up to date"
                    else -> "yt-dlp" to "Update finished"
                }
            postUpdateNotification(context, title, text, ongoing = false)
            status
        }.getOrElse {
            postUpdateNotification(context, "yt-dlp update failed", it.message ?: "Unknown error", ongoing = false)
            null
        }.also {
            if (it == YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE) {
                cancelUpdateNotification(context)
            }
        }
    }

    suspend fun updateYtDlp(): YoutubeDL.UpdateStatus? =
        withContext(Dispatchers.IO) {
            val channel =
                when (YT_DLP_UPDATE_CHANNEL.getInt()) {
                    YT_DLP_NIGHTLY -> YoutubeDL.UpdateChannel.NIGHTLY
                    else -> YoutubeDL.UpdateChannel.STABLE
                }

            YoutubeDL.getInstance()
                .updateYoutubeDL(appContext = OtterApplication.appContext, updateChannel = channel)
                .also { status ->
                    if (status == YoutubeDL.UpdateStatus.DONE) {
                        YoutubeDL.getInstance().version(OtterApplication.appContext)?.let { version ->
                            PreferenceUtil.encodeString(YT_DLP_VERSION, version)
                        }
                    }
                    val now = System.currentTimeMillis()
                    YT_DLP_UPDATE_TIME.updateLong(now)
                }
        }

    fun getYtDlpVersion(context: Context): String? {
        return YoutubeDL.getInstance().version(context)
    }

    suspend fun checkNewPipeUpdates(): String? =
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request =
                    Request.Builder()
                        .url("https://api.github.com/repos/TeamNewPipe/NewPipeExtractor/releases/latest")
                        .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val json = JSONObject(body ?: "")
                    val latestVersion = json.optString("tag_name")
                    val now = System.currentTimeMillis()
                    NEWPIPE_UPDATE_TIME.updateLong(now)
                    latestVersion
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    fun getNewPipeVersion(): String {
        return PreferenceUtil.getStringValue(NEWPIPE_VERSION, "v0.25.2")
    }
}
