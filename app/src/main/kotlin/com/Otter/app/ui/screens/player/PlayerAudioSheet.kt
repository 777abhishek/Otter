package com.Otter.app.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerAudioSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    selectedAudioLanguage: String?,
    selectedAudioStreamSignature: Triple<String?, String?, Int?>? = null,
    availableAudioLanguages: List<String>,
    audioTrackGroups: List<com.Otter.app.player.AudioTrackGroup> = emptyList(),
    streamInfo: com.Otter.app.data.repositories.StreamInfoResult? = null,
    onAudioLanguageSelected: (String?) -> Unit,
    onAudioStreamSignatureSelected: (language: String?, codec: String?, bitrate: Int?) -> Unit = { _, _, _ -> },
    onSelectAudioTrack: (groupIndex: Int, trackIndex: Int) -> Unit = { _, _ -> },
) {
    if (!show) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    data class AudioOption(
        val label: String,
        val languageTag: String?,
        val trackGroup: com.Otter.app.player.AudioTrackGroup?,
        val sublabel: String? = null,
        val streamIndex: Int? = null,
        val streamCodec: String? = null,
        val streamBitrate: Int? = null,
    )

    // Build audio options from available sources
    val audioOptions =
        when {
            streamInfo != null && streamInfo.audioStreams.isNotEmpty() -> {
                streamInfo.audioStreams.mapIndexed { index, audio ->
                    val lang = audio.language?.takeIf { it.isNotBlank() }
                    val bitrateKbps = (audio.bitrate / 1000).takeIf { it > 0 }
                    val codec = audio.codec.takeIf { it.isNotBlank() }
                    val label =
                        buildString {
                            append(lang ?: "Unknown")
                        }
                    val sublabel =
                        buildString {
                            if (bitrateKbps != null) append(bitrateKbps).append("kbps")
                            if (codec != null) {
                                if (bitrateKbps != null) append(" • ")
                                append(codec)
                            }
                        }.takeIf { it.isNotBlank() }
                    AudioOption(
                        label = label,
                        languageTag = lang,
                        trackGroup = null,
                        sublabel = sublabel,
                        streamIndex = index,
                        streamCodec = codec,
                        streamBitrate = audio.bitrate,
                    )
                }
            }
            audioTrackGroups.isNotEmpty() -> {
                audioTrackGroups.map {
                    AudioOption(
                        label = it.label,
                        languageTag = it.languageTag,
                        trackGroup = it,
                        sublabel = if (it.isSelected) "Active" else null,
                    )
                }
            }
            else -> {
                availableAudioLanguages.map { lang ->
                    AudioOption(label = lang, languageTag = lang, trackGroup = null)
                }
            }
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
        ) {
            // Header
            Text(
                text = "Audio Language",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(bottom = 12.dp),
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                // Audio options
                items(audioOptions) { option ->
                    val isSelected =
                        when {
                            option.trackGroup != null -> option.trackGroup.isSelected
                            option.streamCodec != null || option.streamBitrate != null -> {
                                selectedAudioStreamSignature == Triple(option.languageTag, option.streamCodec, option.streamBitrate)
                            }
                            option.languageTag != null -> selectedAudioLanguage == option.languageTag
                            else -> false
                        }

                    AudioOptionRow(
                        label = option.label,
                        sublabel = option.sublabel,
                        isSelected = isSelected,
                        isAuto = false,
                        onSelect = {
                            val trackGroup = option.trackGroup
                            if (trackGroup != null) {
                                onSelectAudioTrack(trackGroup.groupIndex, trackGroup.trackIndex)
                            } else {
                                if (option.streamCodec != null || option.streamBitrate != null) {
                                    onAudioStreamSignatureSelected(option.languageTag, option.streamCodec, option.streamBitrate)
                                } else {
                                    onAudioLanguageSelected(option.languageTag)
                                }
                            }
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioOptionRow(
    label: String,
    sublabel: String?,
    isSelected: Boolean,
    isAuto: Boolean,
    onSelect: () -> Unit,
) {
    val bgColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            Color.Transparent
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(bgColor)
                .clickable(onClick = onSelect)
                .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Auto star icon
        if (isAuto) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        // Label
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
                if (sublabel != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sublabel,
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (isSelected) 0.85f else 0.6f,
                            ),
                    )
                }
            }
        }

        // Selected check
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
