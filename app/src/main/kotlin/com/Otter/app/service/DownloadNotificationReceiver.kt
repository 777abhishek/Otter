package com.Otter.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Broadcast receiver for handling notification actions
 * Handles cancel, retry, and open actions from download notifications
 */
@AndroidEntryPoint
class DownloadNotificationReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "DownloadNotificationReceiver"
    }

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var downloader: com.Otter.app.data.download.Downloader

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val action = intent.action ?: return
        val taskId = intent.getStringExtra("task_id") ?: return

        Log.d(TAG, "Received notification action: $action for task: $taskId")

        when (action) {
            NotificationManager.ACTION_CANCEL -> {
                handleCancel(taskId)
            }
            NotificationManager.ACTION_RETRY -> {
                handleRetry(taskId)
            }
            NotificationManager.ACTION_OPEN -> {
                handleOpen(taskId)
            }
        }
    }

    private fun handleCancel(taskId: String) {
        Log.d(TAG, "Cancel requested for task: $taskId")
        // Find the task by ID and cancel it
        // The downloader has a taskStateMap, but we need to find the task by ID
        // For now, we'll need to iterate through the downloader's tasks
        // This is a placeholder - we need to add a method to Downloader to cancel by ID
        try {
            // TODO: Implement proper task cancellation by ID
            // For now, just cancel the notification
            notificationManager.cancelNotification()
            Log.d(TAG, "Notification cancelled for task: $taskId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel task: $taskId", e)
        }
    }

    private fun handleRetry(taskId: String) {
        Log.d(TAG, "Retry requested for task: $taskId")
        // TODO: Implement retry logic - need to integrate with Downloader
        // For now, just show a toast or log
        try {
            // TODO: Implement proper task retry by ID
            // This would involve finding the task and restarting the download
            Log.d(TAG, "Retry requested for task: $taskId (not yet implemented)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retry task: $taskId", e)
        }
    }

    private fun handleOpen(taskId: String) {
        Log.d(TAG, "Open requested for task: $taskId")
        // TODO: Implement open logic - open the downloaded file
        // For now, just show a toast or log
        try {
            // TODO: Implement proper file opening by task ID
            // This would involve finding the task and opening the downloaded file
            Log.d(TAG, "Open requested for task: $taskId (not yet implemented)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open task: $taskId", e)
        }
    }
}
