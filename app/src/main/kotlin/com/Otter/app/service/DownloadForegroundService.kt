package com.Otter.app.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
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
        private const val WAKE_LOCK_TAG = "Otter:DownloadWakeLock"
    }

    @Inject
    lateinit var notificationManager: NotificationManager

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isServiceRunning = false
    
    // WakeLock to keep CPU running when screen is off
    private var wakeLock: PowerManager.WakeLock? = null
    private val powerManager: PowerManager by lazy {
        getSystemService(Context.POWER_SERVICE) as PowerManager
    }

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

        // Acquire WakeLock to keep CPU running when screen is off
        acquireWakeLock()

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

    /**
     * Acquire a partial WakeLock to keep CPU running during downloads
     */
    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld == true) return
            
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                WAKE_LOCK_TAG
            ).apply {
                // Use reference counting for indefinite lock - will be released when service stops
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            // WakeLock acquisition failed, downloads will still work but may pause when screen off
        }
    }

    /**
     * Extend the WakeLock duration for long downloads
     */
    fun extendWakeLock(durationMs: Long = 10 * 60 * 1000L) {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.acquire(durationMs)
                } else {
                    acquireWakeLock()
                }
            }
        } catch (e: Exception) {
            // Ignore WakeLock errors
        }
    }

    /**
     * Release the WakeLock when downloads complete
     */
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            // Ignore WakeLock errors
        }
    }

    private fun stopForegroundService() {
        if (!isServiceRunning) return

        // Release WakeLock when service stops
        releaseWakeLock()
        
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
        releaseWakeLock()
        serviceScope.cancel()
    }
}
