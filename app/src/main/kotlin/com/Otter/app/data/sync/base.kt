package com.Otter.app.data.sync

/**
 * Shared primitives for sync pipeline.
 */
internal data class SyncProgress(
    val stage: String,
    val progress: Float?,
)

internal interface SyncProgressReporter {
    fun report(progress: SyncProgress)
}
