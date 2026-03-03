package com.Otter.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.Otter.app.util.PreferenceUtil
import com.Otter.app.util.PreferenceUtil.getBoolean
import com.Otter.app.util.PreferenceUtil.getInt
import com.Otter.app.util.PreferenceUtil.getLong
import com.Otter.app.util.PreferenceUtil.updateBoolean
import com.Otter.app.util.PreferenceUtil.updateInt
import com.Otter.app.util.PreferenceUtil.updateLong
import com.Otter.app.util.YT_DLP_AUTO_UPDATE
import com.Otter.app.util.YT_DLP_NIGHTLY
import com.Otter.app.util.YT_DLP_STABLE
import com.Otter.app.util.YT_DLP_UPDATE_CHANNEL
import com.Otter.app.util.YT_DLP_UPDATE_INTERVAL
import com.Otter.app.util.YT_DLP_UPDATE_TIME
import com.Otter.app.util.YT_DLP_VERSION

private const val INTERVAL_DAY = 86_400_000L
private const val INTERVAL_WEEK = 86_400_000L * 7
private const val INTERVAL_MONTH = 86_400_000L * 30

@Composable
fun YtdlpUpdateChannelDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
) {
    val initialUpdateChannel = remember { YT_DLP_UPDATE_CHANNEL.getInt() }
    var ytdlpUpdateChannel by remember { mutableStateOf(initialUpdateChannel) }
    var ytdlpAutoUpdate by remember { mutableStateOf(YT_DLP_AUTO_UPDATE.getBoolean()) }
    var updateInterval by remember { mutableLongStateOf(YT_DLP_UPDATE_INTERVAL.getLong(INTERVAL_WEEK)) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.SyncAlt, null) },
        title = { Text(text = "Update") },
        confirmButton = {
            TextButton(
                onClick = {
                    YT_DLP_AUTO_UPDATE.updateBoolean(ytdlpAutoUpdate)
                    YT_DLP_UPDATE_CHANNEL.updateInt(ytdlpUpdateChannel)
                    YT_DLP_UPDATE_INTERVAL.updateLong(updateInterval)

                    if (ytdlpUpdateChannel != initialUpdateChannel) {
                        // Force next auto-update tick to run with the newly selected channel.
                        YT_DLP_UPDATE_TIME.updateLong(0L)
                        PreferenceUtil.encodeString(YT_DLP_VERSION, "")
                    }
                    onDismissRequest()
                },
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Dismiss")
            }
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    Text(
                        text = "Update channel",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                item {
                    ChannelChoiceRow(
                        title = "yt-dlp",
                        subtitle = "Stable",
                        selected = ytdlpUpdateChannel == YT_DLP_STABLE,
                    ) {
                        ytdlpUpdateChannel = YT_DLP_STABLE
                    }
                }
                item {
                    ChannelChoiceRow(
                        title = "yt-dlp-nightly-builds",
                        subtitle = "Nightly",
                        selected = ytdlpUpdateChannel == YT_DLP_NIGHTLY,
                    ) {
                        ytdlpUpdateChannel = YT_DLP_NIGHTLY
                    }
                }

                item {
                    Text(
                        text = "Auto update",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                item {
                    AutoUpdateRow(
                        enabled = ytdlpAutoUpdate,
                        onToggle = { ytdlpAutoUpdate = it },
                    )
                }

                item {
                    Text(
                        text = "Update interval",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                item {
                    IntervalChoiceRow(
                        title = "Every day",
                        selected = updateInterval == INTERVAL_DAY,
                    ) {
                        updateInterval = INTERVAL_DAY
                    }
                }
                item {
                    IntervalChoiceRow(
                        title = "Every week",
                        selected = updateInterval == INTERVAL_WEEK,
                    ) {
                        updateInterval = INTERVAL_WEEK
                    }
                }
                item {
                    IntervalChoiceRow(
                        title = "Every month",
                        selected = updateInterval == INTERVAL_MONTH,
                    ) {
                        updateInterval = INTERVAL_MONTH
                    }
                }
            }
        },
    )
}

@Composable
private fun ChannelChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    RowItem(selected = selected, title = title, subtitle = subtitle, onClick = onClick)
}

@Composable
private fun IntervalChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    RowItem(selected = selected, title = title, subtitle = "", onClick = onClick)
}

@Composable
private fun AutoUpdateRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Enable auto update",
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
            )
        }
    }
}

@Composable
private fun RowItem(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
            Text(text = title)
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
