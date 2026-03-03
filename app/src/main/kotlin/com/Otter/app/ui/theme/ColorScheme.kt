package com.Otter.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.Otter.app.ui.common.LocalDarkTheme
import com.Otter.app.ui.common.LocalFixedColorRoles
import com.kyant.monet.TonalPalettes.Companion.toTonalPalettes
import io.material.hct.Hct

@Composable
fun Number.autoDark(isDarkTheme: Boolean = LocalDarkTheme.current.isDarkTheme): Double =
    if (!isDarkTheme) {
        this.toDouble()
    } else {
        when (this.toDouble()) {
            6.0 -> 98.0
            10.0 -> 99.0
            20.0 -> 95.0
            25.0 -> 90.0
            30.0 -> 90.0
            40.0 -> 80.0
            50.0 -> 60.0
            60.0 -> 50.0
            70.0 -> 40.0
            80.0 -> 40.0
            90.0 -> 30.0
            95.0 -> 20.0
            98.0 -> 10.0
            99.0 -> 10.0
            100.0 -> 20.0
            else -> this.toDouble()
        }
    }

@Deprecated(
    message = "Deprecated",
    replaceWith =
        ReplaceWith(
            "LocalFixedColorRoles.current",
            imports = arrayOf("com.junkfood.seal.ui.common.LocalFixedColorRoles"),
        ),
)
object FixedAccentColors {
    val primaryFixed: Color
        @Composable get() = LocalFixedColorRoles.current.primaryFixed

    val primaryFixedDim: Color
        @Composable get() = LocalFixedColorRoles.current.primaryFixedDim

    val onPrimaryFixed: Color
        @Composable get() = LocalFixedColorRoles.current.onPrimaryFixed

    val onPrimaryFixedVariant: Color
        @Composable get() = LocalFixedColorRoles.current.onPrimaryFixedVariant

    val secondaryFixed: Color
        @Composable get() = LocalFixedColorRoles.current.secondaryFixed

    val secondaryFixedDim: Color
        @Composable get() = LocalFixedColorRoles.current.secondaryFixedDim

    val onSecondaryFixed: Color
        @Composable get() = LocalFixedColorRoles.current.onSecondaryFixed

    val onSecondaryFixedVariant: Color
        @Composable get() = LocalFixedColorRoles.current.onSecondaryFixedVariant

    val tertiaryFixed: Color
        @Composable get() = LocalFixedColorRoles.current.tertiaryFixed

    val tertiaryFixedDim: Color
        @Composable get() = LocalFixedColorRoles.current.tertiaryFixedDim

    val onTertiaryFixed: Color
        @Composable get() = LocalFixedColorRoles.current.onTertiaryFixed

    val onTertiaryFixedVariant: Color
        @Composable get() = LocalFixedColorRoles.current.onTertiaryFixedVariant
}

const val DEFAULT_SEED_COLOR = 0x00897B // Otter Teal brand color

/**
 * @return a [Color] generated using [Hct] algorithm, harmonized with `primary` color
 * @receiver Seed number used for generating color
 */
@Composable
@ReadOnlyComposable
fun Int.generateLabelColor(): Color = Color(Hct.from(hue = (this % 360).toDouble(), chroma = 36.0, tone = 80.0).toInt())

/**
 * @return a [Color] generated using [Hct] algorithm, harmonized with `primary` color
 */
@ReadOnlyComposable
fun Int.generateOnLabelColor(): Color = Color(Hct.from(hue = (this % 360).toDouble(), chroma = 36.0, tone = 20.0).toInt())

val ErrorTonalPalettes = Color.Red.toTonalPalettes()
