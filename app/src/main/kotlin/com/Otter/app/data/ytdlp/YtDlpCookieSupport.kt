package com.Otter.app.data.ytdlp

import com.Otter.app.util.FileLogger
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpCookieSupport
    @Inject
    constructor(
        private val fileLogger: FileLogger,
    ) {
        companion object {
            private const val TAG = "YtDlpCookieSupport"
        }

        fun addCookiesToRequest(
            request: YoutubeDLRequest,
            cookiesFilePath: String?,
        ) {
            cookiesFilePath?.let { path ->
                fileLogger.log(TAG, "Checking cookies file: $path")
                val cookieFile = File(path)
                if (!cookieFile.exists()) {
                    val errMsg = "Cookies file does not exist: $path"
                    fileLogger.logError(TAG, errMsg, null)
                    throw IllegalStateException(errMsg)
                }
                if (!cookieFile.canRead()) {
                    val errMsg = "Cookies file is not readable: $path"
                    fileLogger.logError(TAG, errMsg, null)
                    throw IllegalStateException(errMsg)
                }
                val fileSize = cookieFile.length()
                fileLogger.log(TAG, "Cookies file exists, size: $fileSize bytes, path: ${cookieFile.absolutePath}")

                try {
                    val firstLines =
                        cookieFile.bufferedReader().use { reader ->
                            reader.readLines().take(3).joinToString("\n")
                        }
                    fileLogger.log(TAG, "Cookies file header preview: $firstLines")
                } catch (e: Exception) {
                    fileLogger.logError(TAG, "Failed to read cookies file preview", e)
                }

                fileLogger.log(TAG, "Adding --cookies option with path: $path")
                request.addOption("--cookies", path)
            } ?: run {
                fileLogger.log(TAG, "No cookies file path provided")
            }
        }
    }
