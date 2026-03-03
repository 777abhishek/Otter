package com.Otter.app.ui.download

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.Otter.app.data.download.Task
import com.Otter.app.ui.download.configure.friendlyDownloadErrorMessage
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TaskCard(
    task: Task,
    state: Task.State,
    onCancel: () -> Unit,
    onRestart: () -> Unit,
    onRemove: () -> Unit,
) {
    val ds = state.downloadState
    val viewState = state.viewState

    val titleText = viewState.title.ifBlank { task.url }
    val channelText = viewState.uploader

    // Progress Parsing
    val progress =
        when (ds) {
            is Task.DownloadState.Running -> ds.progress
            is Task.DownloadState.Canceled -> ds.progress ?: 0f
            is Task.DownloadState.Completed -> 1f
            else -> 0f
        }

    val runningProgressText = (ds as? Task.DownloadState.Running)?.progressText.orEmpty()
    val progressInfo = remember(runningProgressText) { parseProgress(runningProgressText) }
    val formattedTotalSize = if (viewState.fileSizeApprox > 0) formatBytes(viewState.fileSizeApprox.toLong()) else "Unknown"

    val isMerging =
        remember(runningProgressText) {
            val s = runningProgressText.lowercase(Locale.US)
            "merging" in s || "merger" in s
        }

    val errorText =
        remember(ds) {
            val t = (ds as? Task.DownloadState.Error)?.throwable
            if (t == null) return@remember ""

            fun extractMeaningfulLine(raw: String): String {
                val lines =
                    raw
                        .lineSequence()
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .toList()
                // Prefer the last explicit yt-dlp ERROR line.
                val lastError = lines.lastOrNull { it.startsWith("ERROR:", ignoreCase = true) }
                if (lastError != null) return lastError.removePrefix("ERROR:").trim()
                // Else, take last non-warning line.
                val lastNonWarning = lines.lastOrNull { !it.startsWith("WARNING:", ignoreCase = true) }
                return lastNonWarning ?: raw.trim()
            }

            val friendly = friendlyDownloadErrorMessage(t)
            if (friendly.isNotBlank()) return@remember friendly

            val rawMessage = (t.message ?: "").trim()
            val primaryMessage = extractMeaningfulLine(rawMessage)
            primaryMessage.ifBlank { "Download failed" }
        }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
        // Adjusted radius
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Thumbnail (16:9)
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                AsyncImage(
                    model = viewState.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
                if (viewState.duration > 0) {
                    Surface(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = formatDuration(viewState.duration),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            // 2. Content
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Header: Title + Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    // Title & Channel
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (channelText.isNotBlank()) {
                            Text(
                                text = channelText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // Actions (Icon Buttons)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        when (ds) {
                            is Task.DownloadState.Running, is Task.DownloadState.FetchingInfo -> {
                                IconButton(onClick = onCancel) {
                                    Icon(
                                        Icons.Outlined.Cancel,
                                        contentDescription = "Cancel",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                            is Task.DownloadState.Canceled, is Task.DownloadState.Error -> {
                                IconButton(onClick = onRestart) {
                                    Icon(
                                        Icons.Outlined.Refresh,
                                        contentDescription = "Retry",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                IconButton(onClick = onRemove) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            is Task.DownloadState.Completed -> {
                                IconButton(onClick = onRemove) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }

                // Progress & Stats
                when (ds) {
                    is Task.DownloadState.Running -> {
                        val animatedProgress by animateFloatAsState(
                            targetValue = progress.coerceIn(0f, 1f),
                            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                            label = "progress",
                        )
                        LinearWavyProgressIndicator(
                            progress = {
                                if (progress < 0f) 0f else animatedProgress
                            },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            amplitude = { 1f },
                            wavelength = 16.dp,
                        )
                        if (isMerging) {
                            Text(
                                text = "Merging...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        // Stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val size = progressInfo.totalSize.ifBlank { formattedTotalSize }
                            val speed = progressInfo.speed
                            val eta = progressInfo.eta
                            val percent =
                                progressInfo.percentage.ifBlank {
                                    val p = (progress * 100f).coerceIn(0f, 100f)
                                    if (p > 0f) String.format("%.1f%%", p) else ""
                                }

                            Text(
                                text =
                                    buildString {
                                        append(size)
                                        if (speed.isNotBlank()) {
                                            append(" · ")
                                            append(speed)
                                        }
                                        if (percent.isNotBlank()) {
                                            append(" · ")
                                            append(percent)
                                        }
                                    },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )

                            if (eta.isNotBlank()) {
                                Text(
                                    text = eta,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }
                    is Task.DownloadState.FetchingInfo -> {
                        LinearWavyProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        )
                        Text(
                            text = "Preparing...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Task.DownloadState.ReadyWithInfo -> {
                        Text(
                            text = "Queued",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Task.DownloadState.Idle -> {
                        Text(
                            text = "Queued",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    is Task.DownloadState.Completed -> {
                        Text(
                            text = "Downloaded · $formattedTotalSize",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    is Task.DownloadState.Error -> {
                        Text(
                            text = "Failed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        if (errorText.isNotBlank()) {
                            Text(
                                text = errorText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    is Task.DownloadState.Canceled -> {
                        Text(
                            text = "Canceled",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

// --- Data & Parsing ---

data class ProgressInfo(
    val percentage: String = "",
    val totalSize: String = "",
    val speed: String = "",
    val eta: String = "",
)

private fun stripAnsiCodes(s: String): String {
    return s.replace(Regex("\\u001B\\[[;\\d]*m"), "")
}

/*
 * Parses yt-dlp standard output line:
 * [download]  45.0% of 100.00MiB at  5.00MiB/s ETA 00:11
 * [download]  45.0% of 100.00MiB at  5.00MiB/s ETA 00:11 (frag 1/200)
 */
fun parseProgress(line: String): ProgressInfo {
    if (line.isBlank()) return ProgressInfo()

    val cleaned = stripAnsiCodes(line)

    val percentageRegex = Regex("(\\d{1,3}(?:\\.\\d+)?)%")
    val speedRegex = Regex("at\\s+(~?\\d+(?:\\.\\d+)?)\\s*([KMGT]?i?B/s)", RegexOption.IGNORE_CASE)
    val etaRegex = Regex("ETA\\s+([0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?)")
    val sizeRegex = Regex("of\\s+(~?\\d+(?:\\.\\d+)?)\\s*([KMGT]?i?B)", RegexOption.IGNORE_CASE)

    val percentage = percentageRegex.find(cleaned)?.groupValues?.getOrNull(1)?.let { "$it%" }.orEmpty()

    val speedMatch = speedRegex.find(cleaned)
    val speed = speedMatch?.let { "${it.groupValues.getOrNull(1).orEmpty()} ${it.groupValues.getOrNull(2).orEmpty()}" }?.trim().orEmpty()

    val eta = etaRegex.find(cleaned)?.groupValues?.getOrNull(1).orEmpty()

    val sizeMatch = sizeRegex.find(cleaned)
    val size = sizeMatch?.let { "${it.groupValues.getOrNull(1).orEmpty()} ${it.groupValues.getOrNull(2).orEmpty()}" }?.trim().orEmpty()

    return ProgressInfo(percentage = percentage, speed = speed.trim(), eta = eta, totalSize = size.trim())
}

// --- Formatting Helpers ---

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

fun formatDuration(totalSeconds: Int): String {
    if (totalSeconds <= 0) return ""
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
