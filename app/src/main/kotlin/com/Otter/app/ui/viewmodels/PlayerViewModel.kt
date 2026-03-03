package com.Otter.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Otter.app.data.models.Video
import com.Otter.app.data.repositories.ChapterInfo
import com.Otter.app.data.repositories.StreamInfoResult
import com.Otter.app.data.repositories.StreamRepository
import com.Otter.app.data.repositories.VideoProgressRepository
import com.Otter.app.data.repositories.VideoRepository
import com.Otter.app.service.SettingsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        private val videoRepository: VideoRepository,
        private val videoProgressRepository: VideoProgressRepository,
        private val streamRepository: StreamRepository,
        private val settingsService: SettingsService,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
        val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

        private val _currentVideo = MutableStateFlow<Video?>(null)
        val currentVideo: StateFlow<Video?> = _currentVideo.asStateFlow()

        private val _queue = MutableStateFlow<List<Video>>(emptyList())
        val queue: StateFlow<List<Video>> = _queue.asStateFlow()

        private val _currentQueueIndex = MutableStateFlow(0)
        val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

        private val _chapters = MutableStateFlow<List<ChapterInfo>>(emptyList())
        val chapters: StateFlow<List<ChapterInfo>> = _chapters.asStateFlow()

        private val _streamInfo = MutableStateFlow<StreamInfoResult?>(null)
        val streamInfo: StateFlow<StreamInfoResult?> = _streamInfo.asStateFlow()

        val settings =
            settingsService.getSettings()
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.Otter.app.data.models.AppSettings())

        // Expose available stream heights for quality selection
        val availableQualities: StateFlow<List<Int>> =
            _streamInfo.map { info ->
                info?.videoStreams?.map { it.height }?.distinct()?.sortedDescending() ?: emptyList()
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

        // Expose available audio bitrates for audio quality selection (in kbps)
        val availableAudioQualities: StateFlow<List<Int>> =
            _streamInfo.map { info ->
                info?.audioStreams?.map { it.bitrate / 1000 }?.distinct()?.sortedDescending() ?: emptyList()
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

        fun loadVideo(videoId: String) {
            viewModelScope.launch {
                _uiState.value = PlayerUiState.Loading

                // Get video from local DB or fetch info
                val video =
                    videoRepository.getVideoById(videoId)
                        ?: fetchAndSaveVideo(videoId)

                if (video != null) {
                    _currentVideo.value = video
                    _duration.value = video.duration

                    // Extract stream info in background
                    extractStreamInfo(video)

                    // Check for resume position
                    val progress = videoProgressRepository.getProgress(videoId)
                    if (progress != null && !progress.isCompleted) {
                        _currentPosition.value = progress.watchedDuration
                    }

                    _uiState.value = PlayerUiState.Ready(video)
                } else {
                    _uiState.value = PlayerUiState.Error("Video not found")
                }
            }
        }

        suspend fun getResumePositionMs(videoId: String): Long {
            val progress = videoProgressRepository.getProgress(videoId)
            if (progress == null || progress.isCompleted) return 0L
            val seconds = progress.watchedDuration
            if (seconds <= 0) return 0L
            return seconds.toLong() * 1000L
        }

        private suspend fun fetchAndSaveVideo(videoId: String): Video? {
            // This would fetch from API if not in DB
            // For now return null - video should be pre-loaded from playlist
            return null
        }

        private suspend fun extractStreamInfo(video: Video) {
            val settings = settingsService.getSettings().first()
            val result =
                streamRepository.extractStreamInfo(
                    "https://www.youtube.com/watch?v=${video.id}",
                    settings,
                )

            result.onSuccess { info ->
                _streamInfo.value = info
                _chapters.value = info.chapters

                // Update video with stream URLs if needed
                val updatedVideo =
                    video.copy(
                        streamUrl =
                            streamRepository.selectBestVideoStream(info.videoStreams, settings)?.url
                                ?: info.videoStreams.firstOrNull()?.url,
                        audioStreamUrl =
                            streamRepository.selectBestAudioStream(info.audioStreams, settings)?.url
                                ?: info.audioStreams.firstOrNull()?.url,
                    )
                _currentVideo.value = updatedVideo
            }
        }

        private fun prefetchNextStreamInfo(fromIndex: Int) {
            val next = _queue.value.getOrNull(fromIndex + 1) ?: return
            viewModelScope.launch {
                val settings = settingsService.getSettings().first()
                // Warm the StreamRepository cache for smoother queue transitions.
                streamRepository.extractStreamInfo(
                    "https://www.youtube.com/watch?v=${next.id}",
                    settings,
                )
            }
        }

        fun setQueue(
            videos: List<Video>,
            startIndex: Int = 0,
        ) {
            _queue.value = videos
            _currentQueueIndex.value = startIndex

            // Also update current video if we have a valid index
            videos.getOrNull(startIndex)?.let {
                _currentVideo.value = it
            }

            prefetchNextStreamInfo(startIndex)
        }

        fun playQueueItem(index: Int) {
            val queue = _queue.value
            if (index in queue.indices) {
                _currentQueueIndex.value = index
                loadVideo(queue[index].id)
                prefetchNextStreamInfo(index)
            }
        }

        fun playNext() {
            val nextIndex = _currentQueueIndex.value + 1
            if (nextIndex < _queue.value.size) {
                playQueueItem(nextIndex)
            }
        }

        fun playPrevious() {
            val prevIndex = _currentQueueIndex.value - 1
            if (prevIndex >= 0) {
                playQueueItem(prevIndex)
            }
        }

        // Legacy methods for compatibility
        private val _isPlaying = MutableStateFlow(false)
        val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

        private val _currentPosition = MutableStateFlow(0)
        val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

        private val _duration = MutableStateFlow(0)
        val duration: StateFlow<Int> = _duration.asStateFlow()

        private val _isMuted = MutableStateFlow(false)
        val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

        fun onPlayPause() {
            _isPlaying.value = !_isPlaying.value
        }

        fun onSeek(position: Int) {
            _currentPosition.value = position
        }

        fun onRewind(seconds: Int = 10) {
            _currentPosition.value = (_currentPosition.value - seconds).coerceAtLeast(0)
        }

        fun onForward(seconds: Int = 10) {
            _currentPosition.value = (_currentPosition.value + seconds).coerceAtMost(_duration.value)
        }

        fun onMute() {
            _isMuted.value = !_isMuted.value
        }

        fun onLike() {
            val video = _currentVideo.value ?: return
            viewModelScope.launch {
                if (video.isLiked) {
                    videoRepository.unlikeVideo(video.id)
                } else {
                    videoRepository.likeVideo(video.id)
                }
                loadVideo(video.id)
            }
        }

        fun saveProgress(positionMs: Long) {
            val video = _currentVideo.value ?: return
            viewModelScope.launch {
                videoProgressRepository.saveProgress(
                    videoId = video.id,
                    watchedSeconds = (positionMs / 1000).toInt(),
                    totalSeconds = _duration.value,
                )
            }
        }

        fun setPlayerView(playerView: androidx.media3.ui.PlayerView) {
            // Player view is managed by PlayerService, this is for compatibility
        }
    }

sealed class PlayerUiState {
    object Loading : PlayerUiState()

    data class Ready(val video: Video) : PlayerUiState()

    data class Error(val message: String) : PlayerUiState()
}
