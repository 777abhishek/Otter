package com.Otter.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.Otter.app.data.models.IconShape
import com.Otter.app.ui.components.Material3ExpressiveSettingsGroup
import com.Otter.app.ui.components.ModernInfoItem

@Composable
fun SettingsListContent(
    iconBgColor: androidx.compose.ui.graphics.Color,
    iconStyleColor: androidx.compose.ui.graphics.Color,
    iconShape: IconShape,
    onNavigate: (String) -> Unit,
) {
    Material3ExpressiveSettingsGroup(
        modifier = Modifier.fillMaxWidth(),
        items =
            listOf(
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.People, null, modifier = Modifier.size(22.dp)) },
                        title = "Profiles",
                        subtitle = "Manage YouTube accounts and Cookies Files",
                        onClick = { onNavigate("profilesSettings") },
                        showArrow = true,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = iconShape,
                    )
                },
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.Palette, null, modifier = Modifier.size(22.dp)) },
                        title = "Appearance",
                        subtitle = "Theme, colors, and styles",
                        onClick = { onNavigate("appearanceSettings") },
                        showArrow = true,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = iconShape,
                    )
                },
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.Storage, null, modifier = Modifier.size(22.dp)) },
                        title = "Content",
                        subtitle = "Download location and quality",
                        onClick = { onNavigate("contentSettings") },
                        showArrow = true,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = iconShape,
                    )
                },
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.Download, null, modifier = Modifier.size(22.dp)) },
                        title = "Downloads",
                        subtitle = "Speed, SponsorBlock, subtitles, and more",
                        onClick = { onNavigate("downloadSettings") },
                        showArrow = true,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = iconShape,
                    )
                },
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.SdCard, null, modifier = Modifier.size(22.dp)) },
                        title = "Storage",
                        subtitle = "Storage and cache settings",
                        onClick = { onNavigate("storageSettings") },
                        showArrow = true,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = iconShape,
                    )
                },
            ),
    )

    Spacer(modifier = Modifier.height(5.dp))

    // General Settings Section
    Material3ExpressiveSettingsGroup(
        modifier = Modifier.fillMaxWidth(),
        items =
            listOf(
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.Notifications, null, modifier = Modifier.size(22.dp)) },
                        title = "Notifications",
                        subtitle = "Download and update alerts",
                        onClick = { onNavigate("notificationSettings") },
                        showArrow = true,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = iconShape,
                    )
                },
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.BatteryChargingFull, null, modifier = Modifier.size(22.dp)) },
                        title = "Power Saver",
                        subtitle = "Optimize battery usage",
                        onClick = { onNavigate("powerSaverSettings") },
                        showArrow = true,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = iconShape,
                    )
                },
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(22.dp)) },
                        title = "Privacy",
                        subtitle = "Data and privacy settings",
                        onClick = { onNavigate("privacySettings") },
                        showArrow = true,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = iconShape,
                    )
                },
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.Backup, null, modifier = Modifier.size(22.dp)) },
                        title = "Backup & Restore",
                        subtitle = "Save and restore settings",
                        onClick = { onNavigate("backupAndRestore") },
                        showArrow = true,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = iconShape,
                    )
                },
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.NewReleases, null, modifier = Modifier.size(22.dp)) },
                        title = "Updates",
                        subtitle = "yt-dlp and more",
                        onClick = { onNavigate("updatesSettings") },
                        showArrow = true,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = iconShape,
                    )
                },
            ),
    )

    Spacer(modifier = Modifier.height(10.dp))

    // About Section
    Material3ExpressiveSettingsGroup(
        modifier = Modifier.fillMaxWidth(),
        items =
            listOf(
                {
                    ModernInfoItem(
                        icon = { Icon(Icons.Rounded.Info, null, modifier = Modifier.size(22.dp)) },
                        title = "About",
                        subtitle = "Version and information",
                        onClick = { onNavigate("aboutSettings") },
                        showArrow = true,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        iconShape = iconShape,
                    )
                },
            ),
    )
}
