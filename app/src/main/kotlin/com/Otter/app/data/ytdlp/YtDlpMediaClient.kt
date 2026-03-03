package com.Otter.app.data.ytdlp

import com.Otter.app.util.FileLogger
import com.google.gson.Gson
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpMediaClient
    @Inject
    constructor(
        private val core: YtDlpCore,
        private val cookieSupport: YtDlpCookieSupport,
        private val parsers: YtDlpParsers,
        private val fileLogger: FileLogger,
    ) {
        companion object {
            private const val TAG = "YtDlpMediaClient"
        }

        private val gson = Gson()

        suspend fun getVideoInfo(
            url: String,
            cookiesFilePath: String?,
        ): Result<Map<String, Any>> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                fileLogger.log(TAG, "Getting video info: $url")
                runCatching {
                    val request =
                        YoutubeDLRequest(url).apply {
                            addOption("--dump-json")
                            addOption("--no-playlist")
                            cookieSupport.addCookiesToRequest(this, cookiesFilePath)
                        }

                    val response = core.execute(request)
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Video info retrieved in ${duration}ms - Output length: ${response.out.length}")
                    if (response.out.isBlank() && response.err.isNotBlank()) {
                        throw IllegalStateException(response.err.trim())
                    }

                    gson.fromJson(response.out, Map::class.java) as Map<String, Any>
                }
            }

        suspend fun getFormats(
            url: String,
            cookiesFilePath: String?,
        ): Result<List<Map<String, Any>>> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                fileLogger.log(TAG, "Getting formats: $url")
                runCatching {
                    val request =
                        YoutubeDLRequest(url).apply {
                            addOption("-f", "best")
                            addOption("--list-formats")
                            addOption("--no-playlist")
                            cookieSupport.addCookiesToRequest(this, cookiesFilePath)
                        }

                    val response = core.execute(request)
                    val formats = parsers.parseFormats(response.out)
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Successfully retrieved ${formats.size} formats in ${duration}ms")
                    formats
                }
            }

        suspend fun getStreamUrl(
            url: String,
            formatId: String,
            cookiesFilePath: String?,
        ): Result<String> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                fileLogger.log(TAG, "Getting stream URL: $url formatId=$formatId")
                runCatching {
                    val request =
                        YoutubeDLRequest(url).apply {
                            addOption("-f", formatId)
                            addOption("-g")
                            addOption("--no-playlist")
                            cookieSupport.addCookiesToRequest(this, cookiesFilePath)
                        }

                    val response = core.execute(request)
                    val streamUrl = response.out.trim()
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Stream URL retrieved in ${duration}ms - Length: ${streamUrl.length}")
                    if (streamUrl.isBlank() && response.err.isNotBlank()) {
                        throw IllegalStateException(response.err.trim())
                    }
                    streamUrl
                }
            }

        suspend fun downloadVideo(
            url: String,
            outputPath: String,
            format: String,
            cookiesFilePath: String?,
            onProgress: (Float, String, String) -> Unit,
        ): Result<String> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                fileLogger.log(TAG, "Downloading video: $url to $outputPath with format: $format")
                runCatching {
                    val request =
                        YoutubeDLRequest(url).apply {
                            addOption("-f", format)
                            addOption("-o", "$outputPath/%(title)s.%(ext)s")
                            addOption("--no-playlist")
                            cookieSupport.addCookiesToRequest(this, cookiesFilePath)
                            addOption("--newline")
                        }

                    core.execute(request) { progress, eta, line ->
                        onProgress(progress, line, "")
                    }

                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Video download completed in ${duration}ms")
                    outputPath
                }
            }

        suspend fun downloadAudio(
            url: String,
            outputPath: String,
            format: String,
            audioFormat: String,
            audioQuality: String,
            cookiesFilePath: String?,
            onProgress: (Float, String, String) -> Unit,
        ): Result<String> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                fileLogger.log(TAG, "Downloading audio: $url to $outputPath with format: $audioFormat quality: $audioQuality")
                runCatching {
                    val request =
                        YoutubeDLRequest(url).apply {
                            addOption("-f", format)
                            addOption("-x")
                            addOption("--audio-format", audioFormat)
                            addOption("--audio-quality", audioQuality)
                            addOption("-o", "$outputPath/%(title)s.$audioFormat")
                            addOption("--no-playlist")
                            cookieSupport.addCookiesToRequest(this, cookiesFilePath)
                            addOption("--newline")
                        }

                    core.execute(request) { progress, eta, line ->
                        onProgress(progress, line, "")
                    }

                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Audio download completed in ${duration}ms")
                    outputPath
                }
            }
    }
