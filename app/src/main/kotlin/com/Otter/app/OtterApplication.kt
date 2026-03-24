package com.Otter.app

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.Otter.app.data.newpipe.OkHttpNewPipeDownloader
import com.Otter.app.data.ytdlp.YtDlpManager
import com.Otter.app.network.AnalyticsService
import com.Otter.app.network.CrashReportService
import com.Otter.app.network.PrivacySyncService
import com.Otter.app.service.SettingsService
import com.Otter.app.service.StorageService
import com.Otter.app.util.CrashReportManager
import com.Otter.app.util.FileLogger
import com.Otter.app.util.PreferenceUtil
import com.Otter.app.work.PlaylistWorkScheduler
import com.Otter.app.work.SyncWorkScheduler
import com.Otter.app.work.UpdatesWorkScheduler
import dagger.hilt.android.HiltAndroidApp
import com.yausername.ffmpeg.FFmpeg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class OtterApplication : Application(), Configuration.Provider {
    companion object {
        lateinit var appContext: Context
    }

    @Inject
    lateinit var ytDlpManager: YtDlpManager

    @Inject
    lateinit var storageService: com.Otter.app.service.StorageService

    @Inject
    lateinit var settingsService: SettingsService

    @Inject
    lateinit var fileLogger: FileLogger

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var crashReportManager: CrashReportManager

    @Inject
    lateinit var crashReportService: CrashReportService

    @Inject
    lateinit var analyticsService: AnalyticsService

    @Inject
    lateinit var privacySyncService: PrivacySyncService

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

        // Move heavy initialization to background to speed up startup
        applicationScope.launch {
            // Initialize preferences
            PreferenceUtil.init(this@OtterApplication)

            // Sync privacy settings to backend on first run
            try {
                privacySyncService.syncPrivacySettings()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            installCrashHandler()

            val okHttpClient =
                OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()

            NewPipe.init(OkHttpNewPipeDownloader(okHttpClient))
        }

        // Initialize yt-dlp and FFmpeg in background
        applicationScope.launch {
            try {
                ytDlpManager.initialize()
                FFmpeg.init(this@OtterApplication)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Sync download folder with database on app startup
        applicationScope.launch {
            try {
                storageService.syncDownloadFolder()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Periodic background sync (subscriptions/playlists)
        applicationScope.launch {
            settingsService.getSettings()
                .map { it.backgroundSyncEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (enabled) {
                        SyncWorkScheduler.schedule(this@OtterApplication)
                    } else {
                        SyncWorkScheduler.cancel(this@OtterApplication)
                    }
                }
        }

        // Periodic background updates automation
        applicationScope.launch {
            settingsService.getSettings()
                .map { it.updatesAutomationEnabled to it.updatesAutomationInterval }
                .distinctUntilChanged()
                .collect { (enabled, interval) ->
                    if (enabled) {
                        val hours = if (interval == com.Otter.app.data.models.UpdatesAutomationInterval.WEEKLY) 24L * 7L else 24L
                        UpdatesWorkScheduler.schedule(this@OtterApplication, intervalHours = hours)
                    } else {
                        UpdatesWorkScheduler.cancel(this@OtterApplication)
                    }
                }
        }
    }

    private fun installCrashHandler() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            kotlin.runCatching {
                val enabled =
                    kotlinx.coroutines.runBlocking {
                        settingsService.getSettings().first().crashReportingEnabled
                    }
                if (enabled) {
                    fileLogger.logError("Crash", "Uncaught exception on thread ${thread.name}", throwable)
                    crashReportManager.writeCrash(throwable)

                    // Send to backend
                    applicationScope.launch {
                        crashReportService.sendCrashReport(
                            stackTrace = throwable.stackTraceToString(),
                            errorType = throwable.javaClass.simpleName,
                            errorMessage = throwable.message ?: "Unknown error",
                        )
                    }
                }
            }

            previousHandler?.uncaughtException(thread, throwable)
                ?: kotlin.runCatching { android.os.Process.killProcess(android.os.Process.myPid()) }
        }
    }

    // Public method to send analytics from other parts of the app
    fun sendAnalytics(
        eventType: String,
        eventName: String,
        eventData: Map<String, Any>? = null,
    ) {
        applicationScope.launch {
            val enabled =
                runBlocking {
                    settingsService.getSettings().first().analyticsEnabled
                }
            if (enabled) {
                analyticsService.sendAnalyticsEvent(eventType, eventName, eventData)
            }
        }
    }
}
