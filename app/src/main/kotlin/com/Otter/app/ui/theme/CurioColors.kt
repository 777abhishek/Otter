package com.Otter.app.ui.theme

import androidx.compose.ui.graphics.Color

// Premade seed colors from Seal app
// Generated using HCT color space with varying hues
object OtterColors {
    // Color palette - 10 preset colors
    val Color0 = Color(0xFF9C4146) // Red
    val Color1 = Color(0xFF9C5D41) // Orange-Red
    val Color2 = Color(0xFF9C7A41) // Orange
    val Color3 = Color(0xFF9C9641) // Yellow
    val Color4 = Color(0xFF829C41) // Yellow-Green
    val Color5 = Color(0xFF669C41) // Green
    val Color6 = Color(0xFF419C46) // Green-Cyan
    val Color7 = Color(0xFF419C62) // Cyan-Green
    val Color8 = Color(0xFF419C7E) // Cyan
    val Color9 = Color(0xFF419C9A) // Cyan-Blue

    // All preset colors as a list
    val PresetColors =
        listOf(
            Color0,
            Color1,
            Color2,
            Color3,
            Color4,
            Color5,
            Color6,
            Color7,
            Color8,
            Color9,
        )

    // Named colors for specific uses
    val DefaultSeed = Color(0xFF6750A4) // Purple (Material You default)
    val Dynamic = Color(0x00000000) // Special value for dynamic colors
    val Monochrome = Color(0xFF000000) // Black for monochrome theme
}
