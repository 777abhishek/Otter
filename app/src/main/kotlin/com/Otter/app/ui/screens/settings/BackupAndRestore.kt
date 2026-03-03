package com.Otter.app.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
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
import com.Otter.app.ui.viewmodels.StorageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupAndRestore(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
    onRestartApp: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    val storageViewModel: StorageViewModel = hiltViewModel()
    val importResult by storageViewModel.lastActionResult.collectAsState()

    // Dialog state for restart confirmation
    var showRestartDialog by remember { mutableStateOf(false) }

    val createBackupLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip"),
        ) { uri ->
            if (uri != null) {
                storageViewModel.exportDatabase(uri.toString())
            }
        }

    val restoreBackupLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                storageViewModel.importDatabase(uri.toString())
            }
        }

    // Observe import result and show restart dialog
    LaunchedEffect(importResult) {
        importResult?.let { result ->
            if (result.isSuccess) {
                showRestartDialog = true
            }
        }
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
                    text = "Backup & Restore",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            // Backup & Restore content
            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf(
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.Backup, null, modifier = Modifier.size(22.dp)) },
                                title = "Backup settings",
                                subtitle = "Save your preferences",
                                onClick = {
                                    createBackupLauncher.launch("Otter-db-backup.zip")
                                },
                                showArrow = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor,
                                iconShape = settings.iconShape,
                            )
                        },
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.Restore, null, modifier = Modifier.size(22.dp)) },
                                title = "Restore settings",
                                subtitle = "Load from backup",
                                onClick = {
                                    restoreBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                                },
                                showArrow = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor,
                                iconShape = settings.iconShape,
                            )
                        },
                    ),
            )
            // Restart Dialog
            if (showRestartDialog) {
                AlertDialog(
                    onDismissRequest = { },
                    icon = { Icon(Icons.Rounded.RestartAlt, null) },
                    title = { Text("Restart Required") },
                    text = {
                        Text("Data restored successfully. The app needs to restart to apply changes.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showRestartDialog = false
                                onRestartApp()
                            },
                        ) {
                            Text("Restart Now")
                        }
                    },
                    dismissButton = null,
                )
            }
    }
}
