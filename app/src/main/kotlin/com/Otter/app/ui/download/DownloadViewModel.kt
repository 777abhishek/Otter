package com.Otter.app.ui.download

import androidx.lifecycle.ViewModel
import com.Otter.app.data.download.Downloader
import com.Otter.app.data.download.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel
    @Inject
    constructor(
        val downloader: Downloader,
    ) : ViewModel() {
        val taskStateMap = downloader.getTaskStateMap()

        fun cancel(task: Task): Boolean = downloader.cancel(task)

        fun restart(task: Task) = downloader.restart(task)

        fun remove(task: Task): Boolean = downloader.remove(task)

       
        fun cancelAll(): Int = downloader.cancelAll()

        fun removeCompleted(): Int = downloader.removeCompleted()

        fun removeFailed(): Int = downloader.removeFailed()

        fun clearAll(): Int = downloader.clearAll()
    }
