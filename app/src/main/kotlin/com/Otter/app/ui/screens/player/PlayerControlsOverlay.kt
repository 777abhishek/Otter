package com.Otter.app.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoSettings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val topGradient = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
private val bottomGradient = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerControlsOverlay(
    title: String,
    uploaderName: String,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPosition: Long,
    duration: Long,
    isFullscreen: Boolean,
    showCenterControls: Boolean = true,
    onQualityClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onFullscreenToggle: () -> Unit,
    onPipClick: () -> Unit,
    onBackClick: () -> Unit,
    onShowQueue: () -> Unit,
    onShowChapters: () -> Unit,
    onShowInfo: (() -> Unit)? = null,
    hasChapters: Boolean,
    hasQueue: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // ── TOP BAR — Back · Title · Tune (settings only) ─────────────────
        Row(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(topGradient))
                    .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Back
            IconButton(onClick = onBackClick, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }

            // Title + uploader
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = uploaderName,
                    color = Color.White.copy(alpha = 0.60f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Tune — only icon on the right of the top bar
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "Playback settings",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // ── CENTER — auto-hide Rewind / Play-Pause / Forward ──────────────
        AnimatedVisibility(
            visible = showCenterControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                // Rewind 10s
                IconButton(
                    onClick = onRewind,
                    modifier =
                        Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.40f)),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Replay10,
                        contentDescription = "Rewind 10 seconds",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }

                // Play / Pause / Buffering
                IconButton(
                    onClick = { onPlayPauseClick() },
                    modifier =
                        Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f)),
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(38.dp),
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }

                // Forward 10s
                IconButton(
                    onClick = onForward,
                    modifier =
                        Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.40f)),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Forward10,
                        contentDescription = "Forward 10 seconds",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }

        // ── BOTTOM BAR — seekbar · timestamp · all controls · fullscreen ──
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(bottomGradient))
                    .padding(horizontal = 16.dp)
                    .padding(top = 32.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val isDragging by interactionSource.collectIsDraggedAsState()
            var sliderPosition by remember { mutableFloatStateOf(0f) }

            LaunchedEffect(currentPosition, duration, isDragging) {
                if (!isDragging && duration > 0) {
                    sliderPosition = currentPosition.toFloat() / duration.toFloat()
                }
            }

            // Seekbar
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                LinearWavyProgressIndicator(
                    progress = { sliderPosition.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    amplitude = { 1f },
                    wavelength = 18.dp,
                )
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    onValueChangeFinished = { onSeek((sliderPosition * duration).toLong()) },
                    modifier = Modifier.fillMaxWidth(),
                    interactionSource = interactionSource,
                    colors =
                        SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent,
                        ),
                )
            }

            // Bottom controls row
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Timestamp
                Text(
                    text = formatDuration(currentPosition),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                )
                Text(
                    text = " / ${formatDuration(duration)}",
                    color = Color.White.copy(alpha = 0.50f),
                    style = MaterialTheme.typography.labelMedium,
                )

                Spacer(modifier = Modifier.weight(1f))

                // Queue  — QueueMusic
                if (hasQueue || hasChapters) {
                    BottomIconButton(onClick = onShowQueue, contentDescription = "Queue") {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // Chapters  — Bookmarks
                if (hasChapters) {
                    BottomIconButton(onClick = onShowChapters, contentDescription = "Chapters") {
                        Icon(
                            imageVector = Icons.Rounded.Bookmarks,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // Info — Info
                if (onShowInfo != null) {
                    BottomIconButton(onClick = onShowInfo, contentDescription = "Info") {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // PiP  — PictureInPictureAlt
                BottomIconButton(onClick = onPipClick, contentDescription = "Picture in Picture") {
                    Icon(
                        imageVector = Icons.Rounded.PictureInPictureAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Quality  — VideoSettings (correct icon, not the Settings gear)
                BottomIconButton(onClick = onQualityClick, contentDescription = "Video quality") {
                    Icon(
                        imageVector = Icons.Rounded.VideoSettings,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }

                // Fullscreen  — Fullscreen / FullscreenExit
                BottomIconButton(
                    onClick = onFullscreenToggle,
                    contentDescription = if (isFullscreen) "Exit fullscreen" else "Fullscreen",
                ) {
                    Icon(
                        imageVector =
                            if (isFullscreen) {
                                Icons.Rounded.FullscreenExit
                            } else {
                                Icons.Rounded.Fullscreen
                            },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun BottomIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        content()
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
