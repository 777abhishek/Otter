package com.Otter.app.ui.download.configure

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Otter.app.R
import com.Otter.app.data.download.Downloader
import com.Otter.app.data.download.Format
import com.Otter.app.data.download.PlaylistResult
import com.Otter.app.data.download.TaskFactory
import com.Otter.app.data.download.VideoClip
import com.Otter.app.data.download.VideoInfo
import com.Otter.app.ui.components.ButtonOption
import com.Otter.app.ui.components.ConnectedButtonGroup
import com.Otter.app.util.PreferenceUtil.getBoolean
import com.Otter.app.util.PreferenceUtil.getString
import com.Otter.app.util.PreferenceUtil.updateString
import com.Otter.app.util.SubtitleFormat
import com.Otter.app.util.extractAudio
import com.Otter.app.util.mergeMultiAudioStream
import com.Otter.app.util.subtitle
import com.Otter.app.util.subtitleLanguage

private const val TAG = "FormatPage"

data class FormatConfig(
    val formatList: List<Format>,
    val videoClips: List<VideoClip>,
    val splitByChapter: Boolean,
    val newTitle: String,
    val selectedSubtitles: List<String>,
    val selectedAutoCaptions: List<String>,
)

private enum class FormatTab { Combined, Audio, Video, Subtitle }

@Composable
fun FormatPage(
    modifier: Modifier = Modifier,
    videoInfo: VideoInfo,
    downloader: Downloader,
    onNavigateBack: () -> Unit = {},
    playlistResult: PlaylistResult? = null,
    selectedIndices: List<Int> = emptyList(),
) {
    if (videoInfo.formats.isNullOrEmpty()) return
    val isPlaylistDownload = playlistResult != null && selectedIndices.isNotEmpty()
    val isPlaylistUrl =
        videoInfo.originalUrl.contains("playlist", ignoreCase = true) ||
            Regex("[?&]list=").containsMatchIn(videoInfo.originalUrl)
    val audioOnly = extractAudio.getBoolean()
    val mergeAudioStream = mergeMultiAudioStream.getBoolean()
    val subtitleLanguageRegex = subtitleLanguage.getString()
    val downloadSubtitle = subtitle.getBoolean()
    val initialSelectedSubtitles =
        if (downloadSubtitle) {
            videoInfo
                .run { subtitles.keys + automaticCaptions.keys }
                .filterWithRegex(subtitleLanguageRegex)
        } else {
            emptySet()
        }

    var showUpdateSubtitleDialog by remember { mutableStateOf(false) }
    var diffSubtitleLanguages by remember { mutableStateOf(emptySet<String>()) }

    FormatPageImpl(
        modifier = modifier,
        videoInfo = videoInfo,
        onNavigateBack = onNavigateBack,
        audioOnly = audioOnly,
        mergeAudioStream = !audioOnly && mergeAudioStream,
        selectedSubtitleCodes = initialSelectedSubtitles,
        isPlaylistDownload = isPlaylistDownload,
        playlistResult = playlistResult,
        selectedIndices = selectedIndices,
        onDownloadPressed = { config: FormatConfig ->
            val tasksToEnqueue: List<TaskFactory.TaskWithState> =
                when {
                    isPlaylistDownload && playlistResult != null && selectedIndices.isNotEmpty() -> {
                        TaskFactory.createPlaylistWithConfigurations(
                            playlistUrl = videoInfo.originalUrl,
                            playlistResult = playlistResult,
                            selectedIndices = selectedIndices,
                            sampleVideoInfo = videoInfo,
                            formatList = config.formatList,
                            newTitle = config.newTitle,
                            selectedSubtitles = config.selectedSubtitles,
                            selectedAutoCaptions = config.selectedAutoCaptions,
                        )
                    }

                    isPlaylistUrl -> {
                        TaskFactory.createPlaylistWithConfigurations(
                            playlistUrl = videoInfo.originalUrl,
                            playlistResult = playlistResult ?: PlaylistResult(),
                            selectedIndices = selectedIndices,
                            sampleVideoInfo = videoInfo,
                            formatList = config.formatList,
                            newTitle = config.newTitle,
                            selectedSubtitles = config.selectedSubtitles,
                            selectedAutoCaptions = config.selectedAutoCaptions,
                        )
                    }

                    else -> {
                        listOf(
                            TaskFactory.createWithConfigurations(
                                videoInfo = videoInfo,
                                formatList = config.formatList,
                                newTitle = config.newTitle,
                                selectedSubtitles = config.selectedSubtitles,
                                selectedAutoCaptions = config.selectedAutoCaptions,
                            ),
                        )
                    }
                }

            tasksToEnqueue.forEach { downloader.enqueue(it) }

            val diff =
                (config.selectedSubtitles + config.selectedAutoCaptions)
                    .toSet()
                    .run { this - this.filterWithRegex(subtitleLanguageRegex) }
                    .toSet()

            diffSubtitleLanguages = diff

            if (diff.isNotEmpty()) {
                showUpdateSubtitleDialog = true
            } else {
                onNavigateBack()
            }
        },
    )
    if (showUpdateSubtitleDialog) {
        UpdateSubtitleLanguageDialog(
            modifier = Modifier,
            languages = diffSubtitleLanguages,
            onDismissRequest = {
                showUpdateSubtitleDialog = false
                onNavigateBack()
            },
            onConfirm = {
                subtitleLanguage.updateString(
                    (diffSubtitleLanguages + subtitleLanguageRegex).joinToString(separator = ",") {
                        it
                    },
                )
                showUpdateSubtitleDialog = false
                onNavigateBack()
            },
        )
    }
}

private const val NOT_SELECTED = -1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatPageImpl(
    modifier: Modifier = Modifier,
    videoInfo: VideoInfo = VideoInfo(),
    audioOnly: Boolean = false,
    mergeAudioStream: Boolean = false,
    selectedSubtitleCodes: Set<String>,
    onNavigateBack: () -> Unit = {},
    onDownloadPressed: (FormatConfig) -> Unit = { _ -> },
    isPlaylistDownload: Boolean = false,
    playlistResult: PlaylistResult? = null,
    selectedIndices: List<Int> = emptyList(),
) {
    if (videoInfo.formats.isNullOrEmpty()) return

    val videoOnlyFormats = videoInfo.formats.filter { it.isVideoOnly() }.reversed()
    val audioOnlyFormats = videoInfo.formats.filter { it.isAudioOnly() }.reversed()
    val videoAudioFormats =
        videoInfo.formats
            .filter {
                (it.containsVideo() && it.containsAudio()) ||
                    (it.isVideoOnly() && (it.height ?: 0) > 1080)
            }
            .sortedByDescending { it.height ?: 0 }

    val duration = videoInfo.duration ?: 0.0

    val isSuggestedFormatAvailable =
        !videoInfo.requestedFormats.isNullOrEmpty() || !videoInfo.requestedDownloads.isNullOrEmpty()

    var isSuggestedFormatSelected by remember { mutableStateOf(isSuggestedFormatAvailable) }

    var selectedVideoAudioFormat by remember { mutableIntStateOf(NOT_SELECTED) }
    var selectedVideoOnlyFormat by remember { mutableIntStateOf(NOT_SELECTED) }
    val selectedAudioOnlyFormats = remember { mutableStateListOf<Int>() }
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    fun String?.share() =
        this?.let {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            context.startActivity(
                Intent.createChooser(
                    Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, it)
                    },
                    null,
                ),
                null,
            )
        }

    var videoTitle by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showSubtitleSelectionDialog by remember { mutableStateOf(false) }

    val suggestedSubtitleMap: Map<String, List<SubtitleFormat>> =
        videoInfo.subtitles.takeIf { it.isNotEmpty() }
            ?: videoInfo.automaticCaptions.filterKeys { it.endsWith("-orig") }

    val otherSubtitleMap: Map<String, List<SubtitleFormat>> =
        videoInfo.subtitles + videoInfo.automaticCaptions - suggestedSubtitleMap.keys

    val selectedSubtitles =
        remember {
            mutableStateListOf<String>().apply { addAll(selectedSubtitleCodes) }
        }
    val selectedAutoCaptions = remember { mutableStateListOf<String>() }

    // Format list derived from selections
    val formatList: List<Format> by remember {
        androidx.compose.runtime.derivedStateOf {
            mutableListOf<Format>().apply {
                if (isSuggestedFormatSelected) {
                    videoInfo.requestedFormats?.let { addAll(it) }
                        ?: videoInfo.requestedDownloads?.forEach {
                            it.requestedFormats?.let { addAll(it) }
                        }
                } else {
                    selectedAudioOnlyFormats.forEach { index ->
                        add(audioOnlyFormats.elementAt(index))
                    }
                    videoAudioFormats.getOrNull(selectedVideoAudioFormat)?.let { add(it) }
                    videoOnlyFormats.getOrNull(selectedVideoOnlyFormat)?.let { add(it) }
                }
            }
        }
    }

    // Tab state
    val initialTab =
        when {
            audioOnly -> FormatTab.Audio
            videoAudioFormats.isNotEmpty() -> FormatTab.Combined
            videoOnlyFormats.isNotEmpty() -> FormatTab.Video
            audioOnlyFormats.isNotEmpty() -> FormatTab.Audio
            else -> FormatTab.Combined
        }
    var selectedTab by remember { mutableStateOf(initialTab) }
    val tabOptions =
        remember {
            listOf(
                ButtonOption(FormatTab.Combined, "Combined"),
                ButtonOption(FormatTab.Audio, "Audio"),
                ButtonOption(FormatTab.Video, "Video"),
                ButtonOption(FormatTab.Subtitle, "Subtitle"),
            )
        }

    val isFormatSelected = isSuggestedFormatSelected || formatList.isNotEmpty()

    Column(modifier = modifier.fillMaxSize()) {
        // ── Top Bar ────────────────────────────────────────────────
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onNavigateBack() }) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.close),
                )
            }

            Text(
                text =
                    videoTitle.ifEmpty { videoInfo.title }.let {
                        if (it.length > 40) it.take(37) + "…" else it
                    },
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
            )

            IconButton(
                onClick = {
                    if (isFormatSelected) {
                        onDownloadPressed(
                            FormatConfig(
                                formatList = formatList,
                                videoClips = emptyList(),
                                splitByChapter = false,
                                newTitle = videoTitle,
                                selectedSubtitles = selectedSubtitles,
                                selectedAutoCaptions = selectedAutoCaptions,
                            ),
                        )
                    }
                },
                enabled = isFormatSelected,
            ) {
                Icon(
                    Icons.Outlined.FileDownload,
                    contentDescription = stringResource(R.string.start_download),
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        // ── ConnectedButtonGroup Tabs ──────────────────────────────
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            ConnectedButtonGroup(
                options = tabOptions,
                selectedValue = selectedTab,
                onSelectionChange = { selectedTab = it },
                modifier = Modifier.fillMaxWidth(),
                maxWidth = 400.dp,
            )
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // ── Tab Content ────────────────────────────────────────────
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "format_tab_content",
            modifier = Modifier.weight(1f),
        ) { tab ->
            when (tab) {
                FormatTab.Combined ->
                    CombinedTab(
                        videoAudioFormats = videoAudioFormats,
                        duration = duration,
                        isSuggestedFormatAvailable = isSuggestedFormatAvailable,
                        isSuggestedFormatSelected = isSuggestedFormatSelected,
                        selectedVideoAudioFormat = selectedVideoAudioFormat,
                        videoInfo = videoInfo,
                        onSuggestedClick = {
                            isSuggestedFormatSelected = true
                            selectedAudioOnlyFormats.clear()
                            selectedVideoAudioFormat = NOT_SELECTED
                            selectedVideoOnlyFormat = NOT_SELECTED
                        },
                        onFormatClick = { index ->
                            selectedVideoAudioFormat =
                                if (selectedVideoAudioFormat == index) {
                                    NOT_SELECTED
                                } else {
                                    selectedAudioOnlyFormats.clear()
                                    selectedVideoOnlyFormat = NOT_SELECTED
                                    isSuggestedFormatSelected = false
                                    index
                                }
                        },
                        onLongClick = { url -> url.share() },
                    )

                FormatTab.Audio ->
                    AudioTab(
                        audioOnlyFormats = audioOnlyFormats,
                        duration = duration,
                        mergeAudioStream = mergeAudioStream,
                        selectedAudioOnlyFormats = selectedAudioOnlyFormats,
                        isSuggestedFormatSelected = isSuggestedFormatSelected,
                        onFormatClick = { index ->
                            if (selectedAudioOnlyFormats.contains(index)) {
                                selectedAudioOnlyFormats.remove(index)
                            } else {
                                if (!mergeAudioStream) {
                                    selectedAudioOnlyFormats.clear()
                                }
                                isSuggestedFormatSelected = false
                                selectedAudioOnlyFormats.add(index)
                            }
                        },
                        onLongClick = { url -> url.share() },
                    )

                FormatTab.Video ->
                    VideoTab(
                        videoOnlyFormats = videoOnlyFormats,
                        duration = duration,
                        selectedVideoOnlyFormat = selectedVideoOnlyFormat,
                        onFormatClick = { index ->
                            selectedVideoOnlyFormat =
                                if (selectedVideoOnlyFormat == index) {
                                    NOT_SELECTED
                                } else {
                                    selectedVideoAudioFormat = NOT_SELECTED
                                    isSuggestedFormatSelected = false
                                    index
                                }
                        },
                        onLongClick = { url -> url.share() },
                    )

                FormatTab.Subtitle ->
                    SubtitleTab(
                        suggestedSubtitleMap = suggestedSubtitleMap,
                        selectedSubtitles = selectedSubtitles,
                        onShowAll = { showSubtitleSelectionDialog = true },
                    )
            }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────
    if (showRenameDialog) {
        RenameDialog(
            initialValue = videoTitle.ifEmpty { videoInfo.title },
            onDismissRequest = { showRenameDialog = false },
        ) { videoTitle = it }
    }

    if (showSubtitleSelectionDialog) {
        SubtitleSelectionDialog(
            suggestedSubtitles = suggestedSubtitleMap,
            autoCaptions = otherSubtitleMap,
            selectedSubtitles = selectedSubtitles,
            onDismissRequest = { showSubtitleSelectionDialog = false },
            onConfirm = { subs, _ ->
                selectedSubtitles.run {
                    clear()
                    addAll(subs)
                }
                showSubtitleSelectionDialog = false
            },
        )
    }
}

// ── Helper ──────────────────────────────────────────────────────────

private fun Set<String>.filterWithRegex(regex: String): Set<String> {
    if (regex.isBlank()) return this
    val compiled = runCatching { regex.toRegex() }.getOrNull() ?: return this
    return filter { it.contains(compiled) }.toSet()
}
