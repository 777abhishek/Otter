package com.Otter.app.ui.download.configure

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Otter.app.R
import com.Otter.app.ui.components.ConfirmButton
import com.Otter.app.ui.components.DialogCheckBoxItem
import com.Otter.app.ui.components.DismissButton
import com.Otter.app.ui.components.FormatSubtitle
import com.Otter.app.ui.components.SealDialog
import com.Otter.app.util.SubtitleFormat

@Composable
internal fun UpdateSubtitleLanguageDialog(
    modifier: Modifier = Modifier,
    languages: Set<String>,
    onDismissRequest: () -> Unit = {},
    onConfirm: () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { ConfirmButton(onClick = onConfirm) },
        dismissButton = { DismissButton(onClick = onDismissRequest) },
        icon = { Icon(Icons.Outlined.Subtitles, null) },
        title = { Text(stringResource(R.string.subtitle_language)) },
        text = {
            LazyColumn {
                item {
                    Text(
                        text = stringResource(R.string.update_sub_language_msg),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                }
                for (language in languages) {
                    item {
                        DialogCheckBoxItem(
                            text = language,
                            checked = true,
                            onCheckedChange = {},
                        )
                    }
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
internal fun RenameDialog(
    initialValue: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initialValue) }
    SealDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.Edit, null) },
        title = { Text(stringResource(R.string.rename)) },
        confirmButton = {
            ConfirmButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank())
        },
        dismissButton = { DismissButton(onClick = onDismissRequest) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.title)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

@Composable
internal fun SubtitleSelectionDialog(
    suggestedSubtitles: Map<String, List<SubtitleFormat>>,
    autoCaptions: Map<String, List<SubtitleFormat>>,
    selectedSubtitles: List<String>,
    onDismissRequest: () -> Unit,
    onConfirm: (List<String>, List<String>) -> Unit,
) {
    val selected = remember { mutableStateListOf<String>().apply { addAll(selectedSubtitles) } }
    SealDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.Subtitles, null) },
        title = { Text(stringResource(R.string.subtitle_language)) },
        confirmButton = {
            ConfirmButton(onClick = { onConfirm(selected.toList(), emptyList()) })
        },
        dismissButton = { DismissButton(onClick = onDismissRequest) },
        text = {
            LazyColumn {
                if (suggestedSubtitles.isNotEmpty()) {
                    item {
                        FormatSubtitle(
                            text = stringResource(R.string.subtitle_suggested),
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                    for ((code, formats) in suggestedSubtitles) {
                        item {
                            DialogCheckBoxItem(
                                text = formats.first().run { name ?: protocol ?: code },
                                checked = selected.contains(code),
                                onCheckedChange = {
                                    if (selected.contains(code)) {
                                        selected.remove(code)
                                    } else {
                                        selected.add(code)
                                    }
                                },
                            )
                        }
                    }
                }
                if (autoCaptions.isNotEmpty()) {
                    item {
                        FormatSubtitle(
                            text = stringResource(R.string.auto_translated_captions),
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                    for ((code, formats) in autoCaptions) {
                        item {
                            DialogCheckBoxItem(
                                text = formats.first().run { name ?: protocol ?: code },
                                checked = selected.contains(code),
                                onCheckedChange = {
                                    if (selected.contains(code)) {
                                        selected.remove(code)
                                    } else {
                                        selected.add(code)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        },
    )
}
