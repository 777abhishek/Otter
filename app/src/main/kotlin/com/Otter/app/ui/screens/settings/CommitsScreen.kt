package com.Otter.app.ui.screens.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

private data class CommitInfo(
    val message: String,
    val author: String,
    val date: String,
)

private sealed interface CommitsUiState {
    data object Loading : CommitsUiState

    data class Loaded(val commits: List<CommitInfo>) : CommitsUiState

    data class Error(val message: String) : CommitsUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommitsScreen(
    navController: NavController,
    onBack: (() -> Unit)? = null,
) {
    var uiState by remember { mutableStateOf<CommitsUiState>(CommitsUiState.Loading) }

    fun loadCommits() {
        uiState = CommitsUiState.Loading
    }

    LaunchedEffect(Unit, uiState) {
        if (uiState !is CommitsUiState.Loading) return@LaunchedEffect

        uiState =
            runCatching {
                val commits = fetchLatestCommits(
                    repo = "777abhishek/Otter",
                    limit = 50,
                )
                CommitsUiState.Loaded(commits)
            }.getOrElse {
                CommitsUiState.Error(it.message ?: "Failed to load commits")
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
                text = "Commits",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            IconButton(onClick = { loadCommits() }) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Refresh",
                )
            }
        }

        when (val state = uiState) {
            is CommitsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is CommitsUiState.Error -> {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            is CommitsUiState.Loaded -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.commits) { commit ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = commit.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = commit.author,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = commit.date,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

private suspend fun fetchLatestCommits(
    repo: String,
    limit: Int,
): List<CommitInfo> = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    val request =
        Request.Builder()
            .url("https://api.github.com/repos/$repo/commits?per_page=$limit")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()

    val response = client.newCall(request).execute()
    if (!response.isSuccessful) return@withContext emptyList()

    val json = response.body?.string().orEmpty()
    val arr = JSONArray(json)

    (0 until arr.length()).mapNotNull { idx ->
        val commitObj = arr.optJSONObject(idx) ?: return@mapNotNull null
        val commit = commitObj.optJSONObject("commit") ?: return@mapNotNull null
        val message = commit.optString("message").trim()
        if (message.isBlank()) return@mapNotNull null
        
        val authorName = commit.optJSONObject("author")?.optString("name")
            ?: commitObj.optJSONObject("author")?.optString("login")
            ?: "Unknown"
        
        val dateStr = commit.optJSONObject("committer")?.optString("date") ?: ""
        val formattedDate = formatCommitDate(dateStr)
        
        val firstLine = message.lineSequence().firstOrNull()?.trim().takeIf { !it.isNullOrBlank() } ?: return@mapNotNull null
        
        CommitInfo(
            message = firstLine,
            author = authorName,
            date = formattedDate,
        )
    }
}

private fun formatCommitDate(dateStr: String): String {
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        val outputFormat = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault())
        inputFormat.parse(dateStr)?.let { outputFormat.format(it) } ?: dateStr
    } catch (e: Exception) {
        dateStr
    }
}
