package com.Otter.app.data.download

import kotlinx.coroutines.Job
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

private val Task.TypeInfo.id: String
    get() =
        when (this) {
            is Task.TypeInfo.CustomCommand -> "${template.id}_${template.name}"
            is Task.TypeInfo.Playlist -> "$index"
            Task.TypeInfo.URL -> ""
        }

private fun makeId(
    url: String,
    type: Task.TypeInfo,
    preferences: DownloadPreferences,
): String = "${url}_${type.id}_${preferences.hashCode()}"

@Serializable
data class Task(
    val url: String,
    val type: TypeInfo = TypeInfo.URL,
    val preferences: DownloadPreferences,
    val id: String = makeId(url, type, preferences),
) : Comparable<Task> {
    val timeCreated: Long = System.currentTimeMillis()

    override fun compareTo(other: Task): Int = timeCreated.compareTo(other.timeCreated)

    @Serializable
    sealed interface TypeInfo {
        @Serializable
        data class Playlist(val index: Int = 0) : TypeInfo

        @Serializable
        data class CustomCommand(val template: CommandTemplate) : TypeInfo

        @Serializable
        data object URL : TypeInfo
    }

    @Serializable
    data class State(
        val downloadState: DownloadState,
        val videoInfo: VideoInfo?,
        val viewState: ViewState,
    )

    @Serializable
    sealed interface DownloadState : Comparable<DownloadState> {
        interface Cancelable {
            val job: Job
            val taskId: String
            val action: RestartableAction
        }

        interface Restartable {
            val action: RestartableAction
        }

        @Serializable
        data object Idle : DownloadState

        @Serializable
        data class FetchingInfo(
            @Transient override val job: Job = Job(),
            override val taskId: String,
        ) : DownloadState, Cancelable {
            override val action: RestartableAction = RestartableAction.FetchInfo
        }

        @Serializable
        data object ReadyWithInfo : DownloadState

        @Serializable
        data class Running(
            @Transient override val job: Job = Job(),
            override val taskId: String,
            val progress: Float = PROGRESS_INDETERMINATE,
            val progressText: String = "",
        ) : DownloadState, Cancelable {
            override val action: RestartableAction = RestartableAction.Download
        }

        @Serializable
        data class Canceled(
            override val action: RestartableAction,
            val progress: Float? = null,
        ) : DownloadState, Restartable

        @Serializable
        data class Error(
            @Transient val throwable: Throwable = Throwable(),
            override val action: RestartableAction,
        ) : DownloadState, Restartable

        @Serializable
        data class Completed(val filePath: String?) : DownloadState

        override fun compareTo(other: DownloadState): Int = ordinal - other.ordinal

        private val ordinal: Int
            get() =
                when (this) {
                    is Canceled -> 4
                    is Error -> 5
                    is Completed -> 6
                    Idle -> 3
                    is FetchingInfo -> 2
                    ReadyWithInfo -> 1
                    is Running -> 0
                }
    }

    @Serializable
    sealed interface RestartableAction {
        @Serializable
        data object FetchInfo : RestartableAction

        @Serializable
        data object Download : RestartableAction
    }

    @Serializable
    data class ViewState(
        val url: String = "",
        val title: String = "",
        val uploader: String = "",
        val viewCount: Long = 0,
        val extractorKey: String = "",
        val duration: Int = 0,
        val fileSizeApprox: Double = .0,
        val thumbnailUrl: String? = null,
        val videoFormats: List<Format>? = null,
        val audioOnlyFormats: List<Format>? = null,
    ) {
        companion object {
            fun fromVideoInfo(info: VideoInfo): ViewState {
                val formats = info.requestedFormats ?: emptyList()
                val videoFormats = formats.filter { it.containsVideo() }
                val audioOnlyFormats = formats.filter { it.isAudioOnly() }

                return ViewState(
                    url = info.originalUrl,
                    title = info.title,
                    uploader = info.uploader ?: "",
                    viewCount = info.viewCount,
                    extractorKey = info.extractorKey,
                    duration = info.duration?.toInt() ?: 0,
                    thumbnailUrl = info.thumbnailUrl,
                    fileSizeApprox = info.fileSize ?: .0,
                    videoFormats = videoFormats,
                    audioOnlyFormats = audioOnlyFormats,
                )
            }
        }
    }

    companion object {
        private const val PROGRESS_INDETERMINATE = -1f
    }
}
