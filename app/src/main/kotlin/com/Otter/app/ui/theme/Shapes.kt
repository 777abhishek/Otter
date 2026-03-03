package com.Otter.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Expressive Shapes - For modern, playful UI
val ExpressiveShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp), // Tags, chips, small buttons
        small = RoundedCornerShape(12.dp), // Cards, text fields
        medium = RoundedCornerShape(16.dp), // Bottom sheets, dialogs
        large = RoundedCornerShape(24.dp), // FABs, large cards
        extraLarge = RoundedCornerShape(28.dp), // Full screen sheets
    )

// Default Shapes - Professional, subtle rounding
val DefaultShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp), // Tags, chips
        small = RoundedCornerShape(8.dp), // Cards, text fields
        medium = RoundedCornerShape(12.dp), // Bottom sheets, dialogs
        large = RoundedCornerShape(16.dp), // FABs, large cards
        extraLarge = RoundedCornerShape(20.dp), // Full screen sheets
    )
