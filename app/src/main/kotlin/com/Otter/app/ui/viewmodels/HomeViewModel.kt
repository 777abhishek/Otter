package com.Otter.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Otter.app.data.models.Video
import com.Otter.app.data.repositories.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val videoRepository: VideoRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        private val _searchQuery = MutableStateFlow("")
        val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

        private val _videos = MutableStateFlow<List<Video>>(emptyList())
        val videos: StateFlow<List<Video>> = _videos.asStateFlow()

        init {
            loadPopularVideos()
        }

        fun onSearchQueryChange(query: String) {
            _searchQuery.value = query
            if (query.isNotEmpty()) {
                searchVideos(query)
            } else {
                loadPopularVideos()
            }
        }

        private fun loadPopularVideos() {
            viewModelScope.launch {
                _uiState.value = HomeUiState.Loading
                videoRepository.getPopularVideos(20)
                    .onSuccess { videos ->
                        _videos.value = videos
                        _uiState.value = HomeUiState.Success(videos)
                    }
                    .onFailure { error ->
                        _uiState.value = HomeUiState.Error(error.message ?: "Unknown error")
                    }
            }
        }

        private fun searchVideos(query: String) {
            viewModelScope.launch {
                _uiState.value = HomeUiState.Loading
                videoRepository.searchVideos(query)
                    .onSuccess { videos ->
                        _videos.value = videos
                        _uiState.value = HomeUiState.Success(videos)
                    }
                    .onFailure { error ->
                        _uiState.value = HomeUiState.Error(error.message ?: "Unknown error")
                    }
            }
        }

        fun onVideoClick(videoId: String) {
            // Navigate to video player
        }

        fun onLikeClick(
            videoId: String,
            isLiked: Boolean,
        ) {
            viewModelScope.launch {
                if (isLiked) {
                    videoRepository.likeVideo(videoId)
                } else {
                    videoRepository.unlikeVideo(videoId)
                }
            }
        }
    }

sealed class HomeUiState {
    object Loading : HomeUiState()

    data class Success(val videos: List<Video>) : HomeUiState()

    data class Error(val message: String) : HomeUiState()
}
