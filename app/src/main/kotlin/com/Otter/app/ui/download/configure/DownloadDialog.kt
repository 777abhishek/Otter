package com.Otter.app.ui.download.configure

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.Otter.app.data.download.DownloadPreferences
import kotlinx.coroutines.delay
import java.util.Locale

private fun normalizeHttpUrl(input: String): String {
    val raw = input.trim().replace("\n", "").replace("\r", "")
    if (raw.isBlank()) return ""
    val lower = raw.lowercase(Locale.US)
    if (lower.startsWith("http://") || lower.startsWith("https://")) return raw
    if (lower.startsWith("www.")) return "https://$raw"
    if (lower.contains(".") && !lower.contains(" ") && !lower.contains("://")) return "https://$raw"
    return raw
}

private fun isValidHttpUrl(input: String): Boolean {
    val u = input.trim().lowercase(Locale.US)
    return u.startsWith("http://") || u.startsWith("https://")
}

@Composable
private fun rememberIsOnline(): Boolean {
    val context = LocalContext.current
    val cm = remember(context) {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    fun currentOnline(): Boolean {
        val c = cm ?: return false
        val network = c.activeNetwork ?: return false
        val caps = c.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    var online by remember { mutableStateOf(currentOnline()) }

    DisposableEffect(cm) {
        val c = cm
        if (c == null) return@DisposableEffect onDispose { }

        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    online = currentOnline()
                }

                override fun onLost(network: Network) {
                    online = currentOnline()
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    online = currentOnline()
                }
            }

        runCatching { c.registerDefaultNetworkCallback(callback) }
        online = currentOnline()

        onDispose {
            runCatching { c.unregisterNetworkCallback(callback) }
        }
    }

    return online
}

fun friendlyDownloadErrorMessage(throwable: Throwable): String {
    val msg = (throwable.message ?: "").lowercase(Locale.US)
    return when {
        "no network connection" in msg || ("no network" in msg && "connection" in msg) || "offline" in msg ->
            "No internet connection. Please check your network and try again."
        "wi-fi only downloads enabled" in msg || ("wifi" in msg && "only" in msg) ->
            "Wi-Fi only downloads is enabled. Connect to Wi‑Fi or disable it in Settings."
        throwable is IllegalArgumentException && "invalid url" in msg ->
            "Invalid URL. Please paste a valid http/https link."
        "unsupported url" in msg || ("unsupported" in msg && "url" in msg) || "no suitable extractor" in msg ->
            "Unsupported link/format. Try a different URL."
        "requested format is not available" in msg || "requested format" in msg && "not available" in msg ->
            "Selected format is not available. Pick another format and retry."
        "no video formats found" in msg || "no formats" in msg ->
            "No downloadable formats found for this URL."
        throwable is SecurityException || "permission" in msg || "external storage" in msg ->
            "Storage permission not granted. Allow storage access in Settings and try again."
        "no address associated with hostname" in msg ||
            "temporary failure in name resolution" in msg ||
            "unable to resolve host" in msg ->
            "Network/DNS error. Check internet connection and try again."
        "network is unreachable" in msg ||
            "failed to establish a new connection" in msg ||
            ("connection" in msg && "reset" in msg) ->
            "Network connection lost. Please retry."
        "timed out" in msg || "timeout" in msg || "socket-timeout" in msg ->
            "Network timeout. Check your connection and retry."
        else -> throwable.message ?: "Unknown error"
    }
}

@Composable
fun DownloadErrorSnackbar(
    throwable: Throwable,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onRetry: (() -> Unit)? = null,
) {
    val message = remember(throwable) { friendlyDownloadErrorMessage(throwable) }

    LaunchedEffect(throwable) {
        delay(3500)
        onDismiss()
    }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 2,
        )

        if (onRetry != null) {
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }

        IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Rounded.Close, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadDialog(
    sheetState: SheetState,
    state: DownloadDialogViewModel.SheetState,
    preferences: DownloadPreferences,
    onPreferencesUpdate: (DownloadPreferences) -> Unit,
    onActionPost: (DownloadDialogViewModel.Action) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { onActionPost(DownloadDialogViewModel.Action.HideSheet) },
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        when (state) {
            is DownloadDialogViewModel.SheetState.InputUrl -> {
                InputUrlPage(
                    onActionPost = onActionPost,
                )
            }
            is DownloadDialogViewModel.SheetState.Configure -> {
                ConfigurePage(
                    urlList = state.urlList,
                    preferences = preferences,
                    onPreferencesUpdate = onPreferencesUpdate,
                    onActionPost = onActionPost,
                )
            }
            is DownloadDialogViewModel.SheetState.Loading -> {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp)) {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }
            is DownloadDialogViewModel.SheetState.Error -> {
                val friendlyMessage =
                    remember(state.throwable) {
                        friendlyDownloadErrorMessage(state.throwable)
                    }
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Text(text = "Error", style = MaterialTheme.typography.titleLarge)
                    Text(text = friendlyMessage)
                    Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { onActionPost(state.action) }) { Text("Retry") }
                        Button(onClick = { onActionPost(DownloadDialogViewModel.Action.HideSheet) }) { Text("Close") }
                    }
                }
            }
        }
    }
}

@Composable
private fun InputUrlPage(onActionPost: (DownloadDialogViewModel.Action) -> Unit) {
    var url by remember { mutableStateOf("") }
    val normalizedUrl = remember(url) { normalizeHttpUrl(url) }
    val showInvalid = normalizedUrl.isNotBlank() && !isValidHttpUrl(normalizedUrl)
    val isOnline = rememberIsOnline()
    val showOffline = normalizedUrl.isNotBlank() && !isOnline

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(text = "New Download", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            label = { Text("Video URL") },
            maxLines = 3,
            isError = showInvalid || showOffline,
            supportingText = {
                when {
                    showOffline -> Text("No internet connection")
                    showInvalid -> Text("Invalid URL")
                }
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = { onActionPost(DownloadDialogViewModel.Action.HideSheet) }) {
                Icon(Icons.Outlined.Cancel, contentDescription = null)
            }
            IconButton(
                onClick = {
                    onActionPost(DownloadDialogViewModel.Action.ProceedWithURLs(listOf(normalizedUrl)))
                },
                enabled = normalizedUrl.isNotBlank() && !showInvalid && isOnline,
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun ConfigurePage(
    urlList: List<String>,
    preferences: DownloadPreferences,
    onPreferencesUpdate: (DownloadPreferences) -> Unit,
    onActionPost: (DownloadDialogViewModel.Action) -> Unit,
) {
    val url = urlList.firstOrNull().orEmpty()
    var audioOnly by remember(preferences) { mutableStateOf(preferences.extractAudio) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(text = "Before download", style = MaterialTheme.typography.titleLarge)
        Text(text = url, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))

        Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                audioOnly = false
                onPreferencesUpdate(preferences.copy(extractAudio = false))
            }) { Text("Video") }
            Button(onClick = {
                audioOnly = true
                onPreferencesUpdate(preferences.copy(extractAudio = true))
            }) { Text("Audio") }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = { onActionPost(DownloadDialogViewModel.Action.HideSheet) }) {
                Text("Cancel")
            }
            Button(onClick = {
                onActionPost(
                    DownloadDialogViewModel.Action.FetchFormats(
                        url = url,
                        audioOnly = audioOnly,
                        preferences = preferences,
                    ),
                )
            }, enabled = url.isNotBlank()) {
                Text("Select format")
            }
            Button(onClick = {
                onActionPost(
                    DownloadDialogViewModel.Action.DownloadWithPreset(
                        urlList = urlList,
                        preferences = preferences,
                    ),
                )
            }, enabled = url.isNotBlank()) {
                Text("Download")
            }
        }
    }
}
