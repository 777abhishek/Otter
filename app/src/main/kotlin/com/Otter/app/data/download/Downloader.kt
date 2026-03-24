package com.Otter.app.data.download

import androidx.compose.runtime.snapshots.SnapshotStateMap

interface Downloader {
    fun getTaskStateMap(): SnapshotStateMap<Task, Task.State>

    fun cancel(task: Task): Boolean

    fun cancel(taskId: String): Boolean {
        return getTaskStateMap().keys.find { it.id == taskId }?.let { cancel(it) } ?: false
    }

    /** Cancel all running/downloading tasks */
    fun cancelAll(): Int {
        val tasks = getTaskStateMap().entries
            .filter { (_, state) ->
                state.downloadState is Task.DownloadState.Running ||
                state.downloadState is Task.DownloadState.FetchingInfo ||
                state.downloadState == Task.DownloadState.Idle ||
                state.downloadState == Task.DownloadState.ReadyWithInfo
            }
            .map { it.key }
        var count = 0
        tasks.forEach { if (cancel(it)) count++ }
        return count
    }

    /** Remove all completed tasks */
    fun removeCompleted(): Int {
        val tasks = getTaskStateMap().entries
            .filter { (_, state) -> state.downloadState is Task.DownloadState.Completed }
            .map { it.key }
        var count = 0
        tasks.forEach { if (remove(it)) count++ }
        return count
    }

    /** Remove all canceled/failed tasks */
    fun removeFailed(): Int {
        val tasks = getTaskStateMap().entries
            .filter { (_, state) ->
                state.downloadState is Task.DownloadState.Canceled ||
                state.downloadState is Task.DownloadState.Error
            }
            .map { it.key }
        var count = 0
        tasks.forEach { if (remove(it)) count++ }
        return count
    }

    /** Clear all tasks */
    fun clearAll(): Int {
        val tasks = getTaskStateMap().keys.toList()
        var count = 0
        tasks.forEach { if (remove(it)) count++ }
        return count
    }

    fun restart(task: Task)

    fun enqueue(task: Task)

    fun enqueue(
        task: Task,
        state: Task.State,
    )

    fun enqueue(taskWithState: TaskFactory.TaskWithState) {
        val (task, state) = taskWithState
        enqueue(task, state)
    }

    fun remove(task: Task): Boolean
}
