package com.Otter.app.data.models

data class DownloadTask(
    val id: String,
    val videoId: String,
    val title: String,
    val thumbnail: String,
    val status: DownloadStatus,
    val progress: Float,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speed: Long, // bytes per second
    val filePath: String,
    val createdAt: Long,
    val completedAt: Long? = null,
    val error: String? = null,
)

enum class DownloadStatus {
    PENDING,
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED,
}

data class DownloadProgress(
    val taskId: String,
    val progress: Float,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speed: Long,
    val eta: Long, // estimated time in seconds
)

data class DownloadSettings(
    val maxConcurrentDownloads: Int = 3,
    val defaultFormat: DownloadVideoFormat = DownloadVideoFormat.MP4_720P,
    val autoRetry: Boolean = true,
    val maxRetries: Int = 3,
    val downloadPath: String = "App storage (Downloads)",
)

enum class DownloadVideoFormat {
    MP4_360P,
    MP4_480P,
    MP4_720P,
    MP4_1080P,
    WEBM,
    MP3_AUDIO,
}

/**
 * DownloadItem for use with DownloadQueueManager
 */
data class DownloadItem(
    val videoId: String,
    val title: String = "",
    val thumbnail: String = "",
    val format: String = "bestvideo*+bestaudio/best",
    val outputPath: String = "",
    val cookiesFilePath: String? = null,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Float = 0f,
    val filePath: String? = null,
    val error: String? = null,
)
