package com.Otter.app.ui.screens.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Otter.app.ui.components.Material3ExpressiveSettingsGroup
import com.Otter.app.ui.components.ModernInfoItem
import com.Otter.app.ui.viewmodels.SettingsViewModel
import com.Otter.app.util.PreferenceUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onNavigate: ((String) -> Unit)? = null,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current

    val scrollState = rememberScrollState()

    // Update bottom bar visibility based on scroll
    LaunchedEffect(scrollState.value) {
        onBottomBarVisibilityChanged(scrollState.value == 0)
    }

    // Collect settings from ViewModel
    val settings by viewModel.settings.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }

    val settingsShapeTertiary by remember { mutableStateOf(false) }
    val darkMode by remember { mutableStateOf("AUTO") }
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme =
        remember(darkMode, isSystemInDarkTheme) {
            if (darkMode == "AUTO") isSystemInDarkTheme else darkMode == "ON"
        }

    val (iconBgColor, iconStyleColor) =
        if (settingsShapeTertiary) {
            if (useDarkTheme) {
                Pair(
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.onTertiary,
                )
            } else {
                Pair(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        } else {
            Pair(
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                MaterialTheme.colorScheme.primary,
            )
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp),
        )

        // Settings content
        SettingsListContent(
            iconBgColor = iconBgColor,
            iconStyleColor = iconStyleColor,
            iconShape = settings.iconShape,
            onNavigate = { route -> onNavigate?.invoke(route) ?: navController.navigate(route) },
        )

        Spacer(modifier = Modifier.height(10.dp))

        Material3ExpressiveSettingsGroup(
            modifier = Modifier.fillMaxWidth(),
            items =
                listOf(
                    {
                        ModernInfoItem(
                            icon = { Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(22.dp)) },
                            title = "Reset to defaults",
                            subtitle = "Reset all settings",
                            onClick = { showResetDialog = true },
                            showArrow = false,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor,
                            iconShape = settings.iconShape,
                        )
                    },
                ),
        )

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset to defaults") },
            text = { Text("This will reset all app settings, including appearance, privacy and update settings.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetToDefaults()
                        PreferenceUtil.resetToDefaults()
                        showResetDialog = false
                    },
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
