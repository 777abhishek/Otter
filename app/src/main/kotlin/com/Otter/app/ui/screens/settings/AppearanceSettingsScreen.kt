package com.Otter.app.ui.screens.settings

import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Otter.app.data.models.IconShape
import com.Otter.app.data.models.ThemeMode
import com.Otter.app.ui.components.ButtonOption
import com.Otter.app.ui.components.ConnectedButtonGroup
import com.Otter.app.ui.components.Material3ExpressiveSettingsGroup
import com.Otter.app.ui.components.ModernInfoItem
import com.Otter.app.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppearanceSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val scrollState = rememberScrollState()

    // Update bottom bar visibility based on scroll
    LaunchedEffect(scrollState.value) {
        onBottomBarVisibilityChanged(scrollState.value == 0)
    }

    var dynamicTheme by remember(settings.useDynamicColor) { mutableStateOf(settings.useDynamicColor) }
    var darkMode by remember(settings.themeMode) { mutableStateOf(settings.themeMode.name) }
    var pureBlack by remember(settings.pureBlack) { mutableStateOf(settings.pureBlack) }
    var monochromeTheme by remember(settings.monochromeTheme) { mutableStateOf(settings.monochromeTheme) }
    var material3Expressive by remember(settings.expressive) { mutableStateOf(settings.expressive) }
    var settingsShapeTertiary by rememberSaveable { mutableStateOf(false) }
    var highRefreshRate by remember(settings.highRefreshRate) { mutableStateOf(settings.highRefreshRate) }
    var showColorPicker by remember { mutableStateOf(false) }
    var customColor by remember { mutableStateOf(Color(settings.seedColor.toInt())) }
    var iconShape by remember(settings.iconShape) { mutableStateOf(settings.iconShape) }

    val seedColors =
        listOf(
            Color(0xFF00897B) to "Teal",
            Color(0xFFFF7043) to "Coral",
            Color(0xFF7E57C2) to "Purple",
        )

    val iconBgColor =
        if (settingsShapeTertiary) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
        }

    val iconStyleColor =
        if (settingsShapeTertiary) {
            MaterialTheme.colorScheme.onTertiary
        } else {
            MaterialTheme.colorScheme.primary
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        // Back button
        if (onBack != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
        }
        Text(
            text = "THEME",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Material3ExpressiveSettingsGroup(
            modifier = Modifier.fillMaxWidth(),
            items =
                buildList {
                    add {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ModernInfoItem(
                                    icon = { Icon(Icons.Rounded.Palette, null, modifier = Modifier.size(22.dp)) },
                                    title = "Dynamic Theme",
                                    subtitle = "Use colors from wallpaper",
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor,
                                    iconShape = settings.iconShape,
                                )
                            }
                            Switch(
                                checked = dynamicTheme,
                                onCheckedChange = {
                                    dynamicTheme = it
                                    viewModel.setUseDynamicColor(it)
                                },
                                enabled = !monochromeTheme,
                                modifier = Modifier.padding(end = 20.dp),
                            )
                        }
                    }

                    add {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ModernInfoItem(
                                    icon = { Icon(Icons.Rounded.Palette, null, modifier = Modifier.size(22.dp)) },
                                    title = "Monochrome Theme",
                                    subtitle = "Pure black & white look",
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor,
                                    iconShape = settings.iconShape,
                                )
                            }
                            Switch(
                                checked = monochromeTheme,
                                onCheckedChange = {
                                    monochromeTheme = it
                                    viewModel.setMonochromeTheme(it)
                                    if (it && dynamicTheme) {
                                        dynamicTheme = false
                                        viewModel.setUseDynamicColor(false)
                                    }
                                },
                                modifier = Modifier.padding(end = 20.dp),
                            )
                        }
                    }

                    add {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(Icons.Rounded.DarkMode, null, modifier = Modifier.size(22.dp)) },
                                        title = "Dark Theme",
                                        subtitle = "Select theme preference",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val themeOptions =
                                listOf(
                                    ButtonOption(ThemeMode.SYSTEM, "System"),
                                    ButtonOption(ThemeMode.LIGHT, "Light"),
                                    ButtonOption(ThemeMode.DARK, "Dark"),
                                )
                            ConnectedButtonGroup(
                                options = themeOptions,
                                selectedValue = settings.themeMode,
                                onSelectionChange = { viewModel.setThemeMode(it) },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp),
                            )
                        }
                    }

                    add {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(Icons.Rounded.Palette, null, modifier = Modifier.size(22.dp)) },
                                        title = "Accent Color",
                                        subtitle = "Choose seed color for theme",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // ConnectedButton style color picker - 3 colors + custom picker
                            val isCustomSelected = !seedColors.any { it.first.toArgb().toLong() == settings.seedColor }
                            val selectedColorName = if (isCustomSelected) "Custom" else getSeedColorName(settings.seedColor)

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            ) {
                                // Preset colors
                                seedColors.forEachIndexed { index, (color, name) ->
                                    val isSelected = selectedColorName == name
                                    val isDisabled = dynamicTheme || monochromeTheme

                                    ToggleButton(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            if (!isDisabled) {
                                                viewModel.setSeedColor(color.toArgb().toLong())
                                            }
                                        },
                                        enabled = !isDisabled,
                                        modifier =
                                            Modifier
                                                .weight(1f)
                                                .height(40.dp),
                                        shapes =
                                            when (index) {
                                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                                seedColors.lastIndex -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                            },
                                        colors =
                                            ToggleButtonDefaults.toggleButtonColors(
                                                checkedContainerColor =
                                                    if (isDisabled) {
                                                        MaterialTheme.colorScheme.surfaceContainerLow
                                                    } else {
                                                        color.copy(
                                                            alpha = 0.8f,
                                                        )
                                                    },
                                                checkedContentColor = if (isDisabled) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                                contentColor = if (isDisabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                            ),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isDisabled) MaterialTheme.colorScheme.onSurfaceVariant else color),
                                            )
                                            Text(
                                                text = name,
                                                maxLines = 1,
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        }
                                    }
                                }

                                // Custom color picker button
                                val isCustomDisabled = dynamicTheme || monochromeTheme
                                ToggleButton(
                                    checked = isCustomSelected,
                                    onCheckedChange = {
                                        if (!isCustomDisabled) {
                                            showColorPicker = true
                                        }
                                    },
                                    enabled = !isCustomDisabled,
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .height(40.dp),
                                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                                    colors =
                                        ToggleButtonDefaults.toggleButtonColors(
                                            checkedContainerColor =
                                                if (isCustomDisabled) {
                                                    MaterialTheme.colorScheme.surfaceContainerLow
                                                } else {
                                                    customColor.copy(
                                                        alpha = 0.8f,
                                                    )
                                                },
                                            checkedContentColor = if (isCustomDisabled) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                            contentColor = if (isCustomDisabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        ),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Colorize,
                                            contentDescription = "Custom",
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = "Custom",
                                            maxLines = 1,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }

                            if (dynamicTheme) {
                                Text(
                                    text = "Seed color is disabled when dynamic theme is enabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                )
                            }

                            if (monochromeTheme) {
                                Text(
                                    text = "Seed color & dynamic theme are disabled when monochrome is enabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }

                    if (darkMode == "DARK" || (darkMode == "SYSTEM" && isSystemInDarkTheme(context))) {
                        add {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(Icons.Rounded.DarkMode, null, modifier = Modifier.size(22.dp)) },
                                        title = "Pure Black",
                                        subtitle = "Use pure black dark theme",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                                Switch(
                                    checked = pureBlack,
                                    onCheckedChange = {
                                        pureBlack = it
                                        viewModel.setPureBlack(it)
                                    },
                                    enabled = !monochromeTheme,
                                    modifier = Modifier.padding(end = 20.dp),
                                )
                            }
                        }
                    }

                    add {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ModernInfoItem(
                                    icon = { Icon(Icons.Rounded.Palette, null, modifier = Modifier.size(22.dp)) },
                                    title = "Settings Shape Tertiary Color",
                                    subtitle = "Use tertiary color icon backgrounds",
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor,
                                    iconShape = settings.iconShape,
                                )
                            }
                            Switch(
                                checked = settingsShapeTertiary,
                                onCheckedChange = { settingsShapeTertiary = it },
                                modifier = Modifier.padding(end = 20.dp),
                            )
                        }
                    }

                    add {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ModernInfoItem(
                                    icon = { Icon(Icons.Rounded.Palette, null, modifier = Modifier.size(22.dp)) },
                                    title = "High Refresh Rate",
                                    subtitle = "Enable high refresh rate animations",
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor,
                                    iconShape = settings.iconShape,
                                )
                            }
                            Switch(
                                checked = highRefreshRate,
                                onCheckedChange = {
                                    highRefreshRate = it
                                    viewModel.setHighRefreshRate(it)
                                },
                                modifier = Modifier.padding(end = 20.dp),
                            )
                        }
                    }

                    add {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(Icons.Rounded.Palette, null, modifier = Modifier.size(22.dp)) },
                                        title = "Icon Shape",
                                        subtitle = "Choose shape for icon backgrounds",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val iconShapeOptions =
                                listOf<ButtonOption<IconShape>>(
                                    ButtonOption(IconShape.CIRCLE, "Circle"),
                                    ButtonOption(IconShape.ROUNDED_RECTANGLE, "Rounded"),
                                    ButtonOption(IconShape.GHOSTISH, "Ghostish"),
                                    ButtonOption(IconShape.SQUARE, "Square"),
                                )
                            ConnectedButtonGroup(
                                options = iconShapeOptions,
                                selectedValue = iconShape,
                                onSelectionChange = {
                                    iconShape = it
                                    viewModel.setIconShape(it)
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp),
                            )
                        }
                    }
                },
        )

        // Custom Color Picker Dialog
        if (showColorPicker) {
            AlertDialog(
                onDismissRequest = { showColorPicker = false },
                title = { Text("Custom Color") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Preview
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(customColor),
                        )

                        // Hue Slider
                        var hue by remember { mutableStateOf(0f) }
                        var saturation by remember { mutableStateOf(0.8f) }
                        var lightness by remember { mutableStateOf(0.5f) }

                        Text("Hue", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = hue,
                            onValueChange = {
                                hue = it
                                customColor = Color.hsl(hue, saturation, lightness)
                            },
                            valueRange = 0f..360f,
                        )

                        Text("Saturation", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = saturation,
                            onValueChange = {
                                saturation = it
                                customColor = Color.hsl(hue, saturation, lightness)
                            },
                            valueRange = 0f..1f,
                        )

                        Text("Lightness", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = lightness,
                            onValueChange = {
                                lightness = it
                                customColor = Color.hsl(hue, saturation, lightness)
                            },
                            valueRange = 0f..1f,
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.setSeedColor(customColor.toArgb().toLong())
                            showColorPicker = false
                        },
                    ) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showColorPicker = false }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

private fun isSystemInDarkTheme(context: Context): Boolean {
    return when (Build.VERSION.SDK_INT) {
        Build.VERSION_CODES.Q -> {
            val uiModeManager = context.getSystemService(android.content.Context.UI_MODE_SERVICE) as android.app.UiModeManager
            uiModeManager.nightMode == android.app.UiModeManager.MODE_NIGHT_YES
        }
        else -> false
    }
}

private fun getSeedColorName(seedColor: Long): String {
    return when (seedColor) {
        0xFF00897BL -> "Teal"
        0xFFFF7043L -> "Coral"
        0xFF7E57C2L -> "Purple"
        0xFF1B5E20L -> "Green"
        0xFFB71C1CL -> "Red"
        0xFFE65100L -> "Orange"
        0xFF006064L -> "Cyan"
        0xFF4A148CL -> "Violet"
        0xFF1565C0L -> "Blue"
        0xFFF9A825L -> "Yellow"
        0xFFC2185BL -> "Pink"
        0xFF6D4C41L -> "Brown"
        0xFF37474FL -> "Gray"
        else -> "Custom"
    }
}
