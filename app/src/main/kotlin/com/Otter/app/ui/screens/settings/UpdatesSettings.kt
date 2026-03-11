package com.Otter.app.ui.screens.settings

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
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Otter.app.R
import com.Otter.app.ui.components.Material3ExpressiveSettingsGroup
import com.Otter.app.ui.components.ModernInfoItem
import com.Otter.app.ui.viewmodels.SettingsViewModel
import com.Otter.app.util.NEWPIPE_VERSION
import com.Otter.app.util.PreferenceUtil
import com.Otter.app.util.PreferenceUtil.getString
import com.Otter.app.util.UpdateUtil
import com.Otter.app.util.YT_DLP_VERSION
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf(
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
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    } else {
                                        IconButton(
                                            onClick = {
                                                if (isUpdatingYtdlp) return@IconButton
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
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Refresh,
                                                contentDescription = "Check for updates",
                                                modifier = Modifier.size(22.dp),
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
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

            Spacer(modifier = Modifier.height(10.dp))

            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf(
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
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    } else {
                                        IconButton(
                                            onClick = {
                                                if (isUpdatingNewPipe) return@IconButton
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
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Refresh,
                                                contentDescription = "Check for updates",
                                                modifier = Modifier.size(22.dp),
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                },
                            )
                        },
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.SystemUpdate, null, modifier = Modifier.size(22.dp)) },
                                title = "Otter app updates",
                                subtitle = "Check, download, and install",
                                onClick = { navController.navigate("appUpdates") },
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

    if (showYtdlpDialog) {
        YtdlpUpdateChannelDialog(
            onDismissRequest = { showYtdlpDialog = false },
        )
    }
}
