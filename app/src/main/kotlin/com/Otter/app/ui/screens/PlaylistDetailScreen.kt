package com.Otter.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Otter.app.DiagnosticsEntryPoint
import com.Otter.app.data.download.DownloadPreferences
import com.Otter.app.data.download.VideoInfo
import com.Otter.app.data.models.Video
import com.Otter.app.data.sync.SubscriptionSyncService
import com.Otter.app.player.PlayerConnectionManager
import com.Otter.app.ui.components.ContainedExpressiveIndicator
import com.Otter.app.ui.components.Material3ExpressiveSettingsGroup
import com.Otter.app.ui.components.SearchBar
import com.Otter.app.ui.components.VideoCardItem
import com.Otter.app.ui.download.DownloadViewModel
import com.Otter.app.ui.download.configure.ConfigureFormatsSheet
import com.Otter.app.ui.download.configure.DownloadDialogViewModel
import com.Otter.app.ui.download.configure.PlaylistSelectionPageContent
import com.Otter.app.ui.viewmodels.PlayerViewModel
import com.Otter.app.ui.viewmodels.PlaylistUiState
import com.Otter.app.ui.viewmodels.PlaylistViewModel
import com.Otter.app.ui.viewmodels.SyncViewModel
import dagger.hilt.android.EntryPointAccessors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    navController: NavController,
    viewModel: PlaylistViewModel = hiltViewModel(),
    syncViewModel: SyncViewModel = hiltViewModel(),
    downloadViewModel: DownloadViewModel = hiltViewModel(),
    dialogViewModel: DownloadDialogViewModel = hiltViewModel(),
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
    onShowPlaylistSheet: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val syncState by syncViewModel.syncState.collectAsState()

    val playerViewModel: PlayerViewModel = hiltViewModel()

    val context = LocalContext.current

    val entryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            DiagnosticsEntryPoint::class.java,
        )
    val playerConnectionManager = entryPoint.playerConnectionManager()

    var searchQuery by rememberSaveable { mutableStateOf("") }

    fun Video.toQueueItem(): com.Otter.app.player.QueueItem {
        return com.Otter.app.player.QueueItem(
            videoId = this.id,
            title = this.title,
            thumbnailUrl = this.thumbnail,
            duration = this.duration.toLong() * 1000L,
            uploaderName = this.channelName,
        )
    }

    val pullState = rememberPullToRefreshState()
    val isSyncing = syncState is SubscriptionSyncService.SyncState.Syncing
    val scrollState = rememberLazyListState()

    LaunchedEffect(scrollState.firstVisibleItemIndex, scrollState.lastScrolledBackward) {
        onBottomBarVisibilityChanged(
            scrollState.firstVisibleItemIndex == 0 || scrollState.lastScrolledBackward,
        )
    }

    // Defer data loading until after transition animation completes
    var isTransitionComplete by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Wait for transition animation to complete (~300ms)
        kotlinx.coroutines.delay(300)
        isTransitionComplete = true
    }
    
    // Only load data after transition completes
    LaunchedEffect(playlistId, isTransitionComplete) {
        if (isTransitionComplete) {
            viewModel.loadPlaylist(playlistId)
        }
    }

    val playlistTitle = (uiState as? PlaylistUiState.Success)?.playlist?.title ?: "Playlist"

    val videos =
        when (val state = uiState) {
            is PlaylistUiState.Success -> state.videos
            else -> emptyList()
        }

    val filteredVideos by remember(videos, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                videos
            } else {
                videos.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                        it.channelName.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }
    val isLoadingState = uiState is PlaylistUiState.Loading
    val isError = uiState is PlaylistUiState.Error

    val showLoadingIndicator = !isTransitionComplete || (isLoadingState && !isSyncing)

    var showEmptyState by remember { mutableStateOf(false) }
    LaunchedEffect(filteredVideos, isSyncing, isTransitionComplete) {
        // Show empty state only when we have confirmed no data and not loading
        if (filteredVideos.isEmpty() && !isSyncing && isTransitionComplete && !isLoadingState) {
            showEmptyState = true
        } else {
            showEmptyState = false
        }
    }

    var preferences by remember { mutableStateOf(DownloadPreferences.createFromPreferences()) }
    val selectionState by dialogViewModel.selectionStateFlow.collectAsState()
    val formatSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFormatSheet by remember { mutableStateOf(false) }

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
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            when {
                // Error state
                isError -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                            val message = (uiState as? PlaylistUiState.Error)?.message ?: "Unknown error"
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Button(onClick = { viewModel.loadPlaylist(playlistId) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = false,
                        onRefresh = {
                            when (playlistId.uppercase()) {
                                "WL" -> syncViewModel.syncWatchLaterVideos()
                                "LL" -> syncViewModel.syncLikedVideos()
                                else -> syncViewModel.syncPlaylist(playlistId)
                            }
                        },
                        state = pullState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            state = scrollState,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            PlaylistHeaderSection(
                                playlistTitle = playlistTitle,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                            )

                            PlaylistActionsSection(
                                playlistId = playlistId,
                                playlistTitle = playlistTitle,
                                videos = videos,
                                navController = navController,
                                playerConnectionManager = playerConnectionManager,
                                playerViewModel = playerViewModel,
                                onDownloadAll = {
                                    val url = "https://www.youtube.com/playlist?list=$playlistId"
                                    onShowPlaylistSheet(url)
                                },
                            )

                            if (showEmptyState) {
                                item(key = "empty") {
                                    Box(
                                        modifier = Modifier.fillParentMaxSize(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.VideoLibrary,
                                                contentDescription = null,
                                                modifier = Modifier.size(64.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                text =
                                                    if (searchQuery.isNotEmpty()) {
                                                        "No videos found for \"$searchQuery\""
                                                    } else {
                                                        "No videos in playlist"
                                                    },
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                            )
                                            if (searchQuery.isEmpty()) {
                                                Text(
                                                    text = "Pull down to sync",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(
                                    items = filteredVideos,
                                    key = { it.id },
                                ) { video ->
                                    VideoCardItem(
                                        video = video,
                                        onClick = {
                                            val index = videos.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
                                            playerViewModel.setQueue(videos, startIndex = index)
                                            playerConnectionManager.setAudioOnly(false)

                                            // Send full queue to service
                                            playerConnectionManager.play(
                                                videoId = video.id,
                                                title = video.title,
                                                thumbnailUrl = video.thumbnail,
                                                duration = video.duration.toLong() * 1000,
                                                uploaderName = video.channelName,
                                                audioOnly = false,
                                                queue = videos.map { it.toQueueItem() },
                                                queuePosition = index,
                                            )
                                            navController.navigate("player/${video.id}")
                                        },
                                        onDownload = {
                                            val url = "https://www.youtube.com/watch?v=${video.id}"
                                            dialogViewModel.postAction(
                                                DownloadDialogViewModel.Action.FetchFormats(
                                                    url = url,
                                                    audioOnly = false,
                                                    preferences = preferences,
                                                ),
                                            )
                                        },
                                        onPlayAsAudio = {
                                            val index = videos.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
                                            playerViewModel.setQueue(videos, startIndex = index)
                                            playerConnectionManager.setAudioOnly(true)

                                            // Send full queue to service
                                            playerConnectionManager.play(
                                                videoId = video.id,
                                                title = video.title,
                                                thumbnailUrl = video.thumbnail,
                                                duration = video.duration.toLong() * 1000,
                                                uploaderName = video.channelName,
                                                audioOnly = true,
                                                queue = videos.map { it.toQueueItem() },
                                                queuePosition = index,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isSyncing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    ContainedExpressiveIndicator(modifier = Modifier.size(40.dp))
                }
            }

            when (val s = selectionState) {
                is DownloadDialogViewModel.SelectionState.Loading ->
                    if (showFormatSheet) {
                        ConfigureFormatsSheet(
                            sheetState = formatSheetState,
                            info = VideoInfo(),
                            basePreferences = s.preferences,
                            downloader = downloadViewModel.downloader,
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
                            downloader = downloadViewModel.downloader,
                            onDismissRequest = { dialogViewModel.postAction(DownloadDialogViewModel.Action.Reset) },
                            isLoading = false,
                            playlistResult = s.playlistResult,
                            selectedIndices = s.selectedIndices,
                        )
                    }

                is DownloadDialogViewModel.SelectionState.PlaylistSelection -> {
                    PlaylistSelectionPageContent(
                        result = s.result,
                        onDismissRequest = { dialogViewModel.postAction(DownloadDialogViewModel.Action.Reset) },
                        onDownloadSelected = { selectedIndices ->
                            if (selectedIndices.isEmpty()) {
                                dialogViewModel.postAction(DownloadDialogViewModel.Action.Reset)
                                return@PlaylistSelectionPageContent
                            }
                            // Store selected indices and fetch formats for the first selected item
                            dialogViewModel.setPendingPlaylistInfo(s.result, selectedIndices)
                            val firstIndex = selectedIndices.first()
                            val firstEntry = s.result.entries?.getOrNull(firstIndex - 1)
                            val firstItemUrl = firstEntry?.url ?: s.result.webpageUrl
                            dialogViewModel.postAction(
                                DownloadDialogViewModel.Action.FetchFormats(
                                    url = firstItemUrl,
                                    audioOnly = false,
                                    preferences = s.preferences.copy(downloadPlaylist = true),
                                ),
                            )
                        },
                    )
                }

                is DownloadDialogViewModel.SelectionState.Idle -> {
                    if (showFormatSheet) {
                        showFormatSheet = false
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.PlaylistHeaderSection(
    playlistTitle: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
) {
    item {
        Text(
            text = playlistTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
    item {
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            placeholder = "Search in playlist...",
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.PlaylistActionsSection(
    playlistId: String,
    playlistTitle: String,
    videos: List<Video>,
    navController: NavController,
    playerConnectionManager: PlayerConnectionManager,
    playerViewModel: PlayerViewModel,
    onDownloadAll: () -> Unit,
) {
    item {
        val context = LocalContext.current

        Box {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(
                    onClick = {
                        val first = videos.firstOrNull()
                        if (first != null) {
                            val index = 0
                            playerViewModel.setQueue(videos, startIndex = index)
                            playerConnectionManager.setAudioOnly(false)

                            // Send full queue to service
                            playerConnectionManager.play(
                                videoId = first.id,
                                title = first.title,
                                thumbnailUrl = first.thumbnail,
                                duration = first.duration.toLong() * 1000,
                                uploaderName = first.channelName,
                                queue = videos.map { it.toQueueItem() },
                                queuePosition = index,
                            )
                            navController.navigate("player/${first.id}")
                        } else {
                            Toast.makeText(context, "No videos", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play All",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Play",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val first = videos.firstOrNull()
                        if (first != null) {
                            val index = 0
                            playerViewModel.setQueue(videos, startIndex = index)
                            playerConnectionManager.setAudioOnly(true)

                            // Send full queue to service
                            playerConnectionManager.play(
                                videoId = first.id,
                                title = first.title,
                                thumbnailUrl = first.thumbnail,
                                duration = first.duration.toLong() * 1000,
                                uploaderName = first.channelName,
                                queue = videos.map { it.toQueueItem() },
                                queuePosition = index,
                            )
                        } else {
                            Toast.makeText(context, "No videos", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = "Play as Audio",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Audio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                IconButton(
                    onClick = onDownloadAll,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = "Download",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text =
                                if (playlistId.equals("WL", ignoreCase = true) || playlistId.equals("LL", ignoreCase = true)) {
                                    "Download"
                                } else {
                                    "Download All "
                                },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val shareContent = "$playlistTitle\n\nhttps://www.youtube.com/playlist?list=$playlistId"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareContent)
                            putExtra(Intent.EXTRA_SUBJECT, playlistTitle)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share playlist"))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Share",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
