package com.Otter.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Otter.app.DiagnosticsEntryPoint
import com.Otter.app.data.models.Playlist
import com.Otter.app.data.sync.SubscriptionSyncService
import com.Otter.app.ui.components.*
import com.Otter.app.ui.download.DownloadViewModel
import com.Otter.app.ui.download.configure.DownloadDialogViewModel
import com.Otter.app.ui.viewmodels.PlayerViewModel
import com.Otter.app.ui.viewmodels.SyncViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    syncViewModel: SyncViewModel = hiltViewModel(),
    downloadViewModel: DownloadViewModel = hiltViewModel(),
    dialogViewModel: DownloadDialogViewModel = hiltViewModel(),
    onShowPlaylistSheet: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val entryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            DiagnosticsEntryPoint::class.java,
        )
    val playerConnectionManager = entryPoint.playerConnectionManager()

    val playerViewModel: PlayerViewModel = hiltViewModel()

    var searchQuery by rememberSaveable { mutableStateOf("") }

    val playlists by syncViewModel.playlists.collectAsState()
    val watchLater by syncViewModel.watchLater.collectAsState()
    val likedVideos by syncViewModel.likedVideos.collectAsState()
    val syncState by syncViewModel.syncState.collectAsState()
    val isRefreshing by syncViewModel.isRefreshing.collectAsState()

    val pullState = rememberPullToRefreshState()
    val isSyncing = syncState is SubscriptionSyncService.SyncState.Syncing

    val coroutineScope = rememberCoroutineScope()

    fun com.Otter.app.data.models.Video.toQueueItem(): com.Otter.app.player.QueueItem {
        return com.Otter.app.player.QueueItem(
            videoId = this.id,
            title = this.title,
            thumbnailUrl = this.thumbnail,
            duration = this.duration.toLong() * 1000L,
            uploaderName = this.channelName,
        )
    }

    val homePlaylists by remember(playlists, watchLater, likedVideos) {
        derivedStateOf {
            val special =
                listOfNotNull(
                    Playlist(
                        id = "WL",
                        title = "Watch Later",
                        thumbnail = watchLater.firstOrNull()?.thumbnail ?: "",
                        videoCount = watchLater.size,
                        videos = emptyList(),
                    ).takeIf { watchLater.isNotEmpty() },
                    Playlist(
                        id = "LL",
                        title = "Liked Videos",
                        thumbnail = likedVideos.firstOrNull()?.thumbnail ?: "",
                        videoCount = likedVideos.size,
                        videos = emptyList(),
                    ).takeIf { likedVideos.isNotEmpty() },
                )
            (special + playlists).distinctBy { it.id }
        }
    }

    val filteredPlaylists by remember(homePlaylists, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                homePlaylists
            } else {
                homePlaylists.filter { it.title.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    var showEmptyState by remember { mutableStateOf(false) }
    LaunchedEffect(homePlaylists, isRefreshing, isSyncing) {
        // Show empty state only when we have confirmed no data and not loading
        if (homePlaylists.isEmpty() && !isRefreshing && !isSyncing) {
            showEmptyState = true
        } else {
            showEmptyState = false
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
    ) { scaffoldPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding),
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing && !isSyncing,
                onRefresh = { syncViewModel.syncAll() },
                state = pullState,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier =
                        modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item {
                        Text(
                            text = "Playlists",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }

                    item {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = "Search playlists...",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (showEmptyState) {
                        item {
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
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "No playlists found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "Pull down to sync",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = filteredPlaylists,
                            key = { it.id },
                        ) { playlist ->
                            PlaylistCardItem(
                                playlist = playlist,
                                onClick = {
                                    navController.navigate("playlist/${playlist.id}")
                                },
                                onPlay = {
                                    val playlistId = playlist.id
                                    val queueVideos =
                                        when (playlistId.uppercase()) {
                                            "WL" -> watchLater
                                            "LL" -> likedVideos
                                            else -> emptyList()
                                        }

                                    if (queueVideos.isNotEmpty()) {
                                        val first = queueVideos.firstOrNull() ?: return@PlaylistCardItem
                                        playerViewModel.setQueue(queueVideos, startIndex = 0)
                                        playerConnectionManager.setAudioOnly(false)
                                        playerConnectionManager.play(
                                            videoId = first.id,
                                            title = first.title,
                                            thumbnailUrl = first.thumbnail,
                                            duration = first.duration.toLong() * 1000L,
                                            uploaderName = first.channelName,
                                            audioOnly = false,
                                            queue = queueVideos.map { it.toQueueItem() },
                                            queuePosition = 0,
                                        )
                                        navController.navigate("player/${first.id}")
                                    } else {
                                        coroutineScope.launch {
                                            val loaded = syncViewModel.getPlaylistVideos(playlistId).first()
                                            val first = loaded.firstOrNull() ?: return@launch
                                            playerViewModel.setQueue(loaded, startIndex = 0)
                                            playerConnectionManager.setAudioOnly(false)
                                            playerConnectionManager.play(
                                                videoId = first.id,
                                                title = first.title,
                                                thumbnailUrl = first.thumbnail,
                                                duration = first.duration.toLong() * 1000L,
                                                uploaderName = first.channelName,
                                                audioOnly = false,
                                                queue = loaded.map { it.toQueueItem() },
                                                queuePosition = 0,
                                            )
                                            navController.navigate("player/${first.id}")
                                        }
                                    }
                                },
                                onDownloadAll = {
                                    val url = "https://www.youtube.com/playlist?list=${playlist.id}"
                                    onShowPlaylistSheet(url)
                                },
                                onPlayAsAudio = {
                                    val playlistId = playlist.id
                                    val queueVideos =
                                        when (playlistId.uppercase()) {
                                            "WL" -> watchLater
                                            "LL" -> likedVideos
                                            else -> emptyList()
                                        }

                                    if (queueVideos.isNotEmpty()) {
                                        val first = queueVideos.firstOrNull() ?: return@PlaylistCardItem
                                        playerViewModel.setQueue(queueVideos, startIndex = 0)
                                        playerConnectionManager.setAudioOnly(true)
                                        playerConnectionManager.play(
                                            videoId = first.id,
                                            title = first.title,
                                            thumbnailUrl = first.thumbnail,
                                            duration = first.duration.toLong() * 1000L,
                                            uploaderName = first.channelName,
                                            audioOnly = true,
                                            queue = queueVideos.map { it.toQueueItem() },
                                            queuePosition = 0,
                                        )
                                        // Don't navigate to PlayerScreen for audio-only - mini player handles it
                                    } else {
                                        coroutineScope.launch {
                                            val loaded = syncViewModel.getPlaylistVideos(playlistId).first()
                                            val first = loaded.firstOrNull() ?: return@launch
                                            playerViewModel.setQueue(loaded, startIndex = 0)
                                            playerConnectionManager.setAudioOnly(true)
                                            playerConnectionManager.play(
                                                videoId = first.id,
                                                title = first.title,
                                                thumbnailUrl = first.thumbnail,
                                                duration = first.duration.toLong() * 1000L,
                                                uploaderName = first.channelName,
                                                audioOnly = true,
                                                queue = loaded.map { it.toQueueItem() },
                                                queuePosition = 0,
                                            )
                                            // Don't navigate to PlayerScreen for audio-only - mini player handles it
                                        }
                                    }
                                },
                            )
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
        }
    }
}
