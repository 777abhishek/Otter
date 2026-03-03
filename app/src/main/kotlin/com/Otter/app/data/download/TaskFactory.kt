package com.Otter.app.data.download

object TaskFactory {
    fun createWithConfigurations(
        videoInfo: VideoInfo,
        formatList: List<Format>,
        videoClips: List<VideoClip> = emptyList(),
        splitByChapter: Boolean = false,
        newTitle: String,
        selectedSubtitles: List<String> = emptyList(),
        selectedAutoCaptions: List<String> = emptyList(),
    ): TaskWithState {
        val fileSize =
            formatList.fold(.0) { acc, format ->
                acc + (format.fileSize ?: format.fileSizeApprox ?: .0)
            }

        val info =
            videoInfo
                .run { if (fileSize != .0) copy(fileSize = fileSize) else this }
                .run { if (newTitle.isNotEmpty()) copy(title = newTitle) else this }

        val audioOnlyFormats = formatList.filter { it.isAudioOnly() }
        val videoFormats = formatList.filter { it.containsVideo() }
        val videoOnlyFormats = formatList.filter { it.isVideoOnly() }

        // Build format ID string
        var formatId = formatList.joinToString(separator = "+") { it.formatId ?: "" }

        // If user selected a video-only format without any audio format,
        // append +bestaudio so yt-dlp merges the best audio stream automatically
        val hasVideoOnly = videoOnlyFormats.isNotEmpty()
        val hasAudio = audioOnlyFormats.isNotEmpty()
        if (hasVideoOnly && !hasAudio && formatId.isNotBlank()) {
            formatId = "$formatId+bestaudio"
        }

        // mergeAudioStream: true when multiple audio streams or video-only + audio combo
        val mergeAudioStream =
            audioOnlyFormats.size > 1 ||
                (hasVideoOnly && hasAudio)

        val base = DownloadPreferences.createFromPreferences()
        val preferences =
            base.copy(
                extractAudio = audioOnlyFormats.isNotEmpty() && videoFormats.isEmpty(),
                formatIdString = formatId,
                mergeAudioStream = mergeAudioStream,
                downloadSubtitle = selectedSubtitles.isNotEmpty() || selectedAutoCaptions.isNotEmpty(),
                videoClips = videoClips,
                splitByChapter = splitByChapter,
                newTitle = newTitle,
            )

        val task = Task(url = info.originalUrl, preferences = preferences)
        val state =
            Task.State(
                downloadState = Task.DownloadState.ReadyWithInfo,
                videoInfo = info,
                viewState =
                    Task.ViewState.fromVideoInfo(info)
                        .copy(videoFormats = videoFormats, audioOnlyFormats = audioOnlyFormats),
            )

        return TaskWithState(task, state)
    }

    fun createPlaylistWithConfigurations(
        playlistUrl: String,
        playlistResult: PlaylistResult,
        selectedIndices: List<Int>,
        sampleVideoInfo: VideoInfo,
        formatList: List<Format>,
        newTitle: String,
        selectedSubtitles: List<String> = emptyList(),
        selectedAutoCaptions: List<String> = emptyList(),
    ): List<TaskWithState> {
        val fileSize =
            formatList.fold(.0) { acc, format ->
                acc + (format.fileSize ?: format.fileSizeApprox ?: .0)
            }

        val info =
            sampleVideoInfo
                .run { if (fileSize != .0) copy(fileSize = fileSize) else this }
                .run { if (newTitle.isNotBlank()) copy(title = newTitle) else this }

        val audioOnlyFormats = formatList.filter { it.isAudioOnly() }
        val videoFormats = formatList.filter { it.containsVideo() }
        val videoOnlyFormats = formatList.filter { it.isVideoOnly() }

        var formatId = formatList.joinToString(separator = "+") { it.formatId ?: "" }

        val hasVideoOnly = videoOnlyFormats.isNotEmpty()
        val hasAudio = audioOnlyFormats.isNotEmpty()
        if (hasVideoOnly && !hasAudio && formatId.isNotBlank()) {
            formatId = "$formatId+bestaudio"
        }

        val mergeAudioStream = audioOnlyFormats.size > 1 || (hasVideoOnly && hasAudio)

        val base = DownloadPreferences.createFromPreferences()
        val preferences =
            base.copy(
                downloadPlaylist = true,
                extractAudio = audioOnlyFormats.isNotEmpty() && videoFormats.isEmpty(),
                formatIdString = formatId,
                mergeAudioStream = mergeAudioStream,
                downloadSubtitle = selectedSubtitles.isNotEmpty() || selectedAutoCaptions.isNotEmpty(),
                newTitle = newTitle,
            )

        return selectedIndices.map { index ->
            val entry = playlistResult.entries?.getOrNull(index - 1)
            val task =
                Task(
                    url = playlistUrl,
                    type = Task.TypeInfo.Playlist(index),
                    preferences = preferences,
                )
            val state =
                Task.State(
                    downloadState = Task.DownloadState.ReadyWithInfo,
                    videoInfo = info,
                    viewState =
                        Task.ViewState.fromVideoInfo(info)
                            .copy(
                                url = playlistUrl,
                                title = entry?.title ?: info.title,
                                videoFormats = videoFormats,
                                audioOnlyFormats = audioOnlyFormats,
                            ),
                )

            TaskWithState(task, state)
        }
    }

    data class TaskWithState(val task: Task, val state: Task.State)
}
