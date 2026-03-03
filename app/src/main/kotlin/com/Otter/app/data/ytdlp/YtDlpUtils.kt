package com.Otter.app.data.ytdlp

import com.Otter.app.util.FileLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpUtils
    @Inject
    constructor(
        private val fileLogger: FileLogger,
    ) {
        companion object {
            private const val TAG = "YtDlpUtils"
            private const val DEFAULT_MAX_RETRIES = 3
            private const val DEFAULT_RETRY_DELAY_MS = 2000L
            private const val DEFAULT_TIMEOUT_MS = 300000L // 5 minutes
        }

        /**
         * Execute operation with retry logic
         * @param operation The operation to execute
         * @param maxRetries Maximum number of retry attempts
         * @param retryDelayMs Delay between retries in milliseconds
         * @return Result of the operation
         */
        suspend fun <T> withRetry(
            operation: suspend () -> T,
            maxRetries: Int = DEFAULT_MAX_RETRIES,
            retryDelayMs: Long = DEFAULT_RETRY_DELAY_MS,
        ): Result<T> {
            var lastException: Exception? = null

            repeat(maxRetries) { attempt ->
                try {
                    val result = operation()
                    if (attempt > 0) {
                        fileLogger.log(TAG, "Operation succeeded on attempt ${attempt + 1}")
                    }
                    return Result.success(result)
                } catch (e: Exception) {
                    lastException = e
                    fileLogger.log(TAG, "Operation failed on attempt ${attempt + 1}/$maxRetries: ${e.message}")

                    if (attempt < maxRetries - 1) {
                        fileLogger.log(TAG, "Retrying in ${retryDelayMs}ms...")
                        delay(retryDelayMs)
                    } else {
                        fileLogger.logError(TAG, "Operation failed after $maxRetries attempts", e)
                    }
                }
            }

            return Result.failure(lastException ?: IllegalStateException("Operation failed"))
        }

        /**
         * Execute operation with timeout
         * @param operation The operation to execute
         * @param timeoutMs Timeout in milliseconds
         * @return Result of the operation
         */
        suspend fun <T> withTimeout(
            operation: suspend () -> T,
            timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        ): Result<T> {
            return try {
                val result =
                    withTimeout(timeoutMs) {
                        operation()
                    }
                Result.success(result)
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                fileLogger.logError(TAG, "Operation timed out after ${timeoutMs}ms", e)
                Result.failure(e)
            } catch (e: Exception) {
                fileLogger.logError(TAG, "Operation failed", e)
                Result.failure(e)
            }
        }

        /**
         * Execute operation with both retry and timeout
         * @param operation The operation to execute
         * @param maxRetries Maximum number of retry attempts
         * @param retryDelayMs Delay between retries in milliseconds
         * @param timeoutMs Timeout in milliseconds
         * @return Result of the operation
         */
        suspend fun <T> withRetryAndTimeout(
            operation: suspend () -> T,
            maxRetries: Int = DEFAULT_MAX_RETRIES,
            retryDelayMs: Long = DEFAULT_RETRY_DELAY_MS,
            timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        ): Result<T> {
            return withRetry(
                operation = {
                    withTimeout(operation, timeoutMs).getOrThrow()
                },
                maxRetries = maxRetries,
                retryDelayMs = retryDelayMs,
            )
        }

        /**
         * Validate YouTube video ID
         * @param videoId The video ID to validate
         * @return True if valid, false otherwise
         */
        fun isValidVideoId(videoId: String): Boolean {
            if (videoId.isBlank()) return false
            // YouTube video IDs are typically 11 characters
            if (videoId.length < 10 || videoId.length > 12) return false
            // Should only contain alphanumeric characters, underscore, and hyphen
            return videoId.matches(Regex("^[a-zA-Z0-9_-]+$"))
        }

        /**
         * Validate YouTube playlist ID
         * @param playlistId The playlist ID to validate
         * @return True if valid, false otherwise
         */
        fun isValidPlaylistId(playlistId: String): Boolean {
            if (playlistId.isBlank()) return false
            if (playlistId == "playlists") return false
            if (playlistId.length < 2) return false
            // Should only contain alphanumeric characters, underscore, and hyphen
            return playlistId.matches(Regex("^[a-zA-Z0-9_-]+$"))
        }

        /**
         * Validate YouTube URL
         * @param url The URL to validate
         * @return True if valid, false otherwise
         */
        fun isValidYouTubeUrl(url: String): Boolean {
            if (url.isBlank()) return false
            return url.matches(Regex("^(https?://)?(www\\.)?(youtube\\.com|youtu\\.be)/.+$"))
        }

        /**
         * Extract video ID from YouTube URL
         * @param url The YouTube URL
         * @return Video ID or null if not found
         */
        fun extractVideoIdFromUrl(url: String): String? {
            if (!isValidYouTubeUrl(url)) return null

            // Try various YouTube URL patterns
            val patterns =
                listOf(
                    Regex("v=([a-zA-Z0-9_-]{11})"),
                    Regex("youtu\\.be/([a-zA-Z0-9_-]{11})"),
                    Regex("embed/([a-zA-Z0-9_-]{11})"),
                    Regex("shorts/([a-zA-Z0-9_-]{11})"),
                )

            for (pattern in patterns) {
                val match = pattern.find(url)
                if (match != null) {
                    val videoId = match.groupValues[1]
                    if (isValidVideoId(videoId)) {
                        return videoId
                    }
                }
            }

            return null
        }

        /**
         * Extract playlist ID from YouTube URL
         * @param url The YouTube URL
         * @return Playlist ID or null if not found
         */
        fun extractPlaylistIdFromUrl(url: String): String? {
            if (!isValidYouTubeUrl(url)) return null

            val pattern = Regex("list=([a-zA-Z0-9_-]+)")
            val match = pattern.find(url)
            if (match != null) {
                val playlistId = match.groupValues[1]
                if (isValidPlaylistId(playlistId)) {
                    return playlistId
                }
            }

            return null
        }

        /**
         * Sanitize filename by removing invalid characters
         * @param filename The filename to sanitize
         * @return Sanitized filename
         */
        fun sanitizeFilename(filename: String): String {
            val invalidChars = Regex("[<>:\"/\\\\|?*]")
            return filename.replace(invalidChars, "_").trim { it <= ' ' }
        }

        /**
         * Validate output path
         * @param path The path to validate
         * @return True if valid, false otherwise
         */
        fun isValidOutputPath(path: String): Boolean {
            if (path.isBlank()) return false
            // Check for invalid characters
            val invalidChars = Regex("[<>:\"|?*]")
            if (invalidChars.containsMatchIn(path)) return false
            return true
        }
    }
