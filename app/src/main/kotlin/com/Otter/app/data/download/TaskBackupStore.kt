package com.Otter.app.data.download
import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TaskBackupStore(private val appContext: Context) {
    @Serializable
    data class TaskWithStateBackup(val task: Task, val state: Task.State)

    private val prefs by lazy {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    fun writeBackup(map: Map<Task, Task.State>) {
        val list = map.map { (task, state) -> TaskWithStateBackup(task, state) }
        prefs.edit().putString(KEY_TASK_LIST, json.encodeToString(list)).apply()
    }

    fun readBackup(): List<TaskFactory.TaskWithState> {
        val raw = prefs.getString(KEY_TASK_LIST, null) ?: return emptyList()
        return runCatching {
            val list = json.decodeFromString<List<TaskWithStateBackup>>(raw)
            list.map { TaskFactory.TaskWithState(it.task, it.state) }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val PREFS_NAME = "download"
        private const val KEY_TASK_LIST = "task_list_backup"
    }
}
