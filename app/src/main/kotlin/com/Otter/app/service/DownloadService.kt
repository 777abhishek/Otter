package com.Otter.app.service

import com.Otter.app.data.models.DownloadProgress
import com.Otter.app.data.models.DownloadSettings
import com.Otter.app.data.models.DownloadTask
import kotlinx.coroutines.flow.Flow

interface DownloadService {
    fun getDownloads(): Flow<List<DownloadTask>>

    fun getDownloadById(id: String): Flow<DownloadTask?>

    fun getDownloadProgress(id: String): Flow<DownloadProgress>

    suspend fun startDownload(
        videoId: String,
        title: String,
        thumbnail: String,
    ): Result<String>

    suspend fun startDownloadFromUrl(url: String): Result<String>

    suspend fun pauseDownload(id: String): Result<Unit>

    suspend fun resumeDownload(id: String): Result<Unit>

    suspend fun cancelDownload(id: String): Result<Unit>

    suspend fun retryDownload(id: String): Result<Unit>

    suspend fun updateSettings(settings: DownloadSettings): Result<Unit>

    fun getSettings(): Flow<DownloadSettings>

    suspend fun clearCompletedDownloads(): Result<Unit>

    suspend fun clearAllDownloads(): Result<Unit>
}
