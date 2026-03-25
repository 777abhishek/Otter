package com.Otter.app.data.sync

import com.Otter.app.data.auth.CookieAuthStore
import com.Otter.app.data.auth.CookieTargetCatalog
import com.Otter.app.data.models.Playlist
import com.Otter.app.data.models.Video
import com.Otter.app.data.repositories.PlaylistRepository
import com.Otter.app.data.repositories.VideoRepository
import com.Otter.app.data.ytdlp.YtDlpManager
import com.Otter.app.service.NotificationManager
import com.Otter.app.util.FileLogger
import com.Otter.app.work.PlaylistFullMetadataWorkScheduler
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionSyncService
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val ytDlpManager: YtDlpManager,
        private val cookieAuthStore: CookieAuthStore,
        private val playlistRepository: PlaylistRepository,
        private val videoRepository: VideoRepository,
        private val notificationManager: NotificationManager,
        private val fileLogger: FileLogger,
    ) {
        companion object {
            private const val TAG = "SubscriptionSyncService"
            private const val SPECIAL_PLAYLIST_LIKED = "LL"
            private const val SPECIAL_PLAYLIST_WATCH_LATER = "WL"
        }

        // --------------------------------------------------------------------
        // Sync state (UI)
        // --------------------------------------------------------------------

        sealed class SyncState {
            data object Idle : SyncState()

            data class Syncing(
                val stage: String,
                val progress: Float? = null,
            ) : SyncState()

            data class Success(val result: SyncResult) : SyncState()

            data class Error(val message: String) : SyncState()
        }

        data class SyncResult(
            val playlistsCount: Int,
            val watchLaterCount: Int,
            val likedVideosCount: Int,
            val totalVideosCount: Int,
        )

        private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
        val syncState: Flow<SyncState> = _syncState.asStateFlow()

        fun dismissSyncState() {
            setSyncState(SyncState.Idle)
        }

        internal fun setSyncState(state: SyncState) {
            _syncState.value = state

            when (state) {
                is SyncState.Syncing -> {
                    notificationManager.updateSyncNotification(
                        title = "Syncing",
                        text = state.stage,
                        progress = state.progress,
                        ongoing = true,
                    )
                }

                is SyncState.Success -> {
                    val summary = "${state.result.playlistsCount} playlists • ${state.result.totalVideosCount} videos"
                    notificationManager.updateSyncNotification(
                        title = "Sync complete",
                        text = summary,
                        progress = null,
                        ongoing = false,
                    )
                }

                is SyncState.Error -> {
                    notificationManager.updateSyncNotification(
                        title = "Sync failed",
                        text = state.message,
                        progress = null,
                        ongoing = false,
                    )
                }

                SyncState.Idle -> {
                    notificationManager.cancelSyncNotification()
                }
            }
        }

        // --------------------------------------------------------------------
        // Cancellation + job tracking
        // --------------------------------------------------------------------

        private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private val activeJobsLock = Any()
        private val activeSyncJobs = LinkedHashSet<Job>()

        private fun trackJob(job: Job) {
            synchronized(activeJobsLock) {
                activeSyncJobs.add(job)
            }
            job.invokeOnCompletion {
                synchronized(activeJobsLock) {
                    activeSyncJobs.remove(job)
                }
            }
        }

        private suspend fun <T> runCancellableSync(block: suspend () -> T): T {
            val job = syncScope.async { block() }
            trackJob(job)
            return job.await()
        }

        /**
         * Sync a single playlist with exponential backoff retry (max 3 attempts)
         * Handles rate limiting and network errors gracefully
         * Delays between requests: 1s, 2s, 4s
         */
        private suspend fun syncPlaylistWithRetry(
            playlist: Playlist,
            cookiesFilePath: String?,
            attempt: Int = 1,
            maxAttempts: Int = 3,
        ): List<com.Otter.app.data.models.Video>? {
            return try {
                // Add exponential delay between retries (first request: 0.5s, retries: 1s, 2s)
                val delayMs = if (attempt == 1) 500L else (1000L shl (attempt - 2))
                kotlinx.coroutines.delay(delayMs)

                ytDlpManager.extractPlaylistVideos(
                    playlistId = playlist.id,
                    cookiesFilePath = cookiesFilePath,
                ).getOrNull()
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error"
                when {
                    // Rate limiting - wait longer and retry
                    errorMsg.contains("429", ignoreCase = true) || errorMsg.contains("too many", ignoreCase = true) -> {
                        if (attempt < maxAttempts) {
                            fileLogger.log(TAG, "Playlist ${playlist.id}: Rate limited (429), retry attempt $attempt/${maxAttempts}")
                            kotlinx.coroutines.delay(5000L * (attempt))  // 5s, 10s, 15s for rate limits
                            syncPlaylistWithRetry(playlist, cookiesFilePath, attempt + 1, maxAttempts)
                        } else {
                            fileLogger.logError(TAG, "Playlist ${playlist.id}: Rate limited - max retries exceeded", e)
                            null
                        }
                    }
                    // Network timeout - retry with backoff
                    errorMsg.contains("timeout", ignoreCase = true) || errorMsg.contains("connection", ignoreCase = true) -> {
                        if (attempt < maxAttempts) {
                            fileLogger.log(TAG, "Playlist ${playlist.id}: Network error, retry attempt $attempt/${maxAttempts}")
                            syncPlaylistWithRetry(playlist, cookiesFilePath, attempt + 1, maxAttempts)
                        } else {
                            fileLogger.logError(TAG, "Playlist ${playlist.id}: Network error - max retries exceeded", e)
                            null
                        }
                    }
                    // Other errors - don't retry
                    else -> {
                        fileLogger.logError(TAG, "Playlist ${playlist.id}: Sync error (attempt $attempt/$maxAttempts)", e)
                        null
                    }
                }
            }
        }

        fun cancelOngoingSync() {
            val jobsToCancel =
                synchronized(activeJobsLock) {
                    activeSyncJobs
                        .toList()
                        .also { activeSyncJobs.clear() }
                }
            jobsToCancel.forEach { it.cancel(CancellationException("User cancelled")) }
            setSyncState(SyncState.Idle)
            notificationManager.cancelSyncNotification()
        }

        // --------------------------------------------------------------------
        // Public APIs
        // --------------------------------------------------------------------

        suspend fun syncAll(): Result<SyncResult> =
            runCancellableSync {
                val startTime = System.currentTimeMillis()
                setSyncState(SyncState.Syncing(stage = "Starting sync", progress = 0f))

                try {
                    setSyncState(SyncState.Syncing(stage = "Loading cookies", progress = 0.01f))
                    val cookiesFilePath = requireValidCookiesFilePath()

                    setSyncState(SyncState.Syncing(stage = "Discovering playlists", progress = 0.03f))
                    val discovered = ytDlpManager.extractPlaylists(cookiesFilePath).getOrThrow()
                    val playlists =
                        discovered.filter { p ->
                            p.id.isNotBlank() && p.id != "playlists" && p.id.length >= 2
                        }

                    if (playlists.isEmpty()) {
                        val result =
                            SyncResult(
                                playlistsCount = 0,
                                watchLaterCount = 0,
                                likedVideosCount = 0,
                                totalVideosCount = 0,
                            )
                        setSyncState(SyncState.Success(result))
                        return@runCancellableSync Result.success(result)
                    }

                    setSyncState(
                        SyncState.Syncing(
                            stage = "Fetching playlists (0/${playlists.size})",
                            progress = 0.05f,
                        ),
                    )

                    // Reduced to 2 concurrent requests to avoid rate limiting
                    // YouTube blocks aggressive multi-threaded access
                    val semaphore = Semaphore(2)
                    val completed = AtomicInteger(0)
                    val failed = mutableListOf<String>()

                    val playlistsWithVideos =
                        coroutineScope {
                            playlists.map { playlist ->
                                async {
                                    semaphore.withPermit {
                                        val result = syncPlaylistWithRetry(playlist, cookiesFilePath)

                                        if (result != null) {
                                            playlistRepository.upsertPlaylistWithVideos(
                                                playlist = playlist.copy(videoCount = result.size, videos = emptyList()),
                                                videos = result,
                                            )
                                            fileLogger.log(TAG, "Synced playlist: ${playlist.id} → ${result.size} videos")
                                        } else {
                                            failed.add(playlist.id)
                                        }

                                        val done = completed.incrementAndGet()
                                        setSyncState(
                                            SyncState.Syncing(
                                                stage = "Fetched $done/${playlists.size} playlists (${failed.size} retries)",
                                                progress = (0.05f + (0.85f * (done.toFloat() / playlists.size.toFloat()))).coerceIn(0f, 0.99f),
                                            ),
                                        )

                                        playlist.copy(videos = result ?: emptyList())
                                    }
                                }
                            }.awaitAll()
                        }
                    
                    if (failed.isNotEmpty()) {
                        fileLogger.logError(TAG, "${failed.size} playlists failed to sync: ${failed.joinToString(", ")}", null)
                    }

                    val watchLaterCount =
                        playlistsWithVideos.firstOrNull { it.id == SPECIAL_PLAYLIST_WATCH_LATER }?.videos?.size ?: 0
                    val likedVideosCount =
                        playlistsWithVideos.firstOrNull { it.id == SPECIAL_PLAYLIST_LIKED }?.videos?.size ?: 0
                    val totalVideos = playlistsWithVideos.sumOf { it.videos.size }

                    val result =
                        SyncResult(
                            playlistsCount = playlists.size,
                            watchLaterCount = watchLaterCount,
                            likedVideosCount = likedVideosCount,
                            totalVideosCount = totalVideos,
                        )

                    setSyncState(SyncState.Success(result))

                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Sync all completed in ${duration}ms: playlists=${playlists.size} videos=$totalVideos")

                    // Stage 2: Schedule metadata enrichment in background for all synced playlists
                    syncScope.launch(Dispatchers.IO) {
                        playlistsWithVideos
                            .asSequence()
                            .filter { it.videos.isNotEmpty() }
                            .forEach { playlist ->
                                try {
                                    if (playlist.id.isNotBlank() && playlist.id != "playlists" && playlist.id.length >= 2) {
                                        PlaylistFullMetadataWorkScheduler.enqueueFullMetadataSync(context, playlist.id)
                                        fileLogger.log(TAG, "Scheduled Stage 2 metadata enrichment for playlist: ${playlist.id}")
                                    }
                                } catch (e: Exception) {
                                    fileLogger.logError(TAG, "Failed to schedule Stage 2 for playlist ${playlist.id}", e)
                                }
                        }
                    }

                    Result.success(result)
                } catch (e: Exception) {
                    fileLogger.logError(TAG, "Sync all failed", e)
                    setSyncState(SyncState.Error(e.message ?: "Unknown error"))
                    Result.failure(e)
                }
            }

        suspend fun syncPlaylistTwoStage(playlistId: String): Result<SyncResult> =
            runCancellableSync {
                val startTime = System.currentTimeMillis()
                setSyncState(SyncState.Syncing(stage = "Syncing playlist", progress = 0f))

                try {
                    setSyncState(SyncState.Syncing(stage = "Loading cookies", progress = 0.02f))
                    val cookiesFilePath = requireValidCookiesFilePath()

                    setSyncState(
                        SyncState.Syncing(
                            stage = "Fetching videos",
                            progress = 0.05f,
                        ),
                    )

                    val flatVideos =
                        ytDlpManager.extractPlaylistVideos(
                            playlistId = playlistId,
                            cookiesFilePath = cookiesFilePath,
                        ).getOrThrow()

                    val existingPlaylist = playlistRepository.getPlaylistById(playlistId)
                    val playlist =
                        existingPlaylist
                            ?: Playlist(
                                id = playlistId,
                                title = "Playlist",
                                thumbnail = "",
                                videoCount = flatVideos.size,
                                videos = emptyList(),
                            )

                    setSyncState(
                        SyncState.Syncing(
                            stage = "Storing data",
                            progress = 0.50f,
                        ),
                    )

                    playlistRepository.upsertPlaylistWithVideos(playlist, flatVideos)

                    val result =
                        SyncResult(
                            playlistsCount = 1,
                            watchLaterCount = 0,
                            likedVideosCount = 0,
                            totalVideosCount = flatVideos.size,
                        )

                    setSyncState(SyncState.Success(result))

                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Playlist sync completed: id=$playlistId videos=${flatVideos.size} in ${duration}ms")

                    // Stage 2: Schedule metadata enrichment in background for this playlist
                    syncScope.launch(Dispatchers.IO) {
                        try {
                            PlaylistFullMetadataWorkScheduler.enqueueFullMetadataSync(context, playlistId)
                            fileLogger.log(TAG, "Scheduled Stage 2 metadata enrichment for playlist: $playlistId")
                        } catch (e: Exception) {
                            fileLogger.logError(TAG, "Failed to schedule Stage 2 for playlist $playlistId", e)
                        }
                    }

                    Result.success(result)
                } catch (e: Exception) {
                    fileLogger.logError(TAG, "Playlist sync failed: id=$playlistId", e)
                    setSyncState(SyncState.Error(e.message ?: "Unknown error"))
                    Result.failure(e)
                }
            }

        suspend fun syncPlaylistTwoStageSilent(playlistId: String): Result<SyncResult> =
            runCancellableSync {
                val startTime = System.currentTimeMillis()

                try {
                    val cookiesFilePath = requireValidCookiesFilePath()

                    notificationManager.updateSyncNotification(
                        title = "Syncing Playlist",
                        text = "Loading videos...",
                        progress = 0.1f,
                        ongoing = true,
                    )

                    val flatVideos =
                        ytDlpManager.extractPlaylistVideos(
                            playlistId = playlistId,
                            cookiesFilePath = cookiesFilePath,
                        ).getOrThrow()

                    val existingPlaylist = playlistRepository.getPlaylistById(playlistId)
                    val playlist =
                        existingPlaylist
                            ?: Playlist(
                                id = playlistId,
                                title = "Playlist",
                                thumbnail = "",
                                videoCount = flatVideos.size,
                                videos = emptyList(),
                            )

                    playlistRepository.upsertPlaylistWithVideos(playlist, flatVideos)

                    notificationManager.updateSyncNotification(
                        title = "Sync Complete",
                        text = "${flatVideos.size} videos synced",
                        progress = 1f,
                        ongoing = false,
                    )

                    val result =
                        SyncResult(
                            playlistsCount = 1,
                            watchLaterCount = 0,
                            likedVideosCount = 0,
                            totalVideosCount = flatVideos.size,
                        )

                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Playlist sync completed: id=$playlistId videos=${flatVideos.size} in ${duration}ms")

                    // Stage 2: Schedule metadata enrichment in background for this playlist
                    syncScope.launch(Dispatchers.IO) {
                        try {
                            PlaylistFullMetadataWorkScheduler.enqueueFullMetadataSync(context, playlistId)
                            fileLogger.log(TAG, "Scheduled Stage 2 metadata enrichment for playlist: $playlistId")
                        } catch (e: Exception) {
                            fileLogger.logError(TAG, "Failed to schedule Stage 2 for playlist $playlistId", e)
                        }
                    }

                    Result.success(result)
                } catch (e: Exception) {
                    fileLogger.logError(TAG, "Playlist sync failed: id=$playlistId", e)
                    notificationManager.cancelSyncNotification()
                    Result.failure(e)
                }
            }

        suspend fun syncWatchLaterVideos(): Result<Int> =
            runCancellableSync {
                val startTime = System.currentTimeMillis()
                setSyncState(SyncState.Syncing(stage = "Syncing watch later", progress = 0f))

                try {
                    val cookiesFilePath = requireValidCookiesFilePath()
                    val videos = ytDlpManager.extractWatchLater(cookiesFilePath).getOrThrow()

                    val playlist =
                        playlistRepository.getPlaylistById(SPECIAL_PLAYLIST_WATCH_LATER)
                            ?: Playlist(
                                id = SPECIAL_PLAYLIST_WATCH_LATER,
                                title = "Watch Later",
                                thumbnail = videos.firstOrNull()?.thumbnail.orEmpty(),
                                videoCount = videos.size,
                                videos = emptyList(),
                            )

                    playlistRepository.upsertPlaylistWithVideos(
                        playlist = playlist.copy(
                            thumbnail = videos.firstOrNull()?.thumbnail.orEmpty(),
                            videoCount = videos.size,
                        ),
                        videos = videos,
                    )

                    val result =
                        SyncResult(
                            playlistsCount = 0,
                            watchLaterCount = videos.size,
                            likedVideosCount = 0,
                            totalVideosCount = videos.size,
                        )

                    setSyncState(SyncState.Success(result))

                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Watch later sync completed: ${videos.size} videos in ${duration}ms")

                    Result.success(videos.size)
                } catch (e: Exception) {
                    fileLogger.logError(TAG, "Watch later sync failed", e)
                    setSyncState(SyncState.Error(e.message ?: "Unknown error"))
                    Result.failure(e)
                }
            }

        suspend fun syncLikedVideos(): Result<Int> =
            runCancellableSync {
                val startTime = System.currentTimeMillis()
                setSyncState(SyncState.Syncing(stage = "Syncing liked videos", progress = 0f))

                try {
                    val cookiesFilePath = requireValidCookiesFilePath()
                    val videos = ytDlpManager.extractLikedVideos(cookiesFilePath).getOrThrow()

                    val playlist =
                        playlistRepository.getPlaylistById(SPECIAL_PLAYLIST_LIKED)
                            ?: Playlist(
                                id = SPECIAL_PLAYLIST_LIKED,
                                title = "Liked Videos",
                                thumbnail = videos.firstOrNull()?.thumbnail.orEmpty(),
                                videoCount = videos.size,
                                videos = emptyList(),
                            )

                    playlistRepository.upsertPlaylistWithVideos(
                        playlist = playlist.copy(
                            thumbnail = videos.firstOrNull()?.thumbnail.orEmpty(),
                            videoCount = videos.size,
                        ),
                        videos = videos,
                    )

                    val result =
                        SyncResult(
                            playlistsCount = 0,
                            watchLaterCount = 0,
                            likedVideosCount = videos.size,
                            totalVideosCount = videos.size,
                        )

                    setSyncState(SyncState.Success(result))

                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Liked videos sync completed: ${videos.size} videos in ${duration}ms")

                    Result.success(videos.size)
                } catch (e: Exception) {
                    fileLogger.logError(TAG, "Liked videos sync failed", e)
                    setSyncState(SyncState.Error(e.message ?: "Unknown error"))
                    Result.failure(e)
                }
            }

        // --------------------------------------------------------------------
        // Helpers
        // --------------------------------------------------------------------

        private suspend fun requireValidCookiesFilePath(): String {
            val profileId = cookieAuthStore.getActiveProfileIdOrNull() ?: throw IllegalStateException("No active profile selected")
            val cookiesFilePath =
                cookieAuthStore.getCookiesFilePathOnce(
                    profileId = profileId,
                    targetId = CookieTargetCatalog.TARGET_YOUTUBE,
                    requireEnabled = true,
                )
                    ?: throw IllegalStateException(
                        "Cookies are not enabled for YouTube. Enable cookies for yt-dlp in Profiles → Cookie Targets and connect/import cookies.",
                    )

            val cookieFile = File(cookiesFilePath)
            if (!cookieFile.exists() || cookieFile.length() < 50L) {
                throw IllegalStateException("Cookies file missing/empty. Please connect again and export/import cookies.")
            }

            return cookiesFilePath
        }
    }
