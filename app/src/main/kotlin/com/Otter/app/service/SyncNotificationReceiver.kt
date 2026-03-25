package com.Otter.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SyncNotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var syncService: com.Otter.app.data.sync.SubscriptionSyncService

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            NotificationManager.ACTION_SYNC_CANCEL -> {
                syncService.cancelOngoingSync()
            }
        }
    }
}
