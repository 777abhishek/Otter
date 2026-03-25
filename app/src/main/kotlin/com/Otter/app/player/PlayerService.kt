package com.Otter.app.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ControllerInfo
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.ui.PlayerNotificationManager
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.Otter.app.MainActivity
import com.Otter.app.data.models.AppSettings
import com.Otter.app.data.models.PreferredCodec
import com.Otter.app.data.models.SponsorBlockCategory
import com.Otter.app.data.models.StreamingQuality
import com.Otter.app.data.repositories.StreamRepository
import com.Otter.app.data.repositories.VideoStreamInfo
import com.Otter.app.service.SettingsService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject

/**
 * Data class representing a queue item for playback
 */
@kotlinx.parcelize.Parcelize
data class QueueItem(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val duration: Long,
    val uploaderName: String,
) : android.os.Parcelable

/**
 * Playback state for UI observation
 */
sealed class PlaybackState {
    object Idle : PlaybackState()

    object Buffering : PlaybackState()

    data class Playing(
        val position: Long,
        val duration: Long,
        val bufferedPosition: Long,
    ) : PlaybackState()

    data class Paused(
        val position: Long,
        val duration: Long,
    ) : PlaybackState()

    data class Error(val message: String) : PlaybackState()
}

@AndroidEntryPoint
class PlayerService : MediaSessionService() {
    private val logTag = "PlayerService"

    companion object {
        const val ACTION_PLAY = "com.Otter.app.player.ACTION_PLAY"
        const val ACTION_PAUSE = "com.Otter.app.player.ACTION_PAUSE"
        const val ACTION_NEXT = "com.Otter.app.player.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.Otter.app.player.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.Otter.app.player.ACTION_STOP"
        const val ACTION_TOGGLE_PLAYBACK = "com.Otter.app.player.ACTION_TOGGLE_PLAYBACK"

        const val EXTRA_VIDEO_ID = "extra_video_id"
        const val EXTRA_VIDEO_URL = "extra_video_url"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_THUMBNAIL = "extra_thumbnail"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_UPLOADER = "extra_uploader"
        const val EXTRA_START_POSITION = "extra_start_position"
        const val EXTRA_QUEUE = "extra_queue"
        const val EXTRA_QUEUE_POSITION = "extra_queue_position"
        const val EXTRA_AUDIO_ONLY = "extra_audio_only"

        const val NOTIFICATION_ID = 1201
        const val CHANNEL_ID = "Otter_playback_channel"

        private const val PLAYBACK_UPDATE_INTERVAL_MS = 1000L

        const val CMD_SET_MAX_VIDEO_HEIGHT = "com.Otter.app.player.SET_MAX_VIDEO_HEIGHT"
        const val CMD_KEY_MAX_VIDEO_HEIGHT = "max_video_height"

        const val CMD_SET_AUDIO_ONLY = "com.Otter.app.player.SET_AUDIO_ONLY"
        const val CMD_KEY_AUDIO_ONLY = "audio_only"

        const val CMD_SET_DATA_SAVER = "com.Otter.app.player.SET_DATA_SAVER"
        const val CMD_KEY_DATA_SAVER = "data_saver"

        const val CMD_SET_PREFERRED_CODEC = "com.Otter.app.player.SET_PREFERRED_CODEC"
        const val CMD_KEY_PREFERRED_CODEC = "preferred_codec"

        const val CMD_SET_PLAYBACK_SPEED = "com.Otter.app.player.SET_PLAYBACK_SPEED"
        const val CMD_KEY_PLAYBACK_SPEED = "playback_speed"

        const val CMD_SET_PREFERRED_AUDIO_LANGUAGE = "com.Otter.app.player.SET_PREFERRED_AUDIO_LANGUAGE"
        const val CMD_KEY_PREFERRED_AUDIO_LANGUAGE = "preferred_audio_language"

        const val CMD_SET_AUDIO_STREAM_INDEX = "com.Otter.app.player.SET_AUDIO_STREAM_INDEX"
        const val CMD_KEY_AUDIO_STREAM_INDEX = "audio_stream_index"
        const val CMD_KEY_AUDIO_STREAM_LANGUAGE = "audio_stream_language"
        const val CMD_KEY_AUDIO_STREAM_CODEC = "audio_stream_codec"
        const val CMD_KEY_AUDIO_STREAM_BITRATE = "audio_stream_bitrate"
    }

    private fun toUserFacingStreamingError(message: String): String {
        val m = message.lowercase()
        val vpnHint = " Try enabling a VPN, switching networks, or changing DNS."
        return when {
            m.contains("region") || m.contains("geo") || m.contains("not available in your region") ->
                message + vpnHint
            m.contains("recaptcha") || m.contains("captcha") ->
                message + vpnHint
            m.contains("connection reset") || m.contains("reset by peer") ->
                message + vpnHint
            m.contains("network error") || m.contains("timeout") || m.contains("unknownhost") ->
                message + " Try switching Wi‑Fi/mobile data."
            else -> message
        }
    }

    @Inject
    lateinit var streamRepository: StreamRepository

    @Inject
    lateinit var settingsService: SettingsService

    @Inject
    lateinit var playbackResumeStore: PlaybackResumeStore

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var queueAwarePlayer: Player
    private lateinit var mediaSession: MediaSession
    @UnstableApi
    private lateinit var trackSelector: DefaultTrackSelector

    private lateinit var upstreamDataSourceFactory: DefaultDataSource.Factory
    private lateinit var mediaDataSourceFactory: DefaultDataSource.Factory

    @UnstableApi
    private var simpleCache: SimpleCache? = null

    @UnstableApi
    private var playerNotificationManager: PlayerNotificationManager? = null

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentQueueItem = MutableStateFlow<QueueItem?>(null)
    val currentQueueItem: StateFlow<QueueItem?> = _currentQueueItem.asStateFlow()

    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    val queue: StateFlow<List<QueueItem>> = _queue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private var playbackPositionJob: Job? = null
    private var currentSettings: AppSettings = AppSettings()

    private var overrideMaxVideoHeight: Int? = null
    private var overrideAudioOnly: Boolean? = null
    private var overrideDataSaver: Boolean? = null
    private var overridePreferredCodec: com.Otter.app.data.models.PreferredCodec? = null
    private var overridePlaybackSpeed: Float? = null

    private var overridePreferredAudioLanguage: String? = null

    private data class AudioStreamSignature(
        val language: String?,
        val codec: String?,
        val bitrate: Int?,
    )

    private var selectedAudioStreamSignature: AudioStreamSignature? = null

    private var userPaused: Boolean = false

    private var autoPlayRetryJob: Job? = null
    private var bufferingTimeoutJob: Job? = null
    private var isSeeking: Boolean = false
    private var pendingSeekPosition: Long = -1L
    private var bufferingStartTime: Long = -1L
    private var preloadingNextItem: MediaSource? = null
    private var preloadNextJob: Job? = null

    private var currentPageUrl: String? = null

    private var lastSavedPosition: Long = 0L

    private data class SponsorBlockSegment(
        val startMs: Long,
        val endMs: Long,
        val category: String,
    )

    private val sponsorBlockClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private var sponsorBlockSegments: List<SponsorBlockSegment> = emptyList()
    private var sponsorBlockSegmentsForVideoId: String? = null
    private var sponsorBlockFetchJob: Job? = null
    private var lastSponsorBlockSkipToMs: Long = -1L

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        queueAwarePlayer = createQueueAwarePlayer()
        initializeMediaSession()
        createNotificationChannel()
        initializeNotificationManager()

        // Collect settings
        serviceScope.launch {
            settingsService.getSettings().collect { settings ->
                currentSettings = settings
                applySettings(settings)
            }
        }
    }

    @UnstableApi
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_PLAY -> {
                // Explicit per-request audio-only override (separate audio session vs video session).
                // If not provided, service falls back to settings streamingAudioOnly.
                if (intent.hasExtra(EXTRA_AUDIO_ONLY)) {
                    overrideAudioOnly = intent.getBooleanExtra(EXTRA_AUDIO_ONLY, false)
                } else {
                    overrideAudioOnly = null
                }

                val queueItems = intent.getParcelableArrayListExtra<QueueItem>(EXTRA_QUEUE)
                val queuePosition = intent.getIntExtra(EXTRA_QUEUE_POSITION, 0)
                val hasQueue = queueItems != null && queueItems.isNotEmpty()
                
                if (hasQueue) {
                    // Normalize queue durations to milliseconds (older callers stored seconds)
                    val normalizedQueue =
                        queueItems.map { item ->
                            val durationMs = if (item.duration in 1..(24 * 60 * 60)) item.duration * 1000 else item.duration
                            if (durationMs == item.duration) item else item.copy(duration = durationMs)
                        }
                    _queue.value = normalizedQueue
                    _currentQueueIndex.value = queuePosition.coerceIn(0, (queueItems.size - 1).coerceAtLeast(0))
                    _currentQueueItem.value = normalizedQueue.getOrNull(_currentQueueIndex.value)

                    Log.d(
                        logTag,
                        "ACTION_PLAY queueSize=${normalizedQueue.size} queuePosition=${_currentQueueIndex.value} videoId=${normalizedQueue.getOrNull(
                            _currentQueueIndex.value,
                        )?.videoId}",
                    )
                }

                val videoId = intent.getStringExtra(EXTRA_VIDEO_ID)
                val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
                val title = intent.getStringExtra(EXTRA_TITLE)
                val thumbnail = intent.getStringExtra(EXTRA_THUMBNAIL)
                val duration = intent.getLongExtra(EXTRA_DURATION, 0)
                val uploader = intent.getStringExtra(EXTRA_UPLOADER)
                val startPosition = intent.getLongExtra(EXTRA_START_POSITION, 0)

                // If no explicit video was provided, treat ACTION_PLAY as a resume.
                if (videoId.isNullOrBlank() || videoUrl.isNullOrBlank()) {
                    serviceScope.launch {
                        val resume = playbackResumeStore.state.firstOrNull()
                        if (resume != null && resume.videoId.isNotBlank()) {
                            overrideAudioOnly = resume.audioOnly
                            val resumeItem =
                                QueueItem(
                                    videoId = resume.videoId,
                                    title = resume.title.ifBlank { "Unknown" },
                                    thumbnailUrl = resume.thumbnailUrl,
                                    duration = resume.durationMs,
                                    uploaderName = resume.uploaderName,
                                )
                            _currentQueueItem.value = resumeItem
                            playCurrentQueueItem(startPositionMs = resume.positionMs)
                            if (resume.playWhenReady) {
                                userPaused = false
                                exoPlayer.playWhenReady = true
                            } else {
                                userPaused = true
                                exoPlayer.playWhenReady = false
                            }
                        } else {
                            play()
                        }
                    }
                } else if (!hasQueue) {
                    // Only set single-item queue if no queue was provided from intent
                    val queueItem =
                        QueueItem(
                            videoId = videoId,
                            title = title ?: "Unknown",
                            thumbnailUrl = thumbnail ?: "",
                            duration = duration,
                            uploaderName = uploader ?: "",
                        )
                    _currentQueueItem.value = queueItem
                    // Set queue to include current item so media controls recognize it exists
                    _queue.value = listOf(queueItem)
                    _currentQueueIndex.value = 0
                    Log.d(
                        logTag,
                        "ACTION_PLAY explicit videoId=$videoId startPosition=$startPosition queueSize=${_queue.value.size} queueIndex=${_currentQueueIndex.value}",
                    )
                    playCurrentQueueItem(startPositionMs = startPosition)
                } else {
                    // Queue was provided, currentQueueItem already set above
                    Log.d(
                        logTag,
                        "ACTION_PLAY with queue videoId=$videoId startPosition=$startPosition queueSize=${_queue.value.size} queueIndex=${_currentQueueIndex.value}",
                    )
                    playCurrentQueueItem(startPositionMs = startPosition)
                }
            }
            ACTION_PAUSE -> pause()
            ACTION_NEXT -> playNext()
            ACTION_PREVIOUS -> playPrevious()
            ACTION_STOP -> stop()
            ACTION_TOGGLE_PLAYBACK -> togglePlayback()
        }

        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    @UnstableApi
    override fun onDestroy() {
        super.onDestroy()
        savePlaybackPosition()
        playbackPositionJob?.cancel()
        serviceJob.cancel()
        mediaSession.release()
        playerNotificationManager?.setPlayer(null)
        playerNotificationManager = null
        exoPlayer.release()
        runCatching { simpleCache?.release() }
        simpleCache = null
    }

    @OptIn(UnstableApi::class)
    private fun initializeNotificationManager() {
        val contentIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val imageLoader = ImageLoader(this)

        playerNotificationManager =
            PlayerNotificationManager.Builder(this, NOTIFICATION_ID, CHANNEL_ID)
                .setMediaDescriptionAdapter(
                    object : PlayerNotificationManager.MediaDescriptionAdapter {
                        override fun getCurrentContentTitle(player: Player): CharSequence {
                            return _currentQueueItem.value?.title ?: "Otter"
                        }

                        override fun createCurrentContentIntent(player: Player): PendingIntent? {
                            return contentIntent
                        }

                        override fun getCurrentContentText(player: Player): CharSequence? {
                            return _currentQueueItem.value?.uploaderName
                        }

                        override fun getCurrentLargeIcon(
                            player: Player,
                            callback: PlayerNotificationManager.BitmapCallback,
                        ): Bitmap? {
                            val url = _currentQueueItem.value?.thumbnailUrl?.takeIf { it.isNotBlank() } ?: return null

                            serviceScope.launch(Dispatchers.IO) {
                                runCatching {
                                    val request =
                                        ImageRequest.Builder(this@PlayerService)
                                            .data(url)
                                            .size(256, 256)
                                            .build()
                                    val result = imageLoader.execute(request)
                                    val drawable = (result as? SuccessResult)?.image
                                    val bmp = (drawable as? BitmapDrawable)?.bitmap
                                    if (bmp != null) callback.onBitmap(bmp)
                                }
                            }

                            return null
                        }
                    },
                )
                .setNotificationListener(
                    object : PlayerNotificationManager.NotificationListener {
                        override fun onNotificationPosted(
                            notificationId: Int,
                            notification: Notification,
                            ongoing: Boolean,
                        ) {
                            if (ongoing) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    startForeground(
                                        notificationId,
                                        notification,
                                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                                    )
                                } else {
                                    startForeground(notificationId, notification)
                                }
                            } else {
                                stopForeground(false)
                            }
                        }

                        override fun onNotificationCancelled(
                            notificationId: Int,
                            dismissedByUser: Boolean,
                        ) {
                            stopForeground(true)
                            stopSelf()
                        }
                    },
                )
                .build()
                .apply {
                    setUseChronometer(false)
                    setMediaSessionToken(mediaSession.sessionCompatToken)
                    setUseNextAction(true)
                    setUsePreviousAction(true)
                    setPlayer(queueAwarePlayer)
                }
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        // Adaptive track selection for smooth quality switching
        val adaptiveTrackSelectionFactory = AdaptiveTrackSelection.Factory()
        
        trackSelector =
            DefaultTrackSelector(this, adaptiveTrackSelectionFactory).apply {
                setParameters(
                    buildUponParameters()
                        .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                        .setTunnelingEnabled(true)
                        .setAllowVideoMixedMimeTypeAdaptiveness(true)
                        .setAllowAudioMixedMimeTypeAdaptiveness(true)
                        .setPreferredTextLanguage(null) // Auto-select captions
                        .setSelectUndeterminedTextLanguage(true),
                )
            }

        // Configure LoadControl for smooth streaming with proper buffering
        val loadControl =
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs */ 25000,      // 25s minimum buffer for smooth playback
                    /* maxBufferMs */ 60000,      // 60s maximum buffer
                    /* bufferForPlaybackMs */ 1000,   // 1s to start playback quickly
                    /* bufferForPlaybackAfterRebufferMs */ 2000, // 2s after seek/rebuffer
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .setTargetBufferBytes(-1) // No byte limit, use time-based buffering
                .build()

        // Configure HTTP data source with retry and timeout for robust streaming
        val httpDataSourceFactory =
            DefaultHttpDataSource.Factory()
                .setUserAgent("Otter/1.0")
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(20000)
                .setAllowCrossProtocolRedirects(true)
                .setKeepPostFor302Redirects(false)

        upstreamDataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

        // Use a disk cache to reduce rebuffers and speed up audio/video start.
        // NOTE: Cache is best-effort; if it fails, we fall back to upstream.
        val cache = simpleCache ?: runCatching {
            val cacheDir = java.io.File(cacheDir, "media_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(512L * 1024L * 1024L) // 512MB
            SimpleCache(cacheDir, evictor)
        }.getOrNull().also { simpleCache = it }

        mediaDataSourceFactory =
            if (cache != null) {
                DefaultDataSource.Factory(
                    this,
                    CacheDataSource.Factory()
                        .setCache(cache)
                        .setUpstreamDataSourceFactory(upstreamDataSourceFactory)
                        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR),
                )
            } else {
                upstreamDataSourceFactory
            }

        // Renderers factory with hardware acceleration and offload support
        val renderersFactory =
            DefaultRenderersFactory(this).apply {
                setEnableDecoderFallback(true) // Fallback to software if hardware fails
            }

        exoPlayer =
            ExoPlayer.Builder(this)
                .setRenderersFactory(renderersFactory)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(
                    androidx.media3.exoplayer.source.DefaultMediaSourceFactory(mediaDataSourceFactory)
                        .setLiveTargetOffsetMs(5000) // Lower latency for live streams
                )
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    true,
                )
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK) // Keep CPU running during network streaming
                .build()
                .apply {
                    // Disable repeat mode to prevent auto-advancing
                    repeatMode = Player.REPEAT_MODE_OFF
                    
                    addListener(
                        object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                when (playbackState) {
                                    Player.STATE_BUFFERING -> {
                                        // Cancel any previous buffering timeout
                                        bufferingTimeoutJob?.cancel()
                                        bufferingStartTime = System.currentTimeMillis()
                                        _playbackState.value = PlaybackState.Buffering
                                        
                                        // Set up buffering timeout - if buffering takes too long, try to recover
                                        bufferingTimeoutJob = serviceScope.launch {
                                            delay(15000) // 15 second timeout
                                            if (exoPlayer.playbackState == Player.STATE_BUFFERING && !userPaused) {
                                                Log.w(logTag, "Buffering timeout, attempting recovery")
                                                runCatching { reloadCurrentItemAtCurrentPosition() }
                                            }
                                        }
                                    }
                                    Player.STATE_READY -> {
                                        // Cancel buffering timeout and retry jobs
                                        bufferingTimeoutJob?.cancel()
                                        bufferingStartTime = -1L
                                        autoPlayRetryJob?.cancel()
                                        
                                        // Handle pending seek after buffering completes
                                        if (isSeeking && pendingSeekPosition >= 0) {
                                            val seekPos = pendingSeekPosition
                                            isSeeking = false
                                            pendingSeekPosition = -1L
                                            exoPlayer.seekTo(seekPos)
                                            return
                                        }
                                        
                                        // CRITICAL: Explicitly start playback if playWhenReady=true but not playing
                                        // This fixes the issue where buffering completes but playback doesn't auto-start
                                        if (exoPlayer.playWhenReady && !exoPlayer.isPlaying && !userPaused) {
                                            runCatching { exoPlayer.play() }
                                        }
                                        
                                        updatePlaybackStateFromPlayer()
                                    }
                                    Player.STATE_ENDED -> {
                                        // Always continue through the provided queue (playlist playback).
                                        // If there's no next item, end in a paused state.
                                        val currentIndex = _currentQueueIndex.value
                                        val queue = _queue.value
                                        Log.d(logTag, "STATE_ENDED currentIndex=$currentIndex queueSize=${queue.size}")
                                        if (queue.isNotEmpty() && currentIndex < queue.size - 1) {
                                            playNext()
                                        } else {
                                            _playbackState.value =
                                                PlaybackState.Paused(
                                                    exoPlayer.currentPosition,
                                                    exoPlayer.duration.coerceAtLeast(0),
                                                )
                                        }
                                    }
                                    Player.STATE_IDLE -> _playbackState.value = PlaybackState.Idle
                                }
                                updateNotification()
                                updateMediaSession()
                            }

                            override fun onIsPlayingChanged(isPlaying: Boolean) {
                                updatePlaybackStateFromPlayer()
                                updateNotification()
                                updateMediaSession()
                            }

                            override fun onPositionDiscontinuity(
                                oldPosition: Player.PositionInfo,
                                newPosition: Player.PositionInfo,
                                reason: Int,
                            ) {
                                Log.d(logTag, "onPositionDiscontinuity reason=$reason oldPos=${oldPosition.positionMs} newPos=${newPosition.positionMs}")
                                
                                // Track seek completion
                                if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) {
                                    isSeeking = false
                                    pendingSeekPosition = -1L
                                }
                                
                                // Don't auto-advance on AUTO_TRANSITION - let STATE_ENDED handle queue progression
                                // This prevents premature skipping when the first video starts
                                
                                updatePlaybackStateFromPlayer()
                            }

                            override fun onPlayerError(error: PlaybackException) {
                                // Try to recover instead of getting stuck showing a Play button that doesn't work.
                                // This commonly happens after seek/forward on some streams.
                                val raw = error.message ?: "Playback error"
                                val wasPlaying = exoPlayer.playWhenReady
                                
                                Log.e(logTag, "Player error: $raw, wasPlaying=$wasPlaying, userPaused=$userPaused")

                                // Don't try to recover if user explicitly paused or if this is initial load
                                if (userPaused) {
                                    _playbackState.value = PlaybackState.Error(toUserFacingStreamingError(raw))
                                    updateNotification()
                                    updateMediaSession()
                                    return
                                }

                                val recovered =
                                    runCatching {
                                        exoPlayer.prepare()
                                        if (wasPlaying) exoPlayer.play()
                                    }.isSuccess

                                if (!recovered) {
                                    runCatching { reloadCurrentItemAtCurrentPosition() }
                                        .onFailure {
                                            _playbackState.value = PlaybackState.Error(toUserFacingStreamingError(raw))
                                        }
                                }

                                updatePlaybackStateFromPlayer()
                                updateNotification()
                                updateMediaSession()
                            }
                        },
                    )
                }

        // Start position tracking
        startPlaybackPositionTracking()
    }

    @OptIn(UnstableApi::class)
    private fun createQueueAwarePlayer(): Player {
        return object : ForwardingPlayer(exoPlayer) {
            override fun hasNextMediaItem(): Boolean {
                val queue = _queue.value
                if (queue.isEmpty()) return false
                return _currentQueueIndex.value < queue.size - 1
            }

            override fun hasPreviousMediaItem(): Boolean {
                val queue = _queue.value
                if (queue.isEmpty()) return false
                return _currentQueueIndex.value > 0
            }

            override fun seekToNextMediaItem() {
                playNext()
            }

            override fun seekToPreviousMediaItem() {
                playPrevious()
            }
        }
    }

    private fun initializeMediaSession() {
        mediaSession =
            MediaSession.Builder(this, queueAwarePlayer)
                .setId("OtterPlayerService")
                .setCallback(
                    object : MediaSession.Callback {
                        override fun onConnect(
                            session: MediaSession,
                            controller: ControllerInfo,
                        ): MediaSession.ConnectionResult {
                            val base = super.onConnect(session, controller)
                            val commands =
                                base.availableSessionCommands
                                    .buildUpon()
                                    .add(SessionCommand(CMD_SET_MAX_VIDEO_HEIGHT, Bundle.EMPTY))
                                    .add(SessionCommand(CMD_SET_AUDIO_ONLY, Bundle.EMPTY))
                                    .add(SessionCommand(CMD_SET_DATA_SAVER, Bundle.EMPTY))
                                    .add(SessionCommand(CMD_SET_PREFERRED_CODEC, Bundle.EMPTY))
                                    .add(SessionCommand(CMD_SET_PLAYBACK_SPEED, Bundle.EMPTY))
                                    .add(SessionCommand(CMD_SET_PREFERRED_AUDIO_LANGUAGE, Bundle.EMPTY))
                                    .add(SessionCommand(CMD_SET_AUDIO_STREAM_INDEX, Bundle.EMPTY))
                                    .build()
                            return MediaSession.ConnectionResult.accept(
                                commands,
                                base.availablePlayerCommands,
                            )
                        }

                        @UnstableApi
                        override fun onCustomCommand(
                            session: MediaSession,
                            controller: ControllerInfo,
                            customCommand: SessionCommand,
                            args: Bundle,
                        ): ListenableFuture<SessionResult> {
                            return when (customCommand.customAction) {
                                CMD_SET_MAX_VIDEO_HEIGHT -> {
                                    val height = args.getInt(CMD_KEY_MAX_VIDEO_HEIGHT, Int.MAX_VALUE)
                                    overrideMaxVideoHeight = if (height == Int.MAX_VALUE) null else height
                                    applySettings(currentSettings)
                                    reloadCurrentItemAtCurrentPosition()
                                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                                }

                                CMD_SET_AUDIO_ONLY -> {
                                    overrideAudioOnly = args.getBoolean(CMD_KEY_AUDIO_ONLY, false)
                                    applySettings(currentSettings)
                                    reloadCurrentItemAtCurrentPosition()
                                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                                }

                                CMD_SET_DATA_SAVER -> {
                                    overrideDataSaver = args.getBoolean(CMD_KEY_DATA_SAVER, false)
                                    applySettings(currentSettings)
                                    reloadCurrentItemAtCurrentPosition()
                                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                                }

                                CMD_SET_PREFERRED_CODEC -> {
                                    val raw = args.getString(CMD_KEY_PREFERRED_CODEC, "AUTO")
                                    overridePreferredCodec =
                                        runCatching {
                                            com.Otter.app.data.models.PreferredCodec.valueOf(raw)
                                        }.getOrElse { com.Otter.app.data.models.PreferredCodec.AUTO }
                                    applySettings(currentSettings)
                                    reloadCurrentItemAtCurrentPosition()
                                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                                }

                                CMD_SET_PLAYBACK_SPEED -> {
                                    val speed = args.getFloat(CMD_KEY_PLAYBACK_SPEED, 1f)
                                    overridePlaybackSpeed = speed
                                    exoPlayer.setPlaybackSpeed(speed)
                                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                                }

                                CMD_SET_PREFERRED_AUDIO_LANGUAGE -> {
                                    val lang = args.getString(CMD_KEY_PREFERRED_AUDIO_LANGUAGE, null)
                                    overridePreferredAudioLanguage = lang?.takeIf { it.isNotBlank() }
                                    selectedAudioStreamSignature = null
                                    reloadCurrentItemAtCurrentPosition()
                                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                                }

                                CMD_SET_AUDIO_STREAM_INDEX -> {
                                    val language =
                                        args.getString(CMD_KEY_AUDIO_STREAM_LANGUAGE, null)
                                            ?.takeIf { it.isNotBlank() }
                                    val codec =
                                        args.getString(CMD_KEY_AUDIO_STREAM_CODEC, null)
                                            ?.takeIf { it.isNotBlank() }
                                    val bitrate =
                                        args.getInt(CMD_KEY_AUDIO_STREAM_BITRATE, -1)
                                            .takeIf { it > 0 }

                                    selectedAudioStreamSignature =
                                        AudioStreamSignature(
                                            language = language,
                                            codec = codec,
                                            bitrate = bitrate,
                                        )

                                    // When selecting by signature, ignore language preference
                                    overridePreferredAudioLanguage = null
                                    reloadCurrentItemAtCurrentPosition()
                                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                                }

                                else -> super.onCustomCommand(session, controller, customCommand, args)
                            }
                        }
                    },
                )
                .build()
    }

    private fun refreshTrackSelection() {
        if (exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.currentMediaItem == null) return

        val wasPlaying = exoPlayer.isPlaying
        val position = exoPlayer.currentPosition
        exoPlayer.prepare()
        if (position > 0) exoPlayer.seekTo(position)
        exoPlayer.playWhenReady = wasPlaying
    }

    private fun startPlaybackPositionTracking() {
        playbackPositionJob?.cancel()
        playbackPositionJob =
            serviceScope.launch {
                while (isActive) {
                    if (exoPlayer.isPlaying) {
                        updatePlaybackStateFromPlayer()

                        maybeSkipSponsorBlock()

                        // Save position every 5 seconds
                        val position = exoPlayer.currentPosition
                        if (position - lastSavedPosition >= 5000) {
                            lastSavedPosition = position
                            savePlaybackPosition()
                        }
                    }
                    delay(PLAYBACK_UPDATE_INTERVAL_MS)
                }
            }
    }

    private fun updatePlaybackStateFromPlayer() {
        val position = exoPlayer.currentPosition.coerceAtLeast(0)
        val duration = exoPlayer.duration.coerceAtLeast(0)
        val bufferedPosition = exoPlayer.bufferedPosition.coerceAtLeast(0)

        _playbackState.value =
            if (exoPlayer.isPlaying) {
                PlaybackState.Playing(position, duration, bufferedPosition)
            } else {
                PlaybackState.Paused(position, duration)
            }
    }

    @UnstableApi
    private fun play(
        url: String? = null,
        startPosition: Long = 0,
    ) {
        serviceScope.launch {
            autoPlayRetryJob?.cancel()
            bufferingTimeoutJob?.cancel()
            val pageUrl =
                url ?: currentQueueItem.value?.let { item ->
                    "https://www.youtube.com/watch?v=${item.videoId}"
                }
            if (pageUrl.isNullOrBlank()) return@launch
            currentPageUrl = pageUrl

            // Apply current settings before resolving stream to match quality/codec preferences
            applySettings(currentSettings)

            val mediaSource =
                resolveMediaSource(pageUrl)
                    .getOrElse { error ->
                        val raw = error.message ?: "Unable to load stream"
                        _playbackState.value = PlaybackState.Error(toUserFacingStreamingError(raw))
                        return@launch
                    }

            // We are explicitly starting playback (including auto-next).
            // CRITICAL: Set playWhenReady BEFORE prepare() so STATE_BUFFERING captures the play intent.
            userPaused = false
            isSeeking = false
            pendingSeekPosition = -1L
            exoPlayer.apply {
                playWhenReady = true  // Set BEFORE prepare() to avoid race condition
                setMediaSource(mediaSource)
                if (startPosition > 0) {
                    seekTo(startPosition)
                }
                prepare()
            }

            startForegroundService()
            
            // Preload next queue item for faster transitions
            preloadNextQueueItem()
        }
    }

    @UnstableApi
    private fun playCurrentQueueItem(startPositionMs: Long = 0L) {
        val item = _currentQueueItem.value ?: return
        val pageUrl = "https://www.youtube.com/watch?v=${item.videoId}"
        startSponsorBlockFetchIfNeeded(item.videoId, currentSettings)
        play(pageUrl, startPositionMs)
    }

    private fun startSponsorBlockFetchIfNeeded(
        videoId: String,
        settings: AppSettings,
    ) {
        if (!settings.sponsorBlockEnabled) {
            sponsorBlockSegments = emptyList()
            sponsorBlockSegmentsForVideoId = null
            sponsorBlockFetchJob?.cancel()
            sponsorBlockFetchJob = null
            return
        }

        if (sponsorBlockSegmentsForVideoId == videoId && sponsorBlockSegments.isNotEmpty()) return

        sponsorBlockFetchJob?.cancel()
        sponsorBlockFetchJob =
            serviceScope.launch(Dispatchers.IO) {
                runCatching {
                    val cats =
                        settings.sponsorBlockCategories
                            .ifEmpty { setOf(SponsorBlockCategory.SPONSOR, SponsorBlockCategory.INTRO, SponsorBlockCategory.OUTRO) }
                            .mapNotNull { it.toSponsorBlockApiCategoryOrNull() }

                    val categoriesJson = JSONArray(cats).toString()
                    val url =
                        "https://sponsor.ajay.app/api/skipSegments?videoID=" +
                            URLEncoder.encode(videoId, "UTF-8") +
                            "&categories=" + URLEncoder.encode(categoriesJson, "UTF-8")

                    val req = Request.Builder().url(url).get().build()
                    sponsorBlockClient.newCall(req).execute().use { resp ->
                        val body = resp.body?.string().orEmpty()
                        if (!resp.isSuccessful) {
                            throw IllegalStateException("SponsorBlock HTTP ${resp.code}")
                        }

                        val arr = JSONArray(body)
                        val segments =
                            buildList {
                                for (i in 0 until arr.length()) {
                                    val obj = arr.getJSONObject(i)
                                    val seg = obj.optJSONArray("segment")
                                    if (seg == null || seg.length() < 2) continue
                                    val startSec = seg.optDouble(0, -1.0)
                                    val endSec = seg.optDouble(1, -1.0)
                                    if (startSec < 0 || endSec < 0) continue
                                    val category = obj.optString("category", "")
                                    add(
                                        SponsorBlockSegment(
                                            startMs = (startSec * 1000.0).toLong(),
                                            endMs = (endSec * 1000.0).toLong(),
                                            category = category,
                                        ),
                                    )
                                }
                            }

                        sponsorBlockSegments = segments.sortedBy { it.startMs }
                        sponsorBlockSegmentsForVideoId = videoId
                        lastSponsorBlockSkipToMs = -1L
                        Log.d(logTag, "SponsorBlock segments loaded videoId=$videoId count=${sponsorBlockSegments.size}")
                    }
                }.onFailure {
                    sponsorBlockSegments = emptyList()
                    sponsorBlockSegmentsForVideoId = videoId
                    lastSponsorBlockSkipToMs = -1L
                    Log.d(logTag, "SponsorBlock fetch failed videoId=$videoId error=${it.message}")
                }
            }
    }

    private fun maybeSkipSponsorBlock() {
        if (!currentSettings.sponsorBlockEnabled) return

        val videoId = _currentQueueItem.value?.videoId ?: return
        if (sponsorBlockSegmentsForVideoId != videoId) return
        if (sponsorBlockSegments.isEmpty()) return

        val pos = exoPlayer.currentPosition

        // If we just skipped to this position, don't re-trigger.
        if (lastSponsorBlockSkipToMs >= 0 && kotlin.math.abs(pos - lastSponsorBlockSkipToMs) < 750) return

        val seg = sponsorBlockSegments.firstOrNull { pos in it.startMs..it.endMs } ?: return

        val target = (seg.endMs + 250).coerceAtLeast(0)
        lastSponsorBlockSkipToMs = target
        Log.d(logTag, "SponsorBlock skip category=${seg.category} ${seg.startMs}-${seg.endMs} pos=$pos -> $target")
        exoPlayer.seekTo(target)
    }

    private fun SponsorBlockCategory.toSponsorBlockApiCategoryOrNull(): String? {
        return when (this) {
            SponsorBlockCategory.SPONSOR -> "sponsor"
            SponsorBlockCategory.INTRO -> "intro"
            SponsorBlockCategory.OUTRO -> "outro"
            SponsorBlockCategory.INTERACTION -> "interaction"
            SponsorBlockCategory.SELF_PROMO -> "selfpromo"
            SponsorBlockCategory.MUSIC_OFFTOPIC -> "music_offtopic"
            SponsorBlockCategory.PREVIEW -> "preview"
            SponsorBlockCategory.FILLER -> "filler"
        }
    }

    @UnstableApi
    private suspend fun resolveMediaSource(pageUrl: String): Result<MediaSource> {
        val effective =
            currentSettings.copy(
                streamingAudioOnly = overrideAudioOnly ?: currentSettings.streamingAudioOnly,
                streamingDataSaver = overrideDataSaver ?: currentSettings.streamingDataSaver,
                preferredCodec = overridePreferredCodec ?: currentSettings.preferredCodec,
            )

        val queueItem = _currentQueueItem.value
        val metaExtras =
            Bundle().apply {
                if (queueItem != null) {
                    putString("videoId", queueItem.videoId)
                    putString("title", queueItem.title)
                    putString("thumbnailUrl", queueItem.thumbnailUrl)
                    putLong("duration", queueItem.duration)
                    putString("uploaderName", queueItem.uploaderName)
                }
                // Keep UI/controller in sync when the service advances the queue (notification buttons,
                // auto-next, mini-player swipes). These extras are used by PlayerConnectionManager.
                putInt("queueIndex", _currentQueueIndex.value)
                putInt("queueSize", _queue.value.size)
                putBoolean("audioOnly", effective.streamingAudioOnly)
            }

        val mediaMetadata =
            androidx.media3.common.MediaMetadata.Builder().apply {
                if (queueItem != null) {
                    setTitle(queueItem.title)
                    setArtist(queueItem.uploaderName)
                    if (queueItem.thumbnailUrl.isNotBlank()) {
                        setArtworkUri(Uri.parse(queueItem.thumbnailUrl))
                    }
                    setExtras(metaExtras)
                }
            }.build()

        return if (pageUrl.isDirectStreamUrl()) {
            val mediaItem =
                MediaItem.Builder()
                    .setUri(pageUrl)
                    .setMediaMetadata(mediaMetadata)
                    .build()
            Result.success(buildMediaSource(mediaItem))
        } else {
            withContext(Dispatchers.IO) {
                streamRepository.extractStreamInfo(pageUrl, effective).mapCatching { info ->
                    val subtitleConfigs =
                        info.subtitles.map { sub ->
                            MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                                .setMimeType(
                                    when (sub.format.uppercase()) {
                                        "VTT", "WEBVTT" -> MimeTypes.TEXT_VTT
                                        "SRT" -> MimeTypes.APPLICATION_SUBRIP
                                        "TTML" -> MimeTypes.APPLICATION_TTML
                                        else -> MimeTypes.TEXT_VTT
                                    },
                                )
                                .setLanguage(sub.languageCode)
                                .setLabel(sub.languageName.ifBlank { sub.languageCode })
                                .build()
                        }

                    fun selectAudioUrl(): String {
                        val candidates = info.audioStreams

                        val signature = selectedAudioStreamSignature
                        if (signature != null) {
                            val filteredByLang =
                                if (signature.language.isNullOrBlank()) {
                                    candidates
                                } else {
                                    val p = signature.language.lowercase()
                                    candidates.filter { a ->
                                        val lang = a.language?.lowercase()
                                        lang != null && (lang == p || lang.startsWith(p) || lang.contains(p))
                                    }.ifEmpty { candidates }
                                }

                            val filteredByCodec =
                                if (signature.codec.isNullOrBlank()) {
                                    filteredByLang
                                } else {
                                    val c = signature.codec.lowercase()
                                    filteredByLang.filter { a -> a.codec.lowercase().contains(c) }
                                        .ifEmpty { filteredByLang }
                                }

                            val preferredBitrate = signature.bitrate
                            if (preferredBitrate != null) {
                                return filteredByCodec
                                    .minByOrNull { a -> kotlin.math.abs(a.bitrate - preferredBitrate) }
                                    ?.url
                                    ?: throw IllegalStateException("No audio stream available")
                            }

                            return filteredByCodec
                                .sortedByDescending { it.bitrate }
                                .firstOrNull()
                                ?.url
                                ?: throw IllegalStateException("No audio stream available")
                        }

                        val preferred =
                            overridePreferredAudioLanguage
                                ?: effective.streamingPreferredAudioLanguage
                                    .takeIf { it.isNotBlank() && it.lowercase() != "system" }
                                ?: Locale.getDefault().language
                        val filtered =
                            if (preferred.isNullOrBlank()) {
                                candidates
                            } else {
                                val p = preferred.lowercase()
                                candidates.filter { a ->
                                    val lang = a.language?.lowercase()
                                    lang != null && (lang == p || lang.startsWith(p) || lang.contains(p))
                                }.ifEmpty { candidates }
                            }
                        return filtered
                            .sortedByDescending { it.bitrate }
                            .firstOrNull()
                            ?.url
                            ?: throw IllegalStateException("No audio stream available")
                    }

                    if (effective.streamingAudioOnly) {
                        val audioUrl = selectAudioUrl()
                        Log.d(logTag, "resolveMediaSource(audioOnly)=true selectedAudioUrl=$audioUrl")
                        val mediaItem =
                            MediaItem.Builder()
                                .setUri(audioUrl)
                                .setMediaMetadata(mediaMetadata)
                                .setSubtitleConfigurations(subtitleConfigs)
                                .build()
                        buildMediaSource(mediaItem)
                    } else {
                        val audioOverrideRequested =
                            selectedAudioStreamSignature != null ||
                                !overridePreferredAudioLanguage.isNullOrBlank() ||
                                (
                                    effective.streamingPreferredAudioLanguage.isNotBlank() &&
                                        effective.streamingPreferredAudioLanguage.lowercase() != "system"
                                )

                        val baseSelectedVideo =
                            selectVideoStreamForPlayback(info.videoStreams, effective)
                                ?: throw IllegalStateException("No suitable video stream available")

                        // If the user (or settings) requests a specific audio language/track, muxed streams
                        // can't change audio. Prefer a video-only stream so we can merge the chosen audio.
                        val selectedVideo =
                            if (audioOverrideRequested && !baseSelectedVideo.isVideoOnly) {
                                selectVideoStreamForPlayback(info.videoStreams.filter { it.isVideoOnly }, effective)
                                    ?: baseSelectedVideo
                            } else {
                                baseSelectedVideo
                            }

                        Log.d(
                            logTag,
                            "resolveMediaSource(audioOnly)=false audioOverrideRequested=$audioOverrideRequested " +
                                "videoIsVideoOnly=${selectedVideo.isVideoOnly} videoHeight=${selectedVideo.height} codec=${selectedVideo.codec}",
                        )

                        if (!selectedVideo.isVideoOnly) {
                            val mediaItem =
                                MediaItem.Builder()
                                    .setUri(selectedVideo.url)
                                    .setMediaMetadata(mediaMetadata)
                                    .setSubtitleConfigurations(subtitleConfigs)
                                    .build()
                            buildMediaSource(mediaItem)
                        } else {
                            val audioUrl = selectAudioUrl()
                            Log.d(logTag, "resolveMediaSource merging video+audio audioUrl=$audioUrl")
                            val videoItem =
                                MediaItem.Builder()
                                    .setUri(selectedVideo.url)
                                    .setMediaMetadata(mediaMetadata)
                                    .setSubtitleConfigurations(subtitleConfigs)
                                    .build()
                            val videoSource = buildMediaSource(videoItem)
                            val audioSource = buildMediaSource(MediaItem.fromUri(audioUrl))
                            MergingMediaSource(videoSource, audioSource)
                        }
                    }
                }
            }
        }
    }

    private fun selectVideoStreamForPlayback(
        streams: List<VideoStreamInfo>,
        settings: AppSettings,
    ): VideoStreamInfo? {
        val qualityHeight =
            when (settings.streamingQuality) {
                StreamingQuality.AUTO -> Int.MAX_VALUE
                StreamingQuality.SD_360P -> 360
                StreamingQuality.SD_480P -> 480
                StreamingQuality.HD_720P -> 720
                StreamingQuality.HD_1080P -> 1080
                StreamingQuality.UHD_4K -> 2160
                StreamingQuality.HIGHEST -> Int.MAX_VALUE
            }

        val manualMaxHeight = overrideMaxVideoHeight
        val maxHeight =
            when {
                manualMaxHeight != null && manualMaxHeight != Int.MAX_VALUE -> manualMaxHeight
                settings.streamingDataSaver -> 480
                qualityHeight != Int.MAX_VALUE -> qualityHeight
                else -> Int.MAX_VALUE
            }

        var filtered = streams

        // Apply height cap if any
        if (maxHeight != Int.MAX_VALUE) {
            filtered = filtered.filter { it.height in 1..maxHeight }
        }
        if (filtered.isEmpty()) filtered = streams

        // Apply codec preference
        filtered =
            when (settings.preferredCodec) {
                PreferredCodec.AUTO -> filtered
                PreferredCodec.AV1 -> filtered.filter { it.codec.contains("av01", true) || it.codec.contains("av1", true) }
                PreferredCodec.VP9 -> filtered.filter { it.codec.contains("vp9", true) || it.codec.contains("vp09", true) }
                PreferredCodec.H264 -> filtered.filter { it.codec.contains("avc", true) || it.codec.contains("h264", true) }
            }.ifEmpty { filtered }

        return filtered
            .sortedWith(
                compareByDescending<VideoStreamInfo> { it.height }
                    .thenByDescending { it.bitrate },
            )
            .firstOrNull()
    }

    @OptIn(UnstableApi::class)
    private fun buildMediaSource(mediaItem: MediaItem): MediaSource {
        val dataSourceFactory =
            if (this::mediaDataSourceFactory.isInitialized) mediaDataSourceFactory else DefaultDataSource.Factory(this)
        val mediaSourceFactory = DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory)
        return mediaSourceFactory.createMediaSource(mediaItem)
    }

    @OptIn(UnstableApi::class)
    private fun reloadCurrentItemAtCurrentPosition() {
        val pageUrl = currentPageUrl ?: return
        if (exoPlayer.currentMediaItem == null) return

        val position = exoPlayer.currentPosition.coerceAtLeast(0)
        // isPlaying is false during buffering; playWhenReady preserves user's intent.
        val wasPlaying = exoPlayer.playWhenReady
        
        // Reset seek state
        isSeeking = false
        pendingSeekPosition = -1L

        serviceScope.launch {
            val mediaSource =
                resolveMediaSource(pageUrl)
                    .getOrElse { error ->
                        val raw = error.message ?: "Unable to load stream"
                        _playbackState.value = PlaybackState.Error(toUserFacingStreamingError(raw))
                        return@launch
                    }

            // CRITICAL: Set playWhenReady BEFORE prepare()
            exoPlayer.apply {
                playWhenReady = wasPlaying && !userPaused
                setMediaSource(mediaSource)
                prepare()
                seekTo(position)
            }
        }
    }

    private fun String.isDirectStreamUrl(): Boolean {
        return contains("googlevideo.com", ignoreCase = true) ||
            contains("manifest.googlevideo.com", ignoreCase = true) ||
            contains(".m3u8", ignoreCase = true) ||
            contains(".mpd", ignoreCase = true)
    }

    private fun pause() {
        userPaused = true
        autoPlayRetryJob?.cancel()
        bufferingTimeoutJob?.cancel()
        exoPlayer.pause()
        savePlaybackPosition()
        updateNotification()
        stopForeground(false)
    }

    @OptIn(UnstableApi::class)
    private fun play() {
        if (exoPlayer.currentMediaItem == null) {
            if (_currentQueueItem.value != null) {
                play(null)
            }
            return
        }

        // Resume playback
        userPaused = false
        exoPlayer.playWhenReady = true

        // In rare cases ExoPlayer can end up IDLE after a seek/error; prepare/retry to recover.
        if (exoPlayer.playbackState == Player.STATE_IDLE) {
            exoPlayer.prepare()
        }

        runCatching { exoPlayer.play() }
            .onFailure {
                runCatching {
                    exoPlayer.prepare()
                    exoPlayer.play()
                }
                    .onFailure { reloadCurrentItemAtCurrentPosition() }
            }

        startForegroundService()
    }

    private fun togglePlayback() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    @OptIn(UnstableApi::class)
    private fun playNext() {
        val currentIndex = _currentQueueIndex.value
        val queue = _queue.value

        if (queue.isNotEmpty() && currentIndex < queue.size - 1) {
            _currentQueueIndex.value = currentIndex + 1
            _currentQueueItem.value = queue[currentIndex + 1]
            
            // Update metadata immediately for UI sync (prevents audio/UI mismatch in audio mode)
            val nextItem = queue[currentIndex + 1]
            updateCurrentItemMetadata(nextItem)
            
            // Use preloaded item if available for faster transition
            val preloaded = preloadingNextItem
            preloadingNextItem = null
            preloadNextJob?.cancel()
            
            if (preloaded != null) {
                // Use preloaded media source for instant playback
                // CRITICAL: Set playWhenReady BEFORE prepare()
                serviceScope.launch {
                    userPaused = false
                    isSeeking = false
                    pendingSeekPosition = -1L
                    exoPlayer.apply {
                        playWhenReady = true  // Set BEFORE prepare()
                        setMediaSource(preloaded)
                        prepare()
                        // Ensure metadata is propagated to MediaSession for immediate UI sync
                        invalidateMediaSession()
                    }
                    startForegroundService()
                }
            } else {
                playCurrentQueueItem(startPositionMs = 0L)
            }
            
            // Preload the next item after this one
            preloadNextQueueItem()
        }
    }

    @OptIn(UnstableApi::class)
    private fun playPrevious() {
        val currentIndex = _currentQueueIndex.value
        val queue = _queue.value

        if (currentIndex > 0) {
            _currentQueueIndex.value = currentIndex - 1
            _currentQueueItem.value = queue[currentIndex - 1]
            
            // Update metadata immediately for UI sync (prevents audio/UI mismatch in audio mode)
            val prevItem = queue[currentIndex - 1]
            updateCurrentItemMetadata(prevItem)
            playCurrentQueueItem(startPositionMs = 0L)
            
            // Preload the next item (which is now the one we just left)
            preloadNextQueueItem()
        }
    }

    @OptIn(UnstableApi::class)
    private fun updateCurrentItemMetadata(item: QueueItem) {
        // Update notification immediately to show the current item
        // The actual metadata was already set when creating the MediaSource with this item
        updateNotification()
    }

    @OptIn(UnstableApi::class)
    private fun preloadNextQueueItem() {
        val currentIndex = _currentQueueIndex.value
        val queue = _queue.value
        
        // Only preload if there's a next item
        if (queue.isEmpty() || currentIndex >= queue.size - 1) {
            preloadingNextItem = null
            return
        }
        
        val nextItem = queue[currentIndex + 1]
        val nextPageUrl = "https://www.youtube.com/watch?v=${nextItem.videoId}"
        
        preloadNextJob?.cancel()
        preloadNextJob = serviceScope.launch(Dispatchers.IO) {
            try {
                val mediaSource = resolveMediaSource(nextPageUrl).getOrNull()
                if (mediaSource != null && _currentQueueIndex.value == currentIndex) {
                    preloadingNextItem = mediaSource
                    Log.d(logTag, "Preloaded next item: ${nextItem.videoId}")
                }
            } catch (e: Exception) {
                Log.w(logTag, "Failed to preload next item: ${e.message}")
            }
        }
    }

    private fun invalidateMediaSession() {
        // Force MediaSession to refresh metadata
        // This ensures queue info is immediately available to system and controllers
        updateNotification()
    }

    private fun seekTo(position: Long) {
        // Track seeking state for auto-resume after buffering
        isSeeking = true
        pendingSeekPosition = position
        exoPlayer.seekTo(position)
        updatePlaybackStateFromPlayer()
    }

    private fun stop() {
        autoPlayRetryJob?.cancel()
        bufferingTimeoutJob?.cancel()
        preloadNextJob?.cancel()
        preloadingNextItem = null
        isSeeking = false
        pendingSeekPosition = -1L
        savePlaybackPosition()
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _playbackState.value = PlaybackState.Idle
        _currentQueueItem.value = null
        stopForeground(true)
        stopSelf()
    }

    @UnstableApi
    private fun applySettings(settings: AppSettings) {
        val effective =
            settings.copy(
                streamingAudioOnly = overrideAudioOnly ?: settings.streamingAudioOnly,
                streamingDataSaver = overrideDataSaver ?: settings.streamingDataSaver,
                preferredCodec = overridePreferredCodec ?: settings.preferredCodec,
            )

        // Apply audio-only mode
        val builder = trackSelector.buildUponParameters()

        if (effective.streamingAudioOnly) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
        } else {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)

            val maxHeight =
                when (effective.streamingQuality) {
                    com.Otter.app.data.models.StreamingQuality.AUTO -> Int.MAX_VALUE
                    com.Otter.app.data.models.StreamingQuality.SD_360P -> 360
                    com.Otter.app.data.models.StreamingQuality.SD_480P -> 480
                    com.Otter.app.data.models.StreamingQuality.HD_720P -> 720
                    com.Otter.app.data.models.StreamingQuality.HD_1080P -> 1080
                    com.Otter.app.data.models.StreamingQuality.UHD_4K -> 2160
                    com.Otter.app.data.models.StreamingQuality.HIGHEST -> Int.MAX_VALUE
                }

            val manualMaxHeight = overrideMaxVideoHeight

            // Int.MAX_VALUE means no limit (Auto/Highest selection)
            if (manualMaxHeight != null && manualMaxHeight != Int.MAX_VALUE) {
                builder.setMaxVideoSize(Int.MAX_VALUE, manualMaxHeight)
            } else if (effective.streamingDataSaver) {
                builder.setMaxVideoSize(Int.MAX_VALUE, 480)
            } else if (maxHeight != Int.MAX_VALUE) {
                builder.setMaxVideoSize(Int.MAX_VALUE, maxHeight)
            } else {
                builder.setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
            }

            when (effective.preferredCodec) {
                com.Otter.app.data.models.PreferredCodec.AUTO -> Unit
                com.Otter.app.data.models.PreferredCodec.AV1 -> builder.setPreferredVideoMimeTypes(MimeTypes.VIDEO_AV1)
                com.Otter.app.data.models.PreferredCodec.VP9 -> builder.setPreferredVideoMimeTypes(MimeTypes.VIDEO_VP9)
                com.Otter.app.data.models.PreferredCodec.H264 -> builder.setPreferredVideoMimeTypes(MimeTypes.VIDEO_H264)
            }
        }

        trackSelector.setParameters(builder)
    }

    private fun startForegroundService() {
        updateNotification()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Media Playback",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Media playback controls"
                    setShowBadge(false)
                }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @OptIn(UnstableApi::class)
    private fun updateNotification() {
        playerNotificationManager?.invalidate()
    }

    private fun updateMediaSession() {
        // The system media notification reads queue info from the player's media metadata
        // We've already set queueIndex and queueSize in updateCurrentItemMetadata
        // Just ensure the notification is updated to reflect the current state
        updateNotification()
    }

    private fun savePlaybackPosition() {
        val currentItem = _currentQueueItem.value ?: return
        val position = exoPlayer.currentPosition
        val durationMs = exoPlayer.duration.takeIf { it > 0 } ?: currentItem.duration
        val audioOnly = overrideAudioOnly ?: currentSettings.streamingAudioOnly
        val playWhenReady = exoPlayer.playWhenReady && !userPaused

        serviceScope.launch {
            playbackResumeStore.save(
                PlaybackResumeState(
                    videoId = currentItem.videoId,
                    videoUrl = currentPageUrl ?: "https://www.youtube.com/watch?v=${currentItem.videoId}",
                    title = currentItem.title,
                    thumbnailUrl = currentItem.thumbnailUrl,
                    uploaderName = currentItem.uploaderName,
                    durationMs = durationMs,
                    positionMs = position.coerceAtLeast(0L),
                    audioOnly = audioOnly,
                    playWhenReady = playWhenReady,
                ),
            )
        }
    }
}

// Extension for Parcelable QueueItem
@Suppress("DEPRECATION")
inline fun <reified T : android.os.Parcelable> Intent.getParcelableArrayListExtra(key: String): ArrayList<T>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayListExtra(key, T::class.java)
    } else {
        getParcelableArrayListExtra(key)
    }
}
