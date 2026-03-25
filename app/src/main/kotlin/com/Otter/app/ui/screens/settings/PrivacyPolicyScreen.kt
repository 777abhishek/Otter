package com.Otter.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    navController: NavController,
    onBack: (() -> Unit)? = null,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
                text = "Privacy Policy",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Last updated: March 2026",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SectionTitle("1. Data Collection")
                BodyText("Otter is designed with privacy as a core principle. We collect minimal data and only when you explicitly opt-in.")

                SectionTitle("2. Analytics")
                BodyText("When you enable analytics, we collect anonymous usage statistics to improve the app. This includes:")
                BulletItem("Feature usage patterns")
                BulletItem("App performance metrics")
                BulletItem("Crash reports (if enabled)")
                BulletItem("Device type and OS version")

                SectionTitle("3. Crash Reporting")
                BodyText("When enabled, crash reports help us fix bugs. Reports are automatically redacted to remove potential PII like emails, tokens, and URLs with sensitive query parameters.")

                SectionTitle("4. Local Data")
                BodyText("All your playlists, subscriptions, downloads, and viewing history are stored locally on your device. We do not have access to this data.")

                SectionTitle("5. Third-Party Services")
                BodyText("Otter uses YouTube-dl (yt-dlp) to fetch video information. Your viewing habits are not shared with any third parties.")

                SectionTitle("6. Your Rights")
                BodyText("You can:")
                BulletItem("Delete all your data from our servers at any time")
                BulletItem("Disable analytics and crash reporting")
                BulletItem("Export or delete local data from app settings")

                SectionTitle("7. Contact")
                BodyText("For privacy concerns, contact us at: privacy@otterapp.dev")
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun BulletItem(text: String) {
    Row(
        modifier = Modifier.padding(start = 8.dp),
    ) {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
