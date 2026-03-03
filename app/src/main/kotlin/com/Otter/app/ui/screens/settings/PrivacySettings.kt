package com.Otter.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Otter.app.network.PrivacySyncService
import com.Otter.app.ui.components.Material3ExpressiveSettingsGroup
import com.Otter.app.ui.components.ModernInfoItem
import com.Otter.app.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettings(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val settings by viewModel.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Get PrivacySyncService from the ViewModel (injected via Hilt)
    val privacySyncService = viewModel.privacySyncService

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

    var analyticsEnabled by remember(settings.analyticsEnabled) { mutableStateOf(settings.analyticsEnabled) }
    var crashReporting by remember(settings.crashReportingEnabled) { mutableStateOf(settings.crashReportingEnabled) }
    var dataSharing by remember(settings.dataSharingEnabled) { mutableStateOf(settings.dataSharingEnabled) }
    var webViewThirdPartyCookies by remember(settings.webViewThirdPartyCookies) { mutableStateOf(settings.webViewThirdPartyCookies) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteResultMessage by remember { mutableStateOf<String?>(null) }

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
                    text = "Privacy",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            // Privacy settings content
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
                                        icon = { Icon(Icons.Rounded.Analytics, null, modifier = Modifier.size(22.dp)) },
                                        title = "Analytics",
                                        subtitle = "Help improve the app",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                                Switch(
                                    checked = analyticsEnabled,
                                    onCheckedChange = {
                                        analyticsEnabled = it
                                        viewModel.setAnalyticsEnabled(it)
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
                                        icon = { Icon(Icons.Rounded.BugReport, null, modifier = Modifier.size(22.dp)) },
                                        title = "Crash Reporting",
                                        subtitle = "Send crash reports",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                                Switch(
                                    checked = crashReporting,
                                    onCheckedChange = {
                                        crashReporting = it
                                        viewModel.setCrashReportingEnabled(it)
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
                                        icon = { Icon(Icons.Rounded.Security, null, modifier = Modifier.size(22.dp)) },
                                        title = "WebView third-party cookies",
                                        subtitle = if (webViewThirdPartyCookies) "Allowed" else "Blocked",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                                Switch(
                                    checked = webViewThirdPartyCookies,
                                    onCheckedChange = {
                                        webViewThirdPartyCookies = it
                                        viewModel.setWebViewThirdPartyCookies(it)
                                    },
                                    modifier = Modifier.padding(end = 20.dp),
                                )
                            }
                        },
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.BugReport, null, modifier = Modifier.size(22.dp)) },
                                title = "Diagnostics",
                                subtitle = "View logs and crash reports",
                                onClick = { navController.navigate("diagnosticsSettings") },
                                showArrow = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor,
                                iconShape = settings.iconShape,
                            )
                        },
                        {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(Icons.Rounded.Share, null, modifier = Modifier.size(22.dp)) },
                                        title = "Data Sharing",
                                        subtitle = "Share anonymous data",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                                Switch(
                                    checked = dataSharing,
                                    onCheckedChange = {
                                        dataSharing = it
                                        viewModel.setDataSharingEnabled(it)
                                    },
                                    modifier = Modifier.padding(end = 20.dp),
                                )
                            }
                        },
                    ),
            )

            // Security Section
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SECURITY",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf(
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.Security, null, modifier = Modifier.size(22.dp)) },
                                title = "Privacy Policy",
                                subtitle = "View privacy policy",
                                onClick = { /* TODO: Open privacy policy */ },
                                showArrow = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor,
                                iconShape = settings.iconShape,
                            )
                        },
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(22.dp)) },
                                title = "Delete My Data",
                                subtitle = "Request deletion of all your data from our servers",
                                onClick = { showDeleteDialog = true },
                                showArrow = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor,
                                iconShape = settings.iconShape,
                            )
                        },
                    ),
            )

            Spacer(modifier = Modifier.height(80.dp))
    }

    // Delete Data Confirmation Dialog
    // Show result toast
    LaunchedEffect(deleteResultMessage) {
        deleteResultMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            deleteResultMessage = null
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete My Data") },
            text = { 
                Text("This will permanently delete all analytics events, crash reports, and privacy settings associated with your device from our servers. This action cannot be undone.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isDeleting = true
                            val success = privacySyncService.requestDeviceDataDeletion()
                            isDeleting = false
                            showDeleteDialog = false
                            deleteResultMessage = if (success) "Data deletion request sent successfully" else "Failed to delete data. Please try again later."
                        }
                    },
                    enabled = !isDeleting
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
