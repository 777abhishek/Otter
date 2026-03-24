package com.Otter.app.ui.screens.player

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Black
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
    
    Box(
        modifier = modifier
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    setShutterBackgroundColor(Color.TRANSPARENT)
                    setBackgroundColor(Color.TRANSPARENT)
                    setFitsSystemWindows(false)
                    keepScreenOn = true
                    this.resizeMode = resizeMode
                    
                    // Force transparent background on all child views (AspectRatioFrameLayout, etc.)
                    post {
                        for (i in 0 until childCount) {
                            getChildAt(i)?.setBackgroundColor(Color.TRANSPARENT)
                            getChildAt(i)?.fitsSystemWindows = false
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            update = { view ->
                // Always update player and resize mode to ensure proper attachment
                if (view.isAttachedToWindow) {
                    view.player = player
                } else {
                    view.post { view.player = player }
                }
                view.resizeMode = resizeMode
                
                // Re-apply transparent backgrounds on update
                view.setShutterBackgroundColor(Color.TRANSPARENT)
                view.setBackgroundColor(Color.TRANSPARENT)
                
                // Properly handle subtitle view visibility
                view.subtitleView?.let { subtitleView ->
                    subtitleView.setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
                    subtitleView.setApplyEmbeddedStyles(true)
                    subtitleView.setApplyEmbeddedFontSizes(true)
                }
            },
        )
    }
}
