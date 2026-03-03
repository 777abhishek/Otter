package com.Otter.app.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents an audio track with selection info
 */
data class AudioTrackGroup(
    val groupIndex: Int,
    val trackIndex: Int,
    val languageTag: String?,
    val label: String,
    val isSelected: Boolean,
)

/**
 * Represents a caption track with selection info
 */
data class CaptionTrackGroup(
    val groupIndex: Int,
    val trackIndex: Int,
    val languageTag: String?,
    val label: String,
    val isSelected: Boolean,
)

/**
 * Manager for connecting to and controlling the PlayerService.
 * Provides a high-level API for the UI to interact with playback.
 */
@Singleton
class PlayerConnectionManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private var mediaController: MediaController? = null
        private var controllerFuture: ListenableFuture<MediaController>? = null

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private var positionJob: Job? = null

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _currentPosition = MutableStateFlow(0L)
        val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

        private val _duration = MutableStateFlow(0L)
        val duration: StateFlow<Long> = _duration.asStateFlow()

        private val _isPlaying = MutableStateFlow(false)
        val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

        private val _isBuffering = MutableStateFlow(false)
        val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

        private val _bufferedPosition = MutableStateFlow(0L)
        val bufferedPosition: StateFlow<Long> = _bufferedPosition.asStateFlow()

        private val _playbackState = MutableStateFlow<Int?>(null)
        val playbackState: StateFlow<Int?> = _playbackState.asStateFlow()

        private val _playbackSpeed = MutableStateFlow(1f)
        val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

        private val _currentVideoHeight = MutableStateFlow(Int.MAX_VALUE)
        val currentVideoHeight: StateFlow<Int> = _currentVideoHeight.asStateFlow()

        private val _currentMediaItem = MutableStateFlow<androidx.media3.common.MediaMetadata?>(null)
        val currentMediaItem: StateFlow<androidx.media3.common.MediaMetadata?> = _currentMediaItem.asStateFlow()

        private val _isAudioOnlySession = MutableStateFlow(false)
        val isAudioOnlySession: StateFlow<Boolean> = _isAudioOnlySession.asStateFlow()

        private val _tracks = MutableStateFlow<Tracks?>(null)
        val tracks: StateFlow<Tracks?> = _tracks.asStateFlow()

        private val _availableAudioLanguages = MutableStateFlow<List<String>>(emptyList())
        val availableAudioLanguages: StateFlow<List<String>> = _availableAudioLanguages.asStateFlow()

        private val _availableCaptionLanguages = MutableStateFlow<List<String>>(emptyList())
        val availableCaptionLanguages: StateFlow<List<String>> = _availableCaptionLanguages.asStateFlow()

        // Expose track groups with display names for UI selection
        private val _audioTrackGroups = MutableStateFlow<List<AudioTrackGroup>>(emptyList())
        val audioTrackGroups: StateFlow<List<AudioTrackGroup>> = _audioTrackGroups.asStateFlow()

        private val _captionTrackGroups = MutableStateFlow<List<CaptionTrackGroup>>(emptyList())
        val captionTrackGroups: StateFlow<List<CaptionTrackGroup>> = _captionTrackGroups.asStateFlow()

        private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
        val queue: StateFlow<List<QueueItem>> = _queue.asStateFlow()

        private val _queueIndex = MutableStateFlow(0)
        val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

        val player: Player?
            get() = mediaController

        fun sendCustomCommand(
            action: String,
            args: android.os.Bundle = android.os.Bundle.EMPTY,
        ) {
            val controller = mediaController ?: return
            controller.sendCustomCommand(SessionCommand(action, android.os.Bundle.EMPTY), args)
        }

        /**
         * Initialize connection to the PlayerService
         */
        @OptIn(UnstableApi::class)
        fun connect() {
            if (controllerFuture != null) return

            val sessionToken = SessionToken(context, ComponentName(context, PlayerService::class.java))
            controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

            controllerFuture?.addListener({
                try {
                    mediaController = controllerFuture?.get()
                    _isConnected.value = true
                    setupMediaControllerListener()
                    startPositionPolling()
                } catch (e: Exception) {
                    _isConnected.value = false
                }
            }, MoreExecutors.directExecutor())
        }

        /**
         * Disconnect from the PlayerService
         */
        fun disconnect() {
            releaseMediaController()
        }

        private fun startPositionPolling() {
            positionJob?.cancel()
            positionJob =
                scope.launch {
                    while (isActive) {
                        updatePosition()
                        delay(500)
                    }
                }
        }

        private var reconnectionAttempts = 0
        private var reconnectionJob: Job? = null

        private fun setupMediaControllerListener() {
            mediaController?.addListener(
                object : androidx.media3.common.Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        _isBuffering.value = playbackState == androidx.media3.common.Player.STATE_BUFFERING
                        _playbackState.value = playbackState
                        updatePosition()
                        
                        // Reset reconnection counter on successful playback
                        if (playbackState == androidx.media3.common.Player.STATE_READY) {
                            reconnectionAttempts = 0
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (isPlaying) {
                            reconnectionAttempts = 0
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        // Auto-reconnect on certain errors
                        handlePlayerError(error)
                    }

                    override fun onPositionDiscontinuity(
                        oldPosition: androidx.media3.common.Player.PositionInfo,
                        newPosition: androidx.media3.common.Player.PositionInfo,
                        reason: Int,
                    ) {
                        updatePosition()
                    }

                    override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                        _currentMediaItem.value = mediaMetadata
                        _isAudioOnlySession.value = mediaMetadata.extras?.getBoolean("audioOnly", false) == true
                        val extras = mediaMetadata.extras
                        if (extras != null) {
                            val nextIndex = extras.getInt("queueIndex", _queueIndex.value)
                            if (nextIndex != _queueIndex.value) {
                                _queueIndex.value = nextIndex
                            }

                            // If the service is driving playback (auto-next / notification next/prev),
                            // the UI-side queue may be stale or empty. Ensure it's at least consistent
                            // enough for the queue sheet highlight.
                            val size = extras.getInt("queueSize", _queue.value.size)
                            if (size > 0 && _queue.value.size != size) {
                                val currentVideoId = extras.getString("videoId")
                                _queue.value =
                                    List(size) { idx ->
                                        _queue.value.getOrNull(idx) ?: QueueItem(
                                            videoId = if (idx == nextIndex) (currentVideoId ?: "") else "",
                                            title = "",
                                            thumbnailUrl = "",
                                            duration = 0L,
                                            uploaderName = "",
                                        )
                                    }
                            }
                        }
                        updatePosition()
                    }

                    @OptIn(UnstableApi::class)
                    override fun onTracksChanged(tracks: Tracks) {
                        _tracks.value = tracks

                        val audio = linkedSetOf<String>()
                        val text = linkedSetOf<String>()
                        val audioGroups = mutableListOf<AudioTrackGroup>()
                        val captionGroups = mutableListOf<CaptionTrackGroup>()

                        tracks.groups.forEachIndexed { groupIndex, group ->
                            if (group.type == C.TRACK_TYPE_AUDIO || group.type == C.TRACK_TYPE_TEXT) {
                                val trackGroup = group.mediaTrackGroup
                                for (i in 0 until trackGroup.length) {
                                    val format = trackGroup.getFormat(i)
                                    val languageTag = format.language?.takeIf { it.isNotBlank() }
                                    val label = format.label?.toString() ?: languageTag ?: "Track ${i + 1}"

                                    if (group.type == C.TRACK_TYPE_AUDIO) {
                                        if (languageTag != null) audio.add(languageTag)
                                        audioGroups.add(
                                            AudioTrackGroup(
                                                groupIndex = groupIndex,
                                                trackIndex = i,
                                                languageTag = languageTag,
                                                label = label,
                                                isSelected = group.isTrackSelected(i),
                                            ),
                                        )
                                    } else if (group.type == C.TRACK_TYPE_TEXT) {
                                        if (languageTag != null) text.add(languageTag)
                                        captionGroups.add(
                                            CaptionTrackGroup(
                                                groupIndex = groupIndex,
                                                trackIndex = i,
                                                languageTag = languageTag,
                                                label = label,
                                                isSelected = group.isTrackSelected(i),
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                        _availableAudioLanguages.value = audio.toList()
                        _availableCaptionLanguages.value = text.toList()
                        _audioTrackGroups.value = audioGroups
                        _captionTrackGroups.value = captionGroups
                    }

                    override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                        _playbackSpeed.value = playbackParameters.speed
                    }

                    override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                        _currentVideoHeight.value = videoSize.height.takeIf { it > 0 } ?: Int.MAX_VALUE
                    }
                },
            )
        }

        fun setPreferredAudioLanguage(languageTag: String?) {
            val controller = mediaController ?: return
            val params = controller.trackSelectionParameters
            val builder = params.buildUpon()
            if (languageTag.isNullOrBlank()) {
                builder.setPreferredAudioLanguage(null)
            } else {
                builder.setPreferredAudioLanguage(languageTag)
            }
            controller.trackSelectionParameters = builder.build()
        }

        fun setPreferredAudioLanguageForStream(languageTag: String?) {
            val args =
                android.os.Bundle().apply {
                    putString(PlayerService.CMD_KEY_PREFERRED_AUDIO_LANGUAGE, languageTag)
                }
            sendCustomCommand(PlayerService.CMD_SET_PREFERRED_AUDIO_LANGUAGE, args)
        }

        fun setAudioStreamSignature(
            language: String?,
            codec: String?,
            bitrate: Int?,
        ) {
            val args =
                android.os.Bundle().apply {
                    putString(PlayerService.CMD_KEY_AUDIO_STREAM_LANGUAGE, language)
                    putString(PlayerService.CMD_KEY_AUDIO_STREAM_CODEC, codec)
                    putInt(PlayerService.CMD_KEY_AUDIO_STREAM_BITRATE, bitrate ?: -1)
                }
            sendCustomCommand(PlayerService.CMD_SET_AUDIO_STREAM_INDEX, args)
        }

        /**
         * Select a specific audio track by group and track index using track override.
         * This is more reliable than language preference for multi-track streams.
         */
        @OptIn(UnstableApi::class)
        fun selectAudioTrack(
            groupIndex: Int,
            trackIndex: Int,
        ) {
            val controller = mediaController ?: return
            val tracks = controller.currentTracks
            val audioGroup = tracks.groups.getOrNull(groupIndex)
            if (audioGroup == null || audioGroup.type != C.TRACK_TYPE_AUDIO) return

            val trackGroup = audioGroup.mediaTrackGroup
            val trackSelectionParams =
                controller.trackSelectionParameters
                    .buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                    .setOverrideForType(
                        androidx.media3.common.TrackSelectionOverride(
                            trackGroup,
                            listOf(trackIndex),
                        ),
                    )
                    .build()
            controller.trackSelectionParameters = trackSelectionParams
        }

        /**
         * Select a specific caption track by group and track index using track override.
         */
        @OptIn(UnstableApi::class)
        fun selectCaptionTrack(
            groupIndex: Int,
            trackIndex: Int,
        ) {
            val controller = mediaController ?: return
            val tracks = controller.currentTracks
            val textGroup = tracks.groups.getOrNull(groupIndex)
            if (textGroup == null || textGroup.type != C.TRACK_TYPE_TEXT) return

            val trackGroup = textGroup.mediaTrackGroup
            val trackSelectionParams =
                controller.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    .setOverrideForType(
                        androidx.media3.common.TrackSelectionOverride(
                            trackGroup,
                            listOf(trackIndex),
                        ),
                    )
                    .build()
            controller.trackSelectionParameters = trackSelectionParams
        }

        fun setCaptionsEnabled(enabled: Boolean) {
            val controller = mediaController ?: return
            val params = controller.trackSelectionParameters
            val builder = params.buildUpon()
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
            controller.trackSelectionParameters = builder.build()
        }

        fun setPreferredCaptionLanguage(languageTag: String?) {
            val controller = mediaController ?: return
            val params = controller.trackSelectionParameters
            val builder = params.buildUpon()
            if (languageTag.isNullOrBlank()) {
                builder.setPreferredTextLanguage(null)
            } else {
                builder.setPreferredTextLanguage(languageTag)
            }
            controller.trackSelectionParameters = builder.build()
        }

        private fun updatePosition() {
            mediaController?.let { controller ->
                _currentPosition.value = controller.currentPosition.coerceAtLeast(0)
                _duration.value = controller.duration.coerceAtLeast(0)
                _bufferedPosition.value = controller.bufferedPosition.coerceAtLeast(0)
            }
        }

        private fun handlePlayerError(error: androidx.media3.common.PlaybackException) {
            val maxRetries = 3
            if (reconnectionAttempts < maxRetries) {
                reconnectionAttempts++
                reconnectionJob?.cancel()
                reconnectionJob = scope.launch {
                    delay(1000L * reconnectionAttempts) // Progressive delay
                    mediaController?.let { controller ->
                        val position = _currentPosition.value
                        controller.prepare()
                        controller.seekTo(position)
                        if (_isPlaying.value || controller.playWhenReady) {
                            controller.play()
                        }
                    }
                }
            }
        }

        fun getBufferedPercentage(): Int {
            val duration = _duration.value
            if (duration <= 0) return 0
            return ((_bufferedPosition.value.toFloat() / duration) * 100).toInt().coerceIn(0, 100)
        }

        private fun releaseMediaController() {
            positionJob?.cancel()
            positionJob = null
            mediaController?.release()
            mediaController = null
            controllerFuture?.let {
                MediaController.releaseFuture(it)
            }
            controllerFuture = null
            _isConnected.value = false
        }

        /**
         * Play a video with the specified parameters
         */
        fun play(
            videoId: String,
            title: String,
            thumbnailUrl: String,
            duration: Long,
            uploaderName: String,
            audioOnly: Boolean? = null,
            startPosition: Long = 0,
            queue: List<QueueItem> = emptyList(),
            queuePosition: Int = 0,
        ) {
            if (queue.isNotEmpty()) {
                _queue.value = queue
                _queueIndex.value = queuePosition
            }
            val intent =
                Intent(context, PlayerService::class.java).apply {
                    action = PlayerService.ACTION_PLAY
                    putExtra(PlayerService.EXTRA_VIDEO_ID, videoId)
                    putExtra(PlayerService.EXTRA_VIDEO_URL, "https://www.youtube.com/watch?v=$videoId")
                    putExtra(PlayerService.EXTRA_TITLE, title)
                    putExtra(PlayerService.EXTRA_THUMBNAIL, thumbnailUrl)
                    putExtra(PlayerService.EXTRA_DURATION, duration)
                    putExtra(PlayerService.EXTRA_UPLOADER, uploaderName)
                    putExtra(PlayerService.EXTRA_START_POSITION, startPosition)
                    if (audioOnly != null) {
                        putExtra(PlayerService.EXTRA_AUDIO_ONLY, audioOnly)
                    }
                    if (queue.isNotEmpty()) {
                        putExtra(PlayerService.EXTRA_QUEUE_POSITION, queuePosition)
                        putParcelableArrayListExtra(PlayerService.EXTRA_QUEUE, ArrayList(queue))
                    }
                }
            ContextCompat.startForegroundService(context, intent)
        }

        /**
         * Pause playback
         */
        fun pause() {
            val controller = mediaController
            if (controller != null) {
                controller.pause()
                return
            }
            val intent =
                Intent(context, PlayerService::class.java).apply {
                    action = PlayerService.ACTION_PAUSE
                }
            context.startService(intent)
        }

        /**
         * Resume playback
         */
        fun resume() {
            val controller = mediaController
            if (controller != null) {
                controller.play()
                return
            }
            val intent =
                Intent(context, PlayerService::class.java).apply {
                    action = PlayerService.ACTION_PLAY
                }
            context.startService(intent)
        }

        /**
         * Toggle play/pause
         */
        fun togglePlayback() {
            val controller = mediaController
            if (controller != null) {
                if (controller.isPlaying) controller.pause() else controller.play()
                return
            }
            val intent =
                Intent(context, PlayerService::class.java).apply {
                    action = PlayerService.ACTION_TOGGLE_PLAYBACK
                }
            context.startService(intent)
        }

        /**
         * Play next item in queue
         */
        fun playNext() {
            val intent =
                Intent(context, PlayerService::class.java).apply {
                    action = PlayerService.ACTION_NEXT
                }
            context.startService(intent)
        }

        /**
         * Play previous item in queue
         */
        fun playPrevious() {
            val intent =
                Intent(context, PlayerService::class.java).apply {
                    action = PlayerService.ACTION_PREVIOUS
                }
            context.startService(intent)
        }

        /**
         * Stop playback
         */
        fun stop() {
            val intent =
                Intent(context, PlayerService::class.java).apply {
                    action = PlayerService.ACTION_STOP
                }
            context.startService(intent)
        }

        /**
         * Seek to position in milliseconds
         */
        fun seekTo(positionMs: Long) {
            val controller = mediaController ?: return
            val wasPlaying = _isPlaying.value
            controller.seekTo(positionMs)
            if (wasPlaying) {
                controller.play()
            }
        }

        fun seekBack(ms: Long = 10_000L) {
            val controller = mediaController ?: return
            val wasPlaying = _isPlaying.value
            controller.seekTo((controller.currentPosition - ms).coerceAtLeast(0L))
            if (wasPlaying) {
                controller.play()
            }
        }

        fun seekForward(ms: Long = 10_000L) {
            val controller = mediaController ?: return
            val wasPlaying = _isPlaying.value
            val duration = controller.duration
            val target = controller.currentPosition + ms
            if (duration > 0) controller.seekTo(target.coerceAtMost(duration)) else controller.seekTo(target)
            if (wasPlaying) {
                controller.play()
            }
        }

        /**
         * Format duration in milliseconds to mm:ss or hh:mm:ss
         */
        fun formatDuration(durationMs: Long): String {
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

        /**
         * Set audio-only mode
         */
        fun setAudioOnly(audioOnly: Boolean) {
            val args =
                android.os.Bundle().apply {
                    putBoolean(PlayerService.CMD_KEY_AUDIO_ONLY, audioOnly)
                }
            sendCustomCommand(PlayerService.CMD_SET_AUDIO_ONLY, args)
        }

        /**
         * Set max video height for quality preference
         */
        fun setMaxVideoHeight(height: Int) {
            val args =
                android.os.Bundle().apply {
                    putInt(PlayerService.CMD_KEY_MAX_VIDEO_HEIGHT, height)
                }
            sendCustomCommand(PlayerService.CMD_SET_MAX_VIDEO_HEIGHT, args)
        }

        /**
         * Set data saver mode
         */
        fun setDataSaver(dataSaver: Boolean) {
            val args =
                android.os.Bundle().apply {
                    putBoolean(PlayerService.CMD_KEY_DATA_SAVER, dataSaver)
                }
            sendCustomCommand(PlayerService.CMD_SET_DATA_SAVER, args)
        }

        /**
         * Set preferred codec
         */
        fun setPreferredCodec(codec: String) {
            val args =
                android.os.Bundle().apply {
                    putString(PlayerService.CMD_KEY_PREFERRED_CODEC, codec)
                }
            sendCustomCommand(PlayerService.CMD_SET_PREFERRED_CODEC, args)
        }

        /**
         * Set playback speed
         */
        fun setPlaybackSpeed(speed: Float) {
            val args =
                android.os.Bundle().apply {
                    putFloat(PlayerService.CMD_KEY_PLAYBACK_SPEED, speed)
                }
            sendCustomCommand(PlayerService.CMD_SET_PLAYBACK_SPEED, args)
        }
    }

// Extension for ArrayList extra
fun <T : android.os.Parcelable> Intent.putParcelableArrayListExtra(
    key: String,
    value: ArrayList<T>?,
) {
    putExtra(key, value)
}
