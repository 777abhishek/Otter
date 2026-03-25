package com.Otter.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Otter.app.BuildConfig
import com.Otter.app.ui.common.LocalDarkTheme
import com.Otter.app.ui.components.Material3ExpressiveSettingsGroup
import com.Otter.app.ui.components.ModernInfoItem
import com.Otter.app.ui.viewmodels.SettingsViewModel
import com.Otter.app.util.DynamicIconProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettings(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    // Update bottom bar visibility based on scroll
    LaunchedEffect(scrollState.value) {
        onBottomBarVisibilityChanged(scrollState.value == 0)
    }

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkMode = remember { mutableStateOf("AUTO") }
    val useDarkTheme =
        remember(darkMode.value, isSystemInDarkTheme) {
            if (darkMode.value == "AUTO") isSystemInDarkTheme else darkMode.value == "ON"
        }

    val (iconBgColor, iconStyleColor) =
        Pair(
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.primary,
        )

    Column(
        modifier =
            Modifier
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
                text = "About",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // App Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val isDark = LocalDarkTheme.current.isDarkTheme
                val iconBitmap = remember(isDark) {
                    DynamicIconProvider.getIconBitmapForCompose(context, isDark, 512)
                }
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = "App Icon",
                        modifier = Modifier.size(60.dp),
                        contentScale = ContentScale.Fit,
                        filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Otter",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Project & links section
        Material3ExpressiveSettingsGroup(
            modifier = Modifier.fillMaxWidth(),
            items = listOf(
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.Description, null, modifier = Modifier.size(22.dp)) },
                        title = "Changelog",
                        subtitle = "What's new in this version",
                        onClick = { navController.navigate("changelog") },
                        showArrow = true,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = settings.iconShape,
                    )
                },
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.History, null, modifier = Modifier.size(22.dp)) },
                        title = "Commits",
                        subtitle = "Latest changes",
                        onClick = { navController.navigate("commits") },
                        showArrow = true,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = settings.iconShape,
                    )
                },
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.Code, null, modifier = Modifier.size(22.dp)) },
                        title = "GitHub",
                        subtitle = "Source code and releases",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/777abhishek/Otter"))
                            context.startActivity(Intent.createChooser(intent, "Open"))
                        },
                        showArrow = true,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = settings.iconShape,
                    )
                },
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.Chat, null, modifier = Modifier.size(22.dp)) },
                        title = "Discord",
                        subtitle = "Community and support",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg"))
                            context.startActivity(Intent.createChooser(intent, "Open"))
                        },
                        showArrow = true,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = settings.iconShape,
                    )
                },
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.Language, null, modifier = Modifier.size(22.dp)) },
                        title = "Website",
                        subtitle = "otterapp.vercel.app",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://otterapp.vercel.app/"))
                            context.startActivity(Intent.createChooser(intent, "Open"))
                        },
                        showArrow = true,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = settings.iconShape,
                    )
                },
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.People, null, modifier = Modifier.size(22.dp)) },
                        title = "Contributors",
                        subtitle = "People behind Otter",
                        onClick = { navController.navigate("contributors") },
                        showArrow = true,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = settings.iconShape,
                    )
                }
            )
        )

        // Legal Section
        Text(
            text = "Legal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        )

        Material3ExpressiveSettingsGroup(
            modifier = Modifier.fillMaxWidth(),
            items = listOf(
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.Policy, null, modifier = Modifier.size(22.dp)) },
                        title = "Privacy Policy",
                        subtitle = "How we handle your data",
                        onClick = { navController.navigate("privacyPolicy") },
                        showArrow = true,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = settings.iconShape,
                    )
                },
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.Gavel, null, modifier = Modifier.size(22.dp)) },
                        title = "Fair Use & Disclaimer",
                        subtitle = "Legal information about content",
                        onClick = { navController.navigate("fairUsePolicy") },
                        showArrow = true,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = settings.iconShape,
                    )
                },
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.Info, null, modifier = Modifier.size(22.dp)) },
                        title = "Third-Party Licenses",
                        subtitle = "Open source libraries we use",
                        onClick = { navController.navigate("thirdPartyLicenses") },
                        showArrow = true,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = settings.iconShape,
                    )
                }
            )
        )

        // Features Section
        Text(
            text = "Features",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp),
        )

        Material3ExpressiveSettingsGroup(
            modifier = Modifier.fillMaxWidth(),
            items = listOf(
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.VideoLibrary, null, modifier = Modifier.size(22.dp)) },
                        title = "YouTube Download",
                        subtitle = "Download videos in any quality, extract audio, or grab subtitles. Supports batch downloads and custom formats.",
                        onClick = null,
                        showArrow = false,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = settings.iconShape,
                    )
                },
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(22.dp)) },
                        title = "Built-in Player",
                        subtitle = "Stream content directly with gesture controls, background playback, and Picture-in-Picture support.",
                        onClick = null,
                        showArrow = false,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = settings.iconShape,
                    )
                },
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.CloudSync, null, modifier = Modifier.size(22.dp)) },
                        title = "Background Sync",
                        subtitle = "Automatically sync your library and subscriptions. Keep everything up to date without manual refresh.",
                        onClick = null,
                        showArrow = false,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = settings.iconShape,
                    )
                },
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.Palette, null, modifier = Modifier.size(22.dp)) },
                        title = "Material You Design",
                        subtitle = "Beautiful, modern Material 3 design with dynamic colors, expressive shapes, and smooth animations.",
                        onClick = null,
                        showArrow = false,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = settings.iconShape,
                    )
                },
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.Security, null, modifier = Modifier.size(22.dp)) },
                        title = "Privacy Focused",
                        subtitle = "No ads, no tracking, no data collection. Your viewing habits stay on your device.",
                        onClick = null,
                        showArrow = false,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = settings.iconShape,
                    )
                },
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.OfflineBolt, null, modifier = Modifier.size(22.dp)) },
                        title = "Offline Mode",
                        subtitle = "Watch downloaded content anywhere without internet. Perfect for travel and commutes.",
                        onClick = null,
                        showArrow = false,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = settings.iconShape,
                    )
                }
            )
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}
