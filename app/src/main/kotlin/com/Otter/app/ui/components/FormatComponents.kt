package com.Otter.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Otter.app.data.download.Format
import com.Otter.app.data.download.VideoInfo
import kotlin.math.ln
import kotlin.math.pow

@Composable
fun FormatItem(
    formatInfo: Format,
    duration: Double,
    selected: Boolean,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    outlineColor: Color = MaterialTheme.colorScheme.primary,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit,
) {
    val vcodecText = formatInfo.vcodec?.substringBefore(delimiter = ".").orEmpty()
    val acodecText = formatInfo.acodec?.substringBefore(delimiter = ".").orEmpty()
    val codecText = connectWithBlank(vcodecText, acodecText).run { if (isNotBlank()) "($this)" else this }

    val tbrText = formatInfo.tbr?.toBitrateText().orEmpty()
    val fileSizeBytes =
        formatInfo.fileSize
            ?: formatInfo.fileSizeApprox
            ?: (formatInfo.tbr?.times(duration * 125))
    val fileSizeText = fileSizeBytes.toFileSizeText()

    val firstLineText = connectWithDelimiter(fileSizeText, tbrText, delimiter = " ")
    val secondLineText = connectWithDelimiter(formatInfo.ext, codecText, delimiter = " ").uppercase()

    FormatItemCard(
        title = formatInfo.format ?: formatInfo.formatId,
        containsAudio = formatInfo.containsAudio(),
        containsVideo = formatInfo.containsVideo(),
        firstLineText = firstLineText,
        secondLineText = secondLineText,
        selected = selected,
        outlineColor = outlineColor,
        containerColor = containerColor,
        onLongClick = onLongClick,
        onClick = onClick,
    )
}

@Composable
fun FormatSubtitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.titleSmall,
        modifier = modifier,
    )
}

@Composable
fun FormatVideoPreview(
    modifier: Modifier = Modifier,
    title: String,
    author: String,
    thumbnailUrl: String,
    duration: Int,
    isClippingVideo: Boolean,
    isSplittingVideo: Boolean,
    isClippingAvailable: Boolean,
    isSplitByChapterAvailable: Boolean,
    onClippingToggled: () -> Unit,
    onSplittingToggled: () -> Unit,
    onRename: () -> Unit,
    onOpenThumbnail: () -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = author,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SuggestedFormatItem(
    modifier: Modifier = Modifier,
    videoInfo: VideoInfo,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val requestedFormats =
        remember(videoInfo) {
            videoInfo.requestedFormats ?: emptyList()
        }
    val duration = videoInfo.duration ?: 0.0
    val containsVideo = requestedFormats.any { it.containsVideo() }
    val containsAudio = requestedFormats.any { it.containsAudio() }
    val title =
        if (requestedFormats.isEmpty()) {
            videoInfo.title
        } else {
            requestedFormats.joinToString(
                separator = " + ",
            ) { it.format.orEmpty() }
        }

    val totalFileSize =
        requestedFormats.fold(initial = 0.0) { acc, format ->
            acc + (format.fileSize ?: format.fileSizeApprox ?: (duration * (format.tbr ?: 0.0) * 125))
        }
    val fileSizeText = totalFileSize.toFileSizeText()
    val totalTbr = requestedFormats.fold(initial = 0.0) { acc, format -> acc + (format.tbr ?: 0.0) }
    val tbrText = totalTbr.toBitrateText()
    val firstLineText = connectWithDelimiter(fileSizeText, tbrText, delimiter = " ")

    val vcodecText = videoInfo.vcodec?.substringBefore(delimiter = ".") ?: ""
    val acodecText = videoInfo.acodec?.substringBefore(delimiter = ".") ?: ""
    val codecText = connectWithBlank(vcodecText, acodecText).run { if (isNotBlank()) "($this)" else this }
    val secondLineText = connectWithDelimiter(videoInfo.ext, codecText, delimiter = " ").uppercase()

    FormatItemCard(
        modifier = modifier,
        title = title,
        containsAudio = containsAudio,
        containsVideo = containsVideo,
        firstLineText = firstLineText,
        secondLineText = secondLineText,
        selected = selected,
        onClick = onClick,
    )
}

@Composable
fun VideoFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
fun TextButtonWithIcon(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    TextButton(
        onClick = onClick,
    ) {
        Icon(icon, contentDescription = null)
        Text(text)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FormatItemCard(
    modifier: Modifier = Modifier,
    title: String?,
    containsAudio: Boolean,
    containsVideo: Boolean,
    firstLineText: String,
    secondLineText: String,
    selected: Boolean,
    outlineColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    val animatedTitleColor =
        animateColorAsState(
            if (selected) outlineColor else MaterialTheme.colorScheme.onSurface,
            label = "",
        )
    val animatedContainerColor =
        animateColorAsState(
            if (selected) containerColor else MaterialTheme.colorScheme.surface,
            label = "",
        )
    val animatedOutlineColor =
        animateColorAsState(
            targetValue = if (selected) outlineColor else MaterialTheme.colorScheme.outlineVariant,
            label = "",
        )

    Box(
        modifier =
            modifier
                .padding(4.dp)
                .selectable(selected = selected) { onClick() }
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .border(
                    width = 1.dp,
                    color = animatedOutlineColor.value,
                    shape = RoundedCornerShape(12.dp),
                )
                .background(animatedContainerColor.value, shape = RoundedCornerShape(12.dp)),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = title.orEmpty().ifBlank { "Unknown" },
                style = MaterialTheme.typography.titleSmall,
                minLines = 2,
                maxLines = 2,
                color = animatedTitleColor.value,
                overflow = TextOverflow.Clip,
            )
            Text(
                text = firstLineText,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
            Text(
                text = secondLineText,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 2.dp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        Row(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 6.dp, end = 6.dp),
        ) {
            if (containsVideo) {
                Icon(
                    imageVector = Icons.Rounded.Videocam,
                    tint = outlineColor,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            if (containsAudio) {
                Icon(
                    imageVector = Icons.Rounded.Audiotrack,
                    tint = outlineColor,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            if (!containsVideo && !containsAudio) {
                Icon(
                    imageVector = Icons.Rounded.QuestionMark,
                    tint = outlineColor,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun connectWithBlank(
    a: String,
    b: String,
): String {
    return listOf(a, b).filter { it.isNotBlank() }.joinToString(separator = " ")
}

private fun connectWithDelimiter(
    a: String?,
    b: String?,
    delimiter: String,
): String {
    return listOf(a.orEmpty(), b.orEmpty()).filter { it.isNotBlank() }.joinToString(separator = delimiter)
}

private fun Double?.toFileSizeText(): String {
    val bytes = this ?: return ""
    if (bytes <= 0) return ""
    val unit = 1024.0
    val exp = (ln(bytes) / ln(unit)).toInt().coerceIn(0, 4)
    val pre = arrayOf("B", "KB", "MB", "GB", "TB")[exp]
    val value = bytes / unit.pow(exp.toDouble())
    return when {
        value >= 100 -> "%.0f %s".format(value, pre)
        value >= 10 -> "%.1f %s".format(value, pre)
        else -> "%.2f %s".format(value, pre)
    }
}

private fun Double.toBitrateText(): String {
    return when {
        this <= 0 -> ""
        this < 1024.0 -> "%.1f Kbps".format(this)
        else -> "%.2f Mbps".format(this / 1024.0)
    }
}
