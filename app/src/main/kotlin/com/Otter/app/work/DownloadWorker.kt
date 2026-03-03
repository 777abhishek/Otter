package com.Otter.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.Otter.app.data.repositories.DownloadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DownloadWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val downloadRepository: DownloadRepository,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): androidx.work.ListenableWorker.Result {
            return try {
                val videoId = inputData.getString("video_id") ?: return androidx.work.ListenableWorker.Result.failure()
                val title = inputData.getString("title") ?: return androidx.work.ListenableWorker.Result.failure()
                val thumbnail = inputData.getString("thumbnail") ?: ""

                downloadRepository.startDownload(videoId, title, thumbnail)
                androidx.work.ListenableWorker.Result.success()
            } catch (e: Exception) {
                androidx.work.ListenableWorker.Result.failure()
            }
        }

        companion object {
            const val KEY_VIDEO_ID = "video_id"
            const val KEY_TITLE = "title"
            const val KEY_THUMBNAIL = "thumbnail"
        }
    }
