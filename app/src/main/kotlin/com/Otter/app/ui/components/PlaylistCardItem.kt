package com.Otter.app.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
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
import com.Otter.app.data.models.Playlist
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PlaylistCardItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onPlay: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isRearranging: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onDownloadAll: ((String) -> Unit)? = null,
    onPlayAsAudio: (() -> Unit)? = null,
) {
    var showMenu by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp)
                .heightIn(min = 72.dp)
                .clickable {
                    if (isRearranging) {
                        onLongPress?.invoke()
                    } else {
                        onClick()
                    }
                },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val thumbShape = RoundedCornerShape(12.dp)
            Box(
                modifier =
                    Modifier
                        .size(width = 108.dp, height = 60.dp)
                        .clip(thumbShape),
                contentAlignment = Alignment.Center,
            ) {
                if (playlist.thumbnail.isNotEmpty()) {
                    AsyncImage(
                        model = playlist.thumbnail,
                        contentDescription = playlist.title,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlaylistPlay,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${playlist.videoCount} videos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isRearranging) {
                Icon(
                    imageVector = Icons.Rounded.DragHandle,
                    contentDescription = "Drag to reorder",
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                IconButton(
                    onClick = {
                        showMenu = true
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Playlist options",
                        modifier = Modifier.size(20.dp),
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

        PlaylistMenuBottomSheet(
            playlist = playlist,
            sheetState = menuSheetState,
            onDismiss = { dismissMenuSheet() },
            onPlay = {
                dismissMenuSheet()
                (onPlay ?: onClick).invoke()
            },
            onPlayAsAudio = {
                dismissMenuSheet()
                onPlayAsAudio?.invoke()
            },
            onShare = {
                dismissMenuSheet()
                // Share will be handled inside the menu
            },
            onDelete = {
                dismissMenuSheet()
                // TODO: Implement playlist deletion
            },
            onLike = {
                dismissMenuSheet()
                // TODO: Implement playlist liking
            },
            onDownloadAll = {
                dismissMenuSheet()
                if (onDownloadAll != null) {
                    onDownloadAll("https://www.youtube.com/playlist?list=${playlist.id}")
                } else {
                    Toast.makeText(
                        context,
                        "Open the playlist to download videos",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
            onInfo = {
                dismissMenuSheet()
                // TODO: Show playlist info
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistMenuBottomSheet(
    playlist: Playlist,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayAsAudio: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onLike: () -> Unit,
    onDownloadAll: () -> Unit,
    onInfo: () -> Unit,
) {
    val context = LocalContext.current
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
                title = "Play Playlist",
                subtitle = "Start watching playlist",
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
                title = "Download All",
                subtitle = "Save all videos offline",
                onClick = onDownloadAll,
            )
            MenuItem(
                icon = Icons.Rounded.Share,
                title = "Share",
                subtitle = "Share playlist link",
                onClick = {
                    onShare()
                    sharePlaylist(context, playlist)
                },
            )
            MenuItem(
                icon = Icons.Rounded.Favorite,
                title = "Like Playlist",
                subtitle = "Add to liked playlists",
                onClick = onLike,
            )
            MenuItem(
                icon = Icons.Rounded.Info,
                title = "Playlist Info",
                subtitle = "View playlist details",
                onClick = onInfo,
            )
            MenuItem(
                icon = Icons.Rounded.Delete,
                title = "Delete",
                subtitle = "Remove from device",
                onClick = onDelete,
            )
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
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val iconColor = MaterialTheme.colorScheme.primary
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
            )
        },
        headlineContent = {
            Text(
                text = title,
                color = color,
                fontWeight = FontWeight.Medium,
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                color = color.copy(alpha = 0.7f),
            )
        },
        colors =
            ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = color,
                supportingColor = color.copy(alpha = 0.7f),
                leadingIconColor = iconColor,
            ),
        modifier = Modifier.clickable { onClick() },
    )
}

private fun sharePlaylist(
    context: android.content.Context,
    playlist: Playlist,
) {
    val shareContent = "${playlist.title}\n\nhttps://www.youtube.com/playlist?list=${playlist.id}"
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareContent)
            putExtra(Intent.EXTRA_SUBJECT, playlist.title)
        }
    context.startActivity(Intent.createChooser(intent, "Share playlist"))
}
