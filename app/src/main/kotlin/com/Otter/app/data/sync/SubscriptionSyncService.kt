package com.Otter.app.data.sync

import com.Otter.app.data.auth.CookieAuthStore
import com.Otter.app.data.auth.CookieTargetCatalog
import com.Otter.app.data.models.Playlist
import com.Otter.app.data.repositories.PlaylistRepository
import com.Otter.app.data.repositories.VideoRepository
import com.Otter.app.data.ytdlp.YtDlpManager
import com.Otter.app.service.NotificationManager
import com.Otter.app.util.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionSyncService
    @Inject
    constructor(
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

        private fun isSpecialPlaylistId(id: String): Boolean {
            return id.equals(SPECIAL_PLAYLIST_LIKED, ignoreCase = true) ||
                id.equals(SPECIAL_PLAYLIST_WATCH_LATER, ignoreCase = true)
        }

        private suspend fun requireValidCookiesFilePath(): String {
            val cookiesFilePath =
                cookieAuthStore.getCookiesFilePathOnce(
                    profileId = cookieAuthStore.getActiveProfileIdOrNull() ?: throw IllegalStateException("No active profile selected"),
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

        suspend fun syncPlaylist(playlistId: String): Result<SyncResult> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                setSyncState(SyncState.Syncing(stage = "Starting sync", progress = 0f))

                try {
                    setSyncState(SyncState.Syncing(stage = "Loading cookies", progress = 0.02f))
                    val cookiesFilePath = requireValidCookiesFilePath()

                    setSyncState(
                        SyncState.Syncing(
                            stage = "Fetching playlist videos",
                            progress = 0.05f,
                        ),
                    )

                    val videos =
                        ytDlpManager.extractPlaylistVideos(
                            playlistId = playlistId,
                            cookiesFilePath = cookiesFilePath,
                        ).getOrThrow()

                    setSyncState(
                        SyncState.Syncing(
                            stage = "Fetched ${videos.size} videos",
                            progress = 0.70f,
                        ),
                    )

                    val existingPlaylist = playlistRepository.getPlaylistById(playlistId)
                    val playlist =
                        existingPlaylist ?: Playlist(
                            id = playlistId,
                            title = "Playlist",
                            thumbnail = "",
                            videoCount = videos.size,
                            videos = emptyList(),
                        )

                    setSyncState(
                        SyncState.Syncing(
                            stage = "Storing to database",
                            progress = 0.80f,
                        ),
                    )
                    playlistRepository.upsertPlaylistWithVideos(playlist, videos)

                    setSyncState(
                        SyncState.Syncing(
                            stage = "Finalizing",
                            progress = 0.95f,
                        ),
                    )

                    val result =
                        SyncResult(
                            playlistsCount = 1,
                            watchLaterCount = 0,
                            likedVideosCount = 0,
                            totalVideosCount = videos.size,
                        )

                    setSyncState(SyncState.Success(result))

                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Playlist sync completed: id=$playlistId videos=${videos.size} in ${duration}ms")

                    Result.success(result)
                } catch (e: Exception) {
                    fileLogger.logError(TAG, "Playlist sync failed: id=$playlistId", e)
                    setSyncState(SyncState.Error(e.message ?: "Unknown error"))
                    Result.failure(e)
                }
            }

        suspend fun syncWatchLaterVideos(): Result<Int> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                setSyncState(SyncState.Syncing(stage = "Syncing watch later", progress = 0f))

                try {
                    val cookiesFilePath = requireValidCookiesFilePath()

                    val videos =
                        ytDlpManager.extractWatchLater(cookiesFilePath)
                            .getOrThrow()

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
                        playlist =
                            playlist.copy(
                                thumbnail = videos.firstOrNull()?.thumbnail.orEmpty(),
                                videoCount = videos.size,
                            ),
                        videos = videos,
                    )

                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Watch later sync completed: ${videos.size} videos in ${duration}ms")

                    setSyncState(
                        SyncState.Success(
                            SyncResult(
                                playlistsCount = 0,
                                watchLaterCount = videos.size,
                                likedVideosCount = 0,
                                totalVideosCount = videos.size,
                            ),
                        ),
                    )
                    Result.success(videos.size)
                } catch (e: Exception) {
                    fileLogger.logError(TAG, "Watch later sync failed", e)
                    setSyncState(SyncState.Error(e.message ?: "Unknown error"))
                    Result.failure(e)
                }
            }

        suspend fun syncLikedVideos(): Result<Int> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                setSyncState(SyncState.Syncing(stage = "Syncing liked videos", progress = 0f))

                try {
                    val cookiesFilePath = requireValidCookiesFilePath()

                    val videos =
                        ytDlpManager.extractLikedVideos(cookiesFilePath)
                            .getOrThrow()

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
                        playlist =
                            playlist.copy(
                                thumbnail = videos.firstOrNull()?.thumbnail.orEmpty(),
                                videoCount = videos.size,
                            ),
                        videos = videos,
                    )

                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Liked videos sync completed: ${videos.size} videos in ${duration}ms")

                    setSyncState(
                        SyncState.Success(
                            SyncResult(
                                playlistsCount = 0,
                                watchLaterCount = 0,
                                likedVideosCount = videos.size,
                                totalVideosCount = videos.size,
                            ),
                        ),
                    )
                    Result.success(videos.size)
                } catch (e: Exception) {
                    fileLogger.logError(TAG, "Liked videos sync failed", e)
                    setSyncState(SyncState.Error(e.message ?: "Unknown error"))
                    Result.failure(e)
                }
            }

        private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
        val syncState: Flow<SyncState> = _syncState.asStateFlow()

        fun dismissSyncState() {
            setSyncState(SyncState.Idle)
        }

        suspend fun syncAll(): Result<SyncResult> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                setSyncState(SyncState.Syncing(stage = "Starting sync", progress = 0f))

                try {
                    setSyncState(SyncState.Syncing(stage = "Loading cookies", progress = 0.01f))
                    val cookiesFilePath = requireValidCookiesFilePath()

                    setSyncState(SyncState.Syncing(stage = "Discovering playlists", progress = 0.02f))
                    val discovered = syncPlaylists(cookiesFilePath).getOrElse { emptyList() }
                    val validPlaylists =
                        discovered.filter { playlist ->
                            val isValid =
                                playlist.id.isNotBlank() &&
                                    playlist.id != "playlists" &&
                                    playlist.id.length >= 2
                            if (!isValid) {
                                fileLogger.log(TAG, "Filtered out playlist: id='${playlist.id}' title='${playlist.title}'")
                            }
                            isValid
                        }

                    val totalSteps =
                        (1 /* cookies */ + 1 /* discover */ + validPlaylists.size /* fetch */ + validPlaylists.size /* store */ + 1 /* finalize */)
                            .coerceAtLeast(1)
                    val stepsDone = AtomicInteger(0)

                    fun reportStep(stage: String) {
                        val done = stepsDone.incrementAndGet()
                        val progress = done.toFloat() / totalSteps.toFloat()
                        setSyncState(
                            SyncState.Syncing(
                                stage = stage,
                                progress = progress.coerceIn(0f, 0.99f),
                            ),
                        )
                    }

                    // Mark cookies + discover as completed steps now that we have valid playlists.
                    stepsDone.addAndGet(2)
                    setSyncState(
                        SyncState.Syncing(
                            stage = "Fetching playlist videos (0/${validPlaylists.size})",
                            progress = (stepsDone.get().toFloat() / totalSteps.toFloat()).coerceIn(0f, 0.99f),
                        ),
                    )

                    setSyncState(
                        SyncState.Syncing(
                            stage = "Fetching playlist videos",
                            progress = (stepsDone.get().toFloat() / totalSteps.toFloat()).coerceIn(0f, 0.99f),
                        ),
                    )
                    val playlistsWithVideos =
                        fetchPlaylistVideos(
                            playlists = validPlaylists,
                            cookiesFilePath = cookiesFilePath,
                            totalSteps = totalSteps,
                            stepsDone = stepsDone,
                            reportStep = ::reportStep,
                        )

                    setSyncState(
                        SyncState.Syncing(
                            stage = "Storing to database",
                            progress = (stepsDone.get().toFloat() / totalSteps.toFloat()).coerceIn(0f, 0.99f),
                        ),
                    )
                    storePlaylistsAndVideos(
                        playlistsWithVideos,
                        totalSteps = totalSteps,
                        stepsDone = stepsDone,
                        reportStep = ::reportStep,
                    )

                    val watchLaterCount =
                        playlistsWithVideos
                            .firstOrNull { it.id == SPECIAL_PLAYLIST_WATCH_LATER }
                            ?.videos
                            ?.size
                            ?: 0
                    val likedVideosCount =
                        playlistsWithVideos
                            .firstOrNull { it.id == SPECIAL_PLAYLIST_LIKED }
                            ?.videos
                            ?.size
                            ?: 0
                    val totalVideos = playlistsWithVideos.sumOf { it.videos.size }

                    val result =
                        SyncResult(
                            playlistsCount = validPlaylists.size,
                            watchLaterCount = watchLaterCount,
                            likedVideosCount = likedVideosCount,
                            totalVideosCount = totalVideos,
                        )

                    val syncDuration = System.currentTimeMillis() - startTime
                    fileLogger.log(TAG, "Sync all completed in ${syncDuration}ms: playlists=${validPlaylists.size}")

                    reportStep("Finalizing")
                    setSyncState(SyncState.Success(result))
                    Result.success(result)
                } catch (e: Exception) {
                    fileLogger.logError(TAG, "Sync all failed", e)
                    setSyncState(SyncState.Error(e.message ?: "Unknown error"))
                    Result.failure(e)
                }
            }

        private suspend fun syncPlaylists(cookiesFilePath: String?): Result<List<Playlist>> {
            return runCatching {
                val result = ytDlpManager.extractPlaylists(cookiesFilePath)
                val playlists = result.getOrThrow()
                playlists
            }
        }

        private suspend fun fetchPlaylistVideos(
            playlists: List<Playlist>,
            cookiesFilePath: String?,
            totalSteps: Int,
            stepsDone: AtomicInteger,
            reportStep: (String) -> Unit,
        ): List<Playlist> =
            coroutineScope {
                fileLogger.log(TAG, "Starting to fetch videos for ${playlists.size} playlists")
                val semaphore = Semaphore(6)

                val total = playlists.size.coerceAtLeast(1)
                val completed = AtomicInteger(0)

                playlists.mapIndexed { index, playlist ->
                    async {
                        semaphore.withPermit {
                            try {
                                fileLogger.log(TAG, "[${index + 1}/${playlists.size}] Fetching videos for playlist: ${playlist.title}")
                                val videos =
                                    ytDlpManager.extractPlaylistVideos(
                                        playlistId = playlist.id,
                                        cookiesFilePath = cookiesFilePath,
                                    ).getOrNull() ?: emptyList()
                                fileLogger.log(TAG, "[${index + 1}/${playlists.size}] Completed: ${playlist.title} - ${videos.size} videos")

                                val done = completed.incrementAndGet()
                                reportStep("Fetched $done/${playlists.size} playlists")

                                playlist.copy(videos = videos)
                            } catch (e: Exception) {
                                fileLogger.log(TAG, "[${index + 1}/${playlists.size}] Failed: ${playlist.title} - Error: ${e.message}")

                                val done = completed.incrementAndGet()
                                reportStep("Fetched $done/${playlists.size} playlists")

                                playlist.copy(videos = emptyList())
                            }
                        }
                    }
                }.awaitAll()
            }

        private suspend fun storePlaylistsAndVideos(
            playlists: List<Playlist>,
            totalSteps: Int,
            stepsDone: AtomicInteger,
            reportStep: (String) -> Unit,
        ) = coroutineScope {
            fileLogger.log(TAG, "Storing ${playlists.size} playlists and their videos to database")
            val semaphore = Semaphore(6)
            val toStore =
                playlists.filter { playlist ->
                    playlist.id.isNotBlank() && playlist.id != "playlists" && playlist.id.length >= 2
                }

            val completed = AtomicInteger(0)

            toStore.mapIndexed { index, playlist ->
                async {
                    semaphore.withPermit {
                        fileLogger.log(TAG, "[${index + 1}/${playlists.size}] Storing: ${playlist.title} - ${playlist.videos.size} videos")
                        playlistRepository.upsertPlaylistWithVideos(
                            playlist = playlist,
                            videos = playlist.videos,
                        )

                        val done = completed.incrementAndGet()
                        reportStep("Stored $done/${toStore.size} playlists")
                    }
                }
            }.awaitAll()
            val totalVideos = playlists.sumOf { it.videos.size }
            fileLogger.log(TAG, "Successfully stored ${playlists.size} playlists with $totalVideos total videos")
        }

        sealed class SyncState {
            object Idle : SyncState()

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

        private fun setSyncState(state: SyncState) {
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
    }
