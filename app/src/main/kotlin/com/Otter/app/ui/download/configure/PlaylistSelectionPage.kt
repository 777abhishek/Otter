package com.Otter.app.ui.download.configure

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.Otter.app.data.download.DownloadPreferences
import com.Otter.app.data.download.PlaylistEntry
import com.Otter.app.data.download.PlaylistResult
import com.Otter.app.ui.components.ContainedExpressiveIndicator
import com.Otter.app.ui.download.DownloadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSelectionPageRoute(
    url: String,
    onDismissRequest: () -> Unit,
    dialogViewModel: DownloadDialogViewModel = hiltViewModel(),
    downloadViewModel: DownloadViewModel = hiltViewModel(),
) {
    val sheetState by dialogViewModel.sheetStateFlow.collectAsState()
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

    LaunchedEffect(url) {
        if (url.isNotBlank()) {
            dialogViewModel.postAction(
                DownloadDialogViewModel.Action.FetchPlaylist(
                    url = url,
                    preferences = DownloadPreferences.EMPTY,
                ),
            )
        }
    }

    LaunchedEffect(selectionState) {
        if (selectionState is DownloadDialogViewModel.SelectionState.Loading ||
            selectionState is DownloadDialogViewModel.SelectionState.FormatSelection
        ) {
            onDismissRequest()
        }
    }

    when (val s = selectionState) {
        is DownloadDialogViewModel.SelectionState.PlaylistSelection -> {
            PlaylistSelectionPageContent(
                result = s.result,
                onDismissRequest = {
                    dialogViewModel.postAction(DownloadDialogViewModel.Action.Reset)
                    onDismissRequest()
                },
                onDownloadSelected = { selectedIndices ->
                    if (selectedIndices.isEmpty()) return@PlaylistSelectionPageContent

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

        else -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Select videos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    dialogViewModel.postAction(DownloadDialogViewModel.Action.Reset)
                                    onDismissRequest()
                                },
                            ) {
                                Icon(Icons.Outlined.Close, "Close")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                    )
                },
                contentWindowInsets = WindowInsets(0),
            ) { padding ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                ) {
                    when (val st = sheetState) {
                        is DownloadDialogViewModel.SheetState.Error -> {
                            val message =
                                st.throwable.message?.takeIf { it.isNotBlank() }
                                    ?: "Failed to load playlist"
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = "Error",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            dialogViewModel.postAction(DownloadDialogViewModel.Action.Reset)
                                            onDismissRequest()
                                        },
                                    ) {
                                        Text("Close")
                                    }
                                    Button(
                                        onClick = {
                                            dialogViewModel.postAction(
                                                DownloadDialogViewModel.Action.FetchPlaylist(
                                                    url = url,
                                                    preferences = DownloadPreferences.EMPTY,
                                                ),
                                            )
                                        },
                                    ) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }

                        is DownloadDialogViewModel.SheetState.Loading -> {
                            ContainedExpressiveIndicator(
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }

                        else -> {
                            ContainedExpressiveIndicator(
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSelectionSheet(
    url: String,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    dialogViewModel: DownloadDialogViewModel = hiltViewModel(),
) {
    val selectionState by dialogViewModel.selectionStateFlow.collectAsState()

    LaunchedEffect(url) {
        if (url.isNotBlank()) {
            dialogViewModel.postAction(
                DownloadDialogViewModel.Action.FetchPlaylist(
                    url = url,
                    preferences = DownloadPreferences.EMPTY,
                ),
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        when (val s = selectionState) {
            is DownloadDialogViewModel.SelectionState.PlaylistSelection -> {
                PlaylistSelectionPageContent(
                    result = s.result,
                    onDismissRequest = onDismissRequest,
                    onDownloadSelected = { selectedIndices ->
                        if (selectedIndices.isEmpty()) return@PlaylistSelectionPageContent

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

            else -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ContainedExpressiveIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaylistSelectionPageContent(
    result: PlaylistResult,
    onDismissRequest: () -> Unit,
    onDownloadSelected: (List<Int>) -> Unit,
) {
    var selectedItems by remember { mutableStateOf<List<Int>>(emptyList()) }
    val allSelected = selectedItems.size == result.entryCount && result.entryCount > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = result.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${result.entryCount} videos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Outlined.Close, "Close")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            selectedItems = if (allSelected) emptyList() else (1..result.entryCount).toList()
                        },
                    ) {
                        Text(if (allSelected) "Deselect All" else "Select All")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${selectedItems.size} selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { onDownloadSelected(selectedItems) },
                        enabled = selectedItems.isNotEmpty(),
                    ) {
                        Text("Download")
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(result.entries ?: emptyList()) { entry ->
                val isSelected = entry.index in selectedItems
                PlaylistEntryItem(
                    entry = entry,
                    isSelected = isSelected,
                    onClick = {
                        selectedItems =
                            if (isSelected) {
                                selectedItems - entry.index
                            } else {
                                selectedItems + entry.index
                            }
                    },
                )
            }
        }
    }
}

@Composable
private fun PlaylistEntryItem(
    entry: PlaylistEntry,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        tonalElevation = if (isSelected) 2.dp else 0.dp,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "${entry.index}. ${entry.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 2,
                )
                if (entry.uploader.isNotBlank()) {
                    Text(
                        text = entry.uploader,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            if (entry.duration > 0) {
                Text(
                    text = formatDuration(entry.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatDuration(seconds: Double): String {
    val mins = (seconds / 60).toInt()
    val secs = (seconds % 60).toInt()
    return if (mins > 0) {
        String.format("%d:%02d", mins, secs)
    } else {
        "${secs}s"
    }
}
