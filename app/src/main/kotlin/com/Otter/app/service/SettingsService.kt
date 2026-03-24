package com.Otter.app.service

import com.Otter.app.data.models.AppSettings
import com.Otter.app.data.models.AudioMode
import com.Otter.app.data.models.AudioQuality
import com.Otter.app.data.models.DownloadVideoFormat
import com.Otter.app.data.models.IconShape
import com.Otter.app.data.models.PlayerBackgroundStyle
import com.Otter.app.data.models.PlayerButtonsStyle
import com.Otter.app.data.models.PreferredCodec
import com.Otter.app.data.models.SliderStyle
import com.Otter.app.data.models.SponsorBlockCategory
import com.Otter.app.data.models.StorageStats
import com.Otter.app.data.models.StreamFormatPreference
import com.Otter.app.data.models.StreamingQuality
import com.Otter.app.data.models.ThemeMode
import com.Otter.app.data.models.UpdatesAutomationInterval
import com.Otter.app.data.models.UserProfile
import com.Otter.app.data.models.VideoQuality
import kotlinx.coroutines.flow.Flow

interface SettingsService {
    fun getSettings(): Flow<AppSettings>

    suspend fun updateSettings(settings: AppSettings): Result<Unit>

    suspend fun resetToDefaults(): Result<Unit>

    suspend fun setThemeMode(mode: ThemeMode): Result<Unit>

    suspend fun setLanguage(language: String): Result<Unit>

    suspend fun setUseDynamicColor(enabled: Boolean): Result<Unit>

    suspend fun setSeedColor(color: Long): Result<Unit>

    suspend fun setPureBlack(enabled: Boolean): Result<Unit>

    suspend fun setMonochromeTheme(enabled: Boolean): Result<Unit>

    suspend fun setExpressive(enabled: Boolean): Result<Unit>

    suspend fun setNotificationsEnabled(enabled: Boolean): Result<Unit>

    suspend fun setDownloadNotificationsEnabled(enabled: Boolean): Result<Unit>

    suspend fun setSyncNotificationsEnabled(enabled: Boolean): Result<Unit>

    suspend fun setBackgroundSyncEnabled(enabled: Boolean): Result<Unit>

    suspend fun setAutoDownload(enabled: Boolean): Result<Unit>

    suspend fun setDownloadPath(path: String): Result<Unit>

    suspend fun setCountryCode(countryCode: String): Result<Unit>

    suspend fun setWifiOnlyDownloads(enabled: Boolean): Result<Unit>

    suspend fun setMaxConcurrentDownloads(count: Int): Result<Unit>

    suspend fun setPowerSaverEnabled(enabled: Boolean): Result<Unit>

    suspend fun setLowPowerMode(enabled: Boolean): Result<Unit>

    suspend fun setBatteryThresholdPercent(percent: Int): Result<Unit>

    suspend fun setKeepScreenOn(enabled: Boolean): Result<Unit>

    suspend fun setCrashReportingEnabled(enabled: Boolean): Result<Unit>

    suspend fun setAnalyticsEnabled(enabled: Boolean): Result<Unit>

    suspend fun setDataSharingEnabled(enabled: Boolean): Result<Unit>

    suspend fun setWebViewThirdPartyCookies(enabled: Boolean): Result<Unit>

    suspend fun setDefaultVideoQuality(quality: VideoQuality): Result<Unit>

    suspend fun setAudioMode(mode: AudioMode): Result<Unit>

    suspend fun setDefaultDownloadFormat(format: DownloadVideoFormat): Result<Unit>

    suspend fun setDefaultAudioQuality(quality: AudioQuality): Result<Unit>

    suspend fun setIconShape(shape: IconShape): Result<Unit>

    suspend fun setSliderStyle(style: SliderStyle): Result<Unit>

    suspend fun setPlayerBackgroundStyle(style: PlayerBackgroundStyle): Result<Unit>

    suspend fun setPlayerButtonsStyle(style: PlayerButtonsStyle): Result<Unit>

    suspend fun setHighRefreshRate(enabled: Boolean): Result<Unit>

    // Streaming settings
    suspend fun setStreamingQuality(quality: StreamingQuality): Result<Unit>

    suspend fun setStreamFormatPreference(preference: StreamFormatPreference): Result<Unit>

    suspend fun setPreferredCodec(codec: PreferredCodec): Result<Unit>

    suspend fun setStreamingDataSaver(enabled: Boolean): Result<Unit>

    suspend fun setStreamingAudioOnly(enabled: Boolean): Result<Unit>

    suspend fun setStreamingSubtitlesEnabled(enabled: Boolean): Result<Unit>

    suspend fun setStreamingPreferredAudioLanguage(languageTag: String): Result<Unit>

    suspend fun setSponsorBlockEnabled(enabled: Boolean): Result<Unit>

    suspend fun setSponsorBlockCategories(categories: Set<SponsorBlockCategory>): Result<Unit>

    suspend fun setShowMiniPlayerInAudioMode(enabled: Boolean): Result<Unit>

    // Updates automation
    suspend fun setUpdatesAutomationEnabled(enabled: Boolean): Result<Unit>

    suspend fun setUpdatesAutomationInterval(interval: UpdatesAutomationInterval): Result<Unit>

    suspend fun setUpdatesAutomationNotify(enabled: Boolean): Result<Unit>

    suspend fun setUpdatesAutomationAutoDownloadApk(enabled: Boolean): Result<Unit>

    suspend fun setUpdatesAutomationAutoUpdateYtDlp(enabled: Boolean): Result<Unit>

    suspend fun setUpdatesAutomationAutoCheckNewPipe(enabled: Boolean): Result<Unit>

    suspend fun setUpdatesAutomationAutoClearCache(enabled: Boolean): Result<Unit>
}

interface StorageService {
    fun getUserProfile(): Flow<UserProfile?>

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit>

    suspend fun clearUserData(): Result<Unit>

    suspend fun exportData(): Result<String>

    suspend fun importData(path: String): Result<Unit>

    suspend fun getStorageStats(): Result<StorageStats>

    suspend fun clearCache(): Result<Unit>

    suspend fun exportDatabase(destinationUri: String): Result<Unit>

    suspend fun importDatabase(sourceUri: String): Result<Unit>

    suspend fun syncDownloadFolder(): Result<Unit>

    suspend fun clearOldestDownloads(maxSizeBytes: Long): Result<Int>
}
