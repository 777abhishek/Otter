package com.Otter.app.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Otter.app.data.auth.YouTubeProfileStore
import com.Otter.app.data.models.Playlist
import com.Otter.app.data.models.Video
import com.Otter.app.data.repositories.PlaylistRepository
import com.Otter.app.data.repositories.VideoRepository
import com.Otter.app.data.sync.SubscriptionSyncService
import com.Otter.app.data.ytdlp.YtDlpManager
import com.Otter.app.work.PlaylistWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

sealed class PlaylistUiState {
    data object Loading : PlaylistUiState()

    data class Success(val playlist: Playlist?, val videos: List<Video>) : PlaylistUiState()

    data class Error(val message: String) : PlaylistUiState()
}

@HiltViewModel
class PlaylistViewModel
    @Inject
    constructor(
        private val ytDlpManager: YtDlpManager,
        private val cookieStore: YouTubeProfileStore,
        private val syncService: SubscriptionSyncService,
        private val playlistRepository: PlaylistRepository,
        private val videoRepository: VideoRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Loading)
        val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

        private var observationJob: Job? = null

        fun loadPlaylist(playlistId: String) {
            viewModelScope.launch {
                try {
                    observationJob?.cancel()

                    if (playlistId.isBlank() || playlistId == "playlists" || playlistId.length < 2) {
                        _uiState.value = PlaylistUiState.Error("Invalid playlist ID: $playlistId")
                        return@launch
                    }

                    _uiState.value = PlaylistUiState.Loading

                    observationJob =
                        viewModelScope.launch {
                            playlistRepository.observePlaylistById(playlistId)
                                .combine(videoRepository.getVideosByPlaylist(playlistId)) { playlist, videos ->
                                    playlist to videos
                                }
                                .collect { (playlist, videos) ->
                                    if (playlist != null || videos.isNotEmpty()) {
                                        _uiState.value = PlaylistUiState.Success(playlist, videos)
                                    } else {
                                        _uiState.value = PlaylistUiState.Loading
                                    }
                                }
                        }
                } catch (e: Exception) {
                    _uiState.value = PlaylistUiState.Error(e.message ?: "Unknown error")
                }
            }
        }

        fun syncPlaylist(playlistId: String) {
            viewModelScope.launch {
                try {
                    syncService.syncPlaylistTwoStageSilent(playlistId)
                } catch (e: Exception) {
                    // Sync failed silently, don't update UI state - let the observer handle it
                }
            }
        }

        fun likeVideo(videoId: String) {
            viewModelScope.launch {
                videoRepository.likeVideo(videoId)
            }
        }

        fun unlikeVideo(videoId: String) {
            viewModelScope.launch {
                videoRepository.unlikeVideo(videoId)
            }
        }

        fun addToWatchLater(videoId: String) {
            viewModelScope.launch {
                videoRepository.addToWatchLater(videoId)
            }
        }

        fun removeFromWatchLater(videoId: String) {
            viewModelScope.launch {
                videoRepository.removeFromWatchLater(videoId)
            }
        }
    }
