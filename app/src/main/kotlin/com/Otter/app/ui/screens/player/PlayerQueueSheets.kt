package com.Otter.app.ui.screens.player

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.Otter.app.data.repositories.ChapterInfo
import com.Otter.app.data.repositories.StreamInfoResult
import com.Otter.app.player.QueueItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerQueueBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    queue: List<QueueItem>,
    currentIndex: Int,
    onQueueItemClick: (Int) -> Unit,
    chapters: List<ChapterInfo>,
    streamInfo: StreamInfoResult?,
    currentPositionMs: Long,
    onChapterClick: (ChapterInfo) -> Unit,
) {
    if (!show) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.72f)) {
            // ── Tab header ────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                divider = {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                },
            ) {
                SheetTab(
                    selected = selectedTab == 0,
                    label = "Queue",
                    badgeCount = if (queue.isNotEmpty()) queue.size else null,
                    onClick = { onTabSelected(0) },
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                        )
                    },
                )
                SheetTab(
                    selected = selectedTab == 1,
                    label = "Chapters",
                    badgeCount = if (chapters.isNotEmpty()) chapters.size else null,
                    onClick = { onTabSelected(1) },
                    icon = {
                        Icon(
                            Icons.Rounded.Bookmarks,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                        )
                    },
                )
                SheetTab(
                    selected = selectedTab == 2,
                    label = "Info",
                    badgeCount = null,
                    onClick = { onTabSelected(2) },
                    icon = {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                        )
                    },
                )
            }

            // ── Tab content ───────────────────────────────────────────────
            when (selectedTab) {
                0 ->
                    QueueContent(
                        queue = queue,
                        currentIndex = currentIndex,
                        onItemClick = onQueueItemClick,
                    )
                1 ->
                    ChaptersContent(
                        chapters = chapters,
                        currentPositionMs = currentPositionMs,
                        onChapterClick = onChapterClick,
                    )
                2 -> InfoContent(streamInfo = streamInfo)
            }
        }
    }
}

private val urlRegex = Regex("(https?://\\S+)")

private fun stripHtmlTags(text: String): String {
    return text
        // Replace <br> and <br/> with newlines
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        // Replace </p> with newlines
        .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n")
        // Replace </div> with newlines
        .replace(Regex("</div>", RegexOption.IGNORE_CASE), "\n")
        // Remove all other HTML tags
        .replace(Regex("<[^>]+>"), "")
        // Decode common HTML entities
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
        // Clean up multiple newlines
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}

private fun linkify(text: String): AnnotatedString {
    // First strip HTML tags, then linkify URLs
    val cleanText = stripHtmlTags(text)
    return buildAnnotatedString {
        var lastIndex = 0
        for (match in urlRegex.findAll(cleanText)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (start > lastIndex) {
                append(cleanText.substring(lastIndex, start))
            }
            val url = match.value.trimEnd('.', ',', ')', ']', '}', '"', '\'')
            val displayStart = length
            append(url)
            val displayEnd = length
            addStringAnnotation(tag = "URL", annotation = url, start = displayStart, end = displayEnd)
            addStyle(
                style =
                    SpanStyle(
                        color = androidx.compose.ui.graphics.Color(0xFF64B5F6),
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Medium,
                    ),
                start = displayStart,
                end = displayEnd,
            )
            lastIndex = end
        }
        if (lastIndex < cleanText.length) {
            append(cleanText.substring(lastIndex))
        }
    }
}

// ── Queue ─────────────────────────────────────────────────────────────────────

@Composable
private fun QueueContent(
    queue: List<QueueItem>,
    currentIndex: Int,
    onItemClick: (Int) -> Unit,
) {
    if (queue.isEmpty()) {
        EmptyState(message = "Your queue is empty")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(queue) { index, item ->
            QueueItemRow(
                item = item,
                isPlaying = index == currentIndex,
                onClick = { onItemClick(index) },
            )
        }
    }
}

@Composable
private fun QueueItemRow(
    item: QueueItem,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isPlaying) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        Color.Transparent
                    },
                )
                .clickable(onClick = onClick)
                .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail
        Box(
            modifier =
                Modifier
                    .width(96.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Playing indicator overlay
            if (isPlaying) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.VolumeUp,
                        contentDescription = "Now playing",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = item.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                color =
                    if (isPlaying) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
            Text(
                text = item.uploaderName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatDuration(item.duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun InfoContent(streamInfo: StreamInfoResult?) {
    if (streamInfo == null) {
        EmptyState(message = "Loading info...")
        return
    }

    val context = LocalContext.current

    val audioTracks =
        remember(streamInfo) {
            streamInfo.audioStreams
                .mapNotNull { it.language?.takeIf { lang -> lang.isNotBlank() } }
                .distinct()
        }

    val captions =
        remember(streamInfo) {
            streamInfo.subtitles
                .map { sub -> if (sub.languageName.isNotBlank()) sub.languageName else sub.languageCode }
                .filter { it.isNotBlank() }
                .distinct()
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = streamInfo.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        item {
            val subtitle =
                buildString {
                    if (streamInfo.uploaderName.isNotBlank()) append(streamInfo.uploaderName)
                    if (streamInfo.viewCount > 0) {
                        if (isNotEmpty()) append("  •  ")
                        append(streamInfo.viewCount)
                        append(" views")
                    }
                    if (!streamInfo.uploadDate.isNullOrBlank()) {
                        if (isNotEmpty()) append("  •  ")
                        append(streamInfo.uploadDate)
                    }
                }
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            if (streamInfo.description.isBlank()) {
                Text(
                    text = "No description",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            } else {
                val annotated =
                    remember(streamInfo.description) {
                        linkify(streamInfo.description)
                    }
                SelectionContainer {
                    androidx.compose.foundation.text.ClickableText(
                        text = annotated,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                        maxLines = 20,
                        overflow = TextOverflow.Ellipsis,
                        onClick = { offset ->
                            val url =
                                annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                    .firstOrNull()
                                    ?.item
                                    ?: return@ClickableText
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        },
                    )
                }
            }
        }

        item {
            Text(
                text = "Audio tracks",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (audioTracks.isEmpty()) "Not available" else audioTracks.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        item {
            Text(
                text = "Captions",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (captions.isEmpty()) "Not available" else captions.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ── Chapters ──────────────────────────────────────────────────────────────────

@Composable
private fun ChaptersContent(
    chapters: List<ChapterInfo>,
    currentPositionMs: Long,
    onChapterClick: (ChapterInfo) -> Unit,
) {
    if (chapters.isEmpty()) {
        EmptyState(message = "No chapters available")
        return
    }

    val currentChapterIndex =
        chapters
            .indexOfLast { it.startTimeSeconds * 1000L <= currentPositionMs }
            .coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(chapters) { index, chapter ->
            ChapterItemRow(
                chapter = chapter,
                isCurrent = index == currentChapterIndex,
                chapterNumber = index + 1,
                onClick = { onChapterClick(chapter) },
            )
        }
    }
}

@Composable
private fun ChapterItemRow(
    chapter: ChapterInfo,
    isCurrent: Boolean,
    chapterNumber: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isCurrent) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        Color.Transparent
                    },
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Chapter number / play indicator
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isCurrent) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = "Current chapter",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Text(
                    text = chapterNumber.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = chapter.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style =
                MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                ),
            color =
                if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatDuration(chapter.startTimeSeconds * 1000L),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

// ── Shared ────────────────────────────────────────────────────────────────────

@Composable
private fun SheetTab(
    selected: Boolean,
    label: String,
    badgeCount: Int?,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Tab(
        selected = selected,
        onClick = onClick,
        content = {
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                icon()
                Text(
                    text = label,
                    style =
                        MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                )
                if (badgeCount != null && badgeCount > 0) {
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = badgeCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
