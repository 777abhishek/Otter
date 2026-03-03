package com.Otter.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Otter.app.R

@Composable
fun ConfirmButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(stringResource(R.string.confirm))
    }
}

@Composable
fun DismissButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(stringResource(R.string.dismiss))
    }
}

@Composable
fun ClearButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Outlined.Check, contentDescription = stringResource(R.string.clear))
    }
}

@Composable
fun SealDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        modifier = modifier,
    )
}

@Composable
fun PreferenceInfo(
    modifier: Modifier = Modifier,
    text: String,
    applyPaddings: Boolean = true,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = if (applyPaddings) modifier.padding(16.dp) else modifier,
    )
}
