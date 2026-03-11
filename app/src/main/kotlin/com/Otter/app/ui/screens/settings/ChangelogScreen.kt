package com.Otter.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.OpenInNew
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
import com.Otter.app.ui.components.Material3ExpressiveSettingsGroup
import com.Otter.app.ui.components.ModernInfoItem
import com.Otter.app.ui.viewmodels.SettingsViewModel
import com.Otter.app.util.AppUpdateUtil
import com.Otter.app.util.ReleaseNotesUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
    initialTag: String?,
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(scrollState.value) {
        onBottomBarVisibilityChanged(scrollState.value == 0)
    }

    var isLoading by remember { mutableStateOf(false) }
    var releases by remember { mutableStateOf<List<AppUpdateUtil.Release>>(emptyList()) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var selectedRelease by remember { mutableStateOf<AppUpdateUtil.Release?>(null) }

    fun load() {
        if (isLoading) return
        scope.launch {
            isLoading = true
            errorText = null
            runCatching {
                val list = withContext(Dispatchers.IO) { AppUpdateUtil.fetchReleases(limit = 15) }
                releases = list
                selectedRelease =
                    if (!initialTag.isNullOrBlank()) {
                        list.firstOrNull { it.tagName == initialTag } ?: list.firstOrNull()
                    } else {
                        list.firstOrNull()
                    }
            }.onFailure {
                errorText = it.message ?: "Failed to load changelog"
            }
            isLoading = false
        }
    }

    LaunchedEffect(initialTag) {
        load()
    }

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
                text = "Changelog",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { load() }) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Rounded.Description, contentDescription = "Refresh")
                }
            }
        }

        if (errorText != null) {
            Text(text = errorText.orEmpty(), color = MaterialTheme.colorScheme.error)
        }

        val selected = selectedRelease
        if (selected != null) {
            val parsed = remember(selected.body) { ReleaseNotesUtil.parse(selected.body) }
            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf(
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.Description, null, modifier = Modifier.padding(2.dp)) },
                                title = selected.name.ifBlank { selected.tagName },
                                subtitle = selected.publishedAt.ifBlank { selected.tagName },
                                onClick = {},
                                showArrow = false,
                                iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                iconContentColor = MaterialTheme.colorScheme.primary,
                                iconShape = settings.iconShape,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            val url = "https://github.com/777abhishek/Otter/releases/tag/${selected.tagName}"
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                            )
                                        },
                                    ) {
                                        Icon(Icons.Rounded.OpenInNew, contentDescription = "Open")
                                    }
                                },
                            )
                        },
                    ),
            )

            if (
                parsed.fixes.isEmpty() &&
                    parsed.improvements.isEmpty() &&
                    parsed.patches.isEmpty() &&
                    parsed.other.isEmpty()
            ) {
                Text(
                    text = "No release notes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                NotesSection(title = "Fixes", lines = parsed.fixes)
                NotesSection(title = "Improvements", lines = parsed.improvements)
                NotesSection(title = "Patches", lines = parsed.patches)
                NotesSection(title = "Other", lines = parsed.other)
            }
        }

        if (releases.isNotEmpty()) {
            Text(
                text = "All releases",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp),
            )

            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    releases.map { r ->
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.Description, null, modifier = Modifier.padding(2.dp)) },
                                title = r.tagName,
                                subtitle = r.name.ifBlank { "Release" },
                                onClick = {
                                    selectedRelease = r
                                },
                                showArrow = true,
                                iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                iconContentColor = MaterialTheme.colorScheme.primary,
                                iconShape = settings.iconShape,
                            )
                        }
                    },
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
