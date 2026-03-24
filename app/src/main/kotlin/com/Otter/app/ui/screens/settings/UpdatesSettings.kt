package com.Otter.app.ui.screens.settings

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import android.content.Intent
import android.net.Uri
import com.Otter.app.BuildConfig
import com.Otter.app.R
import com.Otter.app.ui.components.Material3ExpressiveSettingsGroup
import com.Otter.app.ui.components.ModernInfoItem
import com.Otter.app.ui.viewmodels.SettingsViewModel
import com.Otter.app.util.NEWPIPE_VERSION
import com.Otter.app.util.PreferenceUtil
import com.Otter.app.util.PreferenceUtil.getString
import com.Otter.app.util.UpdateUtil
import com.Otter.app.util.YT_DLP_VERSION
import com.Otter.app.util.AppUpdateUtil
import com.Otter.app.util.ReleaseNotesUtil
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin
import com.Otter.app.data.models.UpdatesAutomationInterval

@Composable
private fun WavyProgressBar(
    modifier: Modifier,
    progress: Float?,
    height: Dp = 12.dp,
) {
    val infinite = rememberInfiniteTransition(label = "wave")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "phase",
    )

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
    val track = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Canvas(modifier = modifier.height(height)) {
        val w = size.width
        val h = size.height
        
        // Draw rounded track background
        drawRoundRect(color = track, size = size, cornerRadius = CornerRadius(h / 2f))

        // Progress width
        val p = progress?.coerceIn(0f, 1f) ?: 1f
        val waveW = w * p
        if (waveW <= 0f) return@Canvas

        val amp = h * 0.35f
        val baseY = h / 2f
        val cycles = 2f
        val step = 4f

        // Draw secondary wave (behind main wave)
        val path2 = Path()
        path2.moveTo(0f, h)
        path2.lineTo(0f, baseY)
        var x2 = 0f
        while (x2 <= waveW) {
            val t = (x2 / w) * cycles * 2f * Math.PI.toFloat() + phase * 2f * Math.PI.toFloat() + 1f
            val y = baseY + sin(t) * amp * 0.6f
            path2.lineTo(x2, y)
            x2 += step
        }
        path2.lineTo(waveW, h)
        path2.close()
        drawPath(path = path2, color = secondary)

        // Draw main wave
        val path = Path()
        path.moveTo(0f, h)
        path.lineTo(0f, baseY)
        var x = 0f
        while (x <= waveW) {
            val t = (x / w) * cycles * 2f * Math.PI.toFloat() + phase * 2f * Math.PI.toFloat()
            val y = baseY + sin(t) * amp
            path.lineTo(x, y)
            x += step
        }
        path.lineTo(waveW, h)
        path.close()
        drawPath(path = path, color = primary)
    }
}

@Composable
private fun NotesSection(
    title: String,
    lines: List<String>,
) {
    if (lines.isEmpty()) return
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        lines.forEach { line ->
            Text(
                text = "• $line",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FullScreenLoadingOverlay(
    title: String,
    subtitle: String,
    currentVersion: String? = null,
    latestVersion: String? = null,
    releaseNotes: ReleaseNotesUtil.ParsedNotes? = null,
    isChecking: Boolean = false,
) {
    // True fullscreen loading matching app theme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 5.dp,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )

        // Show version info and release notes during checking
        if (isChecking && (currentVersion != null || latestVersion != null)) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Version info
                    if (currentVersion != null) {
                        Text(
                            text = "Current version: $currentVersion",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (latestVersion != null) {
                        Text(
                            text = "Latest version: $latestVersion",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    // Release notes
                    if (releaseNotes != null && (
                        releaseNotes.fixes.isNotEmpty() ||
                        releaseNotes.improvements.isNotEmpty() ||
                        releaseNotes.patches.isNotEmpty() ||
                        releaseNotes.other.isNotEmpty())
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Release notes:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        NotesSection(title = "Fixes", lines = releaseNotes.fixes)
                        NotesSection(title = "Improvements", lines = releaseNotes.improvements)
                        NotesSection(title = "Patches", lines = releaseNotes.patches)
                        NotesSection(title = "Other", lines = releaseNotes.other)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesSettings(
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

    val (settingsShapeTertiary, _) = remember { mutableStateOf(false) }
    val (darkMode, _) = remember { mutableStateOf("AUTO") }

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme =
        remember(darkMode, isSystemInDarkTheme) {
            if (darkMode == "AUTO") isSystemInDarkTheme else darkMode == "ON"
        }

    val (iconBgColor, iconStyleColor) =
        if (settingsShapeTertiary) {
            if (useDarkTheme) {
                MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
            } else {
                MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
            }
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f) to MaterialTheme.colorScheme.primary
        }

    val scope = rememberCoroutineScope()

    var showYtdlpDialog by remember { mutableStateOf(false) }

    var ytdlpVersion by remember {
        mutableStateOf(
            YoutubeDL.getInstance().version(context.applicationContext)
                ?: context.getString(R.string.ytdlp_update),
        )
    }

    var newpipeVersion by remember {
        mutableStateOf(UpdateUtil.getNewPipeVersion())
    }

    // Last update time for yt-dlp
    var lastYtdlpUpdateTime by remember {
        mutableStateOf(
            context.getSharedPreferences("update_prefs", android.content.Context.MODE_PRIVATE)
                .getLong("last_ytdlp_update", 0L),
        )
    }

    // Last update time for NewPipeExtractor
    var lastNewPipeUpdateTime by remember {
        mutableStateOf(
            context.getSharedPreferences("update_prefs", android.content.Context.MODE_PRIVATE)
                .getLong("last_newpipe_update", 0L),
        )
    }

    // Last Otter check time
    var lastOtterCheckTime by remember {
        mutableStateOf(
            context.getSharedPreferences("update_prefs", android.content.Context.MODE_PRIVATE)
                .getLong("last_otter_check", 0L),
        )
    }

    fun formatLastUpdateTime(timestamp: Long): String {
        if (timestamp == 0L) return "Tap to check for updates"
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60000 -> "Updated just now"
            diff < 3600000 -> "Updated ${diff / 60000} min ago"
            diff < 86400000 -> "Updated ${diff / 3600000} hours ago"
            else -> "Updated ${diff / 86400000} days ago"
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
                text = "Updates",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        // Update items section
        Material3ExpressiveSettingsGroup(
            modifier = Modifier.fillMaxWidth(),
            items =
                listOf(
                    {
                        ModernInfoItem(
                            icon = { Icon(Icons.Rounded.SystemUpdate, null, modifier = Modifier.size(22.dp)) },
                            title = "Otter",
                            subtitle = "${BuildConfig.VERSION_NAME} • ${formatLastUpdateTime(lastOtterCheckTime)}",
                            onClick = { navController.navigate("update_checker") },
                            showArrow = true,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor,
                            iconShape = settings.iconShape,
                        )
                    },
                    {
                        ModernInfoItem(
                            icon = { Icon(Icons.Rounded.NewReleases, null, modifier = Modifier.size(22.dp)) },
                            title = "yt-dlp",
                            subtitle = "$ytdlpVersion • ${formatLastUpdateTime(lastYtdlpUpdateTime)}",
                            onClick = { navController.navigate("update_checker_ytdlp") },
                            showArrow = true,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor,
                            iconShape = settings.iconShape,
                        )
                    },
                    {
                        ModernInfoItem(
                            icon = { Icon(Icons.Rounded.Extension, null, modifier = Modifier.size(22.dp)) },
                            title = "NewPipe extractor",
                            subtitle = "$newpipeVersion • ${formatLastUpdateTime(lastNewPipeUpdateTime)}",
                            onClick = { navController.navigate("update_checker_newpipe") },
                            showArrow = true,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor,
                            iconShape = settings.iconShape,
                        )
                    },
                    {
                        ModernInfoItem(
                            icon = { Icon(Icons.Rounded.SystemUpdate, null, modifier = Modifier.size(22.dp)) },
                            title = "yt-dlp update settings",
                            subtitle = "Channel & interval",
                            onClick = { showYtdlpDialog = true },
                            showArrow = true,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor,
                            iconShape = settings.iconShape,
                        )
                    },
                ),
        )

        // Automation settings section
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
                                    icon = { Icon(Icons.Rounded.Update, null, modifier = Modifier.size(22.dp)) },
                                    title = "Auto updates & checks",
                                    subtitle = "Run in background even when app is closed",
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor,
                                    iconShape = settings.iconShape,
                                )
                            }
                            Switch(
                                checked = settings.updatesAutomationEnabled,
                                onCheckedChange = { viewModel.setUpdatesAutomationEnabled(it) },
                                modifier = Modifier.padding(end = 20.dp),
                            )
                        }
                    },
                    {
                        val enabled = settings.updatesAutomationEnabled
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ModernInfoItem(
                                    icon = { Icon(Icons.Rounded.Schedule, null, modifier = Modifier.size(22.dp)) },
                                    title = "Check interval",
                                    subtitle = if (settings.updatesAutomationInterval == UpdatesAutomationInterval.WEEKLY) "Weekly" else "Daily",
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor,
                                    iconShape = settings.iconShape,
                                )
                            }
                            TextButton(
                                enabled = enabled,
                                onClick = {
                                    val next =
                                        if (settings.updatesAutomationInterval == UpdatesAutomationInterval.DAILY) UpdatesAutomationInterval.WEEKLY
                                        else UpdatesAutomationInterval.DAILY
                                    viewModel.setUpdatesAutomationInterval(next)
                                },
                                modifier = Modifier.padding(end = 12.dp),
                            ) {
                                Text(if (settings.updatesAutomationInterval == UpdatesAutomationInterval.WEEKLY) "Weekly" else "Daily")
                            }
                        }
                    },
                    {
                        val enabled = settings.updatesAutomationEnabled
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ModernInfoItem(
                                    icon = { Icon(Icons.Rounded.Notifications, null, modifier = Modifier.size(22.dp)) },
                                    title = "Notify updates",
                                    subtitle = "Show notification when new version is found",
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor,
                                    iconShape = settings.iconShape,
                                )
                            }
                            Switch(
                                checked = settings.updatesAutomationNotify,
                                onCheckedChange = { viewModel.setUpdatesAutomationNotify(it) },
                                enabled = enabled,
                                modifier = Modifier.padding(end = 20.dp),
                            )
                        }
                    },
                    {
                        val enabled = settings.updatesAutomationEnabled
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ModernInfoItem(
                                    icon = { Icon(Icons.Rounded.Download, null, modifier = Modifier.size(22.dp)) },
                                    title = "Auto download Otter APK",
                                    subtitle = "Downloads in background (install still needs tap)",
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor,
                                    iconShape = settings.iconShape,
                                )
                            }
                            Switch(
                                checked = settings.updatesAutomationAutoDownloadApk,
                                onCheckedChange = { viewModel.setUpdatesAutomationAutoDownloadApk(it) },
                                enabled = enabled,
                                modifier = Modifier.padding(end = 20.dp),
                            )
                        }
                    },
                    {
                        val enabled = settings.updatesAutomationEnabled
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ModernInfoItem(
                                    icon = { Icon(Icons.Rounded.NewReleases, null, modifier = Modifier.size(22.dp)) },
                                    title = "Auto update yt-dlp",
                                    subtitle = "Keeps yt-dlp updated automatically",
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor,
                                    iconShape = settings.iconShape,
                                )
                            }
                            Switch(
                                checked = settings.updatesAutomationAutoUpdateYtDlp,
                                onCheckedChange = { viewModel.setUpdatesAutomationAutoUpdateYtDlp(it) },
                                enabled = enabled,
                                modifier = Modifier.padding(end = 20.dp),
                            )
                        }
                    },
                    {
                        val enabled = settings.updatesAutomationEnabled
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ModernInfoItem(
                                    icon = { Icon(Icons.Rounded.Extension, null, modifier = Modifier.size(22.dp)) },
                                    title = "Auto check NewPipe",
                                    subtitle = "Checks extractor updates automatically",
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor,
                                    iconShape = settings.iconShape,
                                )
                            }
                            Switch(
                                checked = settings.updatesAutomationAutoCheckNewPipe,
                                onCheckedChange = { viewModel.setUpdatesAutomationAutoCheckNewPipe(it) },
                                enabled = enabled,
                                modifier = Modifier.padding(end = 20.dp),
                            )
                        }
                    },
                    {
                        val enabled = settings.updatesAutomationEnabled
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ModernInfoItem(
                                    icon = { Icon(Icons.Rounded.DeleteSweep, null, modifier = Modifier.size(22.dp)) },
                                    title = "Auto clear cache",
                                    subtitle = "Clears app cache automatically",
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor,
                                    iconShape = settings.iconShape,
                                )
                            }
                            Switch(
                                checked = settings.updatesAutomationAutoClearCache,
                                onCheckedChange = { viewModel.setUpdatesAutomationAutoClearCache(it) },
                                enabled = enabled,
                                modifier = Modifier.padding(end = 20.dp),
                            )
                        }
                    },
                ),
        )

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showYtdlpDialog) {
        YtdlpUpdateChannelDialog(
            onDismissRequest = { showYtdlpDialog = false },
        )
    }
}
