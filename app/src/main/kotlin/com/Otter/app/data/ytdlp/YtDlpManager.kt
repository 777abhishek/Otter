package com.Otter.app.data.ytdlp

import android.content.Context
import com.Otter.app.util.FileLogger
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val fileLogger: FileLogger,
        private val core: YtDlpCore,
        private val mediaClient: YtDlpMediaClient,
        private val syncClient: YtDlpSyncClient,
    ) {
        companion object {
            private const val TAG = "YtDlpManager"
        }

        suspend fun initialize(): Result<Unit> = core.initialize()

        suspend fun updateYtDlp(): Result<String> =
            withContext(Dispatchers.IO) {
                fileLogger.log(TAG, "Updating yt-dlp...")
                try {
                    val request =
                        com.yausername.youtubedl_android.YoutubeDLRequest("https://www.youtube.com").apply {
                            addOption("--update")
                        }
                    val response = core.execute(request)
                    val output = response.out.trim()
                    fileLogger.log(TAG, "yt-dlp update output: $output")
                    Result.success(output.ifBlank { "yt-dlp updated successfully" })
                } catch (e: Exception) {
                    fileLogger.logError(TAG, "Failed to update yt-dlp", e)
                    Result.failure(e)
                }
            }

        suspend fun getVersion(): Result<String> =
            withContext(Dispatchers.IO) {
                fileLogger.log(TAG, "Getting yt-dlp version")
                try {
                    val request =
                        com.yausername.youtubedl_android.YoutubeDLRequest("https://www.youtube.com").apply {
                            addOption("--version")
                        }
                    fileLogger.log(TAG, "Executing version request")
                    val response = core.execute(request)
                    val version = response.out.trim()
                    fileLogger.log(TAG, "yt-dlp version: $version")
                    if (version.isBlank()) {
                        val errMsg = response.err.ifBlank { "Unable to determine yt-dlp version" }
                        fileLogger.logError(TAG, errMsg, null)
                        Result.failure(IllegalStateException(errMsg))
                    } else {
                        Result.success(version)
                    }
                } catch (e: Exception) {
                    fileLogger.logError(TAG, "Failed to get yt-dlp version", e)
                    Result.failure(e)
                }
            }

        suspend fun downloadVideo(
            url: String,
            outputPath: String,
            format: String = "bestvideo*+bestaudio/best",
            cookiesFilePath: String? = null,
            onProgress: (Float, String, String) -> Unit = { _, _, _ -> },
        ): Result<String> {
            val startTime = System.currentTimeMillis()
            fileLogger.log(TAG, "Downloading video: $url to $outputPath with format: $format")
            return mediaClient.downloadVideo(url, outputPath, format, cookiesFilePath, onProgress)
                .onSuccess {
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Video download completed in ${duration}ms")
                }
                .onFailure { e ->
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.logError(TAG, "Video download failed after ${duration}ms", e)
                }
        }

        suspend fun downloadAudio(
            url: String,
            outputPath: String,
            format: String = "bestaudio",
            audioFormat: String = "mp3",
            audioQuality: String = "192k",
            cookiesFilePath: String? = null,
            onProgress: (Float, String, String) -> Unit = { _, _, _ -> },
        ): Result<String> {
            val startTime = System.currentTimeMillis()
            fileLogger.log(TAG, "Downloading audio: $url to $outputPath with format: $audioFormat quality: $audioQuality")
            return mediaClient.downloadAudio(url, outputPath, format, audioFormat, audioQuality, cookiesFilePath, onProgress)
                .onSuccess {
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Audio download completed in ${duration}ms")
                }
                .onFailure { e ->
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.logError(TAG, "Audio download failed after ${duration}ms", e)
                }
        }

        suspend fun getVideoInfo(
            url: String,
            cookiesFilePath: String? = null,
        ): Result<Map<String, Any>> {
            val startTime = System.currentTimeMillis()
            fileLogger.log(TAG, "Getting video info: $url")
            return mediaClient.getVideoInfo(url, cookiesFilePath)
                .onSuccess {
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Video info retrieved in ${duration}ms")
                }
                .onFailure { e ->
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.logError(TAG, "Failed to get video info after ${duration}ms", e)
                }
        }

        suspend fun getFormats(
            url: String,
            cookiesFilePath: String? = null,
        ): Result<List<Map<String, Any>>> {
            val startTime = System.currentTimeMillis()
            fileLogger.log(TAG, "Getting formats: $url")
            return mediaClient.getFormats(url, cookiesFilePath)
                .onSuccess { formats ->
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Successfully retrieved ${formats.size} formats in ${duration}ms")
                }
                .onFailure { e ->
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.logError(TAG, "Failed to get formats after ${duration}ms", e)
                }
        }

        suspend fun getStreamUrl(
            url: String,
            formatId: String,
            cookiesFilePath: String? = null,
        ): Result<String> {
            val startTime = System.currentTimeMillis()
            fileLogger.log(TAG, "Getting stream URL: $url formatId=$formatId")
            return mediaClient.getStreamUrl(url, formatId, cookiesFilePath)
                .onSuccess {
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Stream URL retrieved in ${duration}ms")
                }
                .onFailure { e ->
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.logError(TAG, "Failed to get stream URL after ${duration}ms", e)
                }
        }

        /**
         * Extract cookies from browser using yt-dlp's native --cookies-from-browser option.
         * This is the preferred method for authenticating with YouTube.
         */
        suspend fun extractCookiesFromBrowser(
            browser: String,
            outputFile: String,
        ): Result<String> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                fileLogger.log(TAG, "Extracting cookies from browser: $browser to $outputFile")
                try {
                    // Use yt-dlp to extract cookies from browser
                    val request =
                        YoutubeDLRequest("https://www.youtube.com").apply {
                            addOption("--cookies-from-browser", "$browser::youtube.com")
                            addOption("--cookies", outputFile)
                            addOption("--no-download")
                        }

                    core.execute(request)
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Successfully extracted cookies in ${duration}ms")
                    Result.success(outputFile)
                } catch (e: Exception) {
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.logError(TAG, "Failed to extract cookies after ${duration}ms", e)
                    Result.failure(e)
                }
            }

        suspend fun cancelDownload(): Result<Unit> =
            withContext(Dispatchers.IO) {
                fileLogger.log(TAG, "Canceling download")
                try {
                    // Note: YoutubeDL doesn't have a destroyProcess method
                    // Cancellation would need to be handled differently
                    fileLogger.log(TAG, "Download cancellation requested")
                    Result.success(Unit)
                } catch (e: Exception) {
                    fileLogger.logError(TAG, "Failed to cancel download", e)
                    Result.failure(e)
                }
            }

        fun getOutputDirectory(): String {
            val downloadDir = File(context.getExternalFilesDir(null), "Downloads")
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            return downloadDir.absolutePath
        }

        suspend fun extractSubscriptions(cookiesFilePath: String?): Result<List<com.Otter.app.data.models.Channel>> =
            syncClient.extractSubscriptions(cookiesFilePath)

        suspend fun extractPlaylists(cookiesFilePath: String?): Result<List<com.Otter.app.data.models.Playlist>> =
            syncClient.extractPlaylists(cookiesFilePath)

        suspend fun extractWatchLater(cookiesFilePath: String?): Result<List<com.Otter.app.data.models.Video>> =
            syncClient.extractWatchLater(cookiesFilePath)

        suspend fun extractLikedVideos(cookiesFilePath: String?): Result<List<com.Otter.app.data.models.Video>> =
            syncClient.extractLikedVideos(cookiesFilePath)

        suspend fun extractPlaylistVideos(
            playlistId: String,
            cookiesFilePath: String?,
        ): Result<List<com.Otter.app.data.models.Video>> = syncClient.extractPlaylistVideos(playlistId, cookiesFilePath)

        suspend fun extractPlaylistVideosChunk(
            playlistId: String,
            cookiesFilePath: String?,
            startIndex: Int,
            endIndex: Int,
        ): Result<List<com.Otter.app.data.models.Video>> =
            syncClient.extractPlaylistVideosChunk(
                playlistId = playlistId,
                cookiesFilePath = cookiesFilePath,
                startIndex = startIndex,
                endIndex = endIndex,
            )

        suspend fun extractPlaylistVideosFull(
            playlistId: String,
            cookiesFilePath: String?,
        ): Result<List<com.Otter.app.data.models.Video>> =
            syncClient.extractPlaylistVideosFull(playlistId, cookiesFilePath)

        suspend fun extractPlaylistVideosChunkFull(
            playlistId: String,
            cookiesFilePath: String?,
            startIndex: Int,
            endIndex: Int,
        ): Result<List<com.Otter.app.data.models.Video>> =
            syncClient.extractPlaylistVideosChunkFull(
                playlistId = playlistId,
                cookiesFilePath = cookiesFilePath,
                startIndex = startIndex,
                endIndex = endIndex,
            )

        fun getLogContent(): String = fileLogger.getLogContent()

        fun clearLog() = fileLogger.clearLog()
    }
