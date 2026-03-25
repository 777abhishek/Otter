package com.Otter.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.OutOfQuotaPolicy
import androidx.work.workDataOf

object PlaylistFullMetadataWorkScheduler {
    fun enqueueFullMetadataSync(
        context: Context,
        playlistId: String,
    ) {
        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val request =
            OneTimeWorkRequestBuilder<PlaylistFullMetadataWorker>()
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(
                    workDataOf(
                        PlaylistFullMetadataWorker.KEY_PLAYLIST_ID to playlistId,
                    ),
                )
                .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                PlaylistFullMetadataWorker.UNIQUE_WORK_PREFIX + playlistId,
                ExistingWorkPolicy.REPLACE, // Allow replacing if already running
                request,
            )
    }
}