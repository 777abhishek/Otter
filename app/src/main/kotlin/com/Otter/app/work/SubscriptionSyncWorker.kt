package com.Otter.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.Otter.app.data.sync.SubscriptionSyncService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SubscriptionSyncWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val syncService: SubscriptionSyncService,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            return try {
                syncService.syncAll().getOrThrow()
                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }

        companion object {
            const val UNIQUE_WORK_NAME = "subscription_sync"
        }
    }
