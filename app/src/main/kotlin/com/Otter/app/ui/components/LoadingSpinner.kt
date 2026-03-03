package com.Otter.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Modern Expressive Loading Indicator using Material 3 Expressive APIs.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    LoadingIndicator(
        modifier = modifier.size(40.dp),
        color = color,
        polygons =
            listOf(
                MaterialShapes.Pill,
                MaterialShapes.Sunny,
                MaterialShapes.SoftBurst,
                MaterialShapes.Cookie7Sided,
                MaterialShapes.Oval,
            ),
    )
}

/**
 * A contained version with a fully rounded shape.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContainedExpressiveIndicator(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    indicatorColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    ContainedLoadingIndicator(
        modifier = modifier.size(40.dp),
        // Use CircleShape for fully rounded container
        containerShape = CircleShape,
        containerColor = containerColor,
        indicatorColor = indicatorColor,
    )
}

/**
 * Full screen variant.
 */
@Composable
fun FullScreenLoadingSpinner(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        ExpressiveLoadingIndicator(color = color)
    }
}

/**
 * Smaller version for inline loading.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SmallLoadingSpinner(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    LoadingIndicator(
        modifier = modifier.size(24.dp),
        color = color,
    )
}
