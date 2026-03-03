package com.Otter.app.data.ytdlp

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadUtil
    @Inject
    constructor(
        private val fileLogger: com.Otter.app.util.FileLogger,
    ) {
        companion object {
            private const val TAG = "DownloadUtil"
            const val BASENAME = "%(title).200B"
            const val EXTENSION = ".%(ext)s"
            const val OUTPUT_TEMPLATE_DEFAULT = BASENAME + EXTENSION
            const val OUTPUT_TEMPLATE_ID = "$BASENAME [%(id)s]$EXTENSION"
        }

        suspend fun fetchVideoInfo(
            url: String,
            context: Context,
        ): Result<Map<String, Any>> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                fileLogger.log(TAG, "Fetching video info: $url")
                try {
                    val youtubeDL = YoutubeDL.getInstance()
                    val request =
                        YoutubeDLRequest(url).apply {
                            addOption("--flat-playlist")
                            addOption("--dump-single-json")
                            addOption("-o $BASENAME")
                            addOption("-R 1")
                            addOption("--socket-timeout 5")
                        }

                    val response = youtubeDL.execute(request)
                    val json = com.google.gson.Gson().fromJson(response.out, Map::class.java) as Map<String, Any>
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Successfully fetched video info in ${duration}ms")
                    Result.success(json)
                } catch (e: Exception) {
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.logError(TAG, "Failed to fetch video info after ${duration}ms", e)
                    Result.failure(e)
                }
            }

        suspend fun downloadVideo(
            url: String,
            outputPath: String,
            format: String = "bestvideo*+bestaudio/best",
            cookies: String? = null,
            onProgress: (Float, String, String) -> Unit = { _, _, _ -> },
            context: Context,
        ): Result<String> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                fileLogger.log(TAG, "Downloading video: $url to $outputPath with format: $format")
                try {
                    val youtubeDL = YoutubeDL.getInstance()
                    val request =
                        YoutubeDLRequest(url).apply {
                            addOption("-f $format")
                            addOption("-o $outputPath/%(title)s.%(ext)s")
                            addOption("--no-playlist")
                            addOption("--newline")

                            cookies?.let {
                                addOption("--cookies $it")
                            }
                        }

                    youtubeDL.execute(request, null) { progress: Float, eta: Long, line: String ->
                        onProgress(progress, line, "")
                    }

                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Successfully downloaded video in ${duration}ms")
                    Result.success(outputPath)
                } catch (e: Exception) {
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.logError(TAG, "Failed to download video after ${duration}ms", e)
                    Result.failure(e)
                }
            }

        suspend fun downloadAudio(
            url: String,
            outputPath: String,
            audioFormat: String = "mp3",
            audioQuality: String = "192k",
            cookies: String? = null,
            onProgress: (Float, String, String) -> Unit = { _, _, _ -> },
            context: Context,
        ): Result<String> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                fileLogger.log(TAG, "Downloading audio: $url to $outputPath with format: $audioFormat quality: $audioQuality")
                try {
                    val youtubeDL = YoutubeDL.getInstance()
                    val request =
                        YoutubeDLRequest(url).apply {
                            addOption("-f bestaudio")
                            addOption("-x")
                            addOption("--audio-format=$audioFormat")
                            addOption("--audio-quality $audioQuality")
                            addOption("-o $outputPath/%(title)s.$audioFormat")
                            addOption("--no-playlist")
                            addOption("--newline")

                            cookies?.let {
                                addOption("--cookies $it")
                            }
                        }

                    youtubeDL.execute(request, null) { progress: Float, eta: Long, line: String ->
                        onProgress(progress, line, "")
                    }

                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Successfully downloaded audio in ${duration}ms")
                    Result.success(outputPath)
                } catch (e: Exception) {
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.logError(TAG, "Failed to download audio after ${duration}ms", e)
                    Result.failure(e)
                }
            }

        suspend fun getFormats(
            url: String,
            context: Context,
        ): Result<List<Map<String, Any>>> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                fileLogger.log(TAG, "Getting formats for: $url")
                try {
                    val youtubeDL = YoutubeDL.getInstance()
                    val request =
                        YoutubeDLRequest(url).apply {
                            addOption("-f best")
                            addOption("--list-formats")
                            addOption("--no-playlist")
                        }

                    val response = youtubeDL.execute(request)
                    val formats = parseFormats(response.out)
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Successfully retrieved ${formats.size} formats in ${duration}ms")
                    Result.success(formats)
                } catch (e: Exception) {
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.logError(TAG, "Failed to get formats after ${duration}ms", e)
                    Result.failure(e)
                }
            }

        private fun parseFormats(output: String): List<Map<String, Any>> {
            val formats = mutableListOf<Map<String, Any>>()
            val lines = output.lines()
            var skippedCount = 0

            for (line in lines) {
                if (line.contains("[download]")) continue
                if (line.contains("format code")) continue

                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 2) {
                    val formatCode = parts[0]
                    val extension = parts.getOrNull(1) ?: "mp4"
                    val resolution = parts.getOrNull(2) ?: "unknown"

                    formats.add(
                        mapOf(
                            "format_code" to formatCode,
                            "ext" to extension,
                            "resolution" to resolution,
                            "note" to "",
                        ),
                    )
                } else {
                    skippedCount++
                }
            }

            return formats
        }

        fun getOutputDirectory(context: Context): String {
            val downloadDir = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
            val OtterDir = java.io.File(downloadDir, "Otter")
            if (!OtterDir.exists()) {
                OtterDir.mkdirs()
            }
            return OtterDir.absolutePath
        }
    }
