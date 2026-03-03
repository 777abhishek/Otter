package com.Otter.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Otter.app.data.download.ARIA2C
import com.Otter.app.data.download.AUTO_SUBTITLE
import com.Otter.app.data.download.AUTO_TRANSLATED_SUBTITLES
import com.Otter.app.data.download.AV1_HARDWARE_ACCELERATED
import com.Otter.app.data.download.CONCURRENT_FRAGMENTS
import com.Otter.app.data.download.COOKIES
import com.Otter.app.data.download.DOWNLOAD_ARCHIVE
import com.Otter.app.data.download.EMBED_METADATA
import com.Otter.app.data.download.MAX_RATE
import com.Otter.app.data.download.OUTPUT_TEMPLATE
import com.Otter.app.data.download.RATE_LIMIT
import com.Otter.app.data.download.RESTRICT_FILENAMES
import com.Otter.app.data.download.SPONSORBLOCK
import com.Otter.app.data.download.SPONSORBLOCK_API_URL
import com.Otter.app.data.download.SPONSORBLOCK_CHAPTER_TITLE
import com.Otter.app.data.download.SPONSORBLOCK_MARK_CATEGORIES
import com.Otter.app.data.download.SPONSORBLOCK_REMOVE_CATEGORIES
import com.Otter.app.data.download.SUBTITLE
import com.Otter.app.data.download.SUBTITLE_LANGUAGE
import com.Otter.app.ui.components.ButtonOption
import com.Otter.app.ui.components.ConnectedButtonGroup
import com.Otter.app.ui.components.Material3ExpressiveSettingsGroup
import com.Otter.app.ui.components.ModernInfoItem
import com.Otter.app.ui.viewmodels.SettingsViewModel
import com.Otter.app.util.PreferenceUtil.getBoolean
import com.Otter.app.util.PreferenceUtil.getInt
import com.Otter.app.util.PreferenceUtil.getString
import com.Otter.app.util.PreferenceUtil.updateBoolean
import com.Otter.app.util.PreferenceUtil.updateInt
import com.Otter.app.util.PreferenceUtil.updateString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSettings(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val settings by viewModel.settings.collectAsState()

    LaunchedEffect(scrollState.value) {
        onBottomBarVisibilityChanged(scrollState.value == 0)
    }

    val iconBgColor = MaterialTheme.colorScheme.primaryContainer
    val iconStyleColor = MaterialTheme.colorScheme.onPrimaryContainer

    var cookiesEnabled by remember { mutableStateOf(COOKIES.getBoolean()) }
    var aria2cEnabled by remember { mutableStateOf(ARIA2C.getBoolean()) }
    var restrictFilenamesEnabled by remember { mutableStateOf(RESTRICT_FILENAMES.getBoolean()) }
    var archiveEnabled by remember { mutableStateOf(DOWNLOAD_ARCHIVE.getBoolean()) }
    var embedMetadataEnabled by remember { mutableStateOf(EMBED_METADATA.getBoolean()) }
    var rateLimitEnabled by remember { mutableStateOf(RATE_LIMIT.getBoolean()) }
    var maxRateDraft by remember { mutableStateOf(MAX_RATE.getString()) }
    var outputTemplateDraft by remember { mutableStateOf(OUTPUT_TEMPLATE.getString()) }

    var sponsorBlockEnabled by remember { mutableStateOf(SPONSORBLOCK.getBoolean()) }
    var sponsorBlockMarkDraft by remember { mutableStateOf(SPONSORBLOCK_MARK_CATEGORIES.getString()) }
    var sponsorBlockRemoveDraft by remember { mutableStateOf(SPONSORBLOCK_REMOVE_CATEGORIES.getString()) }
    var sponsorBlockApiDraft by remember { mutableStateOf(SPONSORBLOCK_API_URL.getString()) }
    var sponsorBlockChapterTitleDraft by remember { mutableStateOf(SPONSORBLOCK_CHAPTER_TITLE.getString()) }

    var subtitlesEnabled by remember { mutableStateOf(SUBTITLE.getBoolean()) }
    var subtitleLanguageDraft by remember { mutableStateOf(SUBTITLE_LANGUAGE.getString()) }
    var autoSubtitleEnabled by remember { mutableStateOf(AUTO_SUBTITLE.getBoolean()) }
    var autoTranslatedSubtitlesEnabled by remember { mutableStateOf(AUTO_TRANSLATED_SUBTITLES.getBoolean()) }

    var concurrentFragments by remember { mutableStateOf(CONCURRENT_FRAGMENTS.getInt()) }
    var av1HardwareDecoding by remember { mutableStateOf(AV1_HARDWARE_ACCELERATED.getBoolean()) }

    var showRateLimitDialog by remember { mutableStateOf(false) }
    var showOutputTemplateDialog by remember { mutableStateOf(false) }
    var showSponsorBlockDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }

    if (showRateLimitDialog) {
        AlertDialog(
            onDismissRequest = { showRateLimitDialog = false },
            title = { Text("Rate limit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Enable rate limit", modifier = Modifier.weight(1f))
                        Switch(
                            checked = rateLimitEnabled,
                            onCheckedChange = { rateLimitEnabled = it },
                        )
                    }
                    OutlinedTextField(
                        value = maxRateDraft,
                        onValueChange = { maxRateDraft = it },
                        enabled = rateLimitEnabled,
                        label = { Text("Max rate (KB/s)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. 5000") },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        RATE_LIMIT.updateBoolean(rateLimitEnabled)
                        MAX_RATE.updateString(maxRateDraft.trim())
                        showRateLimitDialog = false
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRateLimitDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showOutputTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showOutputTemplateDialog = false },
            title = { Text("Output template") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = outputTemplateDraft,
                        onValueChange = { outputTemplateDraft = it },
                        label = { Text("Template") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("%(title).200B.%(ext)s") },
                    )
                    Text(
                        text = "Leave empty to use the default filename",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        OUTPUT_TEMPLATE.updateString(outputTemplateDraft.trim())
                        showOutputTemplateDialog = false
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showOutputTemplateDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showSponsorBlockDialog) {
        AlertDialog(
            onDismissRequest = { showSponsorBlockDialog = false },
            title = { Text("SponsorBlock") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Enable SponsorBlock", modifier = Modifier.weight(1f))
                        Switch(
                            checked = sponsorBlockEnabled,
                            onCheckedChange = { sponsorBlockEnabled = it },
                        )
                    }
                    OutlinedTextField(
                        value = sponsorBlockRemoveDraft,
                        onValueChange = { sponsorBlockRemoveDraft = it },
                        enabled = sponsorBlockEnabled,
                        label = { Text("Remove categories") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("default / sponsor,intro,outro") },
                    )
                    OutlinedTextField(
                        value = sponsorBlockMarkDraft,
                        onValueChange = { sponsorBlockMarkDraft = it },
                        enabled = sponsorBlockEnabled,
                        label = { Text("Mark categories (chapters)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("all,-preview") },
                    )
                    OutlinedTextField(
                        value = sponsorBlockChapterTitleDraft,
                        onValueChange = { sponsorBlockChapterTitleDraft = it },
                        enabled = sponsorBlockEnabled,
                        label = { Text("Chapter title template") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = sponsorBlockApiDraft,
                        onValueChange = { sponsorBlockApiDraft = it },
                        enabled = sponsorBlockEnabled,
                        label = { Text("API URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://sponsor.ajay.app") },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        SPONSORBLOCK.updateBoolean(sponsorBlockEnabled)
                        SPONSORBLOCK_REMOVE_CATEGORIES.updateString(sponsorBlockRemoveDraft.trim())
                        SPONSORBLOCK_MARK_CATEGORIES.updateString(sponsorBlockMarkDraft.trim())
                        SPONSORBLOCK_CHAPTER_TITLE.updateString(sponsorBlockChapterTitleDraft.trim())
                        SPONSORBLOCK_API_URL.updateString(sponsorBlockApiDraft.trim())
                        showSponsorBlockDialog = false
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSponsorBlockDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showSubtitleDialog) {
        AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            title = { Text("Subtitles") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Download subtitles", modifier = Modifier.weight(1f))
                        Switch(
                            checked = subtitlesEnabled,
                            onCheckedChange = { subtitlesEnabled = it },
                        )
                    }
                    OutlinedTextField(
                        value = subtitleLanguageDraft,
                        onValueChange = { subtitleLanguageDraft = it },
                        enabled = subtitlesEnabled,
                        label = { Text("Subtitle languages") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("en,hi,ta") },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Auto subtitles", modifier = Modifier.weight(1f))
                        Switch(
                            checked = autoSubtitleEnabled,
                            onCheckedChange = { autoSubtitleEnabled = it },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Allow translated subtitles", modifier = Modifier.weight(1f))
                        Switch(
                            checked = autoTranslatedSubtitlesEnabled,
                            onCheckedChange = { autoTranslatedSubtitlesEnabled = it },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        SUBTITLE.updateBoolean(subtitlesEnabled)
                        SUBTITLE_LANGUAGE.updateString(subtitleLanguageDraft.trim())
                        AUTO_SUBTITLE.updateBoolean(autoSubtitleEnabled)
                        AUTO_TRANSLATED_SUBTITLES.updateBoolean(autoTranslatedSubtitlesEnabled)
                        showSubtitleDialog = false
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSubtitleDialog = false }) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
            // Header with back button
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
                    text = "Downloads",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf(
                        {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(Icons.Rounded.Cookie, null, modifier = Modifier.size(22.dp)) },
                                        title = "Use cookies",
                                        subtitle = "Fix age-restricted / private videos",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                                Switch(
                                    checked = cookiesEnabled,
                                    onCheckedChange = {
                                        cookiesEnabled = it
                                        COOKIES.updateBoolean(it)
                                    },
                                    modifier = Modifier.padding(end = 20.dp),
                                )
                            }
                        },
                        {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(Icons.Rounded.Extension, null, modifier = Modifier.size(22.dp)) },
                                        title = "Use aria2",
                                        subtitle = "Segmented downloads for speed",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                                Switch(
                                    checked = aria2cEnabled,
                                    onCheckedChange = {
                                        aria2cEnabled = it
                                        ARIA2C.updateBoolean(it)
                                    },
                                    modifier = Modifier.padding(end = 20.dp),
                                )
                            }
                        },
                        {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = { Icon(Icons.Rounded.NetworkCheck, null, modifier = Modifier.size(22.dp)) },
                                            title = "Concurrent fragments",
                                            subtitle = "${concurrentFragments.coerceAtLeast(1)}",
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor,
                                            iconShape = settings.iconShape,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                val options =
                                    listOf(
                                        ButtonOption(1, "1"),
                                        ButtonOption(2, "2"),
                                        ButtonOption(4, "4"),
                                        ButtonOption(8, "8"),
                                    )
                                ConnectedButtonGroup(
                                    options = options,
                                    selectedValue = concurrentFragments.coerceAtLeast(1).let { if (it in setOf(1, 2, 4, 8)) it else 1 },
                                    onSelectionChange = {
                                        concurrentFragments = it
                                        CONCURRENT_FRAGMENTS.updateInt(it)
                                    },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp)
                                            .padding(bottom = 8.dp),
                                )
                            }
                        },
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.Tune, null, modifier = Modifier.size(22.dp)) },
                                title = "Rate limit",
                                subtitle = if (rateLimitEnabled) "Enabled" else "Disabled",
                                onClick = {
                                    rateLimitEnabled = RATE_LIMIT.getBoolean()
                                    maxRateDraft = MAX_RATE.getString()
                                    showRateLimitDialog = true
                                },
                                showArrow = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor,
                                iconShape = settings.iconShape,
                            )
                        },
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.Folder, null, modifier = Modifier.size(22.dp)) },
                                title = "Output template",
                                subtitle = outputTemplateDraft.ifBlank { "Default" },
                                onClick = {
                                    outputTemplateDraft = OUTPUT_TEMPLATE.getString()
                                    showOutputTemplateDialog = true
                                },
                                showArrow = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor,
                                iconShape = settings.iconShape,
                            )
                        },
                    ),
            )

            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf(
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.Download, null, modifier = Modifier.size(22.dp)) },
                                title = "SponsorBlock",
                                subtitle = if (sponsorBlockEnabled) "Enabled" else "Disabled",
                                onClick = {
                                    sponsorBlockEnabled = SPONSORBLOCK.getBoolean()
                                    sponsorBlockRemoveDraft = SPONSORBLOCK_REMOVE_CATEGORIES.getString()
                                    sponsorBlockMarkDraft = SPONSORBLOCK_MARK_CATEGORIES.getString()
                                    sponsorBlockChapterTitleDraft = SPONSORBLOCK_CHAPTER_TITLE.getString()
                                    sponsorBlockApiDraft = SPONSORBLOCK_API_URL.getString()
                                    showSponsorBlockDialog = true
                                },
                                showArrow = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor,
                                iconShape = settings.iconShape,
                            )
                        },
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.Subtitles, null, modifier = Modifier.size(22.dp)) },
                                title = "Subtitles",
                                subtitle = if (subtitlesEnabled) "Enabled" else "Disabled",
                                onClick = {
                                    subtitlesEnabled = SUBTITLE.getBoolean()
                                    subtitleLanguageDraft = SUBTITLE_LANGUAGE.getString()
                                    autoSubtitleEnabled = AUTO_SUBTITLE.getBoolean()
                                    autoTranslatedSubtitlesEnabled = AUTO_TRANSLATED_SUBTITLES.getBoolean()
                                    showSubtitleDialog = true
                                },
                                showArrow = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor,
                                iconShape = settings.iconShape,
                            )
                        },
                    ),
            )

            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf(
                        {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(Icons.Rounded.Tune, null, modifier = Modifier.size(22.dp)) },
                                        title = "Restrict filenames",
                                        subtitle = "Avoid special characters",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                                Switch(
                                    checked = restrictFilenamesEnabled,
                                    onCheckedChange = {
                                        restrictFilenamesEnabled = it
                                        RESTRICT_FILENAMES.updateBoolean(it)
                                    },
                                    modifier = Modifier.padding(end = 20.dp),
                                )
                            }
                        },
                        {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(Icons.Rounded.Extension, null, modifier = Modifier.size(22.dp)) },
                                        title = "Embed metadata",
                                        subtitle = "Write tags and artwork",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                                Switch(
                                    checked = embedMetadataEnabled,
                                    onCheckedChange = {
                                        embedMetadataEnabled = it
                                        EMBED_METADATA.updateBoolean(it)
                                    },
                                    modifier = Modifier.padding(end = 20.dp),
                                )
                            }
                        },
                        {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(Icons.Rounded.Extension, null, modifier = Modifier.size(22.dp)) },
                                        title = "Download archive",
                                        subtitle = "Prevent duplicates",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                                Switch(
                                    checked = archiveEnabled,
                                    onCheckedChange = {
                                        archiveEnabled = it
                                        DOWNLOAD_ARCHIVE.updateBoolean(it)
                                    },
                                    modifier = Modifier.padding(end = 20.dp),
                                )
                            }
                        },
                        {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(Icons.Rounded.Extension, null, modifier = Modifier.size(22.dp)) },
                                        title = "Prefer AV1 (if supported)",
                                        subtitle = "Higher quality at lower bitrate",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor,
                                        iconShape = settings.iconShape,
                                    )
                                }
                                Switch(
                                    checked = av1HardwareDecoding,
                                    onCheckedChange = {
                                        av1HardwareDecoding = it
                                        AV1_HARDWARE_ACCELERATED.updateBoolean(it)
                                    },
                                    modifier = Modifier.padding(end = 20.dp),
                                )
                            }
                        },
                    ),
            )

            Spacer(modifier = Modifier.height(80.dp))
    }
}
