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
fun FairUsePolicyScreen(
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
                text = "Fair Use & Disclaimer",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Important Notice Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = "Important Notice",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Otter does NOT host, store, or distribute any video content. All content is accessed through third-party services.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
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
                SectionTitle("How Otter Works")
                BodyText("Otter is a video player and download manager that uses open-source tools to access publicly available content. We use:")
                BulletItem("yt-dlp - An open-source video extraction tool")
                BulletItem("Invidious - Alternative YouTube frontends")
                BulletItem("Your device's built-in media player")

                SectionTitle("Fair Use Guidelines")
                BodyText("When using Otter, please respect copyright and fair use principles:")
                BulletItem("Only download content you have permission to access")
                BulletItem("Use downloaded content for personal, non-commercial purposes")
                BulletItem("Respect content creators' rights and terms of service")
                BulletItem("Do not redistribute downloaded content")

                SectionTitle("Disclaimer")
                BodyText("1. Otter is provided 'as-is' without any warranties.")
                BodyText("2. We are not responsible for how users utilize this application.")
                BodyText("3. Users are responsible for complying with their local laws and YouTube's Terms of Service.")
                BodyText("4. We do not condone piracy or copyright infringement.")

                SectionTitle("Open Source Tools")
                BodyText("Otter relies on these amazing open-source projects:")
                BulletItem("yt-dlp - Licensed under Unlicense")
                BulletItem("Media3/ExoPlayer - Licensed under Apache 2.0")
                BulletItem("Coil3 - Licensed under Apache 2.0")

                SectionTitle("Your Responsibility")
                BodyText("By using Otter, you agree to:")
                BulletItem("Use the app responsibly and legally")
                BulletItem("Respect intellectual property rights")
                BulletItem("Comply with applicable laws in your jurisdiction")
                BulletItem("Not use Otter for any illegal purposes")
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
