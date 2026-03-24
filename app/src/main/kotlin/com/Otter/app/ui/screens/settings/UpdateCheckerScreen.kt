package com.Otter.app.ui.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.content.Intent
import android.net.Uri
import com.Otter.app.BuildConfig
import com.Otter.app.util.AppUpdateUtil
import com.Otter.app.util.ReleaseInfo
import com.Otter.app.util.UpdateUtil
import com.Otter.app.util.PreferenceUtil
import com.Otter.app.util.NEWPIPE_VERSION
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class UpdateType {
    OTTER,
    YT_DLP,
    NEWPIPE,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateCheckerScreen(
    navController: NavController,
    onBack: () -> Unit,
    updateType: UpdateType = UpdateType.OTTER,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isChecking by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var latestRelease by remember { mutableStateOf<ReleaseInfo?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasChecked by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<String?>(null) }

    val currentVersion = when (updateType) {
        UpdateType.OTTER -> BuildConfig.VERSION_NAME
        UpdateType.YT_DLP -> YoutubeDL.getInstance().version(context) ?: "Unknown"
        UpdateType.NEWPIPE -> PreferenceUtil.getStringValue(NEWPIPE_VERSION, "v0.25.2")
    }

    val latestVersion = remember(latestRelease?.tagName) {
        normalizeVersion(latestRelease?.tagName.orEmpty())
    }

    val isUpdateAvailable = remember(currentVersion, latestVersion) {
        latestVersion.isNotBlank() && compareVersions(latestVersion, normalizeVersion(currentVersion)) > 0
    }

    val title = when (updateType) {
        UpdateType.OTTER -> "App Update"
        UpdateType.YT_DLP -> "yt-dlp Update"
        UpdateType.NEWPIPE -> "NewPipe Extractor"
    }

    val githubRepo = when (updateType) {
        UpdateType.OTTER -> "777abhishek/Otter"
        UpdateType.YT_DLP -> "yt-dlp/yt-dlp"
        UpdateType.NEWPIPE -> "TeamNewPipe/NewPipeExtractor"
    }

    val appName = BuildConfig.APPLICATION_ID.substringAfterLast('.').replaceFirstChar { it.uppercase() }

    fun checkUpdates() {
        if (isChecking) return
        scope.launch {
            isChecking = true
            error = null
            val startTime = System.currentTimeMillis()
            
            runCatching {
                val release = withContext(Dispatchers.IO) {
                    when (updateType) {
                        UpdateType.OTTER -> {
                            val r = AppUpdateUtil.fetchLatestRelease()
                            if (r != null) ReleaseInfo(
                                tagName = r.tagName,
                                name = r.name,
                                body = r.body,
                                publishedAt = r.publishedAt,
                                downloadUrl = r.assets.firstOrNull()?.downloadUrl,
                            ) else null
                        }
                        UpdateType.YT_DLP -> UpdateUtil.fetchYtDlpRelease()
                        UpdateType.NEWPIPE -> UpdateUtil.fetchNewPipeRelease()
                    }
                }
                // Ensure minimum loading time for better UX (at least 1.5 seconds)
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = 1500L - elapsed
                if (remaining > 0) delay(remaining)
                
                latestRelease = release
            }.onFailure {
                error = it.message ?: "Failed to check updates"
            }
            isChecking = false
            hasChecked = true
        }
    }

    fun performUpdate() {
        if (isUpdating) return
        scope.launch {
            isUpdating = true
            error = null
            downloadProgress = 0f
            runCatching {
                when (updateType) {
                    UpdateType.OTTER -> {
                        val r = AppUpdateUtil.fetchLatestRelease()
                        val asset = r?.let { AppUpdateUtil.pickApkAsset(it) }
                        if (asset == null) { error = "No APK found in release"; return@runCatching }
                        val file = withContext(Dispatchers.IO) {
                            AppUpdateUtil.downloadAssetToCache(context, asset) { downloaded, total ->
                                downloadProgress = if (total > 0) downloaded.toFloat() / total.toFloat() else 0f
                            }
                        }
                        if (file == null) { error = "Failed to download APK"; return@runCatching }
                        val installed = AppUpdateUtil.tryStartInstall(context, file)
                        if (!installed) AppUpdateUtil.openUnknownSourcesSettings(context)
                    }
                    UpdateType.YT_DLP -> {
                        val status = withContext(Dispatchers.IO) { UpdateUtil.updateYtDlp() }
                        updateStatus = when (status) {
                            YoutubeDL.UpdateStatus.DONE -> "Updated successfully!"
                            YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> "Already up to date"
                            else -> "Update completed"
                        }
                    }
                    UpdateType.NEWPIPE -> {
                        val version = withContext(Dispatchers.IO) { UpdateUtil.checkNewPipeUpdates() }
                        if (version != null) {
                            PreferenceUtil.setStringValue(NEWPIPE_VERSION, version)
                            updateStatus = "Updated to $version"
                        } else {
                            error = "Failed to update"
                        }
                    }
                }
            }.onFailure {
                error = it.message ?: "Update failed"
            }
            isUpdating = false
        }
    }

    fun openReleases() {
        val tag = latestRelease?.tagName
        val url = if (!tag.isNullOrBlank()) "https://github.com/$githubRepo/releases/tag/$tag"
        else "https://github.com/$githubRepo/releases"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    LaunchedEffect(Unit) {
        if (!hasChecked && !isChecking) checkUpdates()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "progress",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Top bar ──────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            Spacer(Modifier.height(48.dp))

            // ── Main content area ─────────────────────────────────────────────
            AnimatedContent(
                targetState = when {
                    isChecking || isUpdating -> "loading"
                    hasChecked -> "result"
                    else -> "idle"
                },
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "content",
                modifier = Modifier.fillMaxWidth(),
            ) { state ->
                when (state) {

                // ── Loading ────────────────────────────────────────────────
                "loading" -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp),
                        )
                        
                        Text(
                            text = when {
                                isChecking -> "Checking for updates…"
                                isUpdating && updateType == UpdateType.OTTER ->
                                    "Downloading… ${(downloadProgress * 100).toInt()}%"
                                else -> "Updating…"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )

                        LinearWavyProgressIndicator(
                            progress = {
                                when {
                                    isChecking -> animatedProgress
                                    isUpdating && updateType == UpdateType.OTTER ->
                                        downloadProgress.coerceIn(0f, 1f)
                                    else -> animatedProgress
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            amplitude = { 1f },
                            wavelength = 20.dp,
                        )
                    }
                }

                // ── Result ─────────────────────────────────────────────────
                "result" -> {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(bottom = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val (icon, tint) = when {
                            error != null -> Icons.Rounded.ErrorOutline to MaterialTheme.colorScheme.error
                            isUpdateAvailable -> Icons.Rounded.NewReleases to MaterialTheme.colorScheme.primary
                            else -> Icons.Rounded.CheckCircle to MaterialTheme.colorScheme.primary
                        }
                        
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(64.dp),
                        )

                        // Status headline
                        Text(
                            text = when {
                                error != null -> "Something went wrong"
                                updateStatus != null -> updateStatus!!
                                latestRelease == null -> "Couldn't fetch release"
                                isUpdateAvailable -> "Update available"
                                else -> "App is up to date"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                error != null -> MaterialTheme.colorScheme.error
                                isUpdateAvailable -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(8.dp))

                        // Version line
                        Text(
                            text = "$appName version $currentVersion",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        // Last checked timestamp
                        Text(
                            text = "Last checked: ${formatNow()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )

                        // Release notes (plain text) - show when update available or after update
                        if (isUpdateAvailable && latestRelease?.body?.isNotBlank() == true) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "What's new:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = formatReleaseNotes(latestRelease!!.body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        // Error detail
                        if (error != null) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = error.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                // ── Idle (shouldn't show long) ─────────────────────────────
                else -> Box(Modifier.fillMaxWidth().height(200.dp))
                }
            }
        }

        // ── Bottom button ─────────────────────────────────────────────
        Button(
            onClick = {
                when {
                    isUpdateAvailable && latestRelease != null -> performUpdate()
                    else -> checkUpdates()
                }
            },
            enabled = !isChecking && !isUpdating,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .height(56.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            val buttonIcon = when {
                isUpdateAvailable && latestRelease != null -> Icons.Rounded.Download
                else -> Icons.Rounded.Refresh
            }
            Icon(
                imageVector = buttonIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (isUpdateAvailable && latestRelease != null) {
                    if (updateType == UpdateType.OTTER) "Download & Install" else "Update Now"
                } else {
                    "Check for update"
                },
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
private fun formatNow(): String {
    val sdf = java.text.SimpleDateFormat("d MMMM yyyy, h:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date())
}

private fun formatReleaseDate(dateStr: String): String {
    return try {
        val inp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        val out = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US)
        inp.parse(dateStr)?.let { out.format(it) } ?: dateStr
    } catch (e: Exception) { dateStr }
}

private fun formatReleaseNotes(body: String): String {
    return body
        // Remove headers (## ### etc.)
        .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
        // Remove bold/italic (**text**, *text*, __text__, _text_)
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        .replace(Regex("\\*(.+?)\\*"), "$1")
        .replace(Regex("__(.+?)__"), "$1")
        .replace(Regex("_(.+?)_"), "$1")
        // Remove strikethrough (~~text~~)
        .replace(Regex("~~(.+?)~~"), "$1")
        // Remove inline code (`code`)
        .replace(Regex("`(.+?)`"), "$1")
        // Remove links [text](url) -> text
        .replace(Regex("\\[(.+?)\\]\\(.+?\\)"), "$1")
        // Remove images ![alt](url)
        .replace(Regex("!\\[.*?\\]\\(.+?\\)"), "")
        // Remove horizontal rules (--- or ***)
        .replace(Regex("^(---|\\*\\*\\*|___)$", RegexOption.MULTILINE), "")
        // Clean up multiple blank lines
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}

private fun normalizeVersion(v: String): String = v.trim().removePrefix("v").removePrefix("V")

private fun compareVersions(a: String, b: String): Int {
    val pa = a.split(".").map { it.toIntOrNull() ?: 0 }
    val pb = b.split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(pa.size, pb.size)) {
        val diff = (pa.getOrElse(i) { 0 }).compareTo(pb.getOrElse(i) { 0 })
        if (diff != 0) return diff
    }
    return 0
}