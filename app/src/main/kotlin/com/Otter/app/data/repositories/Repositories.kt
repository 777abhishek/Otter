package com.Otter.app.data.repositories

import com.Otter.app.data.database.dao.DownloadTaskDao
import com.Otter.app.data.database.dao.PlaylistDao
import com.Otter.app.data.database.dao.VideoDao
import com.Otter.app.data.database.dao.VideoProgressDao
import com.Otter.app.data.database.entities.DownloadTaskEntity
import com.Otter.app.data.database.entities.PlaylistEntity
import com.Otter.app.data.database.entities.VideoEntity
import com.Otter.app.data.database.entities.VideoProgressEntity
import com.Otter.app.data.models.*
import com.Otter.app.service.DownloadService
import com.Otter.app.service.VideoService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepository
    @Inject
    constructor(
        private val videoDao: VideoDao,
        private val videoService: VideoService,
    ) {
        fun getVideosByPlaylist(playlistId: String): Flow<List<Video>> {
            return videoDao.getVideosByPlaylist(playlistId).map { entities ->
                entities.map { it.toVideo() }
            }
        }

        fun getLikedVideos(): Flow<List<Video>> {
            return videoDao.getLikedVideos().map { entities ->
                entities.map { it.toVideo() }
            }
        }

        fun getWatchLaterVideos(): Flow<List<Video>> {
            return videoDao.getWatchLaterVideos().map { entities ->
                entities.map { it.toVideo() }
            }
        }

        fun getDownloadedVideos(): Flow<List<Video>> {
            return videoDao.getDownloadedVideos().map { entities ->
                entities.map { it.toVideo() }
            }
        }

        suspend fun getVideoById(id: String): Video? {
            return videoDao.getVideoById(id)?.toVideo()
        }

        suspend fun getVideosByPlaylistOnce(playlistId: String): List<Video> {
            return videoDao.getVideosByPlaylistOnce(playlistId).map { it.toVideo() }
        }

        suspend fun saveVideos(videos: List<Video>) {
            videoDao.insertVideos(videos.map { it.toEntity() })
        }

        suspend fun upsertVideoInPlaylistPreservingFlags(
            playlistId: String,
            video: Video,
        ) {
            val existing = videoDao.getVideoById(video.id) ?: return
            if (existing.playlistId != playlistId) return

            val updated =
                existing.copy(
                    title = video.title,
                    channelName = video.channelName,
                    channelId = video.channelId,
                    viewCount = video.views.toString(),
                    uploadDate = video.uploadDate,
                    duration = formatDuration(video.duration),
                    thumbnailUrl = video.thumbnail,
                    description = video.description,
                    streamUrl = video.streamUrl,
                    audioStreamUrl = video.audioStreamUrl,
                )
            videoDao.insertVideo(updated)
        }

        suspend fun replaceLikedVideos(videos: List<Video>) {
            videoDao.deleteVideosByPlaylist("LL")
            val entities =
                videos.mapIndexed { index, video ->
                    video.toEntity(
                        playlistId = "LL",
                        position = index,
                    )
                }
            videoDao.insertVideos(entities)
        }

        suspend fun replaceWatchLaterVideos(videos: List<Video>) {
            videoDao.deleteVideosByPlaylist("WL")
            val entities =
                videos.mapIndexed { index, video ->
                    video.toEntity(
                        playlistId = "WL",
                        position = index,
                    )
                }
            videoDao.insertVideos(entities)
        }

        suspend fun likeVideo(videoId: String) {
            videoDao.updateLikeStatus(videoId, true)
        }

        suspend fun unlikeVideo(videoId: String) {
            videoDao.updateLikeStatus(videoId, false)
        }

        suspend fun addToWatchLater(videoId: String) {
            videoDao.updateWatchLaterStatus(videoId, true)
        }

        suspend fun removeFromWatchLater(videoId: String) {
            videoDao.updateWatchLaterStatus(videoId, false)
        }

        suspend fun searchVideos(query: String): Result<List<Video>> {
            return videoService.searchVideos(query)
        }

        suspend fun getPopularVideos(limit: Int): Result<List<Video>> {
            return videoService.getPopularVideos(limit)
        }
    }

@Singleton
class PlaylistRepository
    @Inject
    constructor(
        private val playlistDao: PlaylistDao,
        private val videoDao: VideoDao,
    ) {
        fun getAllPlaylists(): Flow<List<Playlist>> {
            return playlistDao.getAllPlaylists().map { entities ->
                entities.map { it.toPlaylist() }
            }
        }

        fun getLikedPlaylists(): Flow<List<Playlist>> {
            return playlistDao.getLikedPlaylists().map { entities ->
                entities.map { it.toPlaylist() }
            }
        }

        suspend fun getPlaylistById(id: String): Playlist? {
            return playlistDao.getPlaylistById(id)?.toPlaylist()
        }

        fun observePlaylistById(id: String): Flow<Playlist?> {
            return playlistDao.observePlaylistById(id).map { it?.toPlaylist() }
        }

        suspend fun savePlaylists(playlists: List<Playlist>) {
            playlistDao.insertPlaylists(playlists.map { it.toEntity() })
        }

        suspend fun upsertPlaylistWithVideos(
            playlist: Playlist,
            videos: List<Video>,
        ) {
            if (playlist.id.isBlank() || playlist.id == "playlists" || playlist.id.length < 2) {
                return
            }
            playlistDao.insertPlaylist(playlist.copy(videoCount = videos.size).toEntity())

            val existingById =
                videoDao.getVideoFlagsByPlaylistOnce(playlist.id)
                    .associateBy { it.id }

            val entities =
                videos.mapIndexed { index, video ->
                    val existing = existingById[video.id]
                    val base =
                        video.toEntity(
                            playlistId = playlist.id,
                            position = index,
                        )
                    if (existing == null) {
                        base
                    } else {
                        base.copy(
                            isDownloaded = existing.isDownloaded,
                            filePath = existing.filePath,
                            isLiked = existing.isLiked,
                            isWatchLater = existing.isWatchLater,
                            addedDate = existing.addedDate,
                        )
                    }
                }

            videoDao.insertVideos(entities)

            val incomingIds = entities.map { it.id }
            if (incomingIds.isNotEmpty()) {
                videoDao.deleteVideosByPlaylistNotIn(playlist.id, incomingIds)
            }
        }

        suspend fun appendPlaylistVideos(
            playlist: Playlist,
            videos: List<Video>,
        ) {
            if (playlist.id.isBlank() || playlist.id == "playlists" || playlist.id.length < 2) {
                return
            }
            if (videos.isEmpty()) return

            val existingIds = videoDao.getVideoIdsByPlaylist(playlist.id)
            val existingIdSet = existingIds.toHashSet()

            val newVideos = videos.filter { it.id.isNotBlank() && !existingIdSet.contains(it.id) }
            if (newVideos.isEmpty()) return

            val existingById =
                videoDao.getVideoFlagsByPlaylistOnce(playlist.id)
                    .associateBy { it.id }

            val startPosition = existingIds.size

            val entities =
                newVideos.mapIndexed { index, video ->
                    val existing = existingById[video.id]
                    val base =
                        video.toEntity(
                            playlistId = playlist.id,
                            position = startPosition + index,
                        )
                    if (existing == null) {
                        base
                    } else {
                        base.copy(
                            isDownloaded = existing.isDownloaded,
                            filePath = existing.filePath,
                            isLiked = existing.isLiked,
                            isWatchLater = existing.isWatchLater,
                            addedDate = existing.addedDate,
                        )
                    }
                }

            videoDao.insertVideos(entities)

            val currentCount = existingIds.size + entities.size
            playlistDao.insertPlaylist(playlist.copy(videoCount = currentCount).toEntity())
        }

        suspend fun replacePlaylists(playlists: List<Playlist>) {
            val ids = playlists.map { it.id }
            playlistDao.deletePlaylistsNotIn(ids)
            playlistDao.insertPlaylists(playlists.map { it.toEntity() })

            // Persist videos within these playlists
            playlists.forEach { playlist ->
                playlist.videos.orEmpty().let { videos ->
                    videoDao.deleteVideosByPlaylist(playlist.id)
                    val entities =
                        videos.mapIndexed { index, video ->
                            video.toEntity(
                                playlistId = playlist.id,
                                position = index,
                            )
                        }
                    videoDao.insertVideos(entities)
                }
            }
        }

        suspend fun likePlaylist(id: String) {
            val playlist = playlistDao.getPlaylistById(id)
            playlist?.let {
                playlistDao.updatePlaylist(it.copy(isLiked = true))
            }
        }

        suspend fun unlikePlaylist(id: String) {
            val playlist = playlistDao.getPlaylistById(id)
            playlist?.let {
                playlistDao.updatePlaylist(it.copy(isLiked = false))
            }
        }
    }

@Singleton
class DownloadRepository
    @Inject
    constructor(
        private val downloadTaskDao: DownloadTaskDao,
        private val downloadService: DownloadService,
    ) {
        fun getAllTasks(): Flow<List<DownloadTask>> {
            return downloadTaskDao.getAllTasks().map { entities ->
                entities.map { it.toDownloadTask() }
            }
        }

        fun getTasksByStatus(status: String): Flow<List<DownloadTask>> {
            return downloadTaskDao.getTasksByStatus(status).map { entities ->
                entities.map { it.toDownloadTask() }
            }
        }

        suspend fun getTaskById(id: String): DownloadTask? {
            return downloadTaskDao.getTaskById(id)?.toDownloadTask()
        }

        suspend fun startDownload(
            videoId: String,
            title: String,
            thumbnail: String,
        ): Result<String> {
            return downloadService.startDownload(videoId, title, thumbnail)
        }

        suspend fun pauseDownload(id: String): Result<Unit> {
            return downloadService.pauseDownload(id)
        }

        suspend fun resumeDownload(id: String): Result<Unit> {
            return downloadService.resumeDownload(id)
        }

        suspend fun cancelDownload(id: String): Result<Unit> {
            return downloadService.cancelDownload(id)
        }

        suspend fun clearCompletedDownloads(): Result<Unit> {
            downloadTaskDao.clearCompletedTasks()
            return Result.success(Unit)
        }

        suspend fun clearAllDownloads(): Result<Unit> {
            downloadTaskDao.clearAllTasks()
            return Result.success(Unit)
        }
    }

@Singleton
class VideoProgressRepository
    @Inject
    constructor(
        private val videoProgressDao: VideoProgressDao,
    ) {
        fun getAllProgress(): Flow<List<VideoProgress>> {
            return videoProgressDao.getAllProgress().map { entities ->
                entities.map { it.toVideoProgress() }
            }
        }

        suspend fun getProgress(videoId: String): VideoProgress? {
            return videoProgressDao.getProgress(videoId)?.toVideoProgress()
        }

        suspend fun saveProgress(
            videoId: String,
            watchedSeconds: Int,
            totalSeconds: Int,
        ) {
            val now = System.currentTimeMillis()
            val isCompleted = watchedSeconds >= totalSeconds - 10

            val progress =
                VideoProgressEntity(
                    videoId = videoId,
                    watchedDuration = watchedSeconds,
                    totalDuration = totalSeconds,
                    lastWatched = now,
                    isCompleted = isCompleted,
                )

            videoProgressDao.insertProgress(progress)
        }

        suspend fun clearProgress(videoId: String) {
            videoProgressDao.deleteProgressById(videoId)
        }

        suspend fun cleanupOldProgress(daysOld: Int = 30) {
            val timestamp = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
            videoProgressDao.deleteOldProgress(timestamp)
        }
    }

// Extension functions for mapping
private fun VideoEntity.toVideo(): Video {
    return Video(
        id = id,
        title = title,
        thumbnail = thumbnailUrl ?: "",
        channelName = channelName ?: "",
        channelId = channelId ?: "",
        views = viewCount?.toLongOrNull() ?: 0,
        duration = parseDuration(duration),
        uploadDate = uploadDate ?: "",
        description = description ?: "",
        isLiked = isLiked,
        isWatched = false,
        streamUrl = streamUrl,
        audioStreamUrl = audioStreamUrl,
    )
}

private fun Video.toEntity(): VideoEntity {
    return toEntity(
        playlistId = null,
        position = null,
    )
}

private fun Video.toEntity(
    playlistId: String?,
    position: Int?,
): VideoEntity {
    return VideoEntity(
        id = id,
        title = title,
        channelName = channelName,
        channelId = channelId,
        viewCount = views.toString(),
        uploadDate = uploadDate,
        duration = formatDuration(duration),
        thumbnailUrl = thumbnail,
        isLiked = isLiked,
        isWatchLater = false,
        description = description,
        url = "https://www.youtube.com/watch?v=$id",
        filePath = null,
        isDownloaded = false,
        playlistId = playlistId,
        position = position,
        addedDate = System.currentTimeMillis().toString(),
        streamUrl = streamUrl,
        audioStreamUrl = audioStreamUrl,
    )
}

private fun PlaylistEntity.toPlaylist(): Playlist {
    return Playlist(
        id = id,
        title = title,
        thumbnail = thumbnailUrl ?: "",
        videoCount = videoCount ?: 0,
        videos = emptyList(),
    )
}

private fun Playlist.toEntity(): PlaylistEntity {
    return PlaylistEntity(
        id = id,
        title = title,
        thumbnailUrl = thumbnail,
        videoCount = videoCount,
        description = null,
        uploader = null,
        uploaderUrl = null,
        lastUpdated = System.currentTimeMillis().toString(),
        isLiked = false,
        isWatchLater = false,
        channel = null,
        channelId = null,
        channelUrl = null,
        availability = null,
        modifiedDate = null,
        viewCount = null,
    )
}

private fun DownloadTaskEntity.toDownloadTask(): DownloadTask {
    return DownloadTask(
        id = id,
        videoId = url,
        title = title,
        thumbnail = thumbnailUrl ?: "",
        status = DownloadStatus.valueOf(status),
        progress = progress,
        downloadedBytes = downloadedBytes,
        totalBytes = totalBytes,
        speed = speed?.toLongOrNull() ?: 0,
        filePath = filePath ?: "",
        createdAt = addedDate.toLongOrNull() ?: 0,
        completedAt = null,
        error = error,
    )
}

private fun VideoProgressEntity.toVideoProgress(): VideoProgress {
    return VideoProgress(
        videoId = videoId,
        title = "",
        thumbnail = "",
        watchedAt = lastWatched,
        progress = watchedDuration.toFloat() / totalDuration,
        duration = totalDuration,
        watchedDuration = watchedDuration,
        isCompleted = isCompleted,
    )
}

private fun parseDuration(duration: String?): Int {
    return duration?.let {
        val parts = it.split(":")
        when (parts.size) {
            2 -> parts[0].toInt() * 60 + parts[1].toInt()
            3 -> parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()
            else -> 0
        }
    } ?: 0
}

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}
