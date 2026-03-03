package com.Otter.app.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Otter.app.data.auth.CookieTarget
import com.Otter.app.ui.components.Material3ExpressiveSettingsGroup
import com.Otter.app.ui.components.ModernInfoItem
import com.Otter.app.ui.viewmodels.CookieTargetsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookieTargetsScreen(
    navController: NavController,
    onBack: () -> Unit,
    profileId: String,
    viewModel: CookieTargetsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val entriesByTargetId by viewModel.entriesForProfile(profileId).collectAsState()
    val targets by viewModel.allTargets.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    var selectedTarget by remember { mutableStateOf<CookieTarget?>(null) }
    var showActionsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let {
                val pendingTargetId = (
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.get<String>("pendingTargetId")
                )

                if (pendingTargetId.isNullOrBlank()) {
                    Toast.makeText(context, "No target selected", Toast.LENGTH_SHORT).show()
                    return@rememberLauncherForActivityResult
                }

                viewModel.importCookiesFromUri(context, profileId, pendingTargetId, uri)
                navController.currentBackStackEntry?.savedStateHandle?.remove<String>("pendingTargetId")
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cookie Targets") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf(
                        {
                            ModernInfoItem(
                                icon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Login,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                    )
                                },
                                title = "Cookie access",
                                subtitle = "Cookies are used only for yt-dlp, and only when enabled for a target.",
                                onClick = null,
                                showArrow = false,
                            )
                        },
                    ),
            )

            Spacer(modifier = Modifier.height(2.dp))

            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    targets.map { target ->
                        @Composable {
                            val entry = entriesByTargetId[target.id]
                            val enabled = entry?.enabledForYtDlp ?: false
                            val hasCookies = !entry?.cookiesFilePath.isNullOrBlank()

                            ModernInfoItem(
                                icon = {
                                    Icon(
                                        imageVector = if (hasCookies) Icons.Default.FileUpload else Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                    )
                                },
                                title = target.title,
                                subtitle =
                                    when {
                                        hasCookies && enabled -> "Enabled for yt-dlp"
                                        hasCookies -> "Cookies saved (disabled for yt-dlp)"
                                        else -> "No cookies"
                                    },
                                onClick = {
                                    selectedTarget = target
                                    showActionsSheet = true
                                },
                                showArrow = true,
                                trailingContent = {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Use",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Switch(
                                            checked = enabled,
                                            onCheckedChange = { checked ->
                                                viewModel.setEnabledForYtDlp(profileId, target.id, checked)
                                            },
                                        )
                                    }
                                },
                            )
                        }
                    },
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showActionsSheet && selectedTarget != null) {
        val target = selectedTarget ?: return
        ModalBottomSheet(
            onDismissRequest = { showActionsSheet = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = target.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                HorizontalDivider()

                FilledTonalButton(
                    onClick = {
                        showActionsSheet = false
                        navController.navigate("webview_login?profileId=$profileId&targetId=${target.id}")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect")
                }

                OutlinedButton(
                    onClick = {
                        navController.currentBackStackEntry?.savedStateHandle?.set("pendingTargetId", target.id)
                        filePickerLauncher.launch("*/*")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import cookies.txt")
                }

                OutlinedButton(
                    onClick = {
                        viewModel.disconnect(profileId, target.id)
                        Toast.makeText(context, "Cleared ${target.title}", Toast.LENGTH_SHORT).show()
                        showActionsSheet = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear")
                }

                if (viewModel.isCustomTarget(target.id)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.deleteCustomTarget(target.id)
                            Toast.makeText(context, "Deleted ${target.title}", Toast.LENGTH_SHORT).show()
                            showActionsSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var loginUrl by remember { mutableStateOf("") }
        var domains by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add website") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = loginUrl,
                        onValueChange = { loginUrl = it },
                        label = { Text("Login URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = domains,
                        onValueChange = { domains = it },
                        label = { Text("Domains (comma or space separated)") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val t = title.trim()
                        val u = loginUrl.trim()
                        val d = domains.trim()

                        if (t.isBlank()) {
                            Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        if (u.isBlank() || !(u.startsWith("http://") || u.startsWith("https://"))) {
                            Toast.makeText(context, "Please enter a valid Login URL (http/https)", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        if (d.isBlank()) {
                            Toast.makeText(context, "Please enter at least one domain", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }

                        viewModel.addCustomTarget(t, u, d)
                        showAddDialog = false
                    },
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
