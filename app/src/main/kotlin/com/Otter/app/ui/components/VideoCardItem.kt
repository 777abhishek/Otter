package com.Otter.app.ui.components

import android.content.Intent
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.Otter.app.data.models.Video
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun VideoCardItem(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelect: ((Boolean) -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onPlayAsAudio: (() -> Unit)? = null,
    onLike: (() -> Unit)? = null,
    onWatchLater: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    aiEnabled: Boolean = false,
) {
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showVideoInfo by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val cardColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
                .heightIn(min = 100.dp)
                .clickable {
                    if (isSelectionMode) {
                        onSelect?.invoke(!isSelected)
                    } else {
                        onClick()
                    }
                },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, hoveredElevation = 1.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = cardColor,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
                    .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val thumbShape = RoundedCornerShape(12.dp)
            Box(
                modifier =
                    Modifier
                        .width(160.dp)
                        .height(90.dp)
                        .clip(thumbShape),
                contentAlignment = Alignment.Center,
            ) {
                if (video.thumbnail.isNotEmpty()) {
                    AsyncImage(
                        model = video.thumbnail,
                        contentDescription = video.title,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if (video.duration > 0) {
                    Surface(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                    ) {
                        Text(
                            text = formatDurationTimestamp(video.duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = MaterialTheme.typography.titleSmall.lineHeight,
                )

                if (video.channelName.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = video.channelName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (video.views > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = formatViews(video.views),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    if (video.views > 0 && video.uploadDate.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(3.dp)
                        ) {}
                    }

                    if (video.uploadDate.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = formatUploadDate(video.uploadDate),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelect,
                    modifier = Modifier.size(26.dp),
                )
            } else {
                IconButton(
                    onClick = {
                        showMenu = true
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More options",
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showMenu) {
        val menuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

        LaunchedEffect(Unit) {
            menuSheetState.show()
        }

        fun dismissMenuSheet() {
            scope.launch {
                menuSheetState.hide()
            }.invokeOnCompletion {
                showMenu = false
            }
        }

        VideoMenuBottomSheet(
            video = video,
            sheetState = menuSheetState,
            onDismiss = { dismissMenuSheet() },
            onPlay = {
                dismissMenuSheet()
                onClick()
            },
            onPlayAsAudio = {
                dismissMenuSheet()
                onPlayAsAudio?.invoke()
            },
            onDownload = {
                dismissMenuSheet()
                if (onDownload != null) {
                    onDownload()
                } else {
                    Toast.makeText(context, "Download not available here", Toast.LENGTH_SHORT).show()
                }
            },
            downloadEnabled = onDownload != null,
            onShare = {
                dismissMenuSheet()
                shareVideo(context, video)
            },
            onDelete = {
                dismissMenuSheet()
                showDeleteConfirm = true
            },
            onLike = {
                dismissMenuSheet()
                if (onLike != null) {
                    onLike()
                } else {
                    Toast.makeText(context, "Like not available here", Toast.LENGTH_SHORT).show()
                }
            },
            onWatchLater = {
                dismissMenuSheet()
                if (onWatchLater != null) {
                    onWatchLater()
                } else {
                    Toast.makeText(context, "Watch Later not available here", Toast.LENGTH_SHORT).show()
                }
            },
            onAddToPlaylist = {
                dismissMenuSheet()
                if (onAddToPlaylist != null) {
                    onAddToPlaylist()
                } else {
                    Toast.makeText(context, "Add to Playlist not available here", Toast.LENGTH_SHORT).show()
                }
            },
            onVideoInfo = {
                dismissMenuSheet()
                showVideoInfo = true
            },
            onSummary = {
                dismissMenuSheet()
                if (aiEnabled) {
                    Toast.makeText(context, "Summary: ${video.title}", Toast.LENGTH_SHORT).show()
                }
            },
            aiEnabled = aiEnabled,
        )
    }

    if (showVideoInfo) {
        VideoInfoBottomSheet(
            video = video,
            onDismiss = { showVideoInfo = false },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete from device") },
            text = { Text("Remove the downloaded file?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete?.invoke()
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoMenuBottomSheet(
    video: Video,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayAsAudio: () -> Unit,
    onDownload: () -> Unit,
    downloadEnabled: Boolean,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onLike: () -> Unit,
    onWatchLater: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onVideoInfo: () -> Unit,
    onSummary: () -> Unit,
    aiEnabled: Boolean = false,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.heightIn(min = 200.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            MenuItem(
                icon = Icons.Rounded.PlayArrow,
                title = "Play",
                subtitle = "Start watching now",
                onClick = onPlay,
            )
            MenuItem(
                icon = Icons.Rounded.MusicNote,
                title = "Play as Audio",
                subtitle = "Stream audio only",
                onClick = onPlayAsAudio,
            )
            MenuItem(
                icon = Icons.Rounded.Download,
                title = "Download",
                subtitle = "Save for offline",
                onClick = onDownload,
                enabled = downloadEnabled,
            )
            MenuItem(
                icon = Icons.Rounded.Share,
                title = "Share",
                subtitle = "Send video link",
                onClick = onShare,
            )
            MenuItem(
                icon = Icons.Rounded.Favorite,
                title = "Like",
                subtitle = "Add to liked videos",
                onClick = onLike,
            )
            MenuItem(
                icon = Icons.Rounded.BookmarkBorder,
                title = "Watch Later",
                subtitle = "Save to watch later",
                onClick = onWatchLater,
            )
            MenuItem(
                icon = Icons.Rounded.PlaylistAdd,
                title = "Add to Playlist",
                subtitle = "Organize your library",
                onClick = onAddToPlaylist,
            )
            MenuItem(
                icon = Icons.Rounded.Info,
                title = "Video Info",
                subtitle = "Details and metadata",
                onClick = onVideoInfo,
            )
            if (aiEnabled) {
                MenuItem(
                    icon = Icons.Rounded.Edit,
                    title = "Summary",
                    subtitle = "Generate video summary",
                    onClick = onSummary,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val iconColor = MaterialTheme.colorScheme.primary
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) iconColor else iconColor.copy(alpha = 0.5f),
            )
        },
        headlineContent = {
            Text(
                text = title,
                color = if (enabled) color else color.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium,
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                color = (if (enabled) color else color.copy(alpha = 0.5f)).copy(alpha = 0.7f),
            )
        },
        colors =
            ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = color,
                supportingColor = color.copy(alpha = 0.7f),
                leadingIconColor = iconColor,
            ),
        modifier = Modifier.clickable(enabled = enabled) { onClick() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoInfoBottomSheet(
    video: Video,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Video Info",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            InfoRow(label = "Title", value = video.title)
            InfoRow(label = "Channel", value = video.channelName)
            InfoRow(label = "Duration", value = formatDuration(video.duration))
            InfoRow(label = "Views", value = formatViews(video.views))
            if (video.uploadDate.isNotEmpty()) {
                InfoRow(label = "Uploaded", value = video.uploadDate)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun formatViews(views: Long): String {
    return when {
        views >= 1_000_000 -> String.format("%.1fM", views / 1_000_000.0)
        views >= 1_000 -> String.format("%.1fK", views / 1_000.0)
        else -> views.toString()
    }
}

private fun formatUploadDate(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    
    return try {
        // yt-dlp returns date in YYYYMMDD format
        if (dateStr.matches(Regex("\\d{8}"))) {
            val inputFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
            val date = inputFormat.parse(dateStr) ?: return dateStr
            val now = Date()
            val diffMs = now.time - date.time
            val diffDays = diffMs / (1000 * 60 * 60 * 24)
            
            when {
                diffDays < 1 -> "Today"
                diffDays < 7 -> "$diffDays days ago"
                diffDays < 30 -> "${diffDays / 7} weeks ago"
                diffDays < 365 -> "${diffDays / 30} months ago"
                else -> "${diffDays / 365} years ago"
            }
        } else {
            // Already formatted or different format, return as-is
            dateStr
        }
    } catch (_: Exception) {
        dateStr
    }
}

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

private fun formatDurationTimestamp(seconds: Int): String {
    if (seconds <= 0) return "0:00"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}

private fun shareVideo(
    context: android.content.Context,
    video: Video,
) {
    val shareContent = "${video.title}\n\nhttps://www.youtube.com/watch?v=${video.id}"
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareContent)
            putExtra(Intent.EXTRA_SUBJECT, video.title)
        }
    context.startActivity(Intent.createChooser(intent, "Share video"))
}
