package com.Otter.app.data.sync

internal data class SyncMetadata(
    val totalPlaylists: Int = 0,
    val totalVideos: Int = 0,
    val syncedPlaylists: Int = 0,
    val syncedVideos: Int = 0,
    val failedPlaylists: Int = 0,
    val failedVideos: Int = 0,
    val syncDurationMs: Long = 0,
) {
    val progress: Float
        get() =
            if (totalPlaylists + totalVideos == 0) {
                0f
            } else {
                (syncedPlaylists + syncedVideos).toFloat() / (totalPlaylists + totalVideos).toFloat()
            }
}
