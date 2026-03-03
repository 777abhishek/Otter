package com.Otter.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Otter.app.data.auth.YouTubeProfileStore
import com.Otter.app.data.models.AppSettings
import com.Otter.app.data.models.AudioMode
import com.Otter.app.data.models.AudioQuality
import com.Otter.app.data.models.DownloadVideoFormat
import com.Otter.app.data.models.IconShape
import com.Otter.app.data.models.LyricsVerticalPosition
import com.Otter.app.data.models.PlayerBackgroundStyle
import com.Otter.app.data.models.PlayerButtonsStyle
import com.Otter.app.data.models.PreferredCodec
import com.Otter.app.data.models.SliderStyle
import com.Otter.app.data.models.SponsorBlockCategory
import com.Otter.app.data.models.StreamFormatPreference
import com.Otter.app.data.models.StreamingQuality
import com.Otter.app.data.models.ThemeMode
import com.Otter.app.data.models.VideoQuality
import com.Otter.app.network.PrivacySyncService
import com.Otter.app.service.SettingsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settingsService: SettingsService,
        private val profileStore: YouTubeProfileStore,
        val privacySyncService: PrivacySyncService,
    ) : ViewModel() {
        val settings =
            settingsService.getSettings()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue =
                        AppSettings(
                            lyricsVerticalPosition = com.Otter.app.data.models.LyricsVerticalPosition.BOTTOM,
                        ),
                )

        val accountName = profileStore.activeProfile.map { it?.label ?: "" }
        val accountEmail = profileStore.activeProfile.map { "" }
        val accountImageUrl = profileStore.activeProfile.map { null }
        val isLoggedIn = profileStore.isLoggedIn

        fun setThemeMode(mode: ThemeMode) {
            viewModelScope.launch {
                settingsService.setThemeMode(mode)
            }
        }

        fun setLanguage(language: String) {
            viewModelScope.launch {
                settingsService.setLanguage(language)
            }
        }

        fun setUseDynamicColor(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setUseDynamicColor(enabled)
            }
        }

        fun setNotificationsEnabled(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setNotificationsEnabled(enabled)
            }
        }

        fun setDownloadNotificationsEnabled(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setDownloadNotificationsEnabled(enabled)
            }
        }

        fun setSyncNotificationsEnabled(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setSyncNotificationsEnabled(enabled)
            }
        }

        fun setBackgroundSyncEnabled(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setBackgroundSyncEnabled(enabled)
            }
        }

        fun setAutoDownload(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setAutoDownload(enabled)
            }
        }

        fun setDownloadPath(path: String) {
            viewModelScope.launch {
                settingsService.setDownloadPath(path)
            }
        }

        fun setCountryCode(countryCode: String) {
            viewModelScope.launch {
                settingsService.setCountryCode(countryCode)
            }
        }

        fun setWifiOnlyDownloads(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setWifiOnlyDownloads(enabled)
            }
        }

        fun setMaxConcurrentDownloads(count: Int) {
            viewModelScope.launch {
                settingsService.setMaxConcurrentDownloads(count)
            }
        }

        fun setPowerSaverEnabled(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setPowerSaverEnabled(enabled)
            }
        }

        fun setLowPowerMode(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setLowPowerMode(enabled)
            }
        }

        fun setBatteryThresholdPercent(percent: Int) {
            viewModelScope.launch {
                settingsService.setBatteryThresholdPercent(percent)
            }
        }

        fun setKeepScreenOn(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setKeepScreenOn(enabled)
            }
        }

        fun setCrashReportingEnabled(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setCrashReportingEnabled(enabled)
                privacySyncService.syncPrivacySettings()
            }
        }

        fun setAnalyticsEnabled(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setAnalyticsEnabled(enabled)
                privacySyncService.syncPrivacySettings()
            }
        }

        fun setDataSharingEnabled(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setDataSharingEnabled(enabled)
                privacySyncService.syncPrivacySettings()
            }
        }

        fun setWebViewThirdPartyCookies(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setWebViewThirdPartyCookies(enabled)
            }
        }

        fun setDefaultVideoQuality(quality: VideoQuality) {
            viewModelScope.launch {
                settingsService.setDefaultVideoQuality(quality)
            }
        }

        fun setAudioMode(mode: AudioMode) {
            viewModelScope.launch {
                settingsService.setAudioMode(mode)
            }
        }

        fun setDefaultDownloadFormat(format: DownloadVideoFormat) {
            viewModelScope.launch {
                settingsService.setDefaultDownloadFormat(format)
            }
        }

        fun setDefaultAudioQuality(quality: AudioQuality) {
            viewModelScope.launch {
                settingsService.setDefaultAudioQuality(quality)
            }
        }

        fun setIconShape(shape: IconShape) {
            viewModelScope.launch {
                settingsService.setIconShape(shape)
            }
        }

        fun setSliderStyle(style: SliderStyle) {
            viewModelScope.launch {
                settingsService.setSliderStyle(style)
            }
        }

        fun setPlayerBackgroundStyle(style: PlayerBackgroundStyle) {
            viewModelScope.launch {
                settingsService.setPlayerBackgroundStyle(style)
            }
        }

        fun setPlayerButtonsStyle(style: PlayerButtonsStyle) {
            viewModelScope.launch {
                settingsService.setPlayerButtonsStyle(style)
            }
        }

        fun setHighRefreshRate(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setHighRefreshRate(enabled)
            }
        }

        fun setSeedColor(color: Long) {
            viewModelScope.launch {
                settingsService.setSeedColor(color)
            }
        }

        fun setPureBlack(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setPureBlack(enabled)
            }
        }

        fun setMonochromeTheme(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setMonochromeTheme(enabled)
            }
        }

        fun setExpressive(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setExpressive(enabled)
            }
        }

        // Streaming settings
        fun setStreamingQuality(quality: StreamingQuality) {
            viewModelScope.launch {
                settingsService.setStreamingQuality(quality)
            }
        }

        fun setStreamFormatPreference(preference: StreamFormatPreference) {
            viewModelScope.launch {
                settingsService.setStreamFormatPreference(preference)
            }
        }

        fun setPreferredCodec(codec: PreferredCodec) {
            viewModelScope.launch {
                settingsService.setPreferredCodec(codec)
            }
        }

        fun setStreamingDataSaver(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setStreamingDataSaver(enabled)
            }
        }

        fun setStreamingAudioOnly(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setStreamingAudioOnly(enabled)
            }
        }

        fun setStreamingPreferredAudioLanguage(languageTag: String) {
            viewModelScope.launch {
                settingsService.setStreamingPreferredAudioLanguage(languageTag)
            }
        }

        fun setStreamingSubtitlesEnabled(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setStreamingSubtitlesEnabled(enabled)
            }
        }

        fun setSponsorBlockEnabled(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setSponsorBlockEnabled(enabled)
            }
        }

        fun setSponsorBlockCategories(categories: Set<SponsorBlockCategory>) {
            viewModelScope.launch {
                settingsService.setSponsorBlockCategories(categories)
            }
        }

        fun setShowMiniPlayerInAudioMode(enabled: Boolean) {
            viewModelScope.launch {
                settingsService.setShowMiniPlayerInAudioMode(enabled)
            }
        }

        fun resetToDefaults() {
            viewModelScope.launch {
                settingsService.resetToDefaults()
            }
        }
    }
