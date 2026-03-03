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
fun PlayerQualitySheet(
    show: Boolean,
    onDismiss: () -> Unit,
    availableQualities: List<Int>,
    availableAudioQualities: List<Int> = emptyList(),
    isAudioOnly: Boolean = false,
    currentMaxHeight: Int = Int.MAX_VALUE,
    onSelectMaxHeight: (Int) -> Unit,
) {
    if (!show) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Standard options
    val standardVideoQualities =
        listOf(
            QualityOption("Auto", null, Int.MAX_VALUE, isAuto = true),
            QualityOption("4K", "2160p", 2160),
            QualityOption("2K", "1440p", 1440),
            QualityOption("Full HD", "1080p", 1080),
            QualityOption("HD", "720p", 720),
            QualityOption("SD", "480p", 480),
            QualityOption("Low", "360p", 360),
        )

    val standardAudioQualities =
        listOf(
            QualityOption("Auto", null, Int.MAX_VALUE, isAuto = true),
            QualityOption("320 kbps", null, 320),
            QualityOption("256 kbps", null, 256),
            QualityOption("192 kbps", null, 192),
            QualityOption("128 kbps", null, 128),
            QualityOption("96 kbps", null, 96),
            QualityOption("64 kbps", null, 64),
        )

    val displayList =
        if (isAudioOnly) {
            standardAudioQualities.filter { it.isAuto || availableAudioQualities.contains(it.value) }
        } else {
            standardVideoQualities.filter { it.isAuto || availableQualities.contains(it.value) }
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
                text = if (isAudioOnly) "Audio Quality" else "Video Quality",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(bottom = 12.dp),
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                items(displayList) { option ->
                    QualityOptionRow(
                        option = option,
                        isSelected = option.value == currentMaxHeight,
                        onSelect = {
                            onSelectMaxHeight(option.value)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

// ── Data ──────────────────────────────────────────────────────────────────────

private data class QualityOption(
    val label: String,
    val sublabel: String?,
    val value: Int,
    val isAuto: Boolean = false,
)

// ── Row ───────────────────────────────────────────────────────────────────────

@Composable
private fun QualityOptionRow(
    option: QualityOption,
    isSelected: Boolean,
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
        if (option.isAuto) {
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
                    text = option.label,
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
                if (option.sublabel != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = option.sublabel,
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
