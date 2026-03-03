package com.Otter.app.ui.screens.player

import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@Composable
@UnstableApi
fun VideoPlayerView(
    player: Player?,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    onTap: () -> Unit = {},
) {
    val context = LocalContext.current
    
    // Remember the PlayerView to avoid recreation on recomposition
    val playerView = remember { PlayerView(context) }
    
    // Handle player lifecycle properly
    DisposableEffect(player) {
        playerView.player = player
        playerView.useController = false
        playerView.resizeMode = resizeMode
        playerView.setShutterBackgroundColor(Color.BLACK)
        playerView.setBackgroundColor(Color.BLACK)
        
        // Keep screen on during playback
        playerView.keepScreenOn = true
        
        // Properly handle subtitle view visibility
        playerView.subtitleView?.let { subtitleView ->
            subtitleView.setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
            subtitleView.setApplyEmbeddedStyles(true)
            subtitleView.setApplyEmbeddedFontSizes(true)
        }
        
        onDispose {
            // Clean up when leaving composition
            playerView.player = null
            playerView.keepScreenOn = false
        }
    }
    
    // Update resize mode when it changes
    DisposableEffect(resizeMode) {
        playerView.resizeMode = resizeMode
        onDispose { }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { playerView },
            modifier =
                Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
            update = { view ->
                // Ensure player is attached
                if (view.player != player) {
                    view.player = player
                }
                view.resizeMode = resizeMode
            },
        )
    }
}
