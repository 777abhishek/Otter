package com.Otter.app.ui.theme

import android.os.Build
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextDirection
import com.Otter.app.ui.common.DarkTheme
import com.Otter.app.ui.common.FixedColorRoles
import com.Otter.app.ui.common.LocalDarkTheme
import com.Otter.app.ui.common.LocalFixedColorRoles
import com.Otter.app.ui.common.LocalTonalPalettes
import com.kyant.monet.TonalPalettes
import com.kyant.monet.TonalPalettes.Companion.toTonalPalettes

val DefaultThemeColor = Color(0xFF00897B) // Otter Teal - Professional brand color

@Composable
fun OtterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isHighContrastModeEnabled: Boolean = false,
    useDynamicColor: Boolean = true,
    themeColor: Color = DefaultThemeColor,
    pureBlack: Boolean = false,
    monochromeTheme: Boolean = false,
    expressive: Boolean = false,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val context = LocalContext.current

    LaunchedEffect(darkTheme) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (darkTheme) {
                view.windowInsetsController?.setSystemBarsAppearance(
                    0,
                    APPEARANCE_LIGHT_STATUS_BARS,
                )
            } else {
                view.windowInsetsController?.setSystemBarsAppearance(
                    APPEARANCE_LIGHT_STATUS_BARS,
                    APPEARANCE_LIGHT_STATUS_BARS,
                )
            }
        }
    }

    val colorScheme =
        when {
            monochromeTheme -> createMonochromeColorScheme(darkTheme)
            useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            else -> {
                createCustomColorScheme(darkTheme, themeColor)
            }
        }.let { baseColorScheme ->
            if (darkTheme && pureBlack) {
                baseColorScheme.pureBlack()
            } else {
                baseColorScheme
            }
        }

    val textStyle =
        LocalTextStyle.current.copy(
            lineBreak = LineBreak.Paragraph,
            textDirection = TextDirection.Content,
        )

    val shapes = if (expressive) ExpressiveShapes else DefaultShapes

    val tonalPalettes = colorScheme.toTonalPalettes()
    val fixedColorRoles = FixedColorRoles.fromTonalPalettes(tonalPalettes)

    CompositionLocalProvider(
        LocalFixedColorRoles provides fixedColorRoles,
        LocalDarkTheme provides DarkTheme(darkTheme, isHighContrastModeEnabled),
        LocalTonalPalettes provides tonalPalettes,
        LocalTextStyle provides textStyle,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OtterTypography,
            shapes = shapes,
            content = content,
        )
    }
}

private fun createMonochromeColorScheme(darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFE0E0E0),
            onPrimary = Color(0xFF121212),
            primaryContainer = Color(0xFF2A2A2A),
            onPrimaryContainer = Color(0xFFFAFAFA),
            secondary = Color(0xFFC0C0C0),
            onSecondary = Color(0xFF121212),
            secondaryContainer = Color(0xFF333333),
            onSecondaryContainer = Color(0xFFFAFAFA),
            tertiary = Color(0xFF9E9E9E),
            onTertiary = Color(0xFF121212),
            tertiaryContainer = Color(0xFF3A3A3A),
            onTertiaryContainer = Color(0xFFFAFAFA),
            background = Color(0xFF121212),
            onBackground = Color(0xFFE0E0E0),
            surface = Color(0xFF0F0F0F),
            onSurface = Color(0xFFE0E0E0),
            surfaceVariant = Color(0xFF1B1B1B),
            onSurfaceVariant = Color(0xFFB0B0B0),
            outline = Color(0xFF5A5A5A),
            outlineVariant = Color(0xFF3A3A3A),
            inverseSurface = Color(0xFFE0E0E0),
            inverseOnSurface = Color(0xFF121212),
            inversePrimary = Color(0xFF121212),
            surfaceBright = Color(0xFF2A2A2A),
            surfaceDim = Color(0xFF0A0A0A),
            surfaceContainerLowest = Color(0xFF060606),
            surfaceContainerLow = Color(0xFF121212),
            surfaceContainer = Color(0xFF161616),
            surfaceContainerHigh = Color(0xFF1F1F1F),
            surfaceContainerHighest = Color(0xFF2A2A2A),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF2A2A2A),
            onPrimary = Color(0xFFFAFAFA),
            primaryContainer = Color(0xFFE8E8E8),
            onPrimaryContainer = Color(0xFF121212),
            secondary = Color(0xFF4A4A4A),
            onSecondary = Color(0xFFFAFAFA),
            secondaryContainer = Color(0xFFF0F0F0),
            onSecondaryContainer = Color(0xFF121212),
            tertiary = Color(0xFF6A6A6A),
            onTertiary = Color(0xFFFAFAFA),
            tertiaryContainer = Color(0xFFE0E0E0),
            onTertiaryContainer = Color(0xFF121212),
            background = Color(0xFFFAFAFA),
            onBackground = Color(0xFF1A1A1A),
            surface = Color(0xFFFAFAFA),
            onSurface = Color(0xFF1A1A1A),
            surfaceVariant = Color(0xFFF0F0F0),
            onSurfaceVariant = Color(0xFF4A4A4A),
            outline = Color(0xFF7A7A7A),
            outlineVariant = Color(0xFFD0D0D0),
            inverseSurface = Color(0xFF1A1A1A),
            inverseOnSurface = Color(0xFFFAFAFA),
            inversePrimary = Color(0xFFE0E0E0),
            surfaceBright = Color(0xFFFAFAFA),
            surfaceDim = Color(0xFFE6E6E6),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF7F7F7),
            surfaceContainer = Color(0xFFF2F2F2),
            surfaceContainerHigh = Color(0xFFEEEEEE),
            surfaceContainerHighest = Color(0xFFE8E8E8),
        )
    }
}

private fun ColorScheme.withSeedAccents(
    palettes: TonalPalettes,
    darkTheme: Boolean,
): ColorScheme {
    return if (darkTheme) {
        copy(
            primary = palettes accent1 80.0,
            onPrimary = palettes accent1 20.0,
            primaryContainer = palettes accent1 30.0,
            onPrimaryContainer = palettes accent1 90.0,
            secondary = palettes accent2 80.0,
            onSecondary = palettes accent2 20.0,
            secondaryContainer = palettes accent2 30.0,
            onSecondaryContainer = palettes accent2 90.0,
            tertiary = palettes accent3 80.0,
            onTertiary = palettes accent3 20.0,
            tertiaryContainer = palettes accent3 30.0,
            onTertiaryContainer = palettes accent3 90.0,
            inversePrimary = palettes accent1 40.0,
        )
    } else {
        copy(
            primary = palettes accent1 40.0,
            onPrimary = palettes accent1 100.0,
            primaryContainer = palettes accent1 90.0,
            onPrimaryContainer = palettes accent1 10.0,
            secondary = palettes accent2 40.0,
            onSecondary = palettes accent2 100.0,
            secondaryContainer = palettes accent2 90.0,
            onSecondaryContainer = palettes accent2 10.0,
            tertiary = palettes accent3 40.0,
            onTertiary = palettes accent3 100.0,
            tertiaryContainer = palettes accent3 90.0,
            onTertiaryContainer = palettes accent3 10.0,
            inversePrimary = palettes accent1 80.0,
        )
    }
}

@Composable
private fun createCustomColorScheme(
    darkTheme: Boolean,
    seedColor: Color,
): ColorScheme {
    val palettes: TonalPalettes = seedColor.toTonalPalettes()

    return if (darkTheme) {
        darkColorScheme(
            background = palettes neutral1 6.0,
            inverseOnSurface = palettes neutral1 20.0,
            inversePrimary = palettes accent1 40.0,
            inverseSurface = palettes neutral1 90.0,
            onBackground = palettes neutral1 90.0,
            onPrimary = palettes accent1 20.0,
            onPrimaryContainer = palettes accent1 90.0,
            onSecondary = palettes accent2 20.0,
            onSecondaryContainer = palettes accent2 90.0,
            onSurface = palettes neutral1 90.0,
            onSurfaceVariant = palettes neutral2 80.0,
            onTertiary = palettes accent3 20.0,
            onTertiaryContainer = palettes accent3 90.0,
            outline = palettes neutral2 60.0,
            outlineVariant = palettes neutral2 30.0,
            primary = palettes accent1 80.0,
            primaryContainer = palettes accent1 30.0,
            secondary = palettes accent2 80.0,
            secondaryContainer = palettes accent2 30.0,
            surface = palettes neutral1 6.0,
            surfaceVariant = palettes neutral2 30.0,
            tertiary = palettes accent3 80.0,
            tertiaryContainer = palettes accent3 30.0,
            surfaceBright = palettes neutral1 24.0,
            surfaceDim = palettes neutral1 6.0,
            surfaceContainerLowest = palettes neutral1 4.0,
            surfaceContainerLow = palettes neutral1 10.0,
            surfaceContainer = palettes neutral1 12.0,
            surfaceContainerHigh = palettes neutral1 17.0,
            surfaceContainerHighest = palettes neutral1 22.0,
        )
    } else {
        lightColorScheme(
            background = palettes neutral1 98.0,
            inverseOnSurface = palettes neutral1 95.0,
            inversePrimary = palettes accent1 80.0,
            inverseSurface = palettes neutral1 20.0,
            onBackground = palettes neutral1 10.0,
            onPrimary = palettes accent1 100.0,
            onPrimaryContainer = palettes accent1 10.0,
            onSecondary = palettes accent2 100.0,
            onSecondaryContainer = palettes accent2 10.0,
            onSurface = palettes neutral1 10.0,
            onSurfaceVariant = palettes neutral2 30.0,
            onTertiary = palettes accent3 100.0,
            onTertiaryContainer = palettes accent3 10.0,
            outline = palettes neutral2 50.0,
            outlineVariant = palettes neutral2 80.0,
            primary = palettes accent1 40.0,
            primaryContainer = palettes accent1 90.0,
            secondary = palettes accent2 40.0,
            secondaryContainer = palettes accent2 90.0,
            surface = palettes neutral1 98.0,
            surfaceVariant = palettes neutral2 90.0,
            tertiary = palettes accent3 40.0,
            tertiaryContainer = palettes accent3 90.0,
            surfaceBright = palettes neutral1 98.0,
            surfaceDim = palettes neutral1 87.0,
            surfaceContainerLowest = palettes neutral1 100.0,
            surfaceContainerLow = palettes neutral1 96.0,
            surfaceContainer = palettes neutral1 94.0,
            surfaceContainerHigh = palettes neutral1 92.0,
            surfaceContainerHighest = palettes neutral1 90.0,
        )
    }
}

fun ColorScheme.pureBlack(): ColorScheme =
    copy(
        surface = Color.Black,
        background = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color.Black,
        surfaceContainer = Color.Black,
        surfaceContainerHigh = Color.Black,
        surfaceContainerHighest = Color.Black,
    )

@Composable
@Deprecated("Use OtterTheme instead", replaceWith = ReplaceWith("OtterTheme(content)"))
fun PreviewThemeLight(content: @Composable () -> Unit) {
    OtterTheme(
        darkTheme = false,
        themeColor = DefaultThemeColor,
        content = content,
    )
}
