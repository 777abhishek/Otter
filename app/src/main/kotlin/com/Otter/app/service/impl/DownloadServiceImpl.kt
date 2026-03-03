package com.Otter.app.service.impl

import com.Otter.app.data.auth.CookieAuthStore
import com.Otter.app.data.models.AudioMode
import com.Otter.app.data.models.AudioQuality
import com.Otter.app.data.models.DownloadProgress
import com.Otter.app.data.models.DownloadSettings
import com.Otter.app.data.models.DownloadStatus
import com.Otter.app.data.models.DownloadTask
import com.Otter.app.data.models.DownloadVideoFormat
import com.Otter.app.data.ytdlp.YtDlpManager
import com.Otter.app.service.DownloadService
import com.Otter.app.service.SettingsService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadServiceImpl
    @Inject
    constructor(
        private val ytDlpManager: YtDlpManager,
        private val cookieAuthStore: CookieAuthStore,
        private val settingsService: SettingsService,
    ) : DownloadService {
        private val _downloads = MutableStateFlow<List<DownloadTask>>(emptyList())
        private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)

        override fun getDownloads(): Flow<List<DownloadTask>> = _downloads.asStateFlow()

        override fun getDownloadById(id: String): Flow<DownloadTask?> {
            return _downloads.map { downloads -> downloads.find { task -> task.id == id } }
        }

        override fun getDownloadProgress(id: String): Flow<DownloadProgress> {
            return _downloadProgress
                .filterNotNull()
                .filter { progress -> progress.taskId == id }
        }

        override suspend fun startDownload(
            videoId: String,
            title: String,
            thumbnail: String,
        ): Result<String> {
            return try {
                val settings = settingsService.getSettings().first()
                val taskId = "task_${System.currentTimeMillis()}"
                val outputPath = ytDlpManager.getOutputDirectory()

                val task =
                    DownloadTask(
                        id = taskId,
                        videoId = videoId,
                        title = title,
                        thumbnail = thumbnail,
                        status = DownloadStatus.DOWNLOADING,
                        progress = 0f,
                        downloadedBytes = 0,
                        totalBytes = 0,
                        speed = 0,
                        filePath = outputPath,
                        createdAt = System.currentTimeMillis(),
                    )

                _downloads.value = _downloads.value + task

                val url = "https://www.youtube.com/watch?v=$videoId"

                val cookiesFilePath = cookieAuthStore.getCookiesFilePathOnceForUrl(url, requireEnabled = true)

                val result =
                    when (settings.audioMode) {
                        AudioMode.AUDIO_ONLY -> {
                            ytDlpManager.downloadAudio(
                                url = url,
                                outputPath = outputPath,
                                audioFormat = "mp3",
                                audioQuality = audioQualityToYtDlp(settings.defaultAudioQuality),
                                cookiesFilePath = cookiesFilePath,
                                onProgress = { progress, _, _ ->
                                    updateProgress(taskId, progress)
                                },
                            )
                        }
                        AudioMode.VIDEO -> {
                            ytDlpManager.downloadVideo(
                                url = url,
                                outputPath = outputPath,
                                format = downloadFormatToYtDlp(settings.defaultDownloadFormat),
                                cookiesFilePath = cookiesFilePath,
                                onProgress = { progress, _, _ ->
                                    updateProgress(taskId, progress)
                                },
                            )
                        }
                    }

                result

                Result.success(taskId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun startDownloadFromUrl(url: String): Result<String> {
            return try {
                val settings = settingsService.getSettings().first()
                val taskId = "task_${System.currentTimeMillis()}"
                val outputPath = ytDlpManager.getOutputDirectory()

                val task =
                    DownloadTask(
                        id = taskId,
                        videoId = url,
                        title = url,
                        thumbnail = "",
                        status = DownloadStatus.DOWNLOADING,
                        progress = 0f,
                        downloadedBytes = 0,
                        totalBytes = 0,
                        speed = 0,
                        filePath = outputPath,
                        createdAt = System.currentTimeMillis(),
                    )
                _downloads.value = _downloads.value + task

                val cookiesFilePath = cookieAuthStore.getCookiesFilePathOnceForUrl(url, requireEnabled = true)

                val result =
                    when (settings.audioMode) {
                        AudioMode.AUDIO_ONLY -> {
                            ytDlpManager.downloadAudio(
                                url = url,
                                outputPath = outputPath,
                                audioFormat = "mp3",
                                audioQuality = audioQualityToYtDlp(settings.defaultAudioQuality),
                                cookiesFilePath = cookiesFilePath,
                                onProgress = { progress, _, _ ->
                                    updateProgress(taskId, progress)
                                },
                            )
                        }
                        AudioMode.VIDEO -> {
                            ytDlpManager.downloadVideo(
                                url = url,
                                outputPath = outputPath,
                                format = downloadFormatToYtDlp(settings.defaultDownloadFormat),
                                cookiesFilePath = cookiesFilePath,
                                onProgress = { progress, _, _ ->
                                    updateProgress(taskId, progress)
                                },
                            )
                        }
                    }

                result.map { taskId }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        private fun updateProgress(
            taskId: String,
            progress: Float,
        ) {
            _downloads.value =
                _downloads.value.map {
                    if (it.id == taskId) {
                        it.copy(progress = progress)
                    } else {
                        it
                    }
                }

            _downloadProgress.value =
                DownloadProgress(
                    taskId = taskId,
                    progress = progress,
                    downloadedBytes = (progress * 1000000).toLong(),
                    totalBytes = 1000000,
                    speed = 0,
                    eta = 0,
                )
        }

        private fun downloadFormatToYtDlp(format: DownloadVideoFormat): String {
            return when (format) {
                DownloadVideoFormat.MP4_360P -> "bestvideo[ext=mp4][height<=360]+bestaudio[ext=m4a]/best[height<=360]"
                DownloadVideoFormat.MP4_480P -> "bestvideo[ext=mp4][height<=480]+bestaudio[ext=m4a]/best[height<=480]"
                DownloadVideoFormat.MP4_720P -> "bestvideo[ext=mp4][height<=720]+bestaudio[ext=m4a]/best[height<=720]"
                DownloadVideoFormat.MP4_1080P -> "bestvideo[ext=mp4][height<=1080]+bestaudio[ext=m4a]/best[height<=1080]"
                DownloadVideoFormat.WEBM -> "bestvideo[ext=webm]+bestaudio[ext=webm]/best[ext=webm]/best"
                DownloadVideoFormat.MP3_AUDIO -> "bestaudio"
            }
        }

        private fun audioQualityToYtDlp(quality: AudioQuality): String {
            return when (quality) {
                AudioQuality.KBPS_96 -> "96k"
                AudioQuality.KBPS_128 -> "128k"
                AudioQuality.KBPS_192 -> "192k"
                AudioQuality.KBPS_256 -> "256k"
                AudioQuality.KBPS_320 -> "320k"
            }
        }

        override suspend fun pauseDownload(id: String): Result<Unit> {
            return try {
                ytDlpManager.cancelDownload()
                _downloads.value =
                    _downloads.value.map {
                        if (it.id == id) {
                            it.copy(status = DownloadStatus.PAUSED)
                        } else {
                            it
                        }
                    }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun resumeDownload(id: String): Result<Unit> {
            return try {
                _downloads.value =
                    _downloads.value.map {
                        if (it.id == id) {
                            it.copy(status = DownloadStatus.DOWNLOADING)
                        } else {
                            it
                        }
                    }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun cancelDownload(id: String): Result<Unit> {
            return try {
                ytDlpManager.cancelDownload()
                _downloads.value = _downloads.value.filter { it.id != id }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun retryDownload(id: String): Result<Unit> {
            return try {
                _downloads.value =
                    _downloads.value.map {
                        if (it.id == id) {
                            it.copy(status = DownloadStatus.DOWNLOADING, progress = 0f)
                        } else {
                            it
                        }
                    }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun updateSettings(settings: DownloadSettings): Result<Unit> {
            return Result.success(Unit)
        }

        override fun getSettings(): Flow<DownloadSettings> {
            return MutableStateFlow(DownloadSettings()).asStateFlow()
        }

        override suspend fun clearCompletedDownloads(): Result<Unit> {
            _downloads.value = _downloads.value.filter { it.status != DownloadStatus.COMPLETED }
            return Result.success(Unit)
        }

        override suspend fun clearAllDownloads(): Result<Unit> {
            _downloads.value = emptyList()
            return Result.success(Unit)
        }
    }
