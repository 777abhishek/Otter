package com.Otter.app.ui.screens.settings

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

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

private data class ChangelogItem(
    val title: String,
    val description: String,
)

private data class ChangelogRelease(
    val version: String,
    val versionCode: Int,
    val date: String,
    val type: String,
    val highlights: List<String>,
    val fixes: List<ChangelogItem>,
    val improvements: List<ChangelogItem>,
)

private fun parseChangelogJson(json: String): List<ChangelogRelease> {
    val root = JSONObject(json)
    val releases = root.optJSONArray("releases") ?: JSONArray()
    return (0 until releases.length()).mapNotNull { idx ->
        val r = releases.optJSONObject(idx) ?: return@mapNotNull null
        val categories = r.optJSONObject("categories")
        val fixes = categories?.optJSONArray("fixes")
        val improvements = categories?.optJSONArray("improvements")

        fun parseItems(arr: JSONArray?): List<ChangelogItem> {
            if (arr == null) return emptyList()
            return (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val title = o.optString("title").trim()
                val desc = o.optString("description").trim()
                if (title.isBlank() && desc.isBlank()) return@mapNotNull null
                ChangelogItem(title = title, description = desc)
            }
        }

        val highlightsJson = r.optJSONArray("highlights")
        val highlightsList =
            if (highlightsJson == null) {
                emptyList()
            } else {
                (0 until highlightsJson.length()).mapNotNull { i ->
                    highlightsJson.optString(i).takeIf { it.isNotBlank() }
                }
            }

        ChangelogRelease(
            version = r.optString("version").ifBlank { r.optString("tag") },
            versionCode = r.optInt("versionCode"),
            date = r.optString("date"),
            type = r.optString("type"),
            highlights = highlightsList,
            fixes = parseItems(fixes),
            improvements = parseItems(improvements),
        )
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
    var releases by remember { mutableStateOf<List<ChangelogRelease>>(emptyList()) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var selectedRelease by remember { mutableStateOf<ChangelogRelease?>(null) }

    fun load() {
        if (isLoading) return
        scope.launch {
            isLoading = true
            errorText = null
            runCatching {
                val list =
                    withContext(Dispatchers.IO) {
                        val json = context.assets.open("changelog.json").bufferedReader().use { it.readText() }
                        parseChangelogJson(json)
                    }
                releases = list
                selectedRelease =
                    if (!initialTag.isNullOrBlank()) {
                        list.firstOrNull { it.version == initialTag } ?: list.firstOrNull()
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
            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf(
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.Description, null, modifier = Modifier.padding(2.dp)) },
                                title = "v${selected.version}",
                                subtitle = buildString {
                                    if (selected.date.isNotBlank()) append(selected.date)
                                    if (selected.type.isNotBlank()) {
                                        if (isNotEmpty()) append(" · ")
                                        append(selected.type)
                                    }
                                    if (selected.versionCode > 0) {
                                        if (isNotEmpty()) append(" · ")
                                        append("code ")
                                        append(selected.versionCode)
                                    }
                                },
                                onClick = {},
                                showArrow = false,
                                iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                iconContentColor = MaterialTheme.colorScheme.primary,
                                iconShape = settings.iconShape,
                            )
                        },
                    ),
            )

            NotesSection(title = "Highlights", lines = selected.highlights)
            NotesSection(
                title = "Fixes",
                lines = selected.fixes.map { item ->
                    if (item.title.isBlank()) item.description
                    else if (item.description.isBlank()) item.title
                    else "${item.title}: ${item.description}"
                },
            )
            NotesSection(
                title = "Improvements",
                lines = selected.improvements.map { item ->
                    if (item.title.isBlank()) item.description
                    else if (item.description.isBlank()) item.title
                    else "${item.title}: ${item.description}"
                },
            )
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
                                title = "v${r.version}",
                                subtitle = r.date.ifBlank { "Release" },
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
