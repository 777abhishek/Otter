package com.Otter.app.data.ytdlp

import android.content.Context
import com.Otter.app.util.FileLogger
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpCore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val fileLogger: FileLogger,
    ) {
        companion object {
            private const val TAG = "YtDlpCore"
        }

        private val youtubeDL = YoutubeDL.getInstance()
        private val initialized = AtomicBoolean(false)

        suspend fun initialize(): Result<Unit> =
            withContext(Dispatchers.IO) {
                if (initialized.get()) return@withContext Result.success(Unit)

                fileLogger.log(TAG, "Initializing yt-dlp")
                try {
                    youtubeDL.init(context)
                    initialized.set(true)
                    fileLogger.log(TAG, "yt-dlp initialized successfully")
                    Result.success(Unit)
                } catch (e: Exception) {
                    fileLogger.logError(TAG, "Failed to initialize yt-dlp", e)
                    Result.failure(e)
                }
            }

        suspend fun ensureInitialized(): Unit =
            withContext(Dispatchers.IO) {
                if (initialized.get()) return@withContext
                initialize().getOrThrow()
            }

        suspend fun execute(request: YoutubeDLRequest): YoutubeDLResponse =
            withContext(Dispatchers.IO) {
                ensureInitialized()
                val startTime = System.currentTimeMillis()
                fileLogger.log(TAG, "Executing yt-dlp request")
                try {
                    val response = youtubeDL.execute(request)
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(
                        TAG,
                        "Request completed in ${duration}ms - Output length: ${response.out.length}, Error length: ${response.err.length}",
                    )
                    if (response.err.isNotBlank()) {
                        fileLogger.log(TAG, "Response error (first 200 chars): ${response.err.take(200)}")
                    }
                    response
                } catch (e: Exception) {
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.logError(TAG, "Request failed after ${duration}ms", e)
                    throw e
                }
            }

        suspend fun execute(
            request: YoutubeDLRequest,
            callback: (progress: Float, eta: Long, line: String) -> Unit,
        ) = withContext(Dispatchers.IO) {
            ensureInitialized()
            val startTime = System.currentTimeMillis()
            fileLogger.log(TAG, "Executing yt-dlp request with progress callback")
            try {
                youtubeDL.execute(request, processId = null, callback = callback)
                val duration = System.currentTimeMillis() - startTime
                fileLogger.log(TAG, "Request with callback completed in ${duration}ms")
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                fileLogger.logError(TAG, "Request with callback failed after ${duration}ms", e)
                throw e
            }
        }
    }
