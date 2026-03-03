package com.Otter.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Otter.app.data.models.DownloadStatus
import com.Otter.app.data.repositories.DownloadRepository
import com.Otter.app.data.repositories.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel
    @Inject
    constructor(
        private val downloadRepository: DownloadRepository,
        private val videoRepository: VideoRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<DownloadUiState>(DownloadUiState.Loading)
        val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

        private val _selectedFilter = MutableStateFlow("All")
        val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

        val allDownloads =
            downloadRepository.getAllTasks()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )

        val filteredDownloads =
            combine(allDownloads, _selectedFilter) { downloads, filter ->
                when (filter) {
                    "All" -> downloads
                    "Downloading" -> downloads.filter { it.status == DownloadStatus.DOWNLOADING }
                    "Paused" -> downloads.filter { it.status == DownloadStatus.PAUSED }
                    "Completed" -> downloads.filter { it.status == DownloadStatus.COMPLETED }
                    "Failed" -> downloads.filter { it.status == DownloadStatus.FAILED }
                    else -> downloads
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

        fun onFilterChange(filter: String) {
            _selectedFilter.value = filter
        }

        fun startDownload(
            videoId: String,
            title: String,
            thumbnail: String,
        ) {
            viewModelScope.launch {
                _uiState.value = DownloadUiState.Loading
                downloadRepository.startDownload(videoId, title, thumbnail)
                    .onSuccess { taskId ->
                        _uiState.value = DownloadUiState.Success("Download started")
                    }
                    .onFailure { error ->
                        _uiState.value = DownloadUiState.Error(error.message ?: "Download failed")
                    }
            }
        }

        fun pauseDownload(taskId: String) {
            viewModelScope.launch {
                downloadRepository.pauseDownload(taskId)
            }
        }

        fun resumeDownload(taskId: String) {
            viewModelScope.launch {
                downloadRepository.resumeDownload(taskId)
            }
        }

        fun cancelDownload(taskId: String) {
            viewModelScope.launch {
                downloadRepository.cancelDownload(taskId)
            }
        }

        fun clearCompleted() {
            viewModelScope.launch {
                downloadRepository.clearCompletedDownloads()
            }
        }

        fun clearAll() {
            viewModelScope.launch {
                downloadRepository.clearAllDownloads()
            }
        }
    }

sealed class DownloadUiState {
    object Loading : DownloadUiState()

    data class Success(val message: String) : DownloadUiState()

    data class Error(val message: String) : DownloadUiState()
}
