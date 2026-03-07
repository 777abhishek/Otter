package com.Otter.app.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Otter.app.data.auth.YouTubeProfile
import com.Otter.app.ui.components.Material3ExpressiveSettingsGroup
import com.Otter.app.ui.components.ModernInfoItem
import com.Otter.app.ui.viewmodels.ProfilesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesSettings(
    navController: NavController,
    onBack: () -> Unit,
    viewModel: ProfilesViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profiles by viewModel.profiles.collectAsState()
    val activeProfileId by viewModel.activeProfileId.collectAsState()
    var showAddProfileDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf<YouTubeProfile?>(null) }
    val scrollState = rememberScrollState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header with back button and title
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Profiles",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Instruction Card
        Material3ExpressiveSettingsGroup(
            modifier = Modifier.fillMaxWidth(),
            items =
                listOf(
                    {
                        ModernInfoItem(
                            icon = { Icon(Icons.Rounded.Person, null, modifier = Modifier.size(22.dp)) },
                            title = "Profile Management",
                            subtitle = "Add a profile, then manage cookie targets per profile.",
                            onClick = null,
                            showArrow = false,
                        )
                    },
                ),
        )

        if (profiles.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "No Profiles Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Tap 'Add Profile' to create your first profile. Then open Cookie Targets to connect or import cookies per website.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }

        if (profiles.isNotEmpty()) {
            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    profiles.map { profile ->
                        {
                            val isActive = profile.id == activeProfileId
                            ModernInfoItem(
                                icon = {
                                    Icon(
                                        imageVector = if (isActive) Icons.Rounded.Check else Icons.Rounded.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                    )
                                },
                                title = profile.label,
                                subtitle = if (isActive) "Active" else "Tap to set active",
                                onClick = {
                                    if (!isActive) {
                                        scope.launch { viewModel.setActiveProfile(profile.id) }
                                    }
                                },
                                showArrow = false,
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                navController.navigate("cookieTargets?profileId=${profile.id}")
                                            },
                                        ) {
                                            Icon(Icons.Rounded.Cookie, contentDescription = null)
                                        }
                                        IconButton(
                                            onClick = { showDeleteConfirmDialog = profile },
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    },
            )
        }

        Material3ExpressiveSettingsGroup(
            modifier = Modifier.fillMaxWidth(),
            items =
                listOf(
                    {
                        ModernInfoItem(
                            icon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(22.dp)) },
                            title = "Add Profile",
                            subtitle = "Create a new profile",
                            onClick = { showAddProfileDialog = true },
                            showArrow = true,
                        )
                    },
                ),
        )

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showAddProfileDialog) {
        var profileName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddProfileDialog = false },
            title = { Text("Add Profile") },
            text = {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (profileName.isNotBlank()) {
                            scope.launch {
                                viewModel.createProfile(profileName) { newProfileId ->
                                    navController.navigate("cookieTargets?profileId=$newProfileId")
                                }
                                showAddProfileDialog = false
                            }
                        } else {
                            Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                        }
                    },
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProfileDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    showDeleteConfirmDialog?.let { profile ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Delete Profile") },
            text = { Text("Are you sure you want to delete '${profile.label}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.deleteProfile(profile.id)
                            showDeleteConfirmDialog = null
                        }
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
fun ProfileItem(
    profile: YouTubeProfile,
    isActive: Boolean,
    onSetActive: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isActive) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (profile.isLoggedIn) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (profile.isLoggedIn) Icons.Rounded.Check else Icons.Rounded.Person,
                            contentDescription = null,
                            tint =
                                if (profile.isLoggedIn) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = profile.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text =
                                when {
                                    profile.isLoggedIn && isActive -> "Active - Ready to use"
                                    profile.isLoggedIn -> "Connected - Tap to activate"
                                    else -> "Not connected"
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                when {
                                    profile.isLoggedIn && isActive -> MaterialTheme.colorScheme.primary
                                    profile.isLoggedIn -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }

                if (isActive) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Active",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!isActive) {
                    OutlinedButton(
                        onClick = onSetActive,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        Text("Active", fontSize = 12.sp)
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
