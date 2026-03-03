package com.Otter.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.Otter.app.data.auth.YouTubeProfileStore
import com.Otter.app.data.models.Playlist
import com.Otter.app.data.models.Video
import com.Otter.app.data.repositories.PlaylistRepository
import com.Otter.app.data.repositories.VideoRepository
import com.Otter.app.data.ytdlp.YtDlpManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe

@HiltWorker
class PlaylistMetadataWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val ytDlpManager: YtDlpManager,
        private val cookieStore: YouTubeProfileStore,
        private val playlistRepository: PlaylistRepository,
        private val videoRepository: VideoRepository,
    ) : CoroutineWorker(context, params) {

        override suspend fun doWork(): Result {
            val playlistId = inputData.getString(KEY_PLAYLIST_ID) ?: return Result.failure()
            if (playlistId.isBlank() || playlistId == "playlists" || playlistId.length < 2) return Result.failure()

            return try {
                val cookiesFilePath = withContext(Dispatchers.IO) { cookieStore.getCookiesFilePathOnce() }

                // Phase 1: fetch playlist listing sequentially (chunk by chunk) and append to DB.
                val chunkSize = 200
                var start = 1
                var playlist = playlistRepository.getPlaylistById(playlistId)

                while (true) {
                    if (isStopped) return Result.success()

                    val end = start + chunkSize - 1
                    val chunk =
                        withContext(Dispatchers.IO) {
                            ytDlpManager.extractPlaylistVideosChunk(
                                playlistId = playlistId,
                                cookiesFilePath = cookiesFilePath,
                                startIndex = start,
                                endIndex = end,
                            ).getOrElse { emptyList() }
                        }

                    if (chunk.isEmpty()) break

                    val currentPlaylist =
                        playlist ?: Playlist(
                            id = playlistId,
                            title = "Unknown Playlist",
                            thumbnail = "",
                            videoCount = 0,
                            videos = emptyList(),
                        )

                    if (start == 1) {
                        playlistRepository.upsertPlaylistWithVideos(currentPlaylist, chunk)
                    } else {
                        playlistRepository.appendPlaylistVideos(currentPlaylist, chunk)
                    }

                    playlist = playlistRepository.getPlaylistById(playlistId) ?: currentPlaylist
                    start += chunkSize
                }

                // Phase 2: enrich existing videos one by one (NewPipe) and upsert into DB.
                // This is intentionally sequential to avoid UI jank and avoid hammering the network.
                val existing = videoRepository.getVideosByPlaylistOnce(playlistId)
                for (video in existing) {
                    if (isStopped) return Result.success()
                    val enriched = withContext(Dispatchers.IO) { enrichWithNewPipe(video) }
                    videoRepository.upsertVideoInPlaylistPreservingFlags(playlistId, enriched)
                }

                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }

        private fun enrichWithNewPipe(video: Video): Video {
            return try {
                val url = "https://www.youtube.com/watch?v=${video.id}"
                val service = NewPipe.getServiceByUrl(url)
                val extractor = service.getStreamExtractor(url)
                extractor.fetchPage()
                video.copy(
                    duration = extractor.length.toInt().coerceAtLeast(video.duration),
                    views = extractor.viewCount.takeIf { it > 0 } ?: video.views,
                    thumbnail = extractor.thumbnails.lastOrNull()?.url ?: video.thumbnail,
                    channelName = extractor.uploaderName?.takeIf { it.isNotBlank() } ?: video.channelName,
                    uploadDate = extractor.textualUploadDate ?: video.uploadDate,
                    streamUrl = extractor.hlsUrl,
                    audioStreamUrl = extractor.audioStreams.firstOrNull()?.url,
                )
            } catch (e: Exception) {
                video
            }
        }

        companion object {
            const val KEY_PLAYLIST_ID = "playlist_id"
            const val UNIQUE_WORK_PREFIX = "playlist_metadata_"
        }
    }
