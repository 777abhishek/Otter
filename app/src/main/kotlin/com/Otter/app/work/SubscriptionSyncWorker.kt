package com.Otter.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.Otter.app.data.sync.SubscriptionSyncService
import com.Otter.app.util.FileLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Locale

@HiltWorker
class SubscriptionSyncWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val syncService: SubscriptionSyncService,
        private val fileLogger: FileLogger,
    ) : CoroutineWorker(context, params) {
        companion object {
            private const val TAG = "SubscriptionSyncWorker"
            const val UNIQUE_WORK_NAME = "subscription_sync"
        }

        private fun isTransientFailure(t: Throwable): Boolean {
            val msg = (t.message ?: "").lowercase(Locale.US)
            return "unable to resolve host" in msg ||
                "no address associated with hostname" in msg ||
                "temporary failure in name resolution" in msg ||
                "network is unreachable" in msg ||
                "connection reset" in msg ||
                "timed out" in msg ||
                "timeout" in msg
        }

        override suspend fun doWork(): Result {
            fileLogger.log(TAG, "Sync work started")
            return try {
                syncService.syncAll().getOrThrow()
                fileLogger.log(TAG, "Sync work completed successfully")
                Result.success()
            } catch (e: Exception) {
                fileLogger.logError(TAG, "Sync work failed", e)

                // Avoid infinite retry loops for permanent/config errors (e.g., missing cookies).
                if (isTransientFailure(e)) Result.retry() else Result.failure()
            }
        }
    }
