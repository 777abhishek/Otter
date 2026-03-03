package com.Otter.app.data.download

import com.Otter.app.data.models.DownloadItem
import com.Otter.app.data.models.DownloadStatus
import com.Otter.app.data.ytdlp.YtDlpManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadQueueManager
    @Inject
    constructor(
        private val ytDlpManager: YtDlpManager,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val queue = ConcurrentLinkedQueue<DownloadItem>()
        private var currentJob: Job? = null
        private var isPaused = false

        private val _activeDownloads = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
        val activeDownloads: Flow<Map<String, DownloadProgress>> = _activeDownloads.asStateFlow()

        private val _queueItems = MutableStateFlow<List<DownloadItem>>(emptyList())
        val queueItems: Flow<List<DownloadItem>> = _queueItems.asStateFlow()

        private val _completedDownloads = MutableStateFlow<List<DownloadItem>>(emptyList())
        val completedDownloads: Flow<List<DownloadItem>> = _completedDownloads.asStateFlow()

        private val _isProcessing = MutableStateFlow(false)
        val isProcessing: Flow<Boolean> = _isProcessing.asStateFlow()

        fun addToQueue(item: DownloadItem) {
            queue.offer(item)
            updateQueueList()
            processQueue()
        }

        fun addMultipleToQueue(items: List<DownloadItem>) {
            items.forEach { queue.offer(it) }
            updateQueueList()
            processQueue()
        }

        fun removeFromQueue(videoId: String) {
            queue.removeIf { it.videoId == videoId }
            updateQueueList()
        }

        fun pauseDownload(videoId: String) {
            // Mark as paused - actual pause requires cancel and resume logic
            updateDownloadStatus(videoId, DownloadStatus.PAUSED)
        }

        fun pauseAll() {
            isPaused = true
            currentJob?.cancel()
        }

        fun resumeAll() {
            isPaused = false
            processQueue()
        }

        fun cancelDownload(videoId: String) {
            currentJob?.takeIf {
                _activeDownloads.value[videoId] != null
            }?.cancel()
            queue.removeIf { it.videoId == videoId }
            updateQueueList()
        }

        fun retryDownload(videoId: String) {
            val failed = _completedDownloads.value.find { it.videoId == videoId && it.status == DownloadStatus.FAILED }
            failed?.let {
                val retryItem = it.copy(status = DownloadStatus.PENDING, progress = 0f)
                queue.offer(retryItem)
                updateQueueList()
                processQueue()
            }
        }

        fun clearCompleted() {
            _completedDownloads.value = emptyList()
        }

        fun clearFailed() {
            _completedDownloads.value = _completedDownloads.value.filter { it.status != DownloadStatus.FAILED }
        }

        fun moveUpInQueue(videoId: String) {
            val list = queue.toList()
            val index = list.indexOfFirst { it.videoId == videoId }
            if (index > 0) {
                queue.clear()
                val newList = list.toMutableList()
                newList.add(index - 1, newList.removeAt(index))
                newList.forEach { queue.offer(it) }
                updateQueueList()
            }
        }

        fun moveDownInQueue(videoId: String) {
            val list = queue.toList()
            val index = list.indexOfFirst { it.videoId == videoId }
            if (index >= 0 && index < list.size - 1) {
                queue.clear()
                val newList = list.toMutableList()
                newList.add(index + 1, newList.removeAt(index))
                newList.forEach { queue.offer(it) }
                updateQueueList()
            }
        }

        private fun processQueue() {
            if (isPaused || _isProcessing.value) return

            val item = queue.poll() ?: return
            updateQueueList()

            _isProcessing.value = true
            currentJob =
                scope.launch {
                    downloadItem(item)
                }.apply {
                    invokeOnCompletion {
                        _isProcessing.value = false
                        processQueue() // Process next item
                    }
                }
        }

        private suspend fun downloadItem(item: DownloadItem) {
            val videoId = item.videoId
            updateDownloadProgress(videoId, 0f, DownloadStatus.DOWNLOADING)

            try {
                val result =
                    ytDlpManager.downloadVideo(
                        url = "https://www.youtube.com/watch?v=$videoId",
                        outputPath = item.outputPath,
                        format = item.format,
                        cookiesFilePath = item.cookiesFilePath,
                        onProgress = { progress, _, _ ->
                            updateDownloadProgress(videoId, progress, DownloadStatus.DOWNLOADING)
                        },
                    )

                result.onSuccess { outputPath ->
                    val completed =
                        item.copy(
                            status = DownloadStatus.COMPLETED,
                            progress = 1f,
                            filePath = outputPath,
                        )
                    addToCompleted(completed)
                    updateDownloadProgress(videoId, 1f, DownloadStatus.COMPLETED)
                }.onFailure { error ->
                    val failed =
                        item.copy(
                            status = DownloadStatus.FAILED,
                            error = error.message,
                        )
                    addToCompleted(failed)
                    updateDownloadProgress(videoId, item.progress, DownloadStatus.FAILED)
                }
            } catch (e: Exception) {
                val failed =
                    item.copy(
                        status = DownloadStatus.FAILED,
                        error = e.message,
                    )
                addToCompleted(failed)
                updateDownloadProgress(videoId, item.progress, DownloadStatus.FAILED)
            }

            _activeDownloads.value = _activeDownloads.value - videoId
        }

        private fun updateDownloadProgress(
            videoId: String,
            progress: Float,
            status: DownloadStatus,
        ) {
            _activeDownloads.value = _activeDownloads.value + (videoId to DownloadProgress(videoId, progress, status))
        }

        private fun updateDownloadStatus(
            videoId: String,
            status: DownloadStatus,
        ) {
            _activeDownloads.value =
                _activeDownloads.value.mapValues {
                    if (it.key == videoId) it.value.copy(status = status) else it.value
                }
        }

        private fun updateQueueList() {
            _queueItems.value = queue.toList()
        }

        private fun addToCompleted(item: DownloadItem) {
            _completedDownloads.value = _completedDownloads.value + item
        }

        data class DownloadProgress(
            val videoId: String,
            val progress: Float,
            val status: DownloadStatus,
        )
    }
