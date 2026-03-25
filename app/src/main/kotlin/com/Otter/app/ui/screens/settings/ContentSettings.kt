package com.Otter.app.ui.screens.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Otter.app.data.download.PROXY
import com.Otter.app.data.download.PROXY_URL
import com.Otter.app.data.models.PreferredCodec
import com.Otter.app.data.models.SponsorBlockCategory
import com.Otter.app.data.models.StreamFormatPreference
import com.Otter.app.data.models.StreamingQuality
import com.Otter.app.ui.components.ButtonOption
import com.Otter.app.ui.components.ConnectedButtonGroup
import com.Otter.app.ui.components.Material3ExpressiveSettingsGroup
import com.Otter.app.ui.components.ModernInfoItem
import com.Otter.app.ui.viewmodels.SettingsViewModel
import com.Otter.app.util.PreferenceUtil.getBoolean
import com.Otter.app.util.PreferenceUtil.getString
import com.Otter.app.util.PreferenceUtil.updateBoolean
import com.Otter.app.util.PreferenceUtil.updateString
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettings(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    // Update bottom bar visibility based on scroll
    LaunchedEffect(scrollState.value) {
        onBottomBarVisibilityChanged(scrollState.value == 0)
    }

    val (settingsShapeTertiary, _) = remember { mutableStateOf(false) }
    val (darkMode, _) = remember { mutableStateOf("AUTO") }

    val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val useDarkTheme =
        remember(darkMode, isSystemInDarkTheme) {
            if (darkMode == "AUTO") isSystemInDarkTheme else darkMode == "ON"
        }

    val (iconBgColor, iconStyleColor) =
        if (settingsShapeTertiary) {
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        }

    var showPathDialog by remember { mutableStateOf(false) }
    var downloadPathDraft by remember(settings.downloadPath) { mutableStateOf(settings.downloadPath) }
    var videoQuality by remember(settings.defaultVideoQuality) { mutableStateOf(settings.defaultVideoQuality) }

    var showCountryPicker by remember { mutableStateOf(false) }
    var showProxyDialog by remember { mutableStateOf(false) }
    var proxyEnabled by remember { mutableStateOf(PROXY.getBoolean()) }
    var proxyUrlDraft by remember { mutableStateOf(PROXY_URL.getString()) }
    var countrySearchQuery by remember { mutableStateOf("") }

    // Streaming settings state
    var streamingQuality by remember(settings.streamingQuality) { mutableStateOf(settings.streamingQuality) }
    var streamFormatPreference by remember(settings.streamFormatPreference) { mutableStateOf(settings.streamFormatPreference) }
    var preferredCodec by remember(settings.preferredCodec) { mutableStateOf(settings.preferredCodec) }
    var streamingPreferredAudioLanguage by remember(settings.streamingPreferredAudioLanguage) {
        mutableStateOf(settings.streamingPreferredAudioLanguage)
    }
    var showSponsorBlockCategories by remember { mutableStateOf(false) }
    var tempSponsorBlockCategories by remember(settings.sponsorBlockCategories) { mutableStateOf(settings.sponsorBlockCategories) }

    val scope = rememberCoroutineScope()

    val resolvedCountryCode =
        remember(settings.countryCode) {
            if (settings.countryCode == "system") Locale.getDefault().country.ifBlank { "US" } else settings.countryCode
        }

    val countryName =
        remember(resolvedCountryCode) {
            runCatching { Locale("", resolvedCountryCode).displayCountry }.getOrDefault(resolvedCountryCode)
        }

    fun openSystemLanguageSettings() {
        val intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Intent(Settings.ACTION_LOCALE_SETTINGS)
            } else {
                Intent(Settings.ACTION_LOCALE_SETTINGS)
            }
        kotlin.runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    if (showCountryPicker) {
        val countries =
            remember {
                listOf("system" to "System Default") +
                    Locale.getISOCountries()
                        .map { code -> code to Locale("", code).displayCountry }
                        .filter { it.second.isNotBlank() }
                        .sortedBy { it.second.lowercase(Locale.getDefault()) }
            }

        val filteredCountries =
            remember(countrySearchQuery, countries) {
                if (countrySearchQuery.isBlank()) {
                    countries
                } else {
                    countries.filter { (code, name) ->
                        name.contains(countrySearchQuery, ignoreCase = true) ||
                            code.contains(countrySearchQuery, ignoreCase = true)
                    }
                }
            }

        AlertDialog(
            onDismissRequest = { showCountryPicker = false },
            title = { Text("Select Country") },
            text = {
                if (filteredCountries.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("No countries found")
                    }
                } else {
                    LazyColumn {
                        items(filteredCountries) { (code, name) ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setCountryCode(code)
                                            showCountryPicker = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = settings.countryCode.equals(code, ignoreCase = true),
                                    onClick = null,
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(name)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCountryPicker = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    var showPreferredAudioLanguageDialog by remember { mutableStateOf(false) }
    var preferredAudioLanguageDraft by remember(streamingPreferredAudioLanguage) { mutableStateOf(streamingPreferredAudioLanguage) }

    if (showPreferredAudioLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showPreferredAudioLanguageDialog = false },
            title = { Text("Preferred audio language") },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = preferredAudioLanguageDraft,
                        onValueChange = { preferredAudioLanguageDraft = it },
                        label = { Text("Language tag") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("system / hi / en / ta / te ...") },
                    )

                    val quick =
                        listOf(
                            "system" to "System",
                            "hi" to "Hindi",
                            "en" to "English",
                            "ta" to "Tamil",
                        )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        quick.forEach { (tag, label) ->
                            AssistChip(
                                onClick = { preferredAudioLanguageDraft = tag },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val normalized = preferredAudioLanguageDraft.trim().ifBlank { "system" }
                        streamingPreferredAudioLanguage = normalized
                        viewModel.setStreamingPreferredAudioLanguage(normalized)
                        showPreferredAudioLanguageDialog = false
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreferredAudioLanguageDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showProxyDialog) {
        AlertDialog(
            onDismissRequest = { showProxyDialog = false },
            title = { Text("Proxy settings") },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Enable proxy", modifier = Modifier.weight(1f))
                        Switch(checked = proxyEnabled, onCheckedChange = { proxyEnabled = it })
                    }
                    OutlinedTextField(
                        value = proxyUrlDraft,
                        onValueChange = { proxyUrlDraft = it },
                        enabled = proxyEnabled,
                        label = { Text("Proxy URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("http://127.0.0.1:1080") },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        PROXY.updateBoolean(proxyEnabled)
                        PROXY_URL.updateString(proxyUrlDraft.trim())
                        showProxyDialog = false
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProxyDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showSponsorBlockCategories) {
        AlertDialog(
            onDismissRequest = { showSponsorBlockCategories = false },
            title = { Text("SponsorBlock Categories") },
            text = {
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    items(SponsorBlockCategory.entries) { category ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        tempSponsorBlockCategories =
                                            if (category in tempSponsorBlockCategories) {
                                                tempSponsorBlockCategories - category
                                            } else {
                                                tempSponsorBlockCategories + category
                                            }
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = category in tempSponsorBlockCategories,
                                onCheckedChange = null,
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setSponsorBlockCategories(tempSponsorBlockCategories)
                        showSponsorBlockCategories = false
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSponsorBlockCategories = false }) {
                    Text("Cancel")
                }
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
                    text = "Content",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Text(
                text = "LANGUAGE & REGION",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf(
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.Language, null, modifier = Modifier.size(22.dp)) },
                                title = "Language",
                                subtitle =
                                    if (settings.language == "system") {
                                        "System Default"
                                    } else {
                                        Locale(
                                            settings.language,
                                        ).displayLanguage
                                    },
                                onClick = { openSystemLanguageSettings() },
                                showArrow = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor,
                                iconShape = settings.iconShape,
                            )
                        },
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.Public, null, modifier = Modifier.size(22.dp)) },
                                title = "Country",
                                subtitle = if (settings.countryCode == "system") "System Default" else countryName.ifBlank { settings.countryCode },
                                onClick = { showCountryPicker = true },
                                showArrow = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor,
                                iconShape = settings.iconShape,
                            )
                        },
                    ),
            )

            // Network Section
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "NETWORK",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf(
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.SettingsEthernet, null, modifier = Modifier.size(22.dp)) },
                                title = "Proxy Settings",
                                subtitle = "Configure proxy server",
                                onClick = {
                                    proxyEnabled = PROXY.getBoolean()
                                    proxyUrlDraft = PROXY_URL.getString()
                                    showProxyDialog = true
                                },
                                showArrow = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor,
                                iconShape = settings.iconShape,
                            )
                        },
                        {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = { Icon(Icons.Rounded.Download, null, modifier = Modifier.size(22.dp)) },
                                            title = "Max concurrent downloads",
                                            subtitle = "${settings.maxConcurrentDownloads}",
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
                                        ButtonOption(3, "3"),
                                        ButtonOption(4, "4"),
                                        ButtonOption(5, "5"),
                                    )
                                ConnectedButtonGroup(
                                    options = options,
                                    selectedValue = settings.maxConcurrentDownloads.coerceIn(1, 5),
                                    onSelectionChange = { viewModel.setMaxConcurrentDownloads(it) },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp),
                                )
                            }
                        },
                    ),
            )

            // Streaming Section
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "STREAMING",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items =
                    listOf(
                        {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = { Icon(Icons.Rounded.PlayCircle, null, modifier = Modifier.size(22.dp)) },
                                            title = "Streaming Quality",
                                            subtitle = "Preferred streaming quality",
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor,
                                            iconShape = settings.iconShape,
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val streamingQualityOptions =
                                    listOf<ButtonOption<StreamingQuality>>(
                                        ButtonOption(StreamingQuality.AUTO, "Auto"),
                                        ButtonOption(StreamingQuality.HD_720P, "720p"),
                                        ButtonOption(StreamingQuality.HD_1080P, "1080p"),
                                        ButtonOption(StreamingQuality.HIGHEST, "Highest"),
                                    )
                                ConnectedButtonGroup(
                                    options = streamingQualityOptions,
                                    selectedValue = streamingQuality,
                                    onSelectionChange = {
                                        streamingQuality = it
                                        viewModel.setStreamingQuality(it)
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp),
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
                                            icon = { Icon(Icons.Rounded.SettingsInputComponent, null, modifier = Modifier.size(22.dp)) },
                                            title = "Stream Format",
                                            subtitle = "Preferred streaming format",
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor,
                                            iconShape = settings.iconShape,
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val formatOptions =
                                    listOf<ButtonOption<StreamFormatPreference>>(
                                        ButtonOption(StreamFormatPreference.AUTO, "Auto"),
                                        ButtonOption(StreamFormatPreference.HLS, "HLS"),
                                        ButtonOption(StreamFormatPreference.DASH, "DASH"),
                                        ButtonOption(StreamFormatPreference.PROGRESSIVE, "Progressive"),
                                    )
                                ConnectedButtonGroup(
                                    options = formatOptions,
                                    selectedValue = streamFormatPreference,
                                    onSelectionChange = {
                                        streamFormatPreference = it
                                        viewModel.setStreamFormatPreference(it)
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp),
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
                                            icon = { Icon(Icons.Rounded.VideoSettings, null, modifier = Modifier.size(22.dp)) },
                                            title = "Preferred Codec",
                                            subtitle = "Video codec preference",
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor,
                                            iconShape = settings.iconShape,
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val codecOptions =
                                    listOf<ButtonOption<PreferredCodec>>(
                                        ButtonOption(PreferredCodec.AUTO, "Auto"),
                                        ButtonOption(PreferredCodec.H264, "H.264"),
                                        ButtonOption(PreferredCodec.VP9, "VP9"),
                                        ButtonOption(PreferredCodec.AV1, "AV1"),
                                    )
                                ConnectedButtonGroup(
                                    options = codecOptions,
                                    selectedValue = preferredCodec,
                                    onSelectionChange = {
                                        preferredCodec = it
                                        viewModel.setPreferredCodec(it)
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp),
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
                                            icon = { Icon(Icons.Rounded.DataSaverOn, null, modifier = Modifier.size(22.dp)) },
                                            title = "Data Saver",
                                            subtitle = "Reduce data usage while streaming",
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor,
                                            iconShape = settings.iconShape,
                                        )
                                    }
                                    Switch(
                                        checked = settings.streamingDataSaver,
                                        onCheckedChange = { viewModel.setStreamingDataSaver(it) },
                                        modifier = Modifier.padding(end = 20.dp),
                                    )
                                }
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
                                            icon = { Icon(Icons.Rounded.Audiotrack, null, modifier = Modifier.size(22.dp)) },
                                            title = "Audio Only Mode",
                                            subtitle = "Stream audio without video",
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor,
                                            iconShape = settings.iconShape,
                                        )
                                    }
                                    Switch(
                                        checked = settings.streamingAudioOnly,
                                        onCheckedChange = { viewModel.setStreamingAudioOnly(it) },
                                        modifier = Modifier.padding(end = 20.dp),
                                    )
                                }
                            }
                        },
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.Translate, null, modifier = Modifier.size(22.dp)) },
                                title = "Preferred audio language",
                                subtitle =
                                    when (settings.streamingPreferredAudioLanguage.lowercase()) {
                                        "system" -> "System Default"
                                        "hi" -> "Hindi"
                                        "en" -> "English"
                                        else -> settings.streamingPreferredAudioLanguage
                                    },
                                onClick = {
                                    preferredAudioLanguageDraft = settings.streamingPreferredAudioLanguage
                                    showPreferredAudioLanguageDialog = true
                                },
                                showArrow = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor,
                                iconShape = settings.iconShape,
                            )
                        },
                        {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = { Icon(Icons.Rounded.MusicNote, null, modifier = Modifier.size(22.dp)) },
                                            title = "Mini Player in Audio Mode",
                                            subtitle = "Show mini player at bottom when streaming audio",
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor,
                                            iconShape = settings.iconShape,
                                        )
                                    }
                                    Switch(
                                        checked = settings.showMiniPlayerInAudioMode,
                                        onCheckedChange = { viewModel.setShowMiniPlayerInAudioMode(it) },
                                        modifier = Modifier.padding(end = 20.dp),
                                    )
                                }
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
                                            icon = { Icon(Icons.Rounded.Subtitles, null, modifier = Modifier.size(22.dp)) },
                                            title = "Subtitles",
                                            subtitle = "Show subtitles when available",
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor,
                                            iconShape = settings.iconShape,
                                        )
                                    }
                                    Switch(
                                        checked = settings.streamingSubtitlesEnabled,
                                        onCheckedChange = { viewModel.setStreamingSubtitlesEnabled(it) },
                                        modifier = Modifier.padding(end = 20.dp),
                                    )
                                }
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
                                            icon = { Icon(Icons.Rounded.Block, null, modifier = Modifier.size(22.dp)) },
                                            title = "SponsorBlock",
                                            subtitle = "Skip sponsor segments in videos",
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor,
                                            iconShape = settings.iconShape,
                                        )
                                    }
                                    Switch(
                                        checked = settings.sponsorBlockEnabled,
                                        onCheckedChange = { viewModel.setSponsorBlockEnabled(it) },
                                        modifier = Modifier.padding(end = 20.dp),
                                    )
                                }
                            }
                        },
                        {
                            ModernInfoItem(
                                icon = { Icon(Icons.Rounded.PlaylistAddCheck, null, modifier = Modifier.size(22.dp)) },
                                title = "SponsorBlock Categories",
                                subtitle = "${settings.sponsorBlockCategories.size} categories selected",
                                onClick = {
                                    tempSponsorBlockCategories = settings.sponsorBlockCategories
                                    showSponsorBlockCategories = true
                                },
                                showArrow = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor,
                                iconShape = settings.iconShape,
                            )
                        },
                    ),
            )

            Spacer(modifier = Modifier.height(80.dp))
    }
}
