package com.Otter.app.ui.download.configure

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.Otter.app.R
import com.Otter.app.data.download.Format
import com.Otter.app.data.download.VideoInfo
import com.Otter.app.ui.components.FormatItem
import com.Otter.app.ui.components.FormatSubtitle
import com.Otter.app.ui.components.SuggestedFormatItem
import com.Otter.app.ui.components.VideoFilterChip
import com.Otter.app.util.SubtitleFormat

// ── Combined Tab ────────────────────────────────────────────────────

@Composable
internal fun CombinedTab(
    videoAudioFormats: List<Format>,
    videoOnlyFormats: List<Format>,
    duration: Double,
    isSuggestedFormatAvailable: Boolean,
    isSuggestedFormatSelected: Boolean,
    selectedVideoAudioFormat: Int,
    selectedVideoOnlyFormat: Int,
    videoInfo: VideoInfo,
    bestVideoOnly: Format?,
    bestAudioOnly: Format?,
    hasBestPlusBest: Boolean,
    onSuggestedClick: () -> Unit,
    onVideoAudioFormatClick: (Int) -> Unit,
    onVideoOnlyFormatClick: (Int) -> Unit,
    onLongClick: (String?) -> Unit,
) {
    if (videoAudioFormats.isEmpty() && videoOnlyFormats.isEmpty() && !isSuggestedFormatAvailable) {
        EmptyTabMessage(text = "No combined formats available")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(12.dp),
    ) {
        // Suggested section - Best quality option (yt-dlp auto-selects best)
        if (isSuggestedFormatAvailable) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FormatSubtitle(
                    text = stringResource(R.string.suggested),
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SuggestedFormatItem(
                    videoInfo = videoInfo,
                    bestVideoOnly = bestVideoOnly,
                    bestAudioOnly = bestAudioOnly,
                    hasBestPlusBest = hasBestPlusBest,
                    selected = isSuggestedFormatSelected,
                    onClick = onSuggestedClick,
                )
            }
        }

        // Video-only formats that will be merged with best audio (higher quality options)
        if (videoOnlyFormats.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FormatSubtitle(
                    text = "Video (merges with audio)",
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
            }
            itemsIndexed(videoOnlyFormats) { index, formatInfo ->
                FormatItem(
                    formatInfo = formatInfo,
                    duration = duration,
                    selected = selectedVideoOnlyFormat == index,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    outlineColor = MaterialTheme.colorScheme.primary,
                    onLongClick = { onLongClick(formatInfo.url) },
                    onClick = { onVideoOnlyFormatClick(index) },
                )
            }
        }

        // Pre-merged Video + Audio formats (single file with both streams - usually lower quality)
        if (videoAudioFormats.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FormatSubtitle(
                    text = "Video + Audio (pre-merged)",
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
            }
            itemsIndexed(videoAudioFormats) { index, formatInfo ->
                FormatItem(
                    formatInfo = formatInfo,
                    duration = duration,
                    selected = selectedVideoAudioFormat == index,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    outlineColor = MaterialTheme.colorScheme.secondary,
                    onLongClick = { onLongClick(formatInfo.url) },
                    onClick = { onVideoAudioFormatClick(index) },
                )
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ── Audio Tab ───────────────────────────────────────────────────────

@Composable
internal fun AudioTab(
    audioOnlyFormats: List<Format>,
    duration: Double,
    mergeAudioStream: Boolean,
    selectedAudioOnlyFormats: List<Int>,
    isSuggestedFormatSelected: Boolean,
    onFormatClick: (Int) -> Unit,
    onLongClick: (String?) -> Unit,
) {
    if (audioOnlyFormats.isEmpty()) {
        EmptyTabMessage(text = "No audio formats available")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            FormatSubtitle(
                text = stringResource(R.string.audio),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        itemsIndexed(audioOnlyFormats) { index, formatInfo ->
            FormatItem(
                formatInfo = formatInfo,
                duration = duration,
                selected = selectedAudioOnlyFormats.contains(index),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                outlineColor = MaterialTheme.colorScheme.secondary,
                onLongClick = { onLongClick(formatInfo.url) },
                onClick = { onFormatClick(index) },
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ── Video Tab ───────────────────────────────────────────────────────

@Composable
internal fun VideoTab(
    videoOnlyFormats: List<Format>,
    duration: Double,
    selectedVideoOnlyFormat: Int,
    onFormatClick: (Int) -> Unit,
    onLongClick: (String?) -> Unit,
) {
    if (videoOnlyFormats.isEmpty()) {
        EmptyTabMessage(text = "No video-only formats available")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            FormatSubtitle(
                text = stringResource(R.string.video_only),
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        itemsIndexed(videoOnlyFormats) { index, formatInfo ->
            FormatItem(
                formatInfo = formatInfo,
                duration = duration,
                selected = selectedVideoOnlyFormat == index,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                outlineColor = MaterialTheme.colorScheme.tertiary,
                onLongClick = { onLongClick(formatInfo.url) },
                onClick = { onFormatClick(index) },
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ── Subtitle Tab ────────────────────────────────────────────────────

@Composable
internal fun SubtitleTab(
    suggestedSubtitleMap: Map<String, List<SubtitleFormat>>,
    selectedSubtitles: MutableList<String>,
    onShowAll: () -> Unit,
) {
    if (suggestedSubtitleMap.isEmpty()) {
        EmptyTabMessage(text = "No subtitles available")
        return
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            FormatSubtitle(
                text = stringResource(R.string.subtitle_language),
                modifier = Modifier.weight(1f),
            )
            Text(
                text =
                    stringResource(
                        androidx.appcompat.R.string.abc_activity_chooser_view_see_all,
                    ),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                modifier =
                    Modifier
                        .padding(8.dp)
                        .then(
                            Modifier.clickable { onShowAll() },
                        ),
            )
        }

        LazyRow(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for ((code, formats) in suggestedSubtitleMap) {
                item {
                    VideoFilterChip(
                        selected = selectedSubtitles.contains(code),
                        onClick = {
                            if (selectedSubtitles.contains(code)) {
                                selectedSubtitles.remove(code)
                            } else {
                                selectedSubtitles.add(code)
                            }
                        },
                        label = formats.first().run { name ?: protocol ?: code },
                    )
                }
            }
        }
    }
}

// ── Empty state ─────────────────────────────────────────────────────

@Composable
internal fun EmptyTabMessage(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
