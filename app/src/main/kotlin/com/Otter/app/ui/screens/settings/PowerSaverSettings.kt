package com.Otter.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Battery4Bar
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Otter.app.ui.components.Material3ExpressiveSettingsGroup
import com.Otter.app.ui.components.ModernInfoItem
import com.Otter.app.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerSaverSettings(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings by viewModel.settings.collectAsState()

    // Update bottom bar visibility based on scroll
    LaunchedEffect(scrollState.value) {
        onBottomBarVisibilityChanged(scrollState.value == 0)
    }

    val (settingsShapeTertiary, _) = remember { mutableStateOf(false) }
    val (darkMode, _) = remember { mutableStateOf("AUTO") }

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme =
        remember(darkMode, isSystemInDarkTheme) {
            if (darkMode == "AUTO") isSystemInDarkTheme else darkMode == "ON"
        }

    val (iconBgColor, iconStyleColor) =
        if (settingsShapeTertiary) {
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        }

    var powerSaverEnabled by remember(settings.powerSaverEnabled) { mutableStateOf(settings.powerSaverEnabled) }
    var lowPowerMode by remember(settings.lowPowerMode) { mutableStateOf(settings.lowPowerMode) }
    var wifiOnly by remember(settings.wifiOnlyDownloads) { mutableStateOf(settings.wifiOnlyDownloads) }
    var batteryThreshold by remember(settings.batteryThresholdPercent) { mutableFloatStateOf(settings.batteryThresholdPercent.toFloat()) }
    var keepScreenOn by remember(settings.keepScreenOn) { mutableStateOf(settings.keepScreenOn) }

    val powerManager =
        remember {
            context.getSystemService(android.content.Context.POWER_SERVICE) as? PowerManager
        }

    val isIgnoringBatteryOptimizations =
        remember(powerManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
                kotlin.runCatching { powerManager.isIgnoringBatteryOptimizations(context.packageName) }.getOrDefault(false)
            } else {
                true
            }
        }

    fun openBatteryOptimizationSettings() {
        val intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            } else {
                Intent(Settings.ACTION_SETTINGS)
            }
        kotlin.runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val intent =
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        kotlin.runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            openBatteryOptimizationSettings()
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
            // Header with back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                }
                Text(
                    text = "Power Saver",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            // Power saver settings content
            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf(
                        {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(Icons.Rounded.BatterySaver, null, modifier = Modifier.size(22.dp)) },
                                        title = "Power Saver Mode",
                                        subtitle = "Enable power saving",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                                Switch(
                                    checked = powerSaverEnabled,
                                    onCheckedChange = {
                                        powerSaverEnabled = it
                                        viewModel.setPowerSaverEnabled(it)
                                    },
                                    modifier = Modifier.padding(end = 20.dp),
                                )
                            }
                        },
                        {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(Icons.Rounded.Power, null, modifier = Modifier.size(22.dp)) },
                                        title = "Battery optimization",
                                        subtitle = if (isIgnoringBatteryOptimizations) "Not optimized" else "Optimized (may stop background work)",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                        onClick = { openBatteryOptimizationSettings() },
                                        showArrow = true,
                                    )
                                }
                                TextButton(
                                    onClick = { requestIgnoreBatteryOptimizations() },
                                    modifier = Modifier.padding(end = 12.dp),
                                ) {
                                    Text("Allow")
                                }
                            }
                        },
                        {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(Icons.Rounded.Power, null, modifier = Modifier.size(22.dp)) },
                                        title = "Low Power Mode",
                                        subtitle = "Reduce concurrency and background work",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                                Switch(
                                    checked = lowPowerMode,
                                    onCheckedChange = {
                                        lowPowerMode = it
                                        viewModel.setLowPowerMode(it)
                                    },
                                    modifier = Modifier.padding(end = 20.dp),
                                )
                            }
                        },
                        {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(Icons.Rounded.Wifi, null, modifier = Modifier.size(22.dp)) },
                                        title = "Wi-Fi Only",
                                        subtitle = "Download on Wi-Fi only",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                                Switch(
                                    checked = wifiOnly,
                                    onCheckedChange = {
                                        wifiOnly = it
                                        viewModel.setWifiOnlyDownloads(it)
                                    },
                                    modifier = Modifier.padding(end = 20.dp),
                                )
                            }
                        },
                        {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(Icons.Rounded.Power, null, modifier = Modifier.size(22.dp)) },
                                        title = "Keep screen on",
                                        subtitle = "Prevent screen from sleeping while using the app",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                                Switch(
                                    checked = keepScreenOn,
                                    onCheckedChange = {
                                        keepScreenOn = it
                                        viewModel.setKeepScreenOn(it)
                                    },
                                    modifier = Modifier.padding(end = 20.dp),
                                )
                            }
                        },
                        {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = { Icon(Icons.Rounded.Battery4Bar, null, modifier = Modifier.size(22.dp)) },
                                            title = "Battery Threshold",
                                            subtitle = "Enable power saver at ${batteryThreshold.toInt()}%",
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor,
                                            iconShape = settings.iconShape,
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Slider(
                                    value = batteryThreshold,
                                    onValueChange = { batteryThreshold = it },
                                    valueRange = 10f..50f,
                                    steps = 8,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                TextButton(
                                    onClick = {
                                        viewModel.setBatteryThresholdPercent(batteryThreshold.toInt())
                                    },
                                    modifier = Modifier.align(Alignment.End).padding(end = 12.dp),
                                ) {
                                    Text("Save")
                                }
                            }
                        },
                    ),
            )

            Spacer(modifier = Modifier.height(80.dp))
    }
}
