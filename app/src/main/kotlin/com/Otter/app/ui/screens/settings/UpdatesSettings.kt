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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FullScreenLoadingOverlay(
    title: String,
    subtitle: String,
) {
    // True fullscreen loading matching app theme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
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
    var isUpdatingYtdlp by remember { mutableStateOf(false) }
    var isUpdatingNewPipe by remember { mutableStateOf(false) }
    var isCheckingOtter by remember { mutableStateOf(false) }
    var isDownloadingOtter by remember { mutableStateOf(false) }
    var otterDownloadProgress by remember { mutableStateOf(0f) }
    var latestOtterRelease by remember { mutableStateOf<AppUpdateUtil.Release?>(null) }
    var otterError by remember { mutableStateOf<String?>(null) }

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

    fun saveLastUpdateTime() {
        val now = System.currentTimeMillis()
        lastYtdlpUpdateTime = now
        context.getSharedPreferences("update_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putLong("last_ytdlp_update", now)
            .apply()
    }

    fun saveNewPipeUpdateTime() {
        val now = System.currentTimeMillis()
        lastNewPipeUpdateTime = now
        context.getSharedPreferences("update_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putLong("last_newpipe_update", now)
            .apply()
    }

    val isUpdating = isUpdatingYtdlp || isUpdatingNewPipe || isCheckingOtter || isDownloadingOtter

    // Version comparison functions
    fun normalizeVersion(v: String): String = v.trim().removePrefix("v").removePrefix("V")

    fun compareVersions(a: String, b: String): Int {
        val partsA = a.split(".").map { it.toIntOrNull() ?: 0 }
        val partsB = b.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(partsA.size, partsB.size)) {
            val pa = partsA.getOrElse(i) { 0 }
            val pb = partsB.getOrElse(i) { 0 }
            if (pa != pb) return pa.compareTo(pb)
        }
        return 0
    }

    // Otter version comparison
    val currentOtterVersion = remember { normalizeVersion(BuildConfig.VERSION_NAME) }
    val latestOtterVersion = remember(latestOtterRelease?.tagName) { normalizeVersion(latestOtterRelease?.tagName.orEmpty()) }
    val isOtterUpdateAvailable = remember(currentOtterVersion, latestOtterVersion) {
        latestOtterVersion.isNotBlank() && compareVersions(latestOtterVersion, currentOtterVersion) > 0
    }

    fun checkOtterUpdates() {
        if (isCheckingOtter) return
        scope.launch {
            isCheckingOtter = true
            otterError = null
            runCatching {
                val release = withContext(Dispatchers.IO) { AppUpdateUtil.fetchLatestRelease() }
                latestOtterRelease = release
            }.onFailure {
                otterError = it.message ?: "Failed to check updates"
            }
            isCheckingOtter = false
        }
    }

    fun downloadOtterUpdate() {
        val release = latestOtterRelease ?: return
        val asset = AppUpdateUtil.pickApkAsset(release)
        if (asset == null) {
            otterError = "No APK found in release"
            return
        }
        if (isDownloadingOtter) return
        scope.launch {
            isDownloadingOtter = true
            otterError = null
            otterDownloadProgress = 0f
            val file = withContext(Dispatchers.IO) {
                AppUpdateUtil.downloadAssetToCache(context, asset) { downloaded, total ->
                    otterDownloadProgress = if (total > 0) (downloaded.toFloat() / total.toFloat()) else 0f
                }
            }
            isDownloadingOtter = false
            if (file == null) {
                otterError = "Failed to download APK"
                return@launch
            }
            val installed = AppUpdateUtil.tryStartInstall(context, file)
            if (!installed) {
                AppUpdateUtil.openUnknownSourcesSettings(context)
            }
        }
    }

    fun openOtterReleases() {
        val tag = latestOtterRelease?.tagName
        val url = if (!tag.isNullOrBlank()) {
            "https://github.com/777abhishek/Otter/releases/tag/$tag"
        } else {
            "https://github.com/777abhishek/Otter/releases"
        }
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    Box(modifier = Modifier.fillMaxSize()) {
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

            // Streaming tools (yt-dlp / NewPipe) section - compact
            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf(
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.SystemUpdate, null, modifier = Modifier.size(22.dp)) },
                                title = "Otter",
                                subtitle = when {
                                    isCheckingOtter -> "Checking for updates..."
                                    isDownloadingOtter -> "Downloading ${(otterDownloadProgress * 100).toInt()}%"
                                    isOtterUpdateAvailable -> "Update available: ${latestOtterRelease?.tagName}"
                                    latestOtterRelease != null -> "Up to date: ${BuildConfig.VERSION_NAME}"
                                    else -> "Tap to check for updates"
                                },
                                onClick = {
                                    when {
                                        isCheckingOtter || isDownloadingOtter -> {}
                                        isOtterUpdateAvailable && latestOtterRelease != null -> downloadOtterUpdate()
                                        else -> checkOtterUpdates()
                                    }
                                },
                                showArrow = false,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor,
                                iconShape = settings.iconShape,
                                trailingContent = {
                                    when {
                                        isCheckingOtter || isDownloadingOtter -> CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        isOtterUpdateAvailable -> Icon(
                                            imageVector = Icons.Rounded.Download,
                                            contentDescription = "Download",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                        else -> Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = "Check",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                },
                            )
                        },
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.NewReleases, null, modifier = Modifier.size(22.dp)) },
                                title = "yt-dlp",
                                subtitle = "$ytdlpVersion • ${formatLastUpdateTime(lastYtdlpUpdateTime)}",
                                onClick = {
                                    if (isUpdatingYtdlp) return@ModernInfoItem
                                    scope.launch {
                                        runCatching {
                                            isUpdatingYtdlp = true
                                            withContext(Dispatchers.IO) {
                                                UpdateUtil.updateYtDlp()
                                            }
                                            ytdlpVersion =
                                                YT_DLP_VERSION.getString().ifBlank {
                                                    YoutubeDL.getInstance().version(context.applicationContext) ?: ytdlpVersion
                                                }
                                            saveLastUpdateTime()
                                        }.onFailure {
                                            it.printStackTrace()
                                        }.also {
                                            isUpdatingYtdlp = false
                                        }
                                    }
                                },
                                showArrow = false,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor,
                                iconShape = settings.iconShape,
                                trailingContent = {
                                    if (isUpdatingYtdlp) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = "Update",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                },
                            )
                        },
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.Extension, null, modifier = Modifier.size(22.dp)) },
                                title = "NewPipe extractor",
                                subtitle = "$newpipeVersion • ${formatLastUpdateTime(lastNewPipeUpdateTime)}",
                                onClick = {
                                    if (isUpdatingNewPipe) return@ModernInfoItem
                                    scope.launch {
                                        runCatching {
                                            isUpdatingNewPipe = true
                                            val latestVersion =
                                                withContext(Dispatchers.IO) {
                                                    UpdateUtil.checkNewPipeUpdates()
                                                }
                                            if (latestVersion != null) {
                                                newpipeVersion = latestVersion
                                                PreferenceUtil.setStringValue(NEWPIPE_VERSION, latestVersion)
                                                saveNewPipeUpdateTime()
                                            }
                                        }.onFailure {
                                            it.printStackTrace()
                                        }.also {
                                            isUpdatingNewPipe = false
                                        }
                                    }
                                },
                                showArrow = false,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor,
                                iconShape = settings.iconShape,
                                trailingContent = {
                                    if (isUpdatingNewPipe) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = "Update",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                },
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

            // Automation
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

        // Full-screen overlay when updating
        if (isUpdating) {
            FullScreenLoadingOverlay(
                title = when {
                    isCheckingOtter -> "Checking for Otter updates"
                    isDownloadingOtter -> "Downloading Otter update"
                    isUpdatingYtdlp -> "Updating yt-dlp"
                    isUpdatingNewPipe -> "Updating NewPipe"
                    else -> "Updating"
                },
                subtitle = when {
                    isDownloadingOtter -> "${(otterDownloadProgress * 100).toInt()}% complete"
                    else -> "Please wait..."
                },
            )
        }
    }

    if (showYtdlpDialog) {
        YtdlpUpdateChannelDialog(
            onDismissRequest = { showYtdlpDialog = false },
        )
    }
}
