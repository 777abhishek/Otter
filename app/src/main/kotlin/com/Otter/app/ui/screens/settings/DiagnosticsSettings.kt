package com.Otter.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Otter.app.ui.components.Material3ExpressiveSettingsGroup
import com.Otter.app.ui.components.ModernInfoItem
import com.Otter.app.ui.viewmodels.SettingsViewModel
import com.Otter.app.util.CrashReportManager
import com.Otter.app.util.FileLogger
import dagger.hilt.android.EntryPointAccessors
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsSettings(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    LaunchedEffect(scrollState.value) {
        onBottomBarVisibilityChanged(scrollState.value == 0)
    }

    val entryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            DiagnosticsEntryPoint::class.java,
        )
    val fileLogger = entryPoint.fileLogger()
    val crashReportManager = entryPoint.crashReportManager()

    var logText by remember { mutableStateOf("") }
    var crashText by remember { mutableStateOf("") }
    var showLogDialog by remember { mutableStateOf(false) }
    var showCrashDialog by remember { mutableStateOf(false) }

    fun refresh() {
        logText = fileLogger.getLogContent()
        crashText = crashReportManager.getCrashContent()
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    fun openLogFile() {
        val logFile = File(context.filesDir, "log.txt")
        if (logFile.exists()) {
            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    logFile,
                )
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "text/plain")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            context.startActivity(Intent.createChooser(intent, "Open log file"))
        }
    }

    fun openCrashFile() {
        val crashFile = File(context.filesDir, "crash/last_crash.txt")
        if (crashFile.exists()) {
            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    crashFile,
                )
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "text/plain")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            context.startActivity(Intent.createChooser(intent, "Open crash file"))
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
                    text = "Diagnostics",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

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
                                        icon = { Icon(Icons.Rounded.BugReport, null, modifier = Modifier.padding(2.dp)) },
                                        title = "Crash reporting",
                                        subtitle = if (settings.crashReportingEnabled) "Enabled" else "Disabled",
                                        iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                                        iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        iconShape = settings.iconShape,
                                    )
                                }
                                androidx.compose.material3.Switch(
                                    checked = settings.crashReportingEnabled,
                                    onCheckedChange = { viewModel.setCrashReportingEnabled(it) },
                                    modifier = Modifier.padding(end = 20.dp),
                                )
                            }
                        },
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.Refresh, null, modifier = Modifier.padding(2.dp)) },
                                title = "Refresh",
                                subtitle = "Reload logs and crash report",
                                onClick = { refresh() },
                                showArrow = true,
                                iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                iconShape = settings.iconShape,
                            )
                        },
                    ),
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "LOGS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf(
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.padding(2.dp)) },
                                title = "App log file",
                                subtitle = "log.txt",
                                onClick = { openLogFile() },
                                showArrow = true,
                                iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                iconShape = settings.iconShape,
                            )
                        },
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.Share, null, modifier = Modifier.padding(2.dp)) },
                                title = "Share log file",
                                subtitle = "Send log.txt as file",
                                onClick = {
                                    val logFile = File(context.filesDir, "log.txt")
                                    if (logFile.exists()) {
                                        val uri =
                                            FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                logFile,
                                            )
                                        val intent =
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                putExtra(Intent.EXTRA_SUBJECT, "Otter log.txt")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                        context.startActivity(Intent.createChooser(intent, "Share log file"))
                                    }
                                },
                                showArrow = true,
                                iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                iconShape = settings.iconShape,
                            )
                        },
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.Delete, null, modifier = Modifier.padding(2.dp)) },
                                title = "Clear log file",
                                subtitle = "Delete log.txt",
                                onClick = {
                                    fileLogger.clearLog()
                                    refresh()
                                },
                                showArrow = true,
                                iconBackgroundColor = MaterialTheme.colorScheme.errorContainer,
                                iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
                                iconShape = settings.iconShape,
                            )
                        },
                    ),
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "CRASH REPORT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf(
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.padding(2.dp)) },
                                title = "Crash report file",
                                subtitle = "crash/last_crash.txt",
                                onClick = { openCrashFile() },
                                showArrow = true,
                                iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                iconShape = settings.iconShape,
                            )
                        },
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.Share, null, modifier = Modifier.padding(2.dp)) },
                                title = "Share crash file",
                                subtitle = "Send last_crash.txt as file",
                                onClick = {
                                    val crashFile = File(context.filesDir, "crash/last_crash.txt")
                                    if (crashFile.exists()) {
                                        val uri =
                                            FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                crashFile,
                                            )
                                        val intent =
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                        context.startActivity(Intent.createChooser(intent, "Share crash file"))
                                    }
                                },
                                showArrow = true,
                                iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                iconShape = settings.iconShape,
                            )
                        },
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.Delete, null, modifier = Modifier.padding(2.dp)) },
                                title = "Clear crash file",
                                subtitle = "Delete crash/last_crash.txt",
                                onClick = {
                                    crashReportManager.clearCrash()
                                    refresh()
                                },
                                showArrow = true,
                                iconBackgroundColor = MaterialTheme.colorScheme.errorContainer,
                                iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
                                iconShape = settings.iconShape,
                            )
                        },
                    ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Log preview",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = logText.takeLast(4000).ifBlank { "(empty)" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Crash preview",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = crashText.takeLast(4000).ifBlank { "(none)" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface DiagnosticsEntryPoint {
    fun fileLogger(): FileLogger

    fun crashReportManager(): CrashReportManager
}
