package com.Otter.app.data.download

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.core.content.ContextCompat
import com.Otter.app.data.database.dao.DownloadTaskDao
import com.Otter.app.data.download.Task.RestartableAction
import com.Otter.app.data.models.AppSettings
import com.Otter.app.service.DownloadForegroundService
import com.Otter.app.service.NotificationManager
import com.Otter.app.service.NotificationStatus
import com.Otter.app.service.SettingsService
import com.Otter.app.util.FileLogger
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

private const val TAG = "DownloaderImpl"

@OptIn(FlowPreview::class)
@Singleton
class DownloaderImpl
    @Inject
    constructor(
        @ApplicationContext private val appContext: Context,
        private val engine: DownloadEngine,
        private val fileLogger: FileLogger,
        private val notificationManager: NotificationManager,
        private val downloadTaskDao: DownloadTaskDao,
        private val settingsService: SettingsService,
    ) : Downloader {
        private data class ParsedProgressMeta(
            val speedBytesPerSec: Long? = null,
            val etaSeconds: Int? = null,
        )

        private fun stripAnsiCodes(s: String): String {
            return s.replace(Regex("\\u001B\\[[;\\d]*m"), "")
        }

        private fun parseEtaSeconds(line: String): Int? {
            val cleaned = stripAnsiCodes(line)
            val match = Regex("ETA\\s+([0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?)").find(cleaned) ?: return null
            val token = match.groupValues.getOrNull(1)?.trim().orEmpty()
            if (token.isBlank()) return null
            val parts = token.split(":")
            return when (parts.size) {
                2 -> {
                    val m = parts[0].toIntOrNull() ?: return null
                    val s = parts[1].toIntOrNull() ?: return null
                    m * 60 + s
                }
                3 -> {
                    val h = parts[0].toIntOrNull() ?: return null
                    val m = parts[1].toIntOrNull() ?: return null
                    val s = parts[2].toIntOrNull() ?: return null
                    h * 3600 + m * 60 + s
                }
                else -> null
            }
        }

        private fun parseSpeedBytesPerSec(line: String): Long? {
            val cleaned = stripAnsiCodes(line)
            val match =
                Regex("at\\s+(~?\\d+(?:\\.\\d+)?)\\s*([KMGT]?i?B)/s", RegexOption.IGNORE_CASE).find(cleaned)
                    ?: return null
            val value = match.groupValues.getOrNull(1)?.replace("~", "")?.toDoubleOrNull() ?: return null
            val unit = match.groupValues.getOrNull(2)?.lowercase().orEmpty()
            val multiplier =
                when (unit) {
                    "b" -> 1.0
                    "kb" -> 1000.0
                    "mb" -> 1000.0 * 1000.0
                    "gb" -> 1000.0 * 1000.0 * 1000.0
                    "tb" -> 1000.0 * 1000.0 * 1000.0 * 1000.0
                    "kib" -> 1024.0
                    "mib" -> 1024.0 * 1024.0
                    "gib" -> 1024.0 * 1024.0 * 1024.0
                    "tib" -> 1024.0 * 1024.0 * 1024.0 * 1024.0
                    else -> return null
                }
            return (value * multiplier).roundToLong()
        }

        private fun parseProgressMeta(line: String): ParsedProgressMeta {
            return ParsedProgressMeta(
                speedBytesPerSec = parseSpeedBytesPerSec(line),
                etaSeconds = parseEtaSeconds(line),
            )
        }

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val taskStateMap = mutableStateMapOf<Task, Task.State>()
        private val stateSnapshotFlow = snapshotFlow { taskStateMap.toMap() }

        private val backupStore = TaskBackupStore(appContext)

        @Volatile
        private var latestSettings: AppSettings = AppSettings()

        private fun getMaxConcurrency(): Int {
            val base = latestSettings.maxConcurrentDownloads.coerceIn(1, 10)
            return when {
                latestSettings.powerSaverEnabled && latestSettings.lowPowerMode -> 1
                latestSettings.powerSaverEnabled || latestSettings.lowPowerMode -> base.coerceAtMost(2)
                else -> base
            }
        }

        private fun isOnWifi(): Boolean {
            val cm =
                appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        }

        init {
            scope.launch(Dispatchers.IO) {
                settingsService.getSettings().collect { latestSettings = it }
            }

            scope.launch(Dispatchers.IO) {
                enqueueFromBackup()

                stateSnapshotFlow
                    .map { it.filter { (_, state) -> state.downloadState !is Task.DownloadState.Completed } }
                    .distinctUntilChanged()
                    .collect { backupStore.writeBackup(it) }
            }

            scope.launch(Dispatchers.Default) {
                stateSnapshotFlow
                    .onEach { doYourWork() }
                    .map { it.countRunning() }
                    .distinctUntilChanged()
                    .collect { /* no-op: Otter handles foreground service elsewhere */ }
            }

            scope.launch(Dispatchers.IO) {
                loadCompletedDownloadsFromDatabase()
            }
        }

        private suspend fun loadCompletedDownloadsFromDatabase() {
            try {
                val completedTasks = downloadTaskDao.getTasksByStatus("COMPLETED")
                completedTasks.collect { entities ->
                    entities.forEach { entity ->
                        val task =
                            Task(
                                url = entity.url,
                                preferences = DownloadPreferences(),
                                id = entity.id,
                            )
                        task.downloadState = Task.DownloadState.Completed(entity.filePath ?: "")
                        taskStateMap[task] =
                            Task.State(
                                downloadState = task.downloadState,
                                videoInfo = null,
                                viewState =
                                    Task.ViewState(
                                        title = entity.title,
                                        uploader = "",
                                        thumbnailUrl = entity.thumbnailUrl,
                                        fileSizeApprox = entity.totalBytes.toDouble(),
                                        duration = entity.duration?.toIntOrNull() ?: 0,
                                    ),
                            )
                    }
                    fileLogger.log(TAG, "Loaded ${entities.size} completed downloads from database")
                }
            } catch (e: Exception) {
                fileLogger.logError(TAG, "Failed to load completed downloads from database", e)
            }
        }

        private fun enqueueFromBackup() {
            val tasks =
                backupStore.readBackup()
                    .mapValuesOrEmptyState()
                    .filter { it.state.downloadState !is Task.DownloadState.Completed }

            tasks.forEach { enqueue(it) }
        }

        private fun List<TaskFactory.TaskWithState>.mapValuesOrEmptyState(): List<TaskFactory.TaskWithState> {
            return map { (task, state) ->
                val preState = state.downloadState
                val downloadState =
                    when (preState) {
                        is Task.DownloadState.FetchingInfo,
                        Task.DownloadState.Idle,
                        -> {
                            Task.DownloadState.Canceled(action = Task.RestartableAction.FetchInfo)
                        }
                        is Task.DownloadState.Running -> {
                            Task.DownloadState.Canceled(action = Task.RestartableAction.Download, progress = preState.progress)
                        }
                        Task.DownloadState.ReadyWithInfo -> {
                            Task.DownloadState.Canceled(action = Task.RestartableAction.Download, progress = null)
                        }
                        else -> preState
                    }
                TaskFactory.TaskWithState(task, state.copy(downloadState = downloadState))
            }
        }

        private fun Map<Task, Task.State>.countRunning(): Int =
            count { (_, state) ->
                state.downloadState is Task.DownloadState.Running || state.downloadState is Task.DownloadState.FetchingInfo
            }

        override fun getTaskStateMap(): SnapshotStateMap<Task, Task.State> = taskStateMap

        override fun enqueue(task: Task) {
            Snapshot.withMutableSnapshot {
                taskStateMap += task to
                    Task.State(
                        downloadState = Task.DownloadState.Idle,
                        videoInfo = null,
                        viewState = Task.ViewState(url = task.url, title = task.url),
                    )
            }
        }

        override fun enqueue(
            task: Task,
            state: Task.State,
        ) {
            Snapshot.withMutableSnapshot {
                taskStateMap += task to state
            }
        }

        override fun remove(task: Task): Boolean {
            if (taskStateMap.contains(task)) {
                Snapshot.withMutableSnapshot {
                    taskStateMap.remove(task)
                }
                return true
            }
            return false
        }

        override fun cancel(task: Task): Boolean = task.cancelImpl()

        override fun restart(task: Task) {
            task.restartImpl()
        }

        private var Task.state: Task.State
            get() = taskStateMap[this]!!
            set(value) {
                Snapshot.withMutableSnapshot {
                    taskStateMap[this] = value
                }
            }

        private var Task.downloadState: Task.DownloadState
            get() = state.downloadState
            set(value) {
                val prev = state
                Snapshot.withMutableSnapshot {
                    taskStateMap[this] = prev.copy(downloadState = value)
                }
            }

        private var Task.info: VideoInfo?
            get() = state.videoInfo
            set(value) {
                val prev = state
                Snapshot.withMutableSnapshot {
                    taskStateMap[this] = prev.copy(videoInfo = value)
                }
            }

        private var Task.viewState: Task.ViewState
            get() = state.viewState
            set(value) {
                val prev = state
                Snapshot.withMutableSnapshot {
                    taskStateMap[this] = prev.copy(viewState = value)
                }
            }

        private fun doYourWork() {
            val max = getMaxConcurrency()
            val running = taskStateMap.toMap().countRunning()
            val availableSlots = (max - running).coerceAtLeast(0)
            if (availableSlots <= 0) return

            taskStateMap.entries
                .sortedBy { (_, state) -> state.downloadState }
                .filter { (_, state) ->
                    state.downloadState == Task.DownloadState.ReadyWithInfo || state.downloadState == Task.DownloadState.Idle
                }
                .take(availableSlots)
                .forEach { (task, state) ->
                    when (state.downloadState) {
                        Task.DownloadState.Idle -> task.prepare()
                        Task.DownloadState.ReadyWithInfo -> task.download()
                        else -> Unit
                    }
                }
        }

        private fun Task.prepare() {
            check(downloadState == Task.DownloadState.Idle)
            fetchInfo()
        }

        private fun Task.fetchInfo() {
            check(downloadState == Task.DownloadState.Idle)
            val task = this

            fileLogger.log(TAG, "Fetching info for task: ${task.url}")

            val job =
                scope.launch(Dispatchers.Default) {
                    engine.fetchVideoInfo(
                        url = task.url,
                        taskKey = task.id,
                        preferences = task.preferences.copy(extractAudio = task.preferences.extractAudio),
                        playlistIndex = if (task.preferences.downloadPlaylist) 1 else null,
                    ).onSuccess { fetchedInfo ->
                        fileLogger.log(TAG, "Successfully fetched info: ${fetchedInfo.title}")
                        task.info = fetchedInfo
                        task.downloadState = Task.DownloadState.ReadyWithInfo
                        task.viewState = Task.ViewState.fromVideoInfo(fetchedInfo)
                    }.onFailure { throwable ->
                        if (throwable is YoutubeDL.CanceledException) {
                            fileLogger.log(TAG, "Fetch info canceled for: ${task.url}")
                            return@onFailure
                        }
                        fileLogger.logError(TAG, "Failed to fetch info for ${task.url}", throwable)
                        task.downloadState = Task.DownloadState.Error(throwable = throwable, action = Task.RestartableAction.FetchInfo)
                    }
                }

            downloadState = Task.DownloadState.FetchingInfo(job = job, taskId = task.id)
        }

        private fun Task.download() {
            check(downloadState == Task.DownloadState.ReadyWithInfo && info != null)

            val task = this
            fileLogger.log(TAG, "Starting download for: ${info?.title ?: task.url}")

            if (latestSettings.wifiOnlyDownloads && !isOnWifi()) {
                val error = IllegalStateException("Wi-Fi only downloads enabled")
                fileLogger.logError(TAG, "Download blocked: Wi-Fi only enabled", error)
                downloadState =
                    Task.DownloadState.Error(
                        throwable = error,
                        action = Task.RestartableAction.Download,
                    )
                return
            }

            // Resolve output dir early so we can decide if storage permission is required.
            val outputDir = getOutputDirectory(task.preferences)

            // Check storage permission (only required for non-app-specific paths)
            if (requiresStoragePermission(outputDir) && !hasStoragePermission()) {
                val error = SecurityException("Storage permission required")
                fileLogger.logError(TAG, "Storage permission not granted", error)
                downloadState =
                    Task.DownloadState.Error(
                        throwable = error,
                        action = Task.RestartableAction.Download,
                    )
                return
            }

            // Start foreground service for background downloads
            val serviceIntent =
                Intent(appContext, DownloadForegroundService::class.java).apply {
                    action = DownloadForegroundService.ACTION_START
                }
            appContext.startService(serviceIntent)

            val job =
                scope.launch(context = Dispatchers.Default, start = CoroutineStart.LAZY) {
                    kotlin.runCatching {
                        engine.download(
                            task = task,
                            info = info!!,
                            outputDir = outputDir,
                        ) { progress, text ->
                            fileLogger.log(TAG, "DownloaderImpl callback - progress: $progress, text: ${text.take(100)}")
                            when (val preState = task.downloadState) {
                                is Task.DownloadState.Running -> {
                                    task.downloadState = preState.copy(progress = progress, progressText = text)
                                    fileLogger.log(TAG, "Updated task state - progress: $progress, text: ${text.take(100)}")

                                    val meta = kotlin.runCatching { parseProgressMeta(text) }.getOrDefault(ParsedProgressMeta())

                                    // Update notification with progress
                                    notificationManager.updateNotification(
                                        taskId = task.id,
                                        title = info?.title ?: task.url,
                                        uploader = info?.uploader,
                                        progress = progress,
                                        progressText = text,
                                        status = NotificationStatus.DOWNLOADING,
                                        speed = meta.speedBytesPerSec,
                                        eta = meta.etaSeconds,
                                    )
                                }
                                else -> {
                                    fileLogger.log(
                                        TAG,
                                        "Task not in Running state, skipping progress update. State: ${preState::class.simpleName}",
                                    )
                                }
                            }
                        }
                    }.onSuccess { filePath ->
                        fileLogger.log(TAG, "Download completed: ${info?.title}")
                        task.downloadState = Task.DownloadState.Completed(filePath)

                        // Save completed download to database
                        scope.launch {
                            val downloadTaskEntity =
                                com.Otter.app.data.database.entities.DownloadTaskEntity(
                                    id = task.id,
                                    url = task.url,
                                    title = info?.title ?: task.url,
                                    thumbnailUrl = info?.thumbnailUrl,
                                    duration = info?.duration?.toString() ?: "",
                                    progress = 100f,
                                    speed = null,
                                    eta = null,
                                    totalBytes = 0L,
                                    downloadedBytes = 0L,
                                    status = "COMPLETED",
                                    error = null,
                                    filePath = filePath,
                                    formatId = null,
                                    expectedSize = null,
                                    addedDate = System.currentTimeMillis().toString(),
                                    artist = null,
                                    album = null,
                                    genre = null,
                                    uploadDate = null,
                                    description = null,
                                    embeddedMetadata = null,
                                )
                            downloadTaskDao.insertTask(downloadTaskEntity)
                        }

                        // Update notification to completed state
                        notificationManager.updateNotification(
                            taskId = task.id,
                            title = info?.title ?: task.url,
                            uploader = info?.uploader,
                            status = NotificationStatus.COMPLETED,
                        )

                        // Scan file to media library if not in private mode
                        if (!task.preferences.privateMode && filePath != null) {
                            scanFileToMediaLibrary(filePath)
                        }

                        // Stop foreground service when download completes
                        val stopIntent =
                            Intent(appContext, DownloadForegroundService::class.java).apply {
                                action = DownloadForegroundService.ACTION_STOP
                            }
                        appContext.startService(stopIntent)
                    }.onFailure { throwable ->
                        if (throwable is YoutubeDL.CanceledException) {
                            fileLogger.log(TAG, "Download canceled: ${info?.title}")
                            task.downloadState = Task.DownloadState.Canceled(action = Task.RestartableAction.Download)

                            // Update notification to cancelled state
                            notificationManager.updateNotification(
                                taskId = task.id,
                                title = info?.title ?: task.url,
                                uploader = info?.uploader,
                                status = NotificationStatus.CANCELLED,
                            )

                            // Stop foreground service when download is cancelled
                            val stopIntent =
                                Intent(appContext, DownloadForegroundService::class.java).apply {
                                    action = DownloadForegroundService.ACTION_STOP
                                }
                            appContext.startService(stopIntent)
                            return@onFailure
                        }
                        fileLogger.logError(TAG, "Download failed: ${info?.title}", throwable)
                        task.downloadState = Task.DownloadState.Error(throwable = throwable, action = Task.RestartableAction.Download)

                        // Update notification to failed state
                        notificationManager.updateNotification(
                            taskId = task.id,
                            title = info?.title ?: task.url,
                            uploader = info?.uploader,
                            status = NotificationStatus.FAILED,
                        )

                        // Stop foreground service when download fails
                        val stopIntent =
                            Intent(appContext, DownloadForegroundService::class.java).apply {
                                action = DownloadForegroundService.ACTION_STOP
                            }
                        appContext.startService(stopIntent)
                    }
                }

            downloadState = Task.DownloadState.Running(job = job, taskId = task.id)
            job.start()
        }

        private fun getOutputDirectory(preferences: DownloadPreferences): String {
            val path = preferences.customDownloadPath
            val baseDir =
                if (path.isBlank()) {
                    // Default to system Downloads/Otter folder
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (downloadsDir != null) {
                        File(downloadsDir, "Otter")
                    } else {
                        // Fallback to app-specific directory if public Downloads not available
                        val appDownloadsDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        if (appDownloadsDir != null) {
                            File(appDownloadsDir, "Otter")
                        } else {
                            File(appContext.filesDir, "downloads/Otter")
                        }
                    }
                } else {
                    File(path)
                }

            baseDir.mkdirs()
            return baseDir.absolutePath
        }

        private fun requiresStoragePermission(outputDir: String): Boolean {
            val out = File(outputDir)
            val appExternal = appContext.getExternalFilesDir(null)
            val appExternalPath = appExternal?.absolutePath
            val outputPath = kotlin.runCatching { out.canonicalPath }.getOrDefault(out.absolutePath)

            // App-specific external storage does not require storage permission on any Android version.
            if (appExternalPath != null && outputPath.startsWith(appExternalPath)) return false

            // Non-app-specific paths:
            // - Android 11+ needs "All files access" (MANAGE_EXTERNAL_STORAGE) for direct File I/O.
            // - Android 10 and below need legacy WRITE_EXTERNAL_STORAGE.
            return true
        }

        private fun hasStoragePermission(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        private fun scanFileToMediaLibrary(filePath: String) {
            // Notify media scanner about the new file
            // This would typically use MediaScannerConnection or similar
            fileLogger.log(TAG, "Scanning file to media library: $filePath")
        }

        private fun Task.cancelImpl(): Boolean {
            when (val preState = downloadState) {
                is Task.DownloadState.Cancelable -> {
                    val res = YoutubeDL.destroyProcessById(preState.taskId)
                    if (res) {
                        preState.job.cancel()
                        val progress = if (preState is Task.DownloadState.Running) preState.progress else null
                        downloadState = Task.DownloadState.Canceled(action = preState.action, progress = progress)
                    }
                    return res
                }
                Task.DownloadState.Idle -> {
                    downloadState = Task.DownloadState.Canceled(action = Task.RestartableAction.FetchInfo)
                }
                Task.DownloadState.ReadyWithInfo -> {
                    downloadState = Task.DownloadState.Canceled(action = Task.RestartableAction.Download)
                }
                else -> return false
            }
            return true
        }

        private fun Task.restartImpl() {
            when (val preState = downloadState) {
                is Task.DownloadState.Restartable -> {
                    downloadState =
                        when (preState.action) {
                            Task.RestartableAction.Download -> Task.DownloadState.ReadyWithInfo
                            Task.RestartableAction.FetchInfo -> Task.DownloadState.Idle
                        }
                }
                else -> throw IllegalStateException()
            }
        }
    }
