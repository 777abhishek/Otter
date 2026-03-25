package com.Otter.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.Otter.app.data.auth.YouTubeProfileStore
import com.Otter.app.data.models.Video
import com.Otter.app.data.repositories.PlaylistRepository
import com.Otter.app.data.repositories.VideoRepository
import com.Otter.app.data.ytdlp.YtDlpManager
import com.Otter.app.service.NotificationManager
import com.Otter.app.util.FileLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

@HiltWorker
class PlaylistFullMetadataWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val ytDlpManager: YtDlpManager,
        private val cookieStore: YouTubeProfileStore,
        private val playlistRepository: PlaylistRepository,
        private val videoRepository: VideoRepository,
        private val notificationManager: NotificationManager,
        private val fileLogger: FileLogger,
    ) : CoroutineWorker(context, params) {

        companion object {
            const val KEY_PLAYLIST_ID = "playlist_id"
            const val UNIQUE_WORK_PREFIX = "playlist_full_metadata_"
        }

        override suspend fun doWork(): Result {
            val playlistId = inputData.getString(KEY_PLAYLIST_ID) ?: return Result.failure()
            if (playlistId.isBlank() || playlistId == "playlists" || playlistId.length < 2) return Result.failure()

            return try {
                val cookiesFilePath = withContext(Dispatchers.IO) { cookieStore.getCookiesFilePathOnce() }
                if (cookiesFilePath == null) {
                    fileLogger.log(TAG, "Cookies file not found for playlist $playlistId")
                    val playlist = playlistRepository.getPlaylistById(playlistId)
                    val playlistTitle = playlist?.title ?: "Playlist"
                    notificationManager.updateMetadataNotification(
                        title = "Metadata enhancement failed",
                        text = "$playlistTitle: cookies not configured",
                        progress = null,
                        ongoing = false,
                    )
                    return Result.failure()
                }

                // Get existing videos from Stage 1 sync
                val existingVideos = videoRepository.getVideosByPlaylistOnce(playlistId)
                if (existingVideos.isEmpty()) {
                    fileLogger.log(TAG, "No existing videos found for playlist $playlistId, skipping full metadata sync")
                    val playlist = playlistRepository.getPlaylistById(playlistId)
                    val playlistTitle = playlist?.title ?: "Playlist"
                    notificationManager.updateMetadataNotification(
                        title = "Metadata enhancement skipped",
                        text = "$playlistTitle: no videos found to enhance",
                        progress = null,
                        ongoing = false,
                    )
                    return Result.success()
                }

                val playlist = playlistRepository.getPlaylistById(playlistId)
                val playlistTitle = playlist?.title ?: "Playlist"
                val totalVideos = existingVideos.size
                val processedCount = AtomicInteger(0)

                // Show Stage 2 start notification
                notificationManager.updateMetadataNotification(
                    title = "Enhancing metadata",
                    text = "$playlistTitle: 0/$totalVideos",
                    progress = 0f,
                    ongoing = true,
                )

                fileLogger.log(TAG, "Starting Stage 2 metadata enhancement for $playlistTitle: $totalVideos videos")

                // Fetch full metadata for all videos at once (without --flat-playlist)
                val fullMetadataVideos = withContext(Dispatchers.IO) {
                    ytDlpManager.extractPlaylistVideosFull(playlistId, cookiesFilePath)
                        .getOrElse { emptyList() }
                }

                if (fullMetadataVideos.isEmpty()) {
                    fileLogger.log(TAG, "Failed to fetch full metadata for playlist $playlistId")
                    notificationManager.updateMetadataNotification(
                        title = "Metadata enhancement failed",
                        text = "$playlistTitle: yt-dlp error or network issue",
                        progress = null,
                        ongoing = false,
                    )
                    return Result.retry()
                }

                // Update existing videos with full metadata (incrementally to show progress)
                val updatedVideos = mergeFullMetadata(existingVideos, fullMetadataVideos)

                // Process videos one by one with progress notifications (silently)
                for (updatedVideo in updatedVideos) {
                    if (isStopped) return Result.success()

                    // Update video in database (silent, no UI disturbance)
                    videoRepository.upsertVideoInPlaylistPreservingFlags(playlistId, updatedVideo)

                    // Update progress notification incrementally
                    val processed = processedCount.incrementAndGet()
                    val progressPercent = (processed.toFloat() / totalVideos.toFloat()).coerceIn(0f, 0.99f)

                    notificationManager.updateMetadataNotification(
                        title = "Enhancing metadata",
                        text = "$playlistTitle: $processed/$totalVideos videos",
                        progress = progressPercent,
                        ongoing = true,
                    )

                    fileLogger.log(TAG, "Enhanced video ${updatedVideo.id}: $processed/$totalVideos (${"%.0f".format(progressPercent * 100)}%)")
                }

                // Show Stage 2 completion notification (with checkmark)
                notificationManager.updateMetadataNotification(
                    title = "✓ Metadata enhanced",
                    text = "$playlistTitle: $totalVideos videos with full details",
                    progress = 1f,
                    ongoing = false,
                )

                fileLogger.log(TAG, "Stage 2 metadata sync completed for playlist $playlistId: $totalVideos videos enhanced")
                Result.success()

            } catch (e: Exception) {
                fileLogger.logError(TAG, "Full metadata sync failed for playlist $playlistId", e)
                val playlist = playlistRepository.getPlaylistById(playlistId)
                val playlistTitle = playlist?.title ?: "Playlist"
                notificationManager.updateMetadataNotification(
                    title = "Metadata enhancement failed",
                    text = "$playlistTitle: ${e.message ?: "unknown error"}",
                    progress = null,
                    ongoing = false,
                )
                Result.retry()
            }
        }

        /**
         * Merge full metadata from Stage 2 into existing videos from Stage 1
         * Preserves all existing data and only adds/enhances metadata
         */
        private fun mergeFullMetadata(
            existingVideos: List<Video>,
            fullMetadataVideos: List<Video>
        ): List<Video> {
            val fullMetadataMap = fullMetadataVideos.associateBy { it.id }

            return existingVideos.map { existing ->
                val fullMetadata = fullMetadataMap[existing.id]
                if (fullMetadata != null) {
                    // Merge full metadata while preserving existing video data
                    existing.copy(
                        // Enhanced metadata from full extraction
                        description = fullMetadata.description.takeIf { it.isNotBlank() } ?: existing.description,
                        channelName = fullMetadata.channelName.takeIf { it.isNotBlank() } ?: existing.channelName,
                        channelId = fullMetadata.channelId.takeIf { it.isNotBlank() } ?: existing.channelId,
                        views = maxOf(fullMetadata.views, existing.views), // Use the higher view count
                        uploadDate = fullMetadata.uploadDate.takeIf { it.isNotBlank() } ?: existing.uploadDate,
                        // Keep existing thumbnail if full metadata doesn't have a better one
                        thumbnail = fullMetadata.thumbnail.takeIf { it.isNotBlank() && it != "https://i.ytimg.com/vi/${existing.id}/default.jpg" }
                            ?: existing.thumbnail,
                        // Preserve existing stream URLs and flags
                        streamUrl = existing.streamUrl,
                        audioStreamUrl = existing.audioStreamUrl,
                        isLiked = existing.isLiked,
                        isWatchLater = existing.isWatchLater,
                        isWatched = existing.isWatched
                    )
                } else {
                    existing
                }
            }
        }
    }

private const val TAG = "PlaylistFullMetadataWorker"