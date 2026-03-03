package com.Otter.app.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Update
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.Otter.app.service.NotificationManager
import com.Otter.app.ui.components.Material3ExpressiveSettingsGroup
import com.Otter.app.ui.components.ModernInfoItem
import com.Otter.app.ui.components.PermissionItem
import com.Otter.app.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettings(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    // Update bottom bar visibility based on scroll
    LaunchedEffect(scrollState.value) {
        onBottomBarVisibilityChanged(scrollState.value == 0)
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    var notificationGranted by remember { mutableStateOf(hasNotificationPermission()) }
    val notificationLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            notificationGranted = granted
        }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    notificationGranted = hasNotificationPermission()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val appNotificationSettingsLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) {
            notificationGranted = hasNotificationPermission()
        }

    fun openAppNotificationSettings() {
        val intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            }

        kotlin.runCatching {
            appNotificationSettingsLauncher.launch(intent)
        }.onFailure {
            kotlin.runCatching {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    fun openNotificationChannelSettings(channelId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            openAppNotificationSettings()
            return
        }

        val intent =
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
            }

        kotlin.runCatching {
            appNotificationSettingsLauncher.launch(intent)
        }.onFailure {
            kotlin.runCatching {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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

    var notificationsEnabled by remember(settings.notificationsEnabled) { mutableStateOf(settings.notificationsEnabled) }
    var downloadAlerts by remember(settings.downloadNotificationsEnabled) { mutableStateOf(settings.downloadNotificationsEnabled) }
    var syncNotifications by remember(settings.syncNotificationsEnabled) { mutableStateOf(settings.syncNotificationsEnabled) }
    var backgroundSyncEnabled by remember(settings.backgroundSyncEnabled) { mutableStateOf(settings.backgroundSyncEnabled) }

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
                    text = "Notifications",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            // Notification settings content
            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf(
                        {
                            PermissionItem(
                                icon = Icons.Rounded.Notifications,
                                title = "Notification permission",
                                subtitle = if (notificationGranted) "Granted" else "Allow download alerts",
                                granted = notificationGranted,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor,
                                iconShape = settings.iconShape,
                                onAllowClick =
                                    if (!notificationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        {
                                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    } else if (!notificationGranted) {
                                        {
                                            openAppNotificationSettings()
                                        }
                                    } else {
                                        null
                                    },
                            )
                        },
                        {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(Icons.Rounded.Notifications, null, modifier = Modifier.size(22.dp)) },
                                        title = "Enable Notifications",
                                        subtitle = "Receive push notifications",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                                Switch(
                                    checked = notificationsEnabled,
                                    onCheckedChange = {
                                        notificationsEnabled = it
                                        viewModel.setNotificationsEnabled(it)
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
                                        icon = { Icon(Icons.Rounded.Notifications, null, modifier = Modifier.size(22.dp)) },
                                        title = "System notification settings",
                                        subtitle = "Configure sound, vibration, and importance",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                        onClick = { openAppNotificationSettings() },
                                        showArrow = true,
                                    )
                                }
                            }
                        },
                    ),
            )

            // Notification Types Section
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "NOTIFICATION TYPES",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

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
                                        icon = { Icon(Icons.Rounded.Download, null, modifier = Modifier.size(22.dp)) },
                                        title = "Download Alerts",
                                        subtitle = "Notify when download completes",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                                Switch(
                                    checked = downloadAlerts,
                                    onCheckedChange = {
                                        downloadAlerts = it
                                        viewModel.setDownloadNotificationsEnabled(it)
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
                                        icon = { Icon(Icons.Rounded.Update, null, modifier = Modifier.size(22.dp)) },
                                        title = "Sync Notifications",
                                        subtitle = "Background sync and refresh status",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                                Switch(
                                    checked = syncNotifications,
                                    onCheckedChange = {
                                        syncNotifications = it
                                        viewModel.setSyncNotificationsEnabled(it)
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
                                        icon = { Icon(Icons.Rounded.Update, null, modifier = Modifier.size(22.dp)) },
                                        title = "Background sync",
                                        subtitle = "Keep playlists refreshed automatically",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                                Switch(
                                    checked = backgroundSyncEnabled,
                                    onCheckedChange = {
                                        backgroundSyncEnabled = it
                                        viewModel.setBackgroundSyncEnabled(it)
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
                                        icon = { Icon(Icons.Rounded.Update, null, modifier = Modifier.size(22.dp)) },
                                        title = "Sync channel settings",
                                        subtitle = "Change sync notification behavior in system settings",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                        onClick = { openNotificationChannelSettings(NotificationManager.CHANNEL_ID_SYNC) },
                                        showArrow = true,
                                    )
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
                                        icon = { Icon(Icons.Rounded.Download, null, modifier = Modifier.size(22.dp)) },
                                        title = "Downloads channel settings",
                                        subtitle = "Change download notification behavior in system settings",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                        onClick = { openNotificationChannelSettings(NotificationManager.CHANNEL_ID_DOWNLOAD) },
                                        showArrow = true,
                                    )
                                }
                            }
                        },
                    ),
            )

            Spacer(modifier = Modifier.height(80.dp))
    }
}
