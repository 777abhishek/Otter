package com.Otter.app.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.format.Formatter
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.Otter.app.ui.components.Material3ExpressiveSettingsGroup
import com.Otter.app.ui.components.ModernInfoItem
import com.Otter.app.ui.components.PermissionItem
import com.Otter.app.ui.viewmodels.SettingsViewModel
import com.Otter.app.ui.viewmodels.StorageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSettings(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    val storageViewModel: StorageViewModel = hiltViewModel()
    val storageStats by storageViewModel.storageStats.collectAsState()

    // Update bottom bar visibility based on scroll
    LaunchedEffect(scrollState.value) {
        onBottomBarVisibilityChanged(scrollState.value == 0)
    }

    LaunchedEffect(Unit) {
        storageViewModel.refresh()
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

    // Dialog states
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showClearDownloadsDialog by remember { mutableStateOf(false) }

    // Storage permission launcher for Android 10 and below
    val storagePermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                storageViewModel.refresh()
            }
        }

    // Manage external storage launcher for Android 11+
    val manageExternalStorageLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            storageViewModel.refresh()
        }

    // Directory picker launcher
    val directoryPickerLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            uri?.let {
                val path = it.path ?: "/storage/emulated/0/Download/Otter"
                viewModel.setDownloadPath(path)
            }
        }

    // Refresh permission state when returning from settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    storageViewModel.refresh()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Check storage permissions
    val hasStoragePermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        }

    // Calculate storage totals
    val stats = storageStats
    val totalAppBytes = stats?.totalAppBytes ?: 0L
    val freeDeviceBytes = stats?.freeDeviceBytes ?: 0L
    val totalDeviceBytes = stats?.totalDeviceBytes ?: 1L // avoid div by zero

    // App storage progress (app used / total device)
    val appStorageProgress by animateFloatAsState(
        targetValue = if (totalDeviceBytes > 0) (totalAppBytes.toFloat() / totalDeviceBytes).coerceIn(0f, 1f) else 0f,
        label = "appStorageProgress",
    )

    // Clear Cache Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Cache") },
            text = {
                Text(
                    "This will clear all app cache including temporary files and downloaded thumbnails. Your playlists and liked videos data will be preserved.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        storageViewModel.clearCache()
                        showClearCacheDialog = false
                    },
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Clear Data Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear All Data") },
            text = {
                Text(
                    "This will delete all app data including your playlists, liked videos, download history, and settings. This action cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        storageViewModel.clearUserData()
                        showClearDataDialog = false
                    },
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Clear Downloads Dialog
    if (showClearDownloadsDialog) {
        AlertDialog(
            onDismissRequest = { showClearDownloadsDialog = false },
            title = { Text("Manage Downloads") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Current downloads: ${Formatter.formatFileSize(context, stats?.downloadsOtterBytes ?: 0L)}")
                    Text("Limit: 500 MB")
                    Text("This will delete the oldest downloaded videos to keep total size under 500 MB.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        storageViewModel.clearOldestDownloads(500 * 1024 * 1024)
                        showClearDownloadsDialog = false
                    },
                ) {
                    Text("Limit to 500 MB", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDownloadsDialog = false }) {
                    Text("Cancel")
                }
            },
        )
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
                    text = "Storage",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            // Storage Permission Section (moved to top)
            Text(
                text = "STORAGE PERMISSIONS",
                style =
                    MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )

            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf {
                        PermissionItem(
                            icon = Icons.Rounded.Storage,
                            title = "Storage permission",
                            subtitle = if (hasStoragePermission) "Granted" else "Required to save downloads to custom location",
                            granted = hasStoragePermission,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor,
                            iconShape = settings.iconShape,
                            onAllowClick =
                                if (!hasStoragePermission) {
                                    {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            // Android 11+: Open "All files access" settings
                                            val uri = Uri.parse("package:${context.packageName}")
                                            val appSpecificIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
                                            val generalIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)

                                            val toLaunch =
                                                if (appSpecificIntent.resolveActivity(context.packageManager) != null) {
                                                    appSpecificIntent
                                                } else {
                                                    generalIntent
                                                }

                                            kotlin.runCatching {
                                                manageExternalStorageLauncher.launch(toLaunch)
                                            }.onFailure { t ->
                                                Log.e("StorageSettings", "Failed to open all files access settings", t)
                                                kotlin.runCatching {
                                                    context.startActivity(toLaunch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                                }
                                            }
                                        } else {
                                            // Android 10 and below: Request standard storage permissions
                                            val permissions =
                                                arrayOf(
                                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                )
                                            storagePermissionLauncher.launch(permissions)
                                        }
                                    }
                                } else {
                                    null
                                },
                        )
                    },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Storage Overview Card
            Text(
                text = "STORAGE OVERVIEW",
                style =
                    MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )

            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf {
                        Column(
                            modifier =
                                Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                        ) {
                            // App Storage Section
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "App Storage",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "${Formatter.formatFileSize(context, totalAppBytes)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Device Storage Progress Bar
                            LinearProgressIndicator(
                                progress = { appStorageProgress },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "${Formatter.formatFileSize(
                                        context,
                                        freeDeviceBytes,
                                    )} free of ${Formatter.formatFileSize(context, totalDeviceBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            HorizontalDivider()

                            Spacer(modifier = Modifier.height(16.dp))

                            // Storage Breakdown
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                StorageBreakdownItem(
                                    title = "Downloads (Otter)",
                                    size = stats?.downloadsOtterBytes ?: 0L,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    context = context,
                                )
                                StorageBreakdownItem(
                                    title = "yt-dlp / Python",
                                    size = stats?.ytDlpBytes ?: 0L,
                                    color = MaterialTheme.colorScheme.secondary,
                                    context = context,
                                )
                                StorageBreakdownItem(
                                    title = "Cache",
                                    size = stats?.cacheBytes ?: 0L,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    context = context,
                                )
                                StorageBreakdownItem(
                                    title = "Database",
                                    size = stats?.databaseBytes ?: 0L,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    context = context,
                                )
                                StorageBreakdownItem(
                                    title = "Other",
                                    size = stats?.otherBytes ?: 0L,
                                    color = MaterialTheme.colorScheme.outline,
                                    context = context,
                                )
                            }
                        }
                    },
            )

            // Download Location Section
            Text(
                text = "DOWNLOAD LOCATION",
                style =
                    MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )

            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf(
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.size(22.dp)) },
                                title = "Download location",
                                subtitle = settings.downloadPath.ifEmpty { "Download/Otter (default)" },
                                onClick = {
                                    directoryPickerLauncher.launch(null)
                                },
                                showArrow = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor,
                                iconShape = settings.iconShape,
                            )
                        },
                    ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Storage Management Section
            Text(
                text = "STORAGE MANAGEMENT",
                style =
                    MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )

            // Clear Cache Option
            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf {
                        ModernInfoItem(
                            icon = { Icon(Icons.Rounded.CleaningServices, null, modifier = Modifier.size(22.dp)) },
                            title = "Clear Cache",
                            subtitle = "Free up ${Formatter.formatFileSize(context, stats?.cacheBytes ?: 0L)} of temporary files",
                            onClick = { showClearCacheDialog = true },
                            showArrow = true,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor,
                            iconShape = settings.iconShape,
                        )
                    },
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Manage Downloads Option
            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf {
                        ModernInfoItem(
                            icon = { Icon(Icons.Rounded.Download, null, modifier = Modifier.size(22.dp)) },
                            title = "Manage Downloads",
                            subtitle = "Limit downloads to 500 MB (${Formatter.formatFileSize(
                                context,
                                stats?.downloadsOtterBytes ?: 0L,
                            )} used)",
                            onClick = { showClearDownloadsDialog = true },
                            showArrow = true,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor,
                            iconShape = settings.iconShape,
                        )
                    },
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Clear Data Option
            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf {
                        ModernInfoItem(
                            icon = { Icon(Icons.Rounded.DeleteForever, null, modifier = Modifier.size(22.dp)) },
                            title = "Clear All Data",
                            subtitle = "Delete all app data including playlists and settings",
                            onClick = { showClearDataDialog = true },
                            showArrow = true,
                            iconBackgroundColor = MaterialTheme.colorScheme.errorContainer,
                            iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
                            iconShape = settings.iconShape,
                        )
                    },
            )
            Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun StorageBreakdownItem(
    title: String,
    size: Long,
    color: Color,
    context: Context,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(12.dp)
                        .background(color, CircleShape),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = Formatter.formatFileSize(context, size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
