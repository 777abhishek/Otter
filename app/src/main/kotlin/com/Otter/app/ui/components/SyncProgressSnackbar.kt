package com.Otter.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Otter.app.data.sync.SubscriptionSyncService
import kotlinx.coroutines.delay

@Composable
fun SyncProgressSnackbar(
    syncState: SubscriptionSyncService.SyncState,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
) {
    LaunchedEffect(syncState) {
        when (syncState) {
            is SubscriptionSyncService.SyncState.Success,
            is SubscriptionSyncService.SyncState.Error,
            -> {
                delay(2500)
                onDismiss()
            }

            else -> Unit
        }
    }

    when (syncState) {
        is SubscriptionSyncService.SyncState.Syncing -> {
            SyncingSnackbar(
                stage = syncState.stage,
                progress = syncState.progress,
                modifier = modifier,
                onDismiss = onDismiss,
            )
        }
        is SubscriptionSyncService.SyncState.Success -> {
            SuccessSnackbar(
                result = syncState.result,
                modifier = modifier,
                onDismiss = onDismiss,
            )
        }
        is SubscriptionSyncService.SyncState.Error -> {
            ErrorSnackbar(
                message = syncState.message,
                modifier = modifier,
                onDismiss = onDismiss,
            )
        }
        SubscriptionSyncService.SyncState.Idle -> {}
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SyncingSnackbar(
    stage: String,
    progress: Float?,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress ?: 0f,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "progress",
    )

    // Parse stage to extract playlist name and video counts
    // Stage formats: "Fetching playlist videos (1/5) • 23 videos", "Fetched 1/5 playlists", "Stored 1/5 playlists"
    val (titleLine, subtitleLine) =
        remember(stage) {
            parseStageToDisplay(stage)
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp),
                ) {
                    if (progress != null) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        )
                        // Percentage inside the circular progress
                        val progressPercent = (animatedProgress * 100).toInt()
                        Text(
                            text = "$progressPercent%",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        // Indeterminate progress
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = titleLine,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitleLine,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

private fun parseStageToDisplay(stage: String): Pair<String, String> {
    // Try to extract playlist name and video counts from stage
    // Formats: "Fetching playlist videos (1/5) • 23 videos", "Fetched 1/5 playlists", "Stored 1/5 playlists"

    val bulletIndex = stage.indexOf("•")

    return when {
        // "Fetching playlist videos (1/5) • 23 videos" -> title: "Fetching videos", subtitle: "1/5 playlists • 23 videos"
        stage.contains("Fetching") && bulletIndex != -1 -> {
            val afterBullet = stage.substring(bulletIndex + 1).trim()
            val parenIndex = stage.indexOf("(")
            val playlistProgress =
                if (parenIndex != -1) {
                    val closeParen = stage.indexOf(")", parenIndex)
                    if (closeParen != -1) stage.substring(parenIndex + 1, closeParen) else ""
                } else {
                    ""
                }
            "Fetching videos" to "$playlistProgress playlists • $afterBullet"
        }

        // "Fetched 1/5 playlists" -> title: "Fetching videos", subtitle: "1/5 playlists"
        stage.contains("Fetched") -> {
            val match = Regex("(\\d+/\\d+)").find(stage)
            val progress = match?.value ?: ""
            "Fetching videos" to "$progress playlists"
        }

        // "Stored 1/5 playlists" -> title: "Storing to database", subtitle: "1/5 playlists"
        stage.contains("Stored") -> {
            val match = Regex("(\\d+/\\d+)").find(stage)
            val progress = match?.value ?: ""
            "Storing to database" to "$progress playlists"
        }

        // "Storing to database (1/5) • 23 videos" -> title: "Storing to database", subtitle: "1/5 • 23 videos"
        stage.contains("Storing") && bulletIndex != -1 -> {
            val afterBullet = stage.substring(bulletIndex + 1).trim()
            val parenIndex = stage.indexOf("(")
            val progress =
                if (parenIndex != -1) {
                    val closeParen = stage.indexOf(")", parenIndex)
                    if (closeParen != -1) stage.substring(parenIndex + 1, closeParen) else ""
                } else {
                    ""
                }
            "Storing to database" to "$progress • $afterBullet"
        }

        // Default: use stage as-is
        else -> "Syncing..." to stage
    }
}

@Composable
private fun SuccessSnackbar(
    result: SubscriptionSyncService.SyncResult,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp),
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        strokeWidth = 3.dp,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Sync Complete!",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${result.playlistsCount} playlists • ${result.totalVideosCount} videos",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp),
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.error,
                        strokeWidth = 3.dp,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Sync Failed",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
