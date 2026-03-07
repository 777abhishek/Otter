package com.Otter.app.ui.screens.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Otter.app.data.auth.CookieTarget
import com.Otter.app.ui.components.Material3ExpressiveSettingsGroup
import com.Otter.app.ui.components.ModernInfoItem
import com.Otter.app.ui.viewmodels.CookieTargetsViewModel
import java.io.File
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()

    val entriesByTargetId by viewModel.entriesForProfile(profileId).collectAsState()
    val targets by viewModel.allTargets.collectAsState()

    // Sheet visibility flags
    var showActionSheet by remember { mutableStateOf(false) }
    var showPasteSheet by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }

    // Sheet states
    val actionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pasteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ── Frozen snapshots captured at open-time ────────────────────────────────
    // The sheet NEVER reads from the live flow. All values are snapshotted the
    // moment the sheet opens, so no recomposition caused by flow emissions can
    // change what the sheet displays while it is visible or animating closed.
    var actionTarget by remember { mutableStateOf<CookieTarget?>(null) }
    var actionCookiesPath by remember { mutableStateOf<String?>(null) }  // frozen at open
    var actionEnabled by remember { mutableStateOf(false) }              // frozen at open
    var actionIsCustom by remember { mutableStateOf(false) }             // frozen at open

    var pasteTarget by remember { mutableStateOf<CookieTarget?>(null) }

    // Field state for paste + add sheets
    var pasteText by remember { mutableStateOf("") }
    var addTitle by remember { mutableStateOf("") }
    var addLoginUrl by remember { mutableStateOf("") }
    var addDomains by remember { mutableStateOf("") }

    // Capture the full snapshot here — called before showActionSheet flips true
    fun openActionSheet(target: CookieTarget) {
        val entry = entriesByTargetId[target.id]
        actionTarget = target
        actionCookiesPath = entry?.cookiesFilePath
        actionEnabled = entry?.enabledForYtDlp ?: false
        actionIsCustom = viewModel.isCustomTarget(target.id)
        showActionSheet = true
    }

    fun closeActionSheet(after: (() -> Unit)? = null) {
        scope.launch { actionSheetState.hide() }.invokeOnCompletion {
            showActionSheet = false
            actionTarget = null
            after?.invoke()
        }
    }

    fun openPasteSheet(target: CookieTarget) {
        pasteTarget = target
        pasteText = ""
        showPasteSheet = true
    }

    fun closePasteSheet(after: (() -> Unit)? = null) {
        scope.launch { pasteSheetState.hide() }.invokeOnCompletion {
            showPasteSheet = false
            pasteTarget = null
            after?.invoke()
        }
    }

    fun openAddSheet() {
        addTitle = ""
        addLoginUrl = ""
        addDomains = ""
        showAddSheet = true
    }

    fun closeAddSheet() {
        scope.launch { addSheetState.hide() }.invokeOnCompletion {
            showAddSheet = false
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let {
            val pendingTargetId = navController.currentBackStackEntry
                ?.savedStateHandle?.get<String>("pendingTargetId")
            if (pendingTargetId.isNullOrBlank()) {
                Toast.makeText(context, "No target selected", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            viewModel.importCookiesFromUri(context, profileId, pendingTargetId, uri)
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("pendingTargetId")
        }
    }

    // ── Main screen ───────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Cookie Targets",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { openAddSheet() }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items = listOf({
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
                }),
            )

            Spacer(modifier = Modifier.height(2.dp))

            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items = targets.map { target ->
                    @Composable {
                        key(target.id) {
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
                                subtitle = when {
                                    hasCookies && enabled -> "Enabled for yt-dlp"
                                    hasCookies -> "Cookies saved (disabled for yt-dlp)"
                                    else -> "No cookies"
                                },
                                onClick = { openActionSheet(target) },
                                showArrow = true,
                                trailingContent = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Checkbox(
                                            checked = enabled && hasCookies,
                                            enabled = hasCookies,
                                            onCheckedChange = { checked ->
                                                viewModel.setEnabledForYtDlp(profileId, target.id, checked)
                                            },
                                        )
                                        Text(
                                            text = "Use",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                            )
                        }
                    }
                },
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // ── Action sheet ──────────────────────────────────────────────────────────
    // Uses ONLY the frozen snapshots (actionTarget, actionCookiesPath, actionEnabled,
    // actionIsCustom). Zero live flow reads inside this sheet — no recomposition flicker.
    if (showActionSheet) {
        val target = actionTarget ?: return
        val cookiesPath = actionCookiesPath   // stable, won't change mid-animation
        val hasCookies = !cookiesPath.isNullOrBlank()

        fun shareCookiesFile() {
            if (cookiesPath.isNullOrBlank()) { Toast.makeText(context, "No cookies saved yet", Toast.LENGTH_SHORT).show(); return }
            val f = File(cookiesPath)
            if (!f.exists()) { Toast.makeText(context, "Cookies file not found", Toast.LENGTH_SHORT).show(); return }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "${target.title} cookies.txt")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Share cookies file"))
        }

        fun openCookiesFile() {
            if (cookiesPath.isNullOrBlank()) { Toast.makeText(context, "No cookies saved yet", Toast.LENGTH_SHORT).show(); return }
            val f = File(cookiesPath)
            if (!f.exists()) { Toast.makeText(context, "Cookies file not found", Toast.LENGTH_SHORT).show(); return }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/plain")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Open cookies file"))
        }

        ModalBottomSheet(
            onDismissRequest = { closeActionSheet() },
            sheetState = actionSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 20.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Login,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = target.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Connect
                FilledTonalButton(
                    onClick = {
                        closeActionSheet {
                            navController.navigate("webview_login?profileId=$profileId&targetId=${target.id}")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect with Login")
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                // Import
                Text(
                    text = "Import Cookies",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            closeActionSheet {
                                navController.currentBackStackEntry?.savedStateHandle?.set("pendingTargetId", target.id)
                                filePickerLauncher.launch("*/*")
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("From File")
                    }
                    OutlinedButton(
                        onClick = { closeActionSheet { openPasteSheet(target) } },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                // Export — always rendered, enabled state from frozen snapshot
                Text(
                    text = "Export Cookies",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { shareCookiesFile() },
                        enabled = hasCookies,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                    OutlinedButton(
                        onClick = { openCookiesFile() },
                        enabled = hasCookies,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                // Disconnect
                Text(
                    text = "Disconnect",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                OutlinedButton(
                    onClick = {
                        closeActionSheet {
                            viewModel.disconnect(profileId, target.id)
                            Toast.makeText(context, "Disconnected ${target.title}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnect Account")
                }

                // Delete — always rendered, visibility driven by frozen actionIsCustom snapshot
                if (actionIsCustom) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = {
                            closeActionSheet {
                                viewModel.deleteCustomTarget(target.id)
                                Toast.makeText(context, "Deleted ${target.title}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Target")
                    }
                }
            }
        }
    }

    // ── Paste sheet ───────────────────────────────────────────────────────────
    if (showPasteSheet) {
        val t = pasteTarget ?: return

        ModalBottomSheet(
            onDismissRequest = { closePasteSheet() },
            sheetState = pasteSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Paste cookies (.txt)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                OutlinedTextField(
                    value = pasteText,
                    onValueChange = { pasteText = it },
                    label = { Text("Netscape cookies.txt content") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { closePasteSheet() },
                        modifier = Modifier.weight(1f),
                    ) { Text("Cancel") }
                    Button(
                        onClick = {
                            val content = pasteText.trim()
                            if (content.isBlank()) {
                                Toast.makeText(context, "Paste cookies text first", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.importCookiesFromText(context, profileId, t.id, content)
                            Toast.makeText(context, "Cookies imported", Toast.LENGTH_SHORT).show()
                            closePasteSheet()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Import") }
                }
            }
        }
    }

    // ── Add sheet ─────────────────────────────────────────────────────────────
    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = addSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Add website",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                OutlinedTextField(
                    value = addTitle,
                    onValueChange = { addTitle = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = addLoginUrl,
                    onValueChange = { addLoginUrl = it },
                    label = { Text("Login URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = addDomains,
                    onValueChange = { addDomains = it },
                    label = { Text("Domains (comma or space separated)") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { closeAddSheet() },
                        modifier = Modifier.weight(1f),
                    ) { Text("Cancel") }
                    Button(
                        onClick = {
                            val t = addTitle.trim()
                            val u = addLoginUrl.trim()
                            val d = addDomains.trim()
                            when {
                                t.isBlank() ->
                                    Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                                u.isBlank() || !(u.startsWith("http://") || u.startsWith("https://")) ->
                                    Toast.makeText(context, "Please enter a valid Login URL (http/https)", Toast.LENGTH_SHORT).show()
                                d.isBlank() ->
                                    Toast.makeText(context, "Please enter at least one domain", Toast.LENGTH_SHORT).show()
                                else -> {
                                    viewModel.addCustomTarget(t, u, d)
                                    closeAddSheet()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Add") }
                }
            }
        }
    }
}