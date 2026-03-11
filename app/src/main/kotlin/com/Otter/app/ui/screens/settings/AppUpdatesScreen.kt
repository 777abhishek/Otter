package com.Otter.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Otter.app.BuildConfig
import com.Otter.app.ui.components.Material3ExpressiveSettingsGroup
import com.Otter.app.ui.components.ModernInfoItem
import com.Otter.app.ui.viewmodels.SettingsViewModel
import com.Otter.app.util.AppUpdateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        bytes >= gb -> String.format("%.2f GB", bytes / gb)
        bytes >= mb -> String.format("%.2f MB", bytes / mb)
        bytes >= kb -> String.format("%.2f KB", bytes / kb)
        else -> "$bytes B"
    }
}

private fun normalizeVersion(v: String): String {
    return v.trim().removePrefix("v").removePrefix("V")
}

@Composable
private fun WavyProgressBar(
    modifier: Modifier,
    progress: Float?,
    height: Dp = 10.dp,
) {
    val infinite = rememberInfiniteTransition(label = "wave")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "phase",
    )

    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier.height(height)) {
        val w = size.width
        val h = size.height
        drawRoundRect(color = track, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f))

        val p = progress?.coerceIn(0f, 1f) ?: 1f
        val waveW = w * p
        if (waveW <= 0f) return@Canvas

        val amp = h * 0.28f
        val baseY = h / 2f
        val cycles = 1.5f
        val step = 6f

        val path = androidx.compose.ui.graphics.Path()
        path.moveTo(0f, h)
        path.lineTo(0f, baseY)
        var x = 0f
        while (x <= waveW) {
            val t = (x / w) * cycles * 2f * Math.PI.toFloat() + phase * 2f * Math.PI.toFloat()
            val y = baseY + kotlin.math.sin(t) * amp
            path.lineTo(x, y)
            x += step
        }
        path.lineTo(waveW, h)
        path.close()

        drawPath(path = path, color = primary)
    }
}

@Composable
private fun FullScreenUpdateOverlay(
    title: String,
    subtitle: String,
    progress: Float?,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                WavyProgressBar(
                    modifier = Modifier.fillMaxWidth(),
                    progress = progress,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdatesScreen(
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

    val scope = rememberCoroutineScope()

    var isChecking by remember { mutableStateOf(false) }
    var latestRelease by remember { mutableStateOf<AppUpdateUtil.Release?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadedBytes by remember { mutableLongStateOf(0L) }
    var totalBytes by remember { mutableLongStateOf(0L) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedApkPath by remember { mutableStateOf<String?>(null) }

    val currentVersion = remember { normalizeVersion(BuildConfig.VERSION_NAME) }
    val latestVersion = remember(latestRelease?.tagName) { normalizeVersion(latestRelease?.tagName.orEmpty()) }
    val isUpdateAvailable = remember(currentVersion, latestVersion) {
        latestVersion.isNotBlank() && latestVersion != currentVersion
    }

    fun openLatestOnGitHub() {
        val tag = latestRelease?.tagName
        val url =
            if (!tag.isNullOrBlank()) {
                "https://github.com/777abhishek/Otter/releases/tag/$tag"
            } else {
                "https://github.com/777abhishek/Otter/releases"
            }
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun checkUpdates() {
        if (isChecking || isDownloading) return
        scope.launch {
            isChecking = true
            errorText = null
            downloadedApkPath = null
            runCatching {
                val release = withContext(Dispatchers.IO) { AppUpdateUtil.fetchLatestRelease() }
                latestRelease = release
            }.onFailure {
                errorText = it.message ?: "Failed to check updates"
            }
            isChecking = false
        }
    }

    fun downloadLatest() {
        val release = latestRelease ?: return
        val asset = AppUpdateUtil.pickApkAsset(release) ?: run {
            errorText = "No APK asset found in latest release"
            return
        }
        if (isDownloading || isChecking) return

        scope.launch {
            isDownloading = true
            errorText = null
            downloadedBytes = 0L
            totalBytes = asset.sizeBytes
            downloadProgress = 0f
            downloadedApkPath = null

            val file =
                runCatching {
                    withContext(Dispatchers.IO) {
                        AppUpdateUtil.downloadAssetToCache(
                            context = context.applicationContext,
                            asset = asset,
                            onProgress = { d, t ->
                                downloadedBytes = d
                                totalBytes = t
                                downloadProgress = if (t > 0) (d.toFloat() / t.toFloat()).coerceIn(0f, 1f) else 0f
                            },
                        )
                    }
                }.getOrNull()

            if (file == null) {
                errorText = "Download failed"
            } else {
                downloadedApkPath = file.absolutePath
            }

            isDownloading = false
        }
    }

    fun installDownloaded() {
        val p = downloadedApkPath ?: return
        val ok = AppUpdateUtil.tryStartInstall(context, java.io.File(p))
        if (!ok) {
            errorText = "Unable to start installer. Allow installs from unknown sources and try again."
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Text(
                    text = "App Updates",
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
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.SystemUpdate, null, modifier = Modifier.padding(2.dp)) },
                                title = "Current build",
                                subtitle = BuildConfig.VERSION_NAME,
                                onClick = {},
                                showArrow = false,
                                iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                iconContentColor = MaterialTheme.colorScheme.primary,
                                iconShape = settings.iconShape,
                            )
                        },
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Filled.Refresh, null) },
                                title = "Check for updates",
                                subtitle = latestRelease?.tagName?.let { "Latest build: $it" } ?: "Tap to check GitHub releases",
                                onClick = { checkUpdates() },
                                showArrow = false,
                                iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                iconContentColor = MaterialTheme.colorScheme.primary,
                                iconShape = settings.iconShape,
                                trailingContent = {
                                    if (isChecking) {
                                        CircularProgressIndicator(modifier = Modifier.height(22.dp), strokeWidth = 2.dp)
                                    }
                                },
                            )
                        },
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.Description, null) },
                                title = "Changelog",
                                subtitle = "Fixes, patches and improvements",
                                onClick = {
                                    val tag = latestRelease?.tagName
                                    if (!tag.isNullOrBlank()) {
                                        navController.navigate("changelog/${Uri.encode(tag)}")
                                    } else {
                                        navController.navigate("changelog")
                                    }
                                },
                                showArrow = true,
                                iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                iconContentColor = MaterialTheme.colorScheme.primary,
                                iconShape = settings.iconShape,
                            )
                        },
                    ),
            )

            Text(
                text = "Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp),
            )

            val statusText =
                when {
                    latestRelease == null -> "Not checked yet"
                    isUpdateAvailable -> "Update available"
                    else -> "You are up to date"
                }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = statusText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (!latestRelease?.tagName.isNullOrBlank()) {
                        Text(
                            text = "Latest build: ${latestRelease?.tagName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (errorText != null) {
                        Text(text = errorText.orEmpty(), color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (latestRelease != null) {
                val release = latestRelease!!
                val asset = AppUpdateUtil.pickApkAsset(release)

                Text(
                    text = "Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = { downloadLatest() },
                        enabled = !isDownloading && asset != null,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (isDownloading) "Downloading" else "Download")
                    }
                    OutlinedButton(
                        onClick = { openLatestOnGitHub() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("GitHub")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { AppUpdateUtil.openUnknownSourcesSettings(context) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Install permission")
                    }
                    Button(
                        onClick = { installDownloaded() },
                        enabled = !downloadedApkPath.isNullOrBlank(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Install")
                    }
                }

                if (isDownloading) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            WavyProgressBar(modifier = Modifier.fillMaxWidth(), progress = downloadProgress)
                            Text(
                                text = "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        if (isChecking) {
            FullScreenUpdateOverlay(
                title = "Checking for updates",
                subtitle = "Contacting GitHub releases…",
                progress = null,
            )
        } else if (isDownloading) {
            FullScreenUpdateOverlay(
                title = "Downloading update",
                subtitle = "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}",
                progress = downloadProgress,
            )
        }
    }
}
