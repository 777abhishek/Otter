package com.Otter.app.ui.common

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.kyant.monet.TonalPalettes

@Immutable
data class DarkTheme(
    val isDarkTheme: Boolean,
    val isHighContrastModeEnabled: Boolean,
)

val LocalDarkTheme = staticCompositionLocalOf { DarkTheme(isDarkTheme = false, isHighContrastModeEnabled = false) }

@Immutable
data class FixedColorRoles(
    val primaryFixed: Color,
    val primaryFixedDim: Color,
    val onPrimaryFixed: Color,
    val onPrimaryFixedVariant: Color,
    val secondaryFixed: Color,
    val secondaryFixedDim: Color,
    val onSecondaryFixed: Color,
    val onSecondaryFixedVariant: Color,
    val tertiaryFixed: Color,
    val tertiaryFixedDim: Color,
    val onTertiaryFixed: Color,
    val onTertiaryFixedVariant: Color,
) {
    companion object {
        fun fromTonalPalettes(palettes: TonalPalettes): FixedColorRoles {
            return with(palettes) {
                FixedColorRoles(
                    primaryFixed = accent1(90.0),
                    primaryFixedDim = accent1(80.0),
                    onPrimaryFixed = accent1(10.0),
                    onPrimaryFixedVariant = accent1(30.0),
                    secondaryFixed = accent2(90.0),
                    secondaryFixedDim = accent2(80.0),
                    onSecondaryFixed = accent2(10.0),
                    onSecondaryFixedVariant = accent2(30.0),
                    tertiaryFixed = accent3(90.0),
                    tertiaryFixedDim = accent3(80.0),
                    onTertiaryFixed = accent3(10.0),
                    onTertiaryFixedVariant = accent3(30.0),
                )
            }
        }
    }
}

val LocalFixedColorRoles =
    staticCompositionLocalOf {
        FixedColorRoles(
            primaryFixed = Color.Unspecified,
            primaryFixedDim = Color.Unspecified,
            onPrimaryFixed = Color.Unspecified,
            onPrimaryFixedVariant = Color.Unspecified,
            secondaryFixed = Color.Unspecified,
            secondaryFixedDim = Color.Unspecified,
            onSecondaryFixed = Color.Unspecified,
            onSecondaryFixedVariant = Color.Unspecified,
            tertiaryFixed = Color.Unspecified,
            tertiaryFixedDim = Color.Unspecified,
            onTertiaryFixed = Color.Unspecified,
            onTertiaryFixedVariant = Color.Unspecified,
        )
    }

val LocalTonalPalettes =
    staticCompositionLocalOf {
        TonalPalettes(
            keyColor = Color.Unspecified,
            style = com.kyant.monet.PaletteStyle.TonalSpot,
            accent1 = emptyMap(),
            accent2 = emptyMap(),
            accent3 = emptyMap(),
            neutral1 = emptyMap(),
            neutral2 = emptyMap(),
        )
    }
