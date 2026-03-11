package com.Otter.app.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.Otter.app.R
import com.Otter.app.util.DynamicIconProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service for background downloads
 * Keeps downloads running even when the app is in the background
 */
@AndroidEntryPoint
class DownloadForegroundService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "download_channel"
        private const val CHANNEL_NAME = "Downloads"
        const val ACTION_START = "com.Otter.app.action.START_DOWNLOAD"
        const val ACTION_STOP = "com.Otter.app.action.STOP_DOWNLOAD"
    }

    @Inject
    lateinit var notificationManager: NotificationManager

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isServiceRunning = false

    inner class LocalBinder : Binder() {
        fun getService(): DownloadForegroundService = this@DownloadForegroundService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        // CRITICAL: Must call startForeground() within 5 seconds of startForegroundService()
        // Do this BEFORE any early returns to avoid ForegroundServiceDidNotStartInTimeException
        if (!isServiceRunning) {
            startForegroundServiceInternal()
        }

        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundService()
            }
        }
        return START_STICKY
    }

    private fun canPostNotifications(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun startForegroundServiceInternal() {
        // Create notification channel
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel =
                android.app.NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    android.app.NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Shows download progress in background"
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                }
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // Create initial notification
        val notification = createInitialNotification()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        isServiceRunning = true

        // Keep the foreground notification updated with real download progress.
        observeNotificationState()
    }

    private fun stopForegroundService() {
        if (!isServiceRunning) return

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        isServiceRunning = false
    }

    private fun createInitialNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon_light)
            .setContentTitle("Download Service")
            .setContentText("Background download service running")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    /**
     * Observe notification state and update foreground notification
     */
    fun observeNotificationState() {
        serviceScope.launch {
            notificationManager.notificationState.collect { state ->
                state?.let {
                    updateForegroundNotification(it)
                }
            }
        }
    }

    private fun updateForegroundNotification(state: com.Otter.app.service.NotificationState) {
        val notification = notificationManager.buildNotification(state)
        val notificationManager = getSystemService(android.app.NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
