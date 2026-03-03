package com.Otter.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Otter.app.data.repositories.PlaylistRepository
import com.Otter.app.data.repositories.VideoRepository
import com.Otter.app.data.sync.SubscriptionSyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel
    @Inject
    constructor(
        private val syncService: SubscriptionSyncService,
        private val playlistRepository: PlaylistRepository,
        private val videoRepository: VideoRepository,
    ) : ViewModel() {
        val syncState =
            syncService.syncState
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubscriptionSyncService.SyncState.Idle)

        val playlists =
            playlistRepository.getAllPlaylists()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val watchLater =
            videoRepository.getWatchLaterVideos()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val likedVideos =
            videoRepository.getLikedVideos()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        fun getPlaylistVideos(playlistId: String): Flow<List<com.Otter.app.data.models.Video>> {
            return videoRepository.getVideosByPlaylist(playlistId)
        }

        private val _isRefreshing = MutableStateFlow(false)
        val isRefreshing: StateFlow<Boolean> = _isRefreshing

        fun syncAll() {
            viewModelScope.launch {
                _isRefreshing.value = true
                try {
                    syncService.syncAll()
                } finally {
                    _isRefreshing.value = false
                }
            }
        }

        fun syncLikedVideos() {
            viewModelScope.launch {
                _isRefreshing.value = true
                try {
                    syncService.syncLikedVideos()
                } finally {
                    _isRefreshing.value = false
                }
            }
        }

        fun syncWatchLaterVideos() {
            viewModelScope.launch {
                _isRefreshing.value = true
                try {
                    syncService.syncWatchLaterVideos()
                } finally {
                    _isRefreshing.value = false
                }
            }
        }

        fun syncPlaylist(playlistId: String) {
            viewModelScope.launch {
                _isRefreshing.value = true
                try {
                    syncService.syncPlaylist(playlistId)
                } finally {
                    _isRefreshing.value = false
                }
            }
        }

        fun dismissSyncSnackbar() {
            syncService.dismissSyncState()
        }
    }
