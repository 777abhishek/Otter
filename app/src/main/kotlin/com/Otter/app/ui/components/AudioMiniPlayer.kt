package com.Otter.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.Otter.app.R
import com.Otter.app.player.PlayerConnectionManager
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AudioMiniPlayer(
    playerConnectionManager: PlayerConnectionManager,
    modifier: Modifier = Modifier,
) {
    val isPlaying by playerConnectionManager.isPlaying.collectAsState()
    val isBuffering by playerConnectionManager.isBuffering.collectAsState()
    val currentPosition by playerConnectionManager.currentPosition.collectAsState()
    val duration by playerConnectionManager.duration.collectAsState()
    val currentMediaItem by playerConnectionManager.currentMediaItem.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current

    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec =
        spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
        )

    val overlayAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.0f else 0.4f,
        label = "overlay_alpha",
        animationSpec = animationSpec,
    )

    val videoId = currentMediaItem?.extras?.getString("videoId")
    val title = currentMediaItem?.extras?.getString("title") ?: ""
    val uploaderName = currentMediaItem?.extras?.getString("uploaderName") ?: ""
    val thumbnailUrl = currentMediaItem?.extras?.getString("thumbnailUrl") ?: ""

    if (videoId == null) return

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 12.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragStartTime = System.currentTimeMillis()
                            totalDragDistance = 0f
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetXAnimatable.animateTo(
                                    targetValue = 0f,
                                    animationSpec = animationSpec,
                                )
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            val adjustedDragAmount =
                                if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                            totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                            coroutineScope.launch {
                                offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                            }
                        },
                        onDragEnd = {
                            val dragDuration = System.currentTimeMillis() - dragStartTime
                            val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                            val currentOffset = offsetXAnimatable.value

                            val minDistanceThreshold = 50f
                            val velocityThreshold = 0.5f

                            val shouldChangeSong =
                                (
                                    kotlin.math.abs(currentOffset) > minDistanceThreshold &&
                                        velocity > velocityThreshold
                                ) || kotlin.math.abs(currentOffset) > 300f

                            if (shouldChangeSong) {
                                val isRightSwipe = currentOffset > 0
                                if (isRightSwipe) {
                                    playerConnectionManager.playPrevious()
                                } else {
                                    playerConnectionManager.playNext()
                                }
                            }

                            coroutineScope.launch {
                                offsetXAnimatable.animateTo(
                                    targetValue = 0f,
                                    animationSpec = animationSpec,
                                )
                            }
                        },
                    )
                },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
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
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        )
                    } else if (duration > 0) {
                        CircularProgressIndicator(
                            progress = { (currentPosition.toFloat() / duration).coerceIn(0f, 1f) },
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = CircleShape,
                                )
                                .clickable {
                                    if (playerConnectionManager.player?.playbackState == Player.STATE_ENDED) {
                                        playerConnectionManager.player?.seekTo(0, 0)
                                        playerConnectionManager.player?.playWhenReady = true
                                    } else {
                                        playerConnectionManager.togglePlayback()
                                    }
                                },
                    ) {
                        if (thumbnailUrl.isNotBlank()) {
                            AsyncImage(
                                model = thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                            )
                        }

                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        color = Color.Black.copy(alpha = overlayAlpha),
                                        shape = CircleShape,
                                    ),
                        )

                        AnimatedContent(
                            targetState = (playerConnectionManager.player?.playbackState == Player.STATE_ENDED || !isPlaying),
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "play_pause_icon",
                        ) { showPlayIcon: Boolean ->
                            if (showPlayIcon) {
                                Icon(
                                    imageVector = if (playerConnectionManager.player?.playbackState == Player.STATE_ENDED) {
                                        Icons.Rounded.Refresh
                                    } else {
                                        Icons.Rounded.PlayArrow
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    AnimatedContent(
                        targetState = title,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "",
                    ) { title ->
                        Text(
                            text = title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
                        )
                    }

                    if (uploaderName.isNotBlank()) {
                        AnimatedContent(
                            targetState = uploaderName,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "",
                        ) { uploader ->
                            Text(
                                text = uploader,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = { playerConnectionManager.player?.seekTo(currentPosition - 15000) },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FastRewind,
                        contentDescription = "Rewind 15 seconds",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp),
                    )
                }

                IconButton(
                    onClick = { playerConnectionManager.player?.seekTo(currentPosition + 15000) },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FastForward,
                        contentDescription = "Fast forward 15 seconds",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp),
                    )
                }

                IconButton(
                    onClick = { playerConnectionManager.stop() },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
