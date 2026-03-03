package com.Otter.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Otter.app.data.repositories.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LikedViewModel
    @Inject
    constructor(
        private val videoRepository: VideoRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<LikedUiState>(LikedUiState.Loading)
        val uiState: StateFlow<LikedUiState> = _uiState.asStateFlow()

        val likedVideos =
            videoRepository.getLikedVideos()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )

        init {
            loadLikedVideos()
        }

        private fun loadLikedVideos() {
            viewModelScope.launch {
                _uiState.value = LikedUiState.Loading
                // Videos are loaded via Flow
                _uiState.value = LikedUiState.Success
            }
        }

        fun onUnlike(videoId: String) {
            viewModelScope.launch {
                videoRepository.unlikeVideo(videoId)
            }
        }

        fun onVideoClick(videoId: String) {
            // Navigate to player
        }
    }

sealed class LikedUiState {
    object Loading : LikedUiState()

    object Success : LikedUiState()
}
