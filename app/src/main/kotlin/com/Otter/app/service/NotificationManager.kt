package com.Otter.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.Otter.app.MainActivity
import com.Otter.app.R
import com.Otter.app.util.DynamicIconProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// --- Formatting Helpers ---

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
        else -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
    }
}

private fun formatDuration(totalSeconds: Int): String {
    if (totalSeconds <= 0) return ""
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/**
 * Notification service for displaying download progress and status
 * Supports background downloads with foreground service
 */
@Singleton
class NotificationManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val settingsService: SettingsService,
    ) {
        companion object {
            const val CHANNEL_ID_DOWNLOAD = "download_channel"
            private const val CHANNEL_NAME_DOWNLOAD = "Downloads"
            private const val NOTIFICATION_ID_DOWNLOAD = 1001

            const val CHANNEL_ID_SYNC = "sync_channel"
            private const val CHANNEL_NAME_SYNC = "Sync"
            private const val NOTIFICATION_ID_SYNC = 1101

            const val ACTION_CANCEL = "com.Otter.app.action.CANCEL"
            const val ACTION_RETRY = "com.Otter.app.action.RETRY"
            const val ACTION_OPEN = "com.Otter.app.action.OPEN"
        }

        private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        private val _notificationState = MutableStateFlow<NotificationState?>(null)
        val notificationState: StateFlow<NotificationState?> = _notificationState.asStateFlow()

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        @Volatile private var notificationsEnabled: Boolean = true

        @Volatile private var downloadNotificationsEnabled: Boolean = true

        @Volatile private var syncNotificationsEnabled: Boolean = true

        init {
            createDownloadNotificationChannel()
            createSyncNotificationChannel()

            scope.launch {
                settingsService.getSettings()
                    .map { Triple(it.notificationsEnabled, it.downloadNotificationsEnabled, it.syncNotificationsEnabled) }
                    .distinctUntilChanged()
                    .collect { (allEnabled, downloadEnabled, syncEnabled) ->
                        notificationsEnabled = allEnabled
                        downloadNotificationsEnabled = downloadEnabled
                        syncNotificationsEnabled = syncEnabled

                        if (!allEnabled || !downloadEnabled) {
                            cancelNotification()
                        }
                        if (!allEnabled || !syncEnabled) {
                            cancelSyncNotification()
                        }
                    }
            }
        }

        private fun canPostNotifications(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        private fun createDownloadNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel =
                    NotificationChannel(
                        CHANNEL_ID_DOWNLOAD,
                        CHANNEL_NAME_DOWNLOAD,
                        NotificationManager.IMPORTANCE_LOW,
                    ).apply {
                        description = "Shows download progress and status"
                        setShowBadge(false)
                        setSound(null, null)
                        enableVibration(false)
                    }
                notificationManager.createNotificationChannel(channel)
            }
        }

        private fun createSyncNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel =
                    NotificationChannel(
                        CHANNEL_ID_SYNC,
                        CHANNEL_NAME_SYNC,
                        NotificationManager.IMPORTANCE_LOW,
                    ).apply {
                        description = "Shows sync progress and status"
                        setShowBadge(false)
                        setSound(null, null)
                        enableVibration(false)
                    }
                notificationManager.createNotificationChannel(channel)
            }
        }

        private fun shouldPostDownloadNotifications(): Boolean {
            return notificationsEnabled && downloadNotificationsEnabled && canPostNotifications()
        }

        private fun shouldPostSyncNotifications(): Boolean {
            return notificationsEnabled && syncNotificationsEnabled && canPostNotifications()
        }

        /**
         * Update notification with download progress
         */
        fun updateNotification(
            taskId: String,
            title: String,
            uploader: String? = null,
            progress: Float = 0f,
            progressText: String = "",
            status: NotificationStatus = NotificationStatus.DOWNLOADING,
            fileSize: Long? = null,
            downloadedBytes: Long? = null,
            speed: Long? = null,
            eta: Int? = null,
        ) {
            val state =
                NotificationState(
                    taskId = taskId,
                    title = title,
                    uploader = uploader,
                    progress = progress,
                    progressText = progressText,
                    status = status,
                    fileSize = fileSize,
                    downloadedBytes = downloadedBytes,
                    speed = speed,
                    eta = eta,
                )
            _notificationState.value = state

            if (!shouldPostDownloadNotifications()) return

            // For completed/failed/cancelled status, show a final notification then auto-dismiss after delay
            when (status) {
                NotificationStatus.COMPLETED, NotificationStatus.FAILED, NotificationStatus.CANCELLED -> {
                    val notification = buildNotification(state)
                    kotlin.runCatching {
                        notificationManager.notify(NOTIFICATION_ID_DOWNLOAD, notification)
                    }
                    // Auto-dismiss completed/error notifications after 5 seconds
                    scope.launch {
                        kotlinx.coroutines.delay(5000)
                        // Only cancel if this is still the same task
                        if (_notificationState.value?.taskId == taskId) {
                            cancelNotification()
                        }
                    }
                }
                else -> {
                    val notification = buildNotification(state)
                    kotlin.runCatching {
                        notificationManager.notify(NOTIFICATION_ID_DOWNLOAD, notification)
                    }
                }
            }
        }

        /**
         * Cancel notification
         */
        fun cancelNotification() {
            if (canPostNotifications()) {
                kotlin.runCatching {
                    notificationManager.cancel(NOTIFICATION_ID_DOWNLOAD)
                }
            }
            _notificationState.value = null
        }

        fun updateSyncNotification(
            title: String,
            text: String,
            progress: Float? = null,
            ongoing: Boolean,
        ) {
            if (!shouldPostSyncNotifications()) return

            val builder =
                NotificationCompat.Builder(context, CHANNEL_ID_SYNC)
                    .setSmallIcon(R.drawable.icon_light)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setOngoing(ongoing)
                    .setOnlyAlertOnce(true)

            if (progress != null) {
                builder.setProgress(100, (progress.coerceIn(0f, 1f) * 100).toInt(), false)
            } else {
                builder.setProgress(0, 0, true)
            }

            val contentIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            builder.setContentIntent(contentIntent)

            kotlin.runCatching {
                notificationManager.notify(NOTIFICATION_ID_SYNC, builder.build())
            }
        }

        fun cancelSyncNotification() {
            if (canPostNotifications()) {
                kotlin.runCatching {
                    notificationManager.cancel(NOTIFICATION_ID_SYNC)
                }
            }
        }

        /**
         * Build notification with progress and actions
         */
        fun buildNotification(state: NotificationState): Notification {
            val contentTitle =
                buildString {
                    append(state.title)
                    if (state.uploader != null) {
                        append(" · ")
                        append(state.uploader)
                    }
                }

            val contentText =
                buildString {
                    when (state.status) {
                        NotificationStatus.DOWNLOADING -> {
                            val percent = (state.progress * 100).toInt()
                            append("Downloading: $percent%")
                            if (state.speed != null) {
                                append(" at ${formatBytes(state.speed)}/s")
                            }
                            if (state.eta != null && state.eta > 0) {
                                append(" · ${formatDuration(state.eta)}")
                            }
                        }
                        NotificationStatus.COMPLETED -> {
                            append("Download completed")
                        }
                        NotificationStatus.FAILED -> {
                            append("Download failed")
                        }
                        NotificationStatus.CANCELLED -> {
                            append("Download cancelled")
                        }
                        NotificationStatus.PAUSED -> {
                            append("Paused")
                        }
                        NotificationStatus.QUEUED -> {
                            append("Queued")
                        }
                    }
                }

            val smallIconRes = R.drawable.icon_light

            val builder =
                NotificationCompat.Builder(context, CHANNEL_ID_DOWNLOAD)
                    .setSmallIcon(smallIconRes)
                    .setContentTitle(contentTitle)
                    .setContentText(contentText)
                    .setOngoing(state.status == NotificationStatus.DOWNLOADING)
                    .setOnlyAlertOnce(true)

            // Set progress bar based on status
            when (state.status) {
                NotificationStatus.DOWNLOADING -> {
                    builder.setProgress(100, (state.progress * 100).toInt(), state.progress == 0f)
                }
                NotificationStatus.COMPLETED -> {
                    // Show 100% completed progress, then remove
                    builder.setProgress(0, 0, false)
                }
                else -> {
                    // For failed/cancelled/paused/queued, show indeterminate or no progress
                    builder.setProgress(0, 0, false)
                }
            }

            // Add content intent
            val contentIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            builder.setContentIntent(contentIntent)

            // Add actions based on status
            when (state.status) {
                NotificationStatus.DOWNLOADING -> {
                    // Cancel action
                    val cancelIntent = createPendingIntent(ACTION_CANCEL, state.taskId)
                    builder.addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Cancel",
                        cancelIntent,
                    )
                }
                NotificationStatus.FAILED -> {
                    // Retry action
                    val retryIntent = createPendingIntent(ACTION_RETRY, state.taskId)
                    builder.addAction(
                        android.R.drawable.ic_menu_rotate,
                        "Retry",
                        retryIntent,
                    )
                }
                NotificationStatus.COMPLETED -> {
                    // Open action
                    val openIntent = createPendingIntent(ACTION_OPEN, state.taskId)
                    builder.addAction(
                        android.R.drawable.ic_menu_upload,
                        "Open",
                        openIntent,
                    )
                }
                else -> {}
            }

            return builder.build()
        }

        private fun createPendingIntent(
            action: String,
            taskId: String,
        ): PendingIntent {
            val intent =
                Intent(context, DownloadNotificationReceiver::class.java).apply {
                    this.action = action
                    putExtra("task_id", taskId)
                }
            return PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }

/**
 * Data class for notification state
 */
data class NotificationState(
    val taskId: String,
    val title: String,
    val uploader: String? = null,
    val progress: Float = 0f,
    val progressText: String = "",
    val status: NotificationStatus = NotificationStatus.DOWNLOADING,
    val fileSize: Long? = null,
    val downloadedBytes: Long? = null,
    val speed: Long? = null,
    val eta: Int? = null,
)

/**
 * Notification status enum
 */
enum class NotificationStatus {
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED,
    PAUSED,
    QUEUED,
}
