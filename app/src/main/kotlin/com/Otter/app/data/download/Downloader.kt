package com.Otter.app.data.download

import androidx.compose.runtime.snapshots.SnapshotStateMap

interface Downloader {
    fun getTaskStateMap(): SnapshotStateMap<Task, Task.State>

    fun cancel(task: Task): Boolean

    fun cancel(taskId: String): Boolean {
        return getTaskStateMap().keys.find { it.id == taskId }?.let { cancel(it) } ?: false
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
