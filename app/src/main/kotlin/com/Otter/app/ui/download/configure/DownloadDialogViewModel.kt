package com.Otter.app.ui.download.configure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.Otter.app.data.download.CommandTemplate
import com.Otter.app.data.download.DownloadEngine
import com.Otter.app.data.download.DownloadPreferences
import com.Otter.app.data.download.Downloader
import com.Otter.app.data.download.PlaylistResult
import com.Otter.app.data.download.Task
import com.Otter.app.data.download.VideoInfo
import com.Otter.app.data.models.AppSettings
import com.Otter.app.service.SettingsService
import com.Otter.app.util.FileLogger
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DownloadDialogViewModel
    @Inject
    constructor(
        @ApplicationContext private val appContext: Context,
        private val downloader: Downloader,
        private val engine: DownloadEngine,
        private val fileLogger: FileLogger,
        private val settingsService: SettingsService,
    ) : ViewModel() {
        // Cache to avoid re-fetching video info
        private val videoInfoCache = mutableMapOf<String, VideoInfo>()

        // Temporary storage for playlist info during format fetch
        private var pendingPlaylistResult: PlaylistResult? = null
        private var pendingSelectedIndices: List<Int> = emptyList()

        @Volatile
        private var latestSettings: AppSettings = AppSettings()

        sealed interface SelectionState {
            data object Idle : SelectionState

            data class Loading(
                val url: String,
                val audioOnly: Boolean,
                val preferences: DownloadPreferences,
            ) : SelectionState

            data class FormatSelection(
                val info: VideoInfo,
                val audioOnly: Boolean,
                val preferences: DownloadPreferences,
                val playlistResult: PlaylistResult? = null,
                val selectedIndices: List<Int> = emptyList(),
            ) : SelectionState

            data class PlaylistSelection(
                val result: PlaylistResult,
                val preferences: DownloadPreferences,
            ) : SelectionState
        }

        sealed interface SheetState {
            data object InputUrl : SheetState

            data class Configure(val urlList: List<String>) : SheetState

            data class Loading(val taskKey: String, val job: Job) : SheetState

            data class Error(val action: Action, val throwable: Throwable) : SheetState
        }

        sealed interface SheetValue {
            data object Expanded : SheetValue

            data object Hidden : SheetValue
        }

        sealed interface Action {
            data object HideSheet : Action

            data class ShowSheet(val urlList: List<String>? = null) : Action

            data class ProceedWithURLs(val urlList: List<String>) : Action

            data object Reset : Action

            data class FetchFormats(
                val url: String,
                val audioOnly: Boolean,
                val preferences: DownloadPreferences,
            ) : Action

            data class FetchPlaylist(
                val url: String,
                val preferences: DownloadPreferences,
            ) : Action

            data class DownloadWithPreset(
                val urlList: List<String>,
                val preferences: DownloadPreferences,
            ) : Action

            data class RunCommand(
                val url: String,
                val template: CommandTemplate,
                val preferences: DownloadPreferences,
            ) : Action

            data object Cancel : Action
        }

        private val mSelectionStateFlow: MutableStateFlow<SelectionState> =
            MutableStateFlow(SelectionState.Idle)
        private val mSheetStateFlow: MutableStateFlow<SheetState> = MutableStateFlow(SheetState.InputUrl)
        private val mSheetValueFlow: MutableStateFlow<SheetValue> = MutableStateFlow(SheetValue.Hidden)

        val selectionStateFlow = mSelectionStateFlow.asStateFlow()
        val sheetStateFlow = mSheetStateFlow.asStateFlow()
        val sheetValueFlow = mSheetValueFlow.asStateFlow()

        private val sheetState
            get() = sheetStateFlow.value

        private fun looksLikePlaylistUrl(url: String): Boolean {
            val u = url.lowercase()
            return u.contains("list=") && (u.contains("/playlist") || u.contains("watch?") || u.contains("youtu.be/"))
        }

        private fun isValidHttpUrl(url: String): Boolean {
            val u = url.trim().lowercase()
            return u.startsWith("http://") || u.startsWith("https://")
        }

        private fun hasNetworkConnection(): Boolean {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        private fun isOnWifi(): Boolean {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        }

        private fun checkPreflightOrError(action: Action): Boolean {
            if (!hasNetworkConnection()) {
                mSelectionStateFlow.update { SelectionState.Idle }
                mSheetStateFlow.update {
                    SheetState.Error(
                        action = action,
                        throwable = IllegalStateException("No network connection"),
                    )
                }
                return false
            }
            if (latestSettings.wifiOnlyDownloads && !isOnWifi()) {
                mSelectionStateFlow.update { SelectionState.Idle }
                mSheetStateFlow.update {
                    SheetState.Error(
                        action = action,
                        throwable = IllegalStateException("Wi-Fi only downloads enabled"),
                    )
                }
                return false
            }
            return true
        }

        private fun isTransientFailure(throwable: Throwable): Boolean {
            if (throwable is YoutubeDL.CanceledException) return false
            val msg = (throwable.message ?: "").lowercase()
            return "unable to resolve host" in msg ||
                "no address associated with hostname" in msg ||
                "temporary failure in name resolution" in msg ||
                "network is unreachable" in msg ||
                "failed to establish a new connection" in msg ||
                ("connection" in msg && "reset" in msg) ||
                "timed out" in msg ||
                "timeout" in msg
        }

        private fun isPermanentFailure(throwable: Throwable): Boolean {
            if (throwable is IllegalArgumentException) return true
            val msg = (throwable.message ?: "").lowercase()
            return "unsupported url" in msg ||
                ("unsupported" in msg && "url" in msg) ||
                "no suitable extractor" in msg ||
                "requested format" in msg && "not available" in msg ||
                "no video formats found" in msg ||
                "no formats" in msg
        }

        private suspend fun <T> retryOnTransient(
            maxAttempts: Int = 3,
            initialDelayMs: Long = 800,
            block: suspend () -> Result<T>,
        ): Result<T> {
            var attempt = 1
            var delayMs = initialDelayMs
            var last: Result<T> = block()
            while (attempt < maxAttempts) {
                val ex = last.exceptionOrNull() ?: return last
                if (isPermanentFailure(ex) || !isTransientFailure(ex)) return last
                delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(8000)
                attempt++
                last = block()
            }
            return last
        }

        init {
            viewModelScope.launch(Dispatchers.IO) {
                settingsService.getSettings().collect { latestSettings = it }
            }
        }

        fun setPendingPlaylistInfo(
            result: PlaylistResult,
            selectedIndices: List<Int>,
        ) {
            pendingPlaylistResult = result
            pendingSelectedIndices = selectedIndices
        }

        fun postAction(action: Action) {
            when (action) {
                is Action.ProceedWithURLs -> proceedWithUrls(action)
                is Action.FetchFormats -> fetchFormat(action)
                is Action.FetchPlaylist -> fetchPlaylist(action)
                is Action.DownloadWithPreset -> downloadWithPreset(action.urlList, action.preferences)
                is Action.RunCommand -> runCommand(action.url, action.template, action.preferences)
                Action.HideSheet -> hideDialog()
                is Action.ShowSheet -> showDialog(action)
                Action.Cancel -> cancel()
                Action.Reset -> resetSelectionState()
            }
        }

        private fun proceedWithUrls(action: Action.ProceedWithURLs) {
            mSheetStateFlow.update { SheetState.Configure(action.urlList) }
        }

        private fun fetchPlaylist(action: Action.FetchPlaylist) {
            val (url, preferences) = action

            if (!isValidHttpUrl(url)) {
                mSelectionStateFlow.update { SelectionState.Idle }
                mSheetStateFlow.update {
                    SheetState.Error(
                        action = action,
                        throwable = IllegalArgumentException("Invalid URL"),
                    )
                }
                return
            }

            if (!checkPreflightOrError(action)) return

            val taskKey = "FetchPlaylist_$url"
            val job =
                viewModelScope.launch(Dispatchers.IO) {
                    retryOnTransient(
                        block = { engine.fetchPlaylistMetadata(url, taskKey) },
                    ).onSuccess { result ->
                        withContext(Dispatchers.Main) {
                            // Store playlist result for later use
                            pendingPlaylistResult = result
                            mSelectionStateFlow.update {
                                SelectionState.PlaylistSelection(
                                    result = result,
                                    preferences = preferences,
                                )
                            }
                        }
                    }.onFailure { th ->
                        withContext(Dispatchers.Main) {
                            mSelectionStateFlow.update { SelectionState.Idle }
                            mSheetStateFlow.update { SheetState.Error(action, throwable = th) }
                        }
                    }
                }

            mSheetStateFlow.update { SheetState.Loading(taskKey = taskKey, job = job) }
        }

        private fun fetchFormat(action: Action.FetchFormats) {
            val (url, audioOnly, preferences) = action

            if (!isValidHttpUrl(url)) {
                mSelectionStateFlow.update { SelectionState.Idle }
                mSheetStateFlow.update {
                    SheetState.Error(
                        action = action,
                        throwable = IllegalArgumentException("Invalid URL"),
                    )
                }
                return
            }

            if (!checkPreflightOrError(action)) return

            val isPlaylistUrl = looksLikePlaylistUrl(url)

            // Check cache first
            videoInfoCache[url]?.let { cachedInfo ->
                fileLogger.log("DownloadDialogViewModel", "Using cached VideoInfo for: $url")
                mSelectionStateFlow.update {
                    SelectionState.FormatSelection(
                        info = cachedInfo,
                        audioOnly = audioOnly,
                        preferences = preferences,
                        playlistResult = pendingPlaylistResult,
                        selectedIndices = pendingSelectedIndices,
                    )
                }
                return
            }

            // Open sheet immediately with loading state
            mSelectionStateFlow.update {
                SelectionState.Loading(
                    url = url,
                    audioOnly = audioOnly,
                    preferences = preferences,
                )
            }

            val taskKey = "FetchFormat_$url"
            val job =
                viewModelScope.launch(Dispatchers.IO) {
                    retryOnTransient(
                        block = {
                            engine.fetchVideoInfo(
                                url = url,
                                taskKey = taskKey,
                                preferences = preferences.copy(extractAudio = audioOnly),
                                playlistIndex = if (isPlaylistUrl || preferences.downloadPlaylist) 1 else null,
                            )
                        },
                    ).onSuccess { info ->
                        val playlistUrlForQueue =
                            pendingPlaylistResult?.webpageUrl
                                ?.takeIf { it.isNotBlank() }

                        val normalizedInfo =
                            when {
                                preferences.downloadPlaylist && playlistUrlForQueue != null -> {
                                    // Formats are fetched from a sample entry, but the queued tasks must
                                    // use the playlist URL + playlist index.
                                    info.copy(originalUrl = playlistUrlForQueue)
                                }

                                isPlaylistUrl -> {
                                    info.copy(originalUrl = url)
                                }

                                else -> info
                            }

                        // Log all formats details
                        val formats = normalizedInfo.formats ?: emptyList()
                        fileLogger.log("DownloadDialogViewModel", "=== Fetched ${formats.size} formats ===")
                        fileLogger.log("DownloadDialogViewModel", "Title: ${normalizedInfo.title}")
                        fileLogger.log("DownloadDialogViewModel", "Duration: ${normalizedInfo.duration}s")

                        // Log raw format data for debugging
                        fileLogger.log("DownloadDialogViewModel", "--- Raw Format Data ---")
                        formats.forEach { f ->
                            fileLogger.log(
                                "DownloadDialogViewModel",
                                "ID:${f.formatId} | format:${f.format} | vcodec:'${f.vcodec}' | acodec:'${f.acodec}' | containsVideo:${f.containsVideo()} | isAudioOnly:${f.isAudioOnly()}",
                            )
                        }

                        val avFormats = formats.filter { it.containsVideo() && it.containsAudio() }
                        val videoOnlyFormats = formats.filter { it.isVideoOnly() }
                        val audioOnlyFormats = formats.filter { it.isAudioOnly() }

                        fileLogger.log("DownloadDialogViewModel", "--- A+V Formats (${avFormats.size}) ---")
                        avFormats.forEach { f ->
                            fileLogger.log(
                                "DownloadDialogViewModel",
                                "ID:${f.formatId} | ${f.format} | V:${f.vcodec} | A:${f.acodec} | Size:${f.fileSize?.toLong() ?: f.fileSizeApprox?.toLong()} | Ext:${f.ext}",
                            )
                        }

                        fileLogger.log("DownloadDialogViewModel", "--- Video Only (${videoOnlyFormats.size}) ---")
                        videoOnlyFormats.forEach { f ->
                            fileLogger.log(
                                "DownloadDialogViewModel",
                                "ID:${f.formatId} | ${f.format} | V:${f.vcodec} | Size:${f.fileSize?.toLong() ?: f.fileSizeApprox?.toLong()} | Ext:${f.ext}",
                            )
                        }

                        fileLogger.log("DownloadDialogViewModel", "--- Audio Only (${audioOnlyFormats.size}) ---")
                        audioOnlyFormats.forEach { f ->
                            fileLogger.log(
                                "DownloadDialogViewModel",
                                "ID:${f.formatId} | ${f.format} | A:${f.acodec} | Size:${f.fileSize?.toLong() ?: f.fileSizeApprox?.toLong()} | Ext:${f.ext}",
                            )
                        }

                        withContext(Dispatchers.Main) {
                            // Cache the result
                            videoInfoCache[url] = normalizedInfo
                            fileLogger.log("DownloadDialogViewModel", "Cached VideoInfo for: $url")

                            mSelectionStateFlow.update {
                                SelectionState.FormatSelection(
                                    info = normalizedInfo,
                                    audioOnly = audioOnly,
                                    preferences = preferences,
                                    playlistResult = pendingPlaylistResult,
                                    selectedIndices = pendingSelectedIndices,
                                )
                            }
                        }
                    }.onFailure { th ->
                        withContext(Dispatchers.Main) {
                            mSelectionStateFlow.update { SelectionState.Idle }
                            mSheetStateFlow.update { SheetState.Error(action, throwable = th) }
                        }
                    }
                }

            mSheetStateFlow.update { SheetState.Loading(taskKey = taskKey, job = job) }
        }

        private fun downloadWithPreset(
            urlList: List<String>,
            preferences: DownloadPreferences,
        ) {
            val invalid = urlList.firstOrNull { !isValidHttpUrl(it) }
            if (invalid != null) {
                mSelectionStateFlow.update { SelectionState.Idle }
                mSheetStateFlow.update {
                    SheetState.Error(
                        action = Action.DownloadWithPreset(urlList = urlList, preferences = preferences),
                        throwable = IllegalArgumentException("Invalid URL"),
                    )
                }
                return
            }

            val action = Action.DownloadWithPreset(urlList = urlList, preferences = preferences)
            if (!checkPreflightOrError(action)) return

            urlList.forEach { downloader.enqueue(Task(url = it, preferences = preferences)) }
            hideDialog()
        }

        private fun runCommand(
            url: String,
            template: CommandTemplate,
            preferences: DownloadPreferences,
        ) {
            if (!isValidHttpUrl(url)) {
                mSelectionStateFlow.update { SelectionState.Idle }
                mSheetStateFlow.update {
                    SheetState.Error(
                        action = Action.RunCommand(url = url, template = template, preferences = preferences),
                        throwable = IllegalArgumentException("Invalid URL"),
                    )
                }
                return
            }

            val action = Action.RunCommand(url = url, template = template, preferences = preferences)
            if (!checkPreflightOrError(action)) return
            val task =
                Task(
                    url = url,
                    type = Task.TypeInfo.CustomCommand(template = template),
                    preferences = preferences,
                )
            downloader.enqueue(task)
        }

        private fun hideDialog() {
            mSheetValueFlow.update { SheetValue.Hidden }
            if (sheetState is SheetState.Loading) {
                cancel()
            }
        }

        private fun showDialog(action: Action.ShowSheet) {
            val urlList = action.urlList
            if (!urlList.isNullOrEmpty()) {
                mSheetStateFlow.update { SheetState.Configure(urlList) }
            } else {
                mSheetStateFlow.update { SheetState.InputUrl }
            }
            mSheetValueFlow.update { SheetValue.Expanded }
        }

        private fun cancel(): Boolean {
            return when (val state = sheetState) {
                is SheetState.Loading -> {
                    val res = YoutubeDL.destroyProcessById(id = state.taskKey)
                    if (res) {
                        state.job.cancel()
                    }
                    res
                }
                else -> false
            }
        }

        private fun resetSelectionState() {
            mSelectionStateFlow.update { SelectionState.Idle }
            mSheetStateFlow.update { SheetState.InputUrl }
        }
    }
