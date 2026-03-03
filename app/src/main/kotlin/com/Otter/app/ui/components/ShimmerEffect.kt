package com.Otter.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun Modifier.shimmerEffect(
    baseColor: Color,
    highlightColor: Color,
): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX =
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = 1200,
                            easing = LinearEasing,
                        ),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "shimmer_translation",
        )

    return this.then(
        Modifier.background(
            brush =
                Brush.linearGradient(
                    colors =
                        listOf(
                            baseColor,
                            highlightColor,
                            baseColor,
                        ),
                    start = Offset.Zero,
                    end = Offset(x = translateX.value, y = 0f),
                ),
        ),
    )
}
