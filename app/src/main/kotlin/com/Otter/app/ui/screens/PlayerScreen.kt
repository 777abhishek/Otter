@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.Otter.app.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.navigation.NavController
import com.Otter.app.data.models.Video
import com.Otter.app.player.PlayerConnectionManager
import com.Otter.app.player.PlayerService
import com.Otter.app.player.QueueItem
import com.Otter.app.ui.screens.player.CodecChoice
import com.Otter.app.ui.screens.player.PlaybackSettingsSheet
import com.Otter.app.ui.screens.player.PlayerAudioSheet
import com.Otter.app.ui.screens.player.PlayerControlsOverlay
import com.Otter.app.ui.screens.player.PlayerQualitySheet
import com.Otter.app.ui.screens.player.PlayerQueueBottomSheet
import com.Otter.app.ui.screens.player.VideoPlayerView
import com.Otter.app.ui.viewmodels.PlayerViewModel
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerScreen(
    videoId: String,
    navController: NavController,
    playerConnectionManager: PlayerConnectionManager,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentVideo by viewModel.currentVideo.collectAsStateWithLifecycle()
    val isPlaying by playerConnectionManager.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by playerConnectionManager.currentPosition.collectAsStateWithLifecycle()
    val duration by playerConnectionManager.duration.collectAsStateWithLifecycle()
    val isBuffering by playerConnectionManager.isBuffering.collectAsStateWithLifecycle()
    val availableAudioLanguages by playerConnectionManager.availableAudioLanguages.collectAsStateWithLifecycle()
    val availableCaptionLanguages by playerConnectionManager.availableCaptionLanguages.collectAsStateWithLifecycle()
    val audioTrackGroups by playerConnectionManager.audioTrackGroups.collectAsStateWithLifecycle()
    val captionTrackGroups by playerConnectionManager.captionTrackGroups.collectAsStateWithLifecycle()
    val sessionQueue by playerConnectionManager.queue.collectAsStateWithLifecycle()
    val sessionQueueIndex by playerConnectionManager.queueIndex.collectAsStateWithLifecycle()
    val isAudioOnlySession by playerConnectionManager.isAudioOnlySession.collectAsStateWithLifecycle()
    val currentMediaMeta by playerConnectionManager.currentMediaItem.collectAsStateWithLifecycle()
    val tracks by playerConnectionManager.tracks.collectAsStateWithLifecycle()

    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()

    val streamInfo by viewModel.streamInfo.collectAsStateWithLifecycle()
    val appSettings by viewModel.settings.collectAsStateWithLifecycle()

    var isFullscreen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showPlaybackSettingsSheet by remember { mutableStateOf(false) }

    var sessionDataSaver by remember { mutableStateOf(false) }
    var currentMaxHeight by remember { mutableStateOf(Int.MAX_VALUE) }
    var currentCodec by remember { mutableStateOf(CodecChoice.AUTO) }

    var captionsEnabled by remember { mutableStateOf(false) }
    var selectedCaptionLanguage by remember { mutableStateOf<String?>(null) }
    var selectedAudioLanguage by remember { mutableStateOf<String?>(null) }
    var selectedAudioStreamSignature by remember { mutableStateOf<Triple<String?, String?, Int?>?>(null) }
    
    // Get actual values from player
    val actualPlaybackSpeed by playerConnectionManager.playbackSpeed.collectAsStateWithLifecycle()
    val actualVideoHeight by playerConnectionManager.currentVideoHeight.collectAsStateWithLifecycle()

    var gestureStart by remember { mutableStateOf(Offset.Zero) }
    var startBrightness by remember { mutableStateOf<Float?>(null) }
    var startVolume by remember { mutableStateOf<Int?>(null) }
    var showBrightnessHUD by remember { mutableStateOf(false) }
    var showVolumeHUD by remember { mutableStateOf(false) }
    var hudText by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    var controlsHideJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var lastPlayedVideoId by remember { mutableStateOf<String?>(null) }

    var isInPip by remember { mutableStateOf(false) }

    DisposableEffect(isInPip, isAudioOnlySession) {
        onDispose {
            // Ensure video streaming is stopped and the player is cleaned up when leaving this screen.
            // Keep audio-only sessions alive so the mini player can continue.
            if (!isInPip && !isAudioOnlySession) {
                playerConnectionManager.stop()
            }
        }
    }

    val audioManager =
        remember(context) {
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }

    fun scheduleControlsHide() {
        controlsHideJob?.cancel()
        controlsHideJob =
            coroutineScope.launch {
                delay(3000)
                if (isPlaying) showControls = false
            }
    }

    fun resetControlsTimer() {
        showControls = true
        scheduleControlsHide()
    }

    DisposableEffect(videoId) {
        viewModel.loadVideo(videoId)

        onDispose { }
    }

    // Keep settings UI state in sync with persisted settings. Without this, the sheet defaults to
    // "Auto" even after choosing a different option.
    LaunchedEffect(appSettings) {
        sessionDataSaver = appSettings.streamingDataSaver
        currentCodec =
            when (appSettings.preferredCodec) {
                com.Otter.app.data.models.PreferredCodec.AUTO -> CodecChoice.AUTO
                com.Otter.app.data.models.PreferredCodec.AV1 -> CodecChoice.AV1
                com.Otter.app.data.models.PreferredCodec.VP9 -> CodecChoice.VP9
                com.Otter.app.data.models.PreferredCodec.H264 -> CodecChoice.H264
            }
        // Sync max height from settings
        currentMaxHeight = when (appSettings.streamingQuality) {
            com.Otter.app.data.models.StreamingQuality.AUTO -> Int.MAX_VALUE
            com.Otter.app.data.models.StreamingQuality.SD_360P -> 360
            com.Otter.app.data.models.StreamingQuality.SD_480P -> 480
            com.Otter.app.data.models.StreamingQuality.HD_720P -> 720
            com.Otter.app.data.models.StreamingQuality.HD_1080P -> 1080
            com.Otter.app.data.models.StreamingQuality.UHD_4K -> 2160
            com.Otter.app.data.models.StreamingQuality.HIGHEST -> Int.MAX_VALUE
        }
    }

    // Reflect caption enabled state from actual track selection state.
    LaunchedEffect(tracks) {
        val t = tracks ?: return@LaunchedEffect
        captionsEnabled = t.groups.any { it.type == C.TRACK_TYPE_TEXT && it.isSelected }
    }

    // Sync selected audio/caption languages from actual track selection
    LaunchedEffect(audioTrackGroups, captionTrackGroups) {
        // Find the actually selected audio track
        val selectedAudio = audioTrackGroups.find { it.isSelected }
        selectedAudioLanguage = selectedAudio?.languageTag ?: selectedAudioLanguage

        // Find the actually selected caption track
        val selectedCaption = captionTrackGroups.find { it.isSelected }
        selectedCaptionLanguage = selectedCaption?.languageTag ?: selectedCaptionLanguage
    }

    LaunchedEffect(currentVideo?.id) {
        val video = currentVideo ?: return@LaunchedEffect
        if (lastPlayedVideoId == video.id) return@LaunchedEffect
        lastPlayedVideoId = video.id

        playerConnectionManager.setAudioOnly(false)

        val resumePositionMs = viewModel.getResumePositionMs(video.id)

        playerConnectionManager.play(
            videoId = video.id,
            title = video.title,
            thumbnailUrl = video.thumbnail,
            duration = video.duration.toLong() * 1000,
            uploaderName = video.channelName,
            audioOnly = false,
            startPosition = resumePositionMs,
            queue = viewModel.queue.value.map { it.toQueueItem() },
            queuePosition = viewModel.currentQueueIndex.value,
        )
    }

    // Persist playback progress while playing so resume works after app close.
    LaunchedEffect(currentVideo?.id, isPlaying) {
        if (currentVideo == null) return@LaunchedEffect
        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            if (isPlaying) {
                viewModel.saveProgress(currentPosition)
            }
            delay(5_000)
        }
    }

    LaunchedEffect(isFullscreen) {
        activity?.let {
            if (isFullscreen) {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                it.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

                val controller = WindowInsetsControllerCompat(it.window, it.window.decorView)
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                it.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

                val controller = WindowInsetsControllerCompat(it.window, it.window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(activity) {
        val act = activity
        if (act == null) return@DisposableEffect onDispose { }

        val window = act.window
        val previousStatusBarColor = window.statusBarColor
        val previousNavigationBarColor = window.navigationBarColor
        val previousController = WindowInsetsControllerCompat(window, act.window.decorView)
        val previousLightStatusBars = previousController.isAppearanceLightStatusBars
        val previousLightNavigationBars = previousController.isAppearanceLightNavigationBars

        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        val controller = WindowInsetsControllerCompat(window, act.window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false

        // Hide system bars completely in player
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            window.statusBarColor = previousStatusBarColor
            window.navigationBarColor = previousNavigationBarColor
            val restoreController = WindowInsetsControllerCompat(window, act.window.decorView)
            restoreController.isAppearanceLightStatusBars = previousLightStatusBars
            restoreController.isAppearanceLightNavigationBars = previousLightNavigationBars
        }
    }

    LaunchedEffect(activity) {
        val act = activity ?: return@LaunchedEffect
        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            val pip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) act.isInPictureInPictureMode else false
            if (pip != isInPip) {
                isInPip = pip
                if (pip) {
                    showControls = false
                    showBottomSheet = false
                    showQualitySheet = false
                    showPlaybackSettingsSheet = false
                }
            }
            delay(500)
        }
    }

    BackHandler(enabled = isFullscreen) { isFullscreen = false }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Crossfade(
            targetState = currentVideo != null,
            animationSpec = tween(durationMillis = 220),
            modifier = Modifier.fillMaxSize(),
        ) { hasVideo ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (hasVideo) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .then(if (isFullscreen) Modifier.fillMaxHeight() else Modifier.aspectRatio(16f / 9f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        VideoPlayerView(
                            player = playerConnectionManager.player,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .pointerInput(isInPip, showControls) {
                                        if (isInPip) return@pointerInput

                                        detectTapGestures(
                                            onTap = {
                                                if (showControls) {
                                                    showControls = false
                                                    controlsHideJob?.cancel()
                                                } else {
                                                    resetControlsTimer()
                                                }
                                            },
                                            onDoubleTap = { offset ->
                                                val w = size.width.toFloat().coerceAtLeast(1f)
                                                when {
                                                    offset.x < w * 0.35f -> {
                                                        playerConnectionManager.seekBack(10_000L)
                                                    }
                                                    offset.x > w * 0.65f -> {
                                                        playerConnectionManager.seekForward(10_000L)
                                                    }
                                                    else -> {
                                                        playerConnectionManager.togglePlayback()
                                                    }
                                                }
                                                resetControlsTimer()
                                            },
                                        )
                                    }
                                    .pointerInput(isInPip) {
                                        if (isInPip) return@pointerInput

                                        detectDragGestures(
                                            onDragStart = { startOffset ->
                                                gestureStart = startOffset

                                                val act = activity
                                                startBrightness = act?.window?.attributes?.screenBrightness
                                                if (startBrightness == null || startBrightness!! < 0f) startBrightness = 0.5f

                                                startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()

                                                val w = size.width.toFloat().coerceAtLeast(1f)
                                                val h = size.height.toFloat().coerceAtLeast(1f)
                                                val isLeft = gestureStart.x < w / 2f

                                                // Allow both up and down drag
                                                val totalDragY = change.position.y - gestureStart.y
                                                val delta = -(totalDragY / h) * 2.0f

                                                if (isLeft) {
                                                    val act = activity ?: return@detectDragGestures
                                                    val base = startBrightness ?: 0.5f
                                                    val next = (base + delta).coerceIn(0.05f, 1.0f)
                                                    val lp = act.window.attributes
                                                    lp.screenBrightness = next
                                                    act.window.attributes = lp

                                                    showBrightnessHUD = true
                                                    hudText = "${(next * 100).toInt()}%"
                                                    showVolumeHUD = false
                                                } else {
                                                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                                    val base = startVolume ?: audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                                    val step = (delta * maxVol).toInt()
                                                    val next = (base + step).coerceIn(0, maxVol)
                                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, next, AudioManager.FLAG_SHOW_UI)

                                                    showVolumeHUD = true
                                                    hudText = "$next/$maxVol"
                                                    showBrightnessHUD = false
                                                }
                                            },
                                            onDragEnd = {
                                                showBrightnessHUD = false
                                                showVolumeHUD = false
                                            },
                                        )
                                    },
                            resizeMode = if (isFullscreen) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT,
                            onTap = { },
                        )
                    }

                    AnimatedVisibility(
                        visible = showBrightnessHUD || showVolumeHUD,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                hudText,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                } else {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(Color.Black),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(48.dp),
                            )
                            Text(
                                "Loading video...",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !isInPip,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            val uiTitle =
                currentMediaMeta?.title?.toString()
                    ?: currentMediaMeta?.extras?.getString("title")
                    ?: currentVideo?.title
                    ?: ""
            val uiUploader =
                currentMediaMeta?.artist?.toString()
                    ?: currentMediaMeta?.extras?.getString("uploaderName")
                    ?: currentVideo?.channelName
                    ?: ""
            PlayerControlsOverlay(
                title = uiTitle,
                uploaderName = uiUploader,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                duration = duration,
                currentPosition = currentPosition,
                isFullscreen = isFullscreen,
                onQualityClick = {
                    showQualitySheet = true
                    resetControlsTimer()
                },
                onSettingsClick = {
                    showPlaybackSettingsSheet = true
                    resetControlsTimer()
                },
                onRewind = {
                    playerConnectionManager.seekBack()
                    resetControlsTimer()
                },
                onForward = {
                    playerConnectionManager.seekForward()
                    resetControlsTimer()
                },
                onPlayPauseClick = {
                    playerConnectionManager.togglePlayback()
                    resetControlsTimer()
                },
                onSeek = { pos ->
                    playerConnectionManager.seekTo(pos)
                    resetControlsTimer()
                },
                onFullscreenToggle = {
                    isFullscreen = !isFullscreen
                    resetControlsTimer()
                },
                onPipClick = {
                    val act = activity ?: return@PlayerControlsOverlay
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val params = PictureInPictureParams.Builder().build()
                        act.enterPictureInPictureMode(params)
                    }
                    resetControlsTimer()
                },
                onBackClick = {
                    if (isFullscreen) isFullscreen = false else navController.popBackStack()
                },
                onShowQueue = {
                    selectedTab = 0
                    showBottomSheet = true
                    resetControlsTimer()
                },
                onShowChapters = {
                    selectedTab = 1
                    showBottomSheet = true
                    resetControlsTimer()
                },
                onShowInfo = {
                    selectedTab = 2
                    showBottomSheet = true
                    resetControlsTimer()
                },
                hasChapters = chapters.isNotEmpty(),
                hasQueue = queue.size > 1,
                modifier = Modifier.fillMaxSize(),
            )
        }

        val availableQualities by viewModel.availableQualities.collectAsStateWithLifecycle()
        val availableAudioQualities by viewModel.availableAudioQualities.collectAsStateWithLifecycle()

        PlayerQualitySheet(
            show = showQualitySheet,
            onDismiss = { showQualitySheet = false },
            availableQualities = availableQualities,
            availableAudioQualities = availableAudioQualities,
            isAudioOnly = false,
            currentMaxHeight = currentMaxHeight,
            onSelectMaxHeight = { height ->
                currentMaxHeight = height
                val args =
                    android.os.Bundle().apply {
                        putInt(PlayerService.CMD_KEY_MAX_VIDEO_HEIGHT, height)
                    }
                playerConnectionManager.sendCustomCommand(PlayerService.CMD_SET_MAX_VIDEO_HEIGHT, args)
            },
        )

        PlaybackSettingsSheet(
            show = showPlaybackSettingsSheet,
            onDismiss = { showPlaybackSettingsSheet = false },
            dataSaver = sessionDataSaver,
            currentMaxHeight = currentMaxHeight,
            currentCodec = currentCodec,
            captionsEnabled = captionsEnabled,
            selectedCaptionLanguage = selectedCaptionLanguage,
            availableCaptionLanguages = availableCaptionLanguages,
            onCaptionsEnabledChanged = { enabled ->
                captionsEnabled = enabled
                playerConnectionManager.setCaptionsEnabled(enabled)
            },
            onCaptionLanguageSelected = { lang ->
                selectedCaptionLanguage = lang
                playerConnectionManager.setPreferredCaptionLanguage(lang)
            },
            selectedAudioLanguage = selectedAudioLanguage,
            availableAudioLanguages = availableAudioLanguages,
            onAudioLanguageSelected = { lang ->
                selectedAudioLanguage = lang
                playerConnectionManager.setPreferredAudioLanguage(lang)
                playerConnectionManager.setPreferredAudioLanguageForStream(lang)
            },
            onDataSaverChanged = { enabled ->
                sessionDataSaver = enabled
                val args =
                    android.os.Bundle().apply {
                        putBoolean(PlayerService.CMD_KEY_DATA_SAVER, enabled)
                    }
                playerConnectionManager.sendCustomCommand(PlayerService.CMD_SET_DATA_SAVER, args)
            },
            onQualityClick = {
                showQualitySheet = true
                showPlaybackSettingsSheet = false
            },
            onAudioClick = {
                showAudioSheet = true
                showPlaybackSettingsSheet = false
            },
            onCodecSelected = { codec ->
                currentCodec = codec
                val raw =
                    when (codec) {
                        CodecChoice.AUTO -> "AUTO"
                        CodecChoice.AV1 -> "AV1"
                        CodecChoice.VP9 -> "VP9"
                        CodecChoice.H264 -> "H264"
                    }
                val args =
                    android.os.Bundle().apply {
                        putString(PlayerService.CMD_KEY_PREFERRED_CODEC, raw)
                    }
                playerConnectionManager.sendCustomCommand(PlayerService.CMD_SET_PREFERRED_CODEC, args)
            },
            playbackSpeed = actualPlaybackSpeed,
            onPlaybackSpeedChanged = { speed ->
                playerConnectionManager.setPlaybackSpeed(speed)
            },
            streamInfo = streamInfo,
            audioTrackGroups = audioTrackGroups,
            captionTrackGroups = captionTrackGroups,
            onSelectAudioTrack = { groupIndex, trackIndex ->
                playerConnectionManager.selectAudioTrack(groupIndex, trackIndex)
            },
            onSelectCaptionTrack = { groupIndex, trackIndex ->
                playerConnectionManager.selectCaptionTrack(groupIndex, trackIndex)
            },
        )

        PlayerAudioSheet(
            show = showAudioSheet,
            onDismiss = { showAudioSheet = false },
            selectedAudioLanguage = selectedAudioLanguage,
            selectedAudioStreamSignature = selectedAudioStreamSignature,
            availableAudioLanguages = availableAudioLanguages,
            audioTrackGroups = audioTrackGroups,
            streamInfo = streamInfo,
            onAudioLanguageSelected = { lang ->
                selectedAudioLanguage = lang
                selectedAudioStreamSignature = null
                playerConnectionManager.setPreferredAudioLanguage(lang)
                playerConnectionManager.setPreferredAudioLanguageForStream(lang)
            },
            onAudioStreamSignatureSelected = { language, codec, bitrate ->
                // This applies to extractor-based streaming audio variants (bitrate/codec)
                selectedAudioStreamSignature = Triple(language, codec, bitrate)
                selectedAudioLanguage = null
                playerConnectionManager.setAudioStreamSignature(language, codec, bitrate)
            },
            onSelectAudioTrack = { groupIndex, trackIndex ->
                playerConnectionManager.selectAudioTrack(groupIndex, trackIndex)
            },
        )

        PlayerQueueBottomSheet(
            show = showBottomSheet,
            onDismiss = { showBottomSheet = false },
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            queue = sessionQueue,
            currentIndex = sessionQueueIndex,
            onQueueItemClick = { index ->
                val item = sessionQueue.getOrNull(index) ?: return@PlayerQueueBottomSheet
                viewModel.playQueueItem(index)
                playerConnectionManager.play(
                    videoId = item.videoId,
                    title = item.title,
                    thumbnailUrl = item.thumbnailUrl,
                    duration = item.duration,
                    uploaderName = item.uploaderName,
                    queue = sessionQueue,
                    queuePosition = index,
                )
                showBottomSheet = false
            },
            chapters = chapters,
            streamInfo = streamInfo,
            currentPositionMs = currentPosition,
            onChapterClick = { chapter ->
                playerConnectionManager.seekTo(chapter.startTimeSeconds * 1000L)
                showBottomSheet = false
            },
        )
    }
}

internal fun Video.toQueueItem(): QueueItem {
    return QueueItem(
        videoId = this.id,
        title = this.title,
        thumbnailUrl = this.thumbnail,
        duration = this.duration.toLong() * 1000L,
        uploaderName = this.channelName,
    )
}
