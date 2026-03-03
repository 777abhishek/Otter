package com.Otter.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

object PlaylistWorkScheduler {
    fun enqueueRefresh(
        context: Context,
        playlistId: String,
    ) {
        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val request =
            OneTimeWorkRequestBuilder<PlaylistMetadataWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        PlaylistMetadataWorker.KEY_PLAYLIST_ID to playlistId,
                    ),
                )
                .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                PlaylistMetadataWorker.UNIQUE_WORK_PREFIX + playlistId,
                ExistingWorkPolicy.KEEP,
                request,
            )
    }
}
