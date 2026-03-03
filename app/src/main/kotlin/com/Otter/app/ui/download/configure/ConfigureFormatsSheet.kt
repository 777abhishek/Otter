package com.Otter.app.ui.download.configure

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.Otter.app.data.download.DownloadPreferences
import com.Otter.app.data.download.Downloader
import com.Otter.app.data.download.PlaylistResult
import com.Otter.app.data.download.VideoInfo
import com.Otter.app.ui.components.ContainedExpressiveIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigureFormatsSheet(
    sheetState: SheetState,
    info: VideoInfo,
    basePreferences: DownloadPreferences,
    downloader: Downloader,
    onDismissRequest: () -> Unit,
    isLoading: Boolean,
    playlistResult: PlaylistResult? = null,
    selectedIndices: List<Int> = emptyList(),
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0) },
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                ContainedExpressiveIndicator()
            }
        } else {
            // Delegate to the Seal-like FormatPage UI
            FormatPage(
                modifier = Modifier.padding(PaddingValues(bottom = 16.dp)),
                videoInfo = info,
                downloader = downloader,
                onNavigateBack = onDismissRequest,
                playlistResult = playlistResult,
                selectedIndices = selectedIndices,
            )
        }
    }
}
