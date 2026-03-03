package com.Otter.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Otter.app.data.models.StorageStats
import com.Otter.app.service.StorageService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageViewModel
    @Inject
    constructor(
        private val storageService: StorageService,
    ) : ViewModel() {
        private val _storageStats = MutableStateFlow<StorageStats?>(null)
        val storageStats: StateFlow<StorageStats?> = _storageStats.asStateFlow()

        private val _lastActionResult = MutableStateFlow<Result<Unit>?>(null)
        val lastActionResult: StateFlow<Result<Unit>?> = _lastActionResult.asStateFlow()

        fun refresh() {
            viewModelScope.launch {
                val result = storageService.getStorageStats()
                _storageStats.value = result.getOrNull()
            }
        }

        fun clearCache() {
            viewModelScope.launch {
                val result = storageService.clearCache()
                _lastActionResult.value = result
                refresh()
            }
        }

        fun exportDatabase(destinationUri: String) {
            viewModelScope.launch {
                val result = storageService.exportDatabase(destinationUri)
                _lastActionResult.value = result
            }
        }

        fun importDatabase(sourceUri: String) {
            viewModelScope.launch {
                val result = storageService.importDatabase(sourceUri)
                _lastActionResult.value = result
                refresh()
            }
        }

        fun clearUserData() {
            viewModelScope.launch {
                val result = storageService.clearUserData()
                _lastActionResult.value = result
                refresh()
            }
        }

        fun syncDownloadFolder() {
            viewModelScope.launch {
                val result = storageService.syncDownloadFolder()
                _lastActionResult.value = result
                refresh()
            }
        }

        fun clearOldestDownloads(maxSizeBytes: Long) {
            viewModelScope.launch {
                val result = storageService.clearOldestDownloads(maxSizeBytes)
                _lastActionResult.value = result.map { Unit }
                refresh()
            }
        }
    }
