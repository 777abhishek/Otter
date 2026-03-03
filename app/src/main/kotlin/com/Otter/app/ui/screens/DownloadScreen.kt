package com.Otter.app.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Otter.app.data.download.DownloadPreferences
import com.Otter.app.data.download.Task
import com.Otter.app.data.download.VideoInfo
import com.Otter.app.ui.components.ButtonOption
import com.Otter.app.ui.components.ConnectedButtonGroup
import com.Otter.app.ui.download.DownloadViewModel
import com.Otter.app.ui.download.TaskCard
import com.Otter.app.ui.download.configure.ConfigureFormatsSheet
import com.Otter.app.ui.download.configure.DownloadDialogViewModel
import com.Otter.app.ui.download.configure.DownloadErrorSnackbar
import com.Otter.app.ui.download.configure.PlaylistSelectionSheet

private enum class DownloadFilter { All, Downloading, Canceled, Finished }

private fun normalizeHttpUrl(input: String): String {
    val raw = input.trim().replace("\n", "").replace("\r", "")
    if (raw.isBlank()) return ""
    val lower = raw.lowercase()
    if (lower.startsWith("http://") || lower.startsWith("https://")) return raw
    if (lower.startsWith("www.")) return "https://$raw"
    if (lower.contains(".") && !lower.contains(" ") && !lower.contains("://")) return "https://$raw"
    return raw
}

private fun isValidHttpUrl(input: String): Boolean {
    val u = input.trim().lowercase()
    return u.startsWith("http://") || u.startsWith("https://")
}

@Composable
private fun rememberIsOnline(): Boolean {
    val context = LocalContext.current
    val cm = remember(context) {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    fun currentOnline(): Boolean {
        val c = cm ?: return false
        val network = c.activeNetwork ?: return false
        val caps = c.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    var online by remember { mutableStateOf(currentOnline()) }

    DisposableEffect(cm) {
        val c = cm
        if (c == null) return@DisposableEffect onDispose { }

        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    online = currentOnline()
                }

                override fun onLost(network: Network) {
                    online = currentOnline()
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    online = currentOnline()
                }
            }

        runCatching { c.registerDefaultNetworkCallback(callback) }
        online = currentOnline()

        onDispose {
            runCatching { c.unregisterNetworkCallback(callback) }
        }
    }

    return online
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    navController: NavController,
    viewModel: DownloadViewModel = hiltViewModel(),
    dialogViewModel: DownloadDialogViewModel = hiltViewModel(),
) {
    val tasks = viewModel.taskStateMap

    var urlInput by remember { mutableStateOf("") }
    val normalizedUrlInput = remember(urlInput) { normalizeHttpUrl(urlInput) }
    val showUrlInvalid = normalizedUrlInput.isNotBlank() && !isValidHttpUrl(normalizedUrlInput)
    val isOnline = rememberIsOnline()
    val showOffline = normalizedUrlInput.isNotBlank() && !isOnline
    val clipboard = LocalClipboardManager.current

    var selectedFilter by remember { mutableStateOf(DownloadFilter.All) }
    val filterOptions =
        remember {
            listOf(
                ButtonOption(DownloadFilter.All, "All"),
                ButtonOption(DownloadFilter.Downloading, "Downloading"),
                ButtonOption(DownloadFilter.Canceled, "Canceled"),
                ButtonOption(DownloadFilter.Finished, "Finished"),
            )
        }

    val filteredTasks by remember(tasks, selectedFilter) {
        derivedStateOf {
            tasks.toList().filter { (_, state) ->
                when (selectedFilter) {
                    DownloadFilter.All -> true
                    DownloadFilter.Downloading ->
                        state.downloadState is Task.DownloadState.FetchingInfo ||
                            state.downloadState is Task.DownloadState.Running ||
                            state.downloadState == Task.DownloadState.Idle ||
                            state.downloadState == Task.DownloadState.ReadyWithInfo
                    DownloadFilter.Canceled ->
                        state.downloadState is Task.DownloadState.Canceled || state.downloadState is Task.DownloadState.Error
                    DownloadFilter.Finished -> state.downloadState is Task.DownloadState.Completed
                }
            }.sortedBy { (_, state) -> state.downloadState }
        }
    }

    var preferences by remember { mutableStateOf(DownloadPreferences.createFromPreferences()) }
    val selectionState by dialogViewModel.selectionStateFlow.collectAsState()
    val sheetState by dialogViewModel.sheetStateFlow.collectAsState()

    val formatSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFormatSheet by remember { mutableStateOf(false) }

    var pendingPlaylistUrl by remember { mutableStateOf<String?>(null) }
    val playlistSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isFetching = selectionState is DownloadDialogViewModel.SelectionState.Loading

    LaunchedEffect(selectionState) {
        showFormatSheet =
            when (selectionState) {
                is DownloadDialogViewModel.SelectionState.Loading,
                is DownloadDialogViewModel.SelectionState.FormatSelection,
                -> true
                else -> false
            }
        if (!showFormatSheet) {
            formatSheetState.hide()
        }
        // Clear URL input when fetching starts
        if (selectionState is DownloadDialogViewModel.SelectionState.Loading) {
            urlInput = ""
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Downloads",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Paste link to download...") },
                    isError = showUrlInvalid || showOffline,
                    supportingText = {
                        when {
                            showOffline -> Text("No internet connection")
                            showUrlInvalid -> Text("Invalid URL")
                        }
                    },
                    trailingIcon = {
                        Row {
                            // Clear button - visible when there's text
                            if (urlInput.isNotBlank()) {
                                IconButton(onClick = { urlInput = "" }) {
                                    Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                                }
                            }
                            IconButton(onClick = {
                                val text = clipboard.getText()?.text.orEmpty()
                                if (text.isNotBlank()) urlInput = text
                            }) {
                                Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste")
                            }
                            IconButton(
                                onClick = {
                                    val u = normalizedUrlInput
                                    if (u.isNotBlank() && !showUrlInvalid && isOnline) {
                                        val lower = u.lowercase()
                                        val isPlaylistUrl =
                                            lower.contains("list=") &&
                                                (lower.contains("/playlist") || lower.contains("watch?") || lower.contains("youtu.be/"))
                                        if (isPlaylistUrl) {
                                            pendingPlaylistUrl = u
                                        } else {
                                            val currentPreferences = DownloadPreferences.createFromPreferences()
                                            preferences = currentPreferences
                                            dialogViewModel.postAction(
                                                DownloadDialogViewModel.Action.FetchFormats(
                                                    url = u,
                                                    audioOnly = false,
                                                    preferences = currentPreferences,
                                                ),
                                            )
                                        }
                                    }
                                },
                                enabled = normalizedUrlInput.isNotBlank() && !showUrlInvalid && isOnline,
                            ) {
                                Icon(Icons.Outlined.Download, contentDescription = "Download")
                            }
                        }
                    },
                )

                ConnectedButtonGroup(
                    options = filterOptions,
                    selectedValue = selectedFilter,
                    onSelectionChange = { selectedFilter = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxWidth = 360.dp,
                )

                if (filteredTasks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No downloads",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 96.dp),
                    ) {
                        items(
                            items = filteredTasks,
                            key = { (task, _) -> task.id },
                        ) { (task, state) ->
                            TaskCard(
                                task = task,
                                state = state,
                                onCancel = { viewModel.cancel(task) },
                                onRestart = { viewModel.restart(task) },
                                onRemove = { viewModel.remove(task) },
                            )
                        }
                    }
                }
            }

            if (sheetState is DownloadDialogViewModel.SheetState.Error) {
                val err = sheetState as DownloadDialogViewModel.SheetState.Error
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DownloadErrorSnackbar(
                        throwable = err.throwable,
                        onDismiss = { dialogViewModel.postAction(DownloadDialogViewModel.Action.Reset) },
                        onRetry = { dialogViewModel.postAction(err.action) },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }

            when (val s = selectionState) {
                is DownloadDialogViewModel.SelectionState.Loading ->
                    if (showFormatSheet) {
                        ConfigureFormatsSheet(
                            sheetState = formatSheetState,
                            info = VideoInfo(), // Empty info while loading
                            basePreferences = s.preferences,
                            downloader = viewModel.downloader,
                            onDismissRequest = { dialogViewModel.postAction(DownloadDialogViewModel.Action.Reset) },
                            isLoading = true,
                        )
                    }
                is DownloadDialogViewModel.SelectionState.FormatSelection ->
                    if (showFormatSheet) {
                        ConfigureFormatsSheet(
                            sheetState = formatSheetState,
                            info = s.info,
                            basePreferences = s.preferences,
                            downloader = viewModel.downloader,
                            onDismissRequest = { dialogViewModel.postAction(DownloadDialogViewModel.Action.Reset) },
                            isLoading = false,
                        )
                    }
                is DownloadDialogViewModel.SelectionState.PlaylistSelection -> {
                    // Playlist selection not supported in DownloadScreen
                }
                is DownloadDialogViewModel.SelectionState.Idle -> {
                    if (showFormatSheet) {
                        showFormatSheet = false
                    }
                }
                else -> {}
            }

            pendingPlaylistUrl?.let { url ->
                PlaylistSelectionSheet(
                    url = url,
                    sheetState = playlistSheetState,
                    onDismissRequest = {
                        pendingPlaylistUrl = null
                        dialogViewModel.postAction(DownloadDialogViewModel.Action.Reset)
                    },
                )
            }
        }
    }
}
