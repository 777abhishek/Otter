package com.Otter.app.service.impl

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.Otter.app.data.database.OtterDatabase
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
import com.Otter.app.data.models.UserProfile
import com.Otter.app.data.models.VideoQuality
import com.Otter.app.service.SettingsService
import com.Otter.app.service.StorageService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsServiceImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SettingsService {
        private object PreferencesKeys {
            val THEME_MODE = stringPreferencesKey("theme_mode")
            val LANGUAGE = stringPreferencesKey("language")
            val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
            val SEED_COLOR = stringPreferencesKey("seed_color")
            val PURE_BLACK = booleanPreferencesKey("pure_black")
            val MONOCHROME_THEME = booleanPreferencesKey("monochrome_theme")
            val EXPRESSIVE = booleanPreferencesKey("expressive")
            val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
            val DOWNLOAD_NOTIFICATIONS_ENABLED = booleanPreferencesKey("download_notifications_enabled")
            val SYNC_NOTIFICATIONS_ENABLED = booleanPreferencesKey("sync_notifications_enabled")
            val BACKGROUND_SYNC_ENABLED = booleanPreferencesKey("background_sync_enabled")
            val AUTO_DOWNLOAD = booleanPreferencesKey("auto_download")
            val DOWNLOAD_PATH = stringPreferencesKey("download_path")
            val COUNTRY_CODE = stringPreferencesKey("country_code")
            val WIFI_ONLY_DOWNLOADS = booleanPreferencesKey("wifi_only_downloads")
            val MAX_CONCURRENT_DOWNLOADS = intPreferencesKey("max_concurrent_downloads")

            val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
            val DATA_SHARING_ENABLED = booleanPreferencesKey("data_sharing_enabled")

            val POWER_SAVER_ENABLED = booleanPreferencesKey("power_saver_enabled")
            val LOW_POWER_MODE = booleanPreferencesKey("low_power_mode")
            val BATTERY_THRESHOLD_PERCENT = intPreferencesKey("battery_threshold_percent")
            val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")

            val CRASH_REPORTING_ENABLED = booleanPreferencesKey("crash_reporting_enabled")
            val WEBVIEW_THIRD_PARTY_COOKIES = booleanPreferencesKey("webview_third_party_cookies")

            val DEFAULT_VIDEO_QUALITY = stringPreferencesKey("default_video_quality")
            val AUDIO_MODE = stringPreferencesKey("audio_mode")
            val DEFAULT_DOWNLOAD_FORMAT = stringPreferencesKey("default_download_format")
            val DEFAULT_AUDIO_QUALITY = stringPreferencesKey("default_audio_quality")
            val ICON_SHAPE = stringPreferencesKey("icon_shape")
            val SLIDER_STYLE = stringPreferencesKey("slider_style")
            val PLAYER_BACKGROUND_STYLE = stringPreferencesKey("player_background_style")
            val PLAYER_BUTTONS_STYLE = stringPreferencesKey("player_buttons_style")

            val HIDE_PLAYER_THUMBNAIL = booleanPreferencesKey("hide_player_thumbnail")

            val HIGH_REFRESH_RATE = booleanPreferencesKey("high_refresh_rate")

            // Streaming settings
            val STREAMING_QUALITY = stringPreferencesKey("streaming_quality")
            val STREAM_FORMAT_PREFERENCE = stringPreferencesKey("stream_format_preference")
            val PREFERRED_CODEC = stringPreferencesKey("preferred_codec")
            val STREAMING_DATA_SAVER = booleanPreferencesKey("streaming_data_saver")
            val STREAMING_AUDIO_ONLY = booleanPreferencesKey("streaming_audio_only")
            val STREAMING_SUBTITLES_ENABLED = booleanPreferencesKey("streaming_subtitles_enabled")
            val STREAMING_PREFERRED_AUDIO_LANGUAGE = stringPreferencesKey("streaming_preferred_audio_language")
            val SPONSOR_BLOCK_ENABLED = booleanPreferencesKey("sponsor_block_enabled")
            val SPONSOR_BLOCK_CATEGORIES = stringPreferencesKey("sponsor_block_categories")
            val SHOW_MINI_PLAYER_IN_AUDIO_MODE = booleanPreferencesKey("show_mini_player_in_audio_mode")
        }

        override fun getSettings(): Flow<AppSettings> {
            return context.dataStore.data.map { preferences ->
                AppSettings(
                    themeMode =
                        when (preferences[PreferencesKeys.THEME_MODE] ?: "system") {
                            "light" -> ThemeMode.LIGHT
                            "dark" -> ThemeMode.DARK
                            else -> ThemeMode.SYSTEM
                        },
                    language = preferences[PreferencesKeys.LANGUAGE] ?: "system",
                    useDynamicColor = preferences[PreferencesKeys.USE_DYNAMIC_COLOR] ?: false,
                    seedColor = preferences[PreferencesKeys.SEED_COLOR]?.toLongOrNull() ?: 0xFF00897BL, // Otter Teal
                    pureBlack = preferences[PreferencesKeys.PURE_BLACK] ?: false,
                    monochromeTheme = preferences[PreferencesKeys.MONOCHROME_THEME] ?: false,
                    expressive = preferences[PreferencesKeys.EXPRESSIVE] ?: false,
                    notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
                    downloadNotificationsEnabled = preferences[PreferencesKeys.DOWNLOAD_NOTIFICATIONS_ENABLED] ?: true,
                    syncNotificationsEnabled = preferences[PreferencesKeys.SYNC_NOTIFICATIONS_ENABLED] ?: true,
                    backgroundSyncEnabled = preferences[PreferencesKeys.BACKGROUND_SYNC_ENABLED] ?: true,
                    autoDownload = preferences[PreferencesKeys.AUTO_DOWNLOAD] ?: false,
                    downloadPath = preferences[PreferencesKeys.DOWNLOAD_PATH] ?: "App storage (Downloads)",
                    countryCode = preferences[PreferencesKeys.COUNTRY_CODE] ?: "system",
                    wifiOnlyDownloads = preferences[PreferencesKeys.WIFI_ONLY_DOWNLOADS] ?: true,
                    maxConcurrentDownloads = preferences[PreferencesKeys.MAX_CONCURRENT_DOWNLOADS] ?: 3,
                    analyticsEnabled = preferences[PreferencesKeys.ANALYTICS_ENABLED] ?: true,
                    dataSharingEnabled = preferences[PreferencesKeys.DATA_SHARING_ENABLED] ?: false,
                    powerSaverEnabled = preferences[PreferencesKeys.POWER_SAVER_ENABLED] ?: true,
                    lowPowerMode = preferences[PreferencesKeys.LOW_POWER_MODE] ?: true,
                    batteryThresholdPercent = preferences[PreferencesKeys.BATTERY_THRESHOLD_PERCENT] ?: 20,
                    keepScreenOn = preferences[PreferencesKeys.KEEP_SCREEN_ON] ?: false,
                    crashReportingEnabled = preferences[PreferencesKeys.CRASH_REPORTING_ENABLED] ?: true,
                    webViewThirdPartyCookies = preferences[PreferencesKeys.WEBVIEW_THIRD_PARTY_COOKIES] ?: true,
                    defaultVideoQuality =
                        preferences[PreferencesKeys.DEFAULT_VIDEO_QUALITY]?.let { stored ->
                            runCatching { VideoQuality.valueOf(stored) }.getOrNull()
                        } ?: VideoQuality.HD_720P,
                    audioMode =
                        preferences[PreferencesKeys.AUDIO_MODE]?.let { stored ->
                            runCatching { AudioMode.valueOf(stored) }.getOrNull()
                        } ?: AudioMode.VIDEO,
                    defaultDownloadFormat =
                        preferences[PreferencesKeys.DEFAULT_DOWNLOAD_FORMAT]?.let { stored ->
                            runCatching { DownloadVideoFormat.valueOf(stored) }.getOrNull()
                        } ?: DownloadVideoFormat.MP4_720P,
                    defaultAudioQuality =
                        preferences[PreferencesKeys.DEFAULT_AUDIO_QUALITY]?.let { stored ->
                            runCatching { AudioQuality.valueOf(stored) }.getOrNull()
                        } ?: AudioQuality.KBPS_192,
                    iconShape =
                        preferences[PreferencesKeys.ICON_SHAPE]?.let { stored ->
                            runCatching { IconShape.valueOf(stored) }.getOrNull()
                        } ?: IconShape.GHOSTISH,
                    sliderStyle =
                        preferences[PreferencesKeys.SLIDER_STYLE]?.let { stored ->
                            runCatching { SliderStyle.valueOf(stored) }.getOrNull()
                        } ?: SliderStyle.DEFAULT,
                    playerBackgroundStyle =
                        preferences[PreferencesKeys.PLAYER_BACKGROUND_STYLE]?.let { stored ->
                            runCatching { PlayerBackgroundStyle.valueOf(stored) }.getOrNull()
                        } ?: PlayerBackgroundStyle.GRADIENT,
                    playerButtonsStyle =
                        preferences[PreferencesKeys.PLAYER_BUTTONS_STYLE]?.let { stored ->
                            runCatching { PlayerButtonsStyle.valueOf(stored) }.getOrNull()
                        } ?: PlayerButtonsStyle.DEFAULT,
                    hidePlayerThumbnail = preferences[PreferencesKeys.HIDE_PLAYER_THUMBNAIL] ?: false,
                    highRefreshRate = preferences[PreferencesKeys.HIGH_REFRESH_RATE] ?: false,
                    // Streaming settings
                    streamingQuality =
                        preferences[PreferencesKeys.STREAMING_QUALITY]?.let { stored ->
                            runCatching { StreamingQuality.valueOf(stored) }.getOrNull()
                        } ?: StreamingQuality.HD_1080P,
                    streamFormatPreference =
                        preferences[PreferencesKeys.STREAM_FORMAT_PREFERENCE]?.let { stored ->
                            runCatching { StreamFormatPreference.valueOf(stored) }.getOrNull()
                        } ?: StreamFormatPreference.AUTO,
                    preferredCodec =
                        preferences[PreferencesKeys.PREFERRED_CODEC]?.let { stored ->
                            runCatching { PreferredCodec.valueOf(stored) }.getOrNull()
                        } ?: PreferredCodec.AUTO,
                    streamingDataSaver = preferences[PreferencesKeys.STREAMING_DATA_SAVER] ?: true,
                    streamingAudioOnly = preferences[PreferencesKeys.STREAMING_AUDIO_ONLY] ?: false,
                    streamingSubtitlesEnabled = preferences[PreferencesKeys.STREAMING_SUBTITLES_ENABLED] ?: true,
                    streamingPreferredAudioLanguage = preferences[PreferencesKeys.STREAMING_PREFERRED_AUDIO_LANGUAGE] ?: "system",
                    sponsorBlockEnabled = preferences[PreferencesKeys.SPONSOR_BLOCK_ENABLED] ?: true,
                    sponsorBlockCategories =
                        preferences[PreferencesKeys.SPONSOR_BLOCK_CATEGORIES]?.let { stored ->
                            runCatching {
                                stored.split(",").mapNotNull { catStr ->
                                    runCatching { SponsorBlockCategory.valueOf(catStr.trim()) }.getOrNull()
                                }.toSet()
                            }.getOrNull()
                        } ?: setOf(
                            SponsorBlockCategory.SPONSOR,
                            SponsorBlockCategory.INTRO,
                            SponsorBlockCategory.OUTRO,
                        ),
                    showMiniPlayerInAudioMode = preferences[PreferencesKeys.SHOW_MINI_PLAYER_IN_AUDIO_MODE] ?: true,
                )
            }
        }

        override suspend fun resetToDefaults(): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences.clear()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun updateSettings(settings: AppSettings): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.THEME_MODE] = settings.themeMode.name.lowercase()
                    preferences[PreferencesKeys.LANGUAGE] = settings.language
                    preferences[PreferencesKeys.USE_DYNAMIC_COLOR] = settings.useDynamicColor
                    preferences[PreferencesKeys.SEED_COLOR] = settings.seedColor.toString()
                    preferences[PreferencesKeys.PURE_BLACK] = settings.pureBlack
                    preferences[PreferencesKeys.EXPRESSIVE] = settings.expressive
                    preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = settings.notificationsEnabled
                    preferences[PreferencesKeys.DOWNLOAD_NOTIFICATIONS_ENABLED] = settings.downloadNotificationsEnabled
                    preferences[PreferencesKeys.SYNC_NOTIFICATIONS_ENABLED] = settings.syncNotificationsEnabled
                    preferences[PreferencesKeys.BACKGROUND_SYNC_ENABLED] = settings.backgroundSyncEnabled
                    preferences[PreferencesKeys.AUTO_DOWNLOAD] = settings.autoDownload
                    preferences[PreferencesKeys.DOWNLOAD_PATH] = settings.downloadPath
                    preferences[PreferencesKeys.COUNTRY_CODE] = settings.countryCode
                    preferences[PreferencesKeys.WIFI_ONLY_DOWNLOADS] = settings.wifiOnlyDownloads
                    preferences[PreferencesKeys.MAX_CONCURRENT_DOWNLOADS] = settings.maxConcurrentDownloads

                    preferences[PreferencesKeys.ANALYTICS_ENABLED] = settings.analyticsEnabled
                    preferences[PreferencesKeys.DATA_SHARING_ENABLED] = settings.dataSharingEnabled

                    preferences[PreferencesKeys.POWER_SAVER_ENABLED] = settings.powerSaverEnabled
                    preferences[PreferencesKeys.LOW_POWER_MODE] = settings.lowPowerMode
                    preferences[PreferencesKeys.BATTERY_THRESHOLD_PERCENT] = settings.batteryThresholdPercent
                    preferences[PreferencesKeys.KEEP_SCREEN_ON] = settings.keepScreenOn
                    preferences[PreferencesKeys.CRASH_REPORTING_ENABLED] = settings.crashReportingEnabled
                    preferences[PreferencesKeys.WEBVIEW_THIRD_PARTY_COOKIES] = settings.webViewThirdPartyCookies
                    preferences[PreferencesKeys.DEFAULT_VIDEO_QUALITY] = settings.defaultVideoQuality.name
                    preferences[PreferencesKeys.AUDIO_MODE] = settings.audioMode.name
                    preferences[PreferencesKeys.DEFAULT_DOWNLOAD_FORMAT] = settings.defaultDownloadFormat.name
                    preferences[PreferencesKeys.DEFAULT_AUDIO_QUALITY] = settings.defaultAudioQuality.name
                    preferences[PreferencesKeys.ICON_SHAPE] = settings.iconShape.name
                    preferences[PreferencesKeys.SLIDER_STYLE] = settings.sliderStyle.name
                    preferences[PreferencesKeys.PLAYER_BACKGROUND_STYLE] = settings.playerBackgroundStyle.name
                    preferences[PreferencesKeys.PLAYER_BUTTONS_STYLE] = settings.playerButtonsStyle.name

                    preferences[PreferencesKeys.HIDE_PLAYER_THUMBNAIL] = settings.hidePlayerThumbnail

                    preferences[PreferencesKeys.HIGH_REFRESH_RATE] = settings.highRefreshRate

                    // Streaming settings
                    preferences[PreferencesKeys.STREAMING_QUALITY] = settings.streamingQuality.name
                    preferences[PreferencesKeys.STREAM_FORMAT_PREFERENCE] = settings.streamFormatPreference.name
                    preferences[PreferencesKeys.PREFERRED_CODEC] = settings.preferredCodec.name
                    preferences[PreferencesKeys.STREAMING_DATA_SAVER] = settings.streamingDataSaver
                    preferences[PreferencesKeys.STREAMING_AUDIO_ONLY] = settings.streamingAudioOnly
                    preferences[PreferencesKeys.STREAMING_SUBTITLES_ENABLED] = settings.streamingSubtitlesEnabled
                    preferences[PreferencesKeys.STREAMING_PREFERRED_AUDIO_LANGUAGE] = settings.streamingPreferredAudioLanguage
                    preferences[PreferencesKeys.SPONSOR_BLOCK_ENABLED] = settings.sponsorBlockEnabled
                    preferences[PreferencesKeys.SPONSOR_BLOCK_CATEGORIES] = settings.sponsorBlockCategories.joinToString(",")
                    preferences[PreferencesKeys.SHOW_MINI_PLAYER_IN_AUDIO_MODE] = settings.showMiniPlayerInAudioMode
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setThemeMode(mode: ThemeMode): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.THEME_MODE] = mode.name.lowercase()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setLanguage(language: String): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.LANGUAGE] = language
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setUseDynamicColor(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.USE_DYNAMIC_COLOR] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setSeedColor(color: Long): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.SEED_COLOR] = color.toString()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setPureBlack(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.PURE_BLACK] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setMonochromeTheme(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.MONOCHROME_THEME] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setExpressive(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.EXPRESSIVE] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setNotificationsEnabled(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setDownloadNotificationsEnabled(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.DOWNLOAD_NOTIFICATIONS_ENABLED] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setSyncNotificationsEnabled(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.SYNC_NOTIFICATIONS_ENABLED] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setBackgroundSyncEnabled(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.BACKGROUND_SYNC_ENABLED] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setAutoDownload(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.AUTO_DOWNLOAD] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setDownloadPath(path: String): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.DOWNLOAD_PATH] = path
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setCountryCode(countryCode: String): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.COUNTRY_CODE] = countryCode
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setWifiOnlyDownloads(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.WIFI_ONLY_DOWNLOADS] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setMaxConcurrentDownloads(count: Int): Result<Unit> {
            val sanitized = count.coerceIn(1, 10)
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.MAX_CONCURRENT_DOWNLOADS] = sanitized
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setPowerSaverEnabled(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.POWER_SAVER_ENABLED] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setLowPowerMode(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.LOW_POWER_MODE] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setBatteryThresholdPercent(percent: Int): Result<Unit> {
            val sanitized = percent.coerceIn(5, 95)
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.BATTERY_THRESHOLD_PERCENT] = sanitized
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setKeepScreenOn(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.KEEP_SCREEN_ON] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setCrashReportingEnabled(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.CRASH_REPORTING_ENABLED] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setAnalyticsEnabled(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.ANALYTICS_ENABLED] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setDataSharingEnabled(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.DATA_SHARING_ENABLED] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setWebViewThirdPartyCookies(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.WEBVIEW_THIRD_PARTY_COOKIES] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setDefaultVideoQuality(quality: VideoQuality): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.DEFAULT_VIDEO_QUALITY] = quality.name
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setAudioMode(mode: AudioMode): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.AUDIO_MODE] = mode.name
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setDefaultDownloadFormat(format: DownloadVideoFormat): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.DEFAULT_DOWNLOAD_FORMAT] = format.name
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setDefaultAudioQuality(quality: AudioQuality): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.DEFAULT_AUDIO_QUALITY] = quality.name
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setIconShape(shape: IconShape): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.ICON_SHAPE] = shape.name
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setSliderStyle(style: SliderStyle): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.SLIDER_STYLE] = style.name
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setPlayerBackgroundStyle(style: PlayerBackgroundStyle): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.PLAYER_BACKGROUND_STYLE] = style.name
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setPlayerButtonsStyle(style: PlayerButtonsStyle): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.PLAYER_BUTTONS_STYLE] = style.name
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setHighRefreshRate(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.HIGH_REFRESH_RATE] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        // Streaming settings
        override suspend fun setStreamingQuality(quality: StreamingQuality): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.STREAMING_QUALITY] = quality.name
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setStreamFormatPreference(preference: StreamFormatPreference): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.STREAM_FORMAT_PREFERENCE] = preference.name
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setPreferredCodec(codec: PreferredCodec): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.PREFERRED_CODEC] = codec.name
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setStreamingDataSaver(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.STREAMING_DATA_SAVER] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setStreamingAudioOnly(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.STREAMING_AUDIO_ONLY] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setStreamingSubtitlesEnabled(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.STREAMING_SUBTITLES_ENABLED] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setStreamingPreferredAudioLanguage(languageTag: String): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.STREAMING_PREFERRED_AUDIO_LANGUAGE] = languageTag
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setSponsorBlockEnabled(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.SPONSOR_BLOCK_ENABLED] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setSponsorBlockCategories(categories: Set<SponsorBlockCategory>): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.SPONSOR_BLOCK_CATEGORIES] = categories.joinToString(",")
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun setShowMiniPlayerInAudioMode(enabled: Boolean): Result<Unit> {
            return try {
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.SHOW_MINI_PLAYER_IN_AUDIO_MODE] = enabled
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

@Singleton
class StorageServiceImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: OtterDatabase,
    ) : StorageService {
        override fun getUserProfile(): Flow<UserProfile?> {
            // TODO: Implement user profile storage
            return kotlinx.coroutines.flow.flowOf(null)
        }

        override suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
            return try {
                // TODO: Implement user profile saving
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun clearUserData(): Result<Unit> =
            withContext(Dispatchers.IO) {
                runCatching {
                    // Clear database - delete all tables data
                    database.clearAllTables()

                    // Clear cache
                    context.cacheDir.deleteRecursivelyContents()

                    // Clear downloads
                    val downloadsDir = File(context.getExternalFilesDir(null), "Downloads")
                    downloadsDir.deleteRecursivelyContents()

                    // Clear files directory
                    context.filesDir.deleteRecursivelyContents()

                    // Clear no-backup files
                    context.noBackupFilesDir.deleteRecursivelyContents()

                    // Clear shared preferences / DataStore
                    val prefsDir = File(context.filesDir.parentFile, "shared_prefs")
                    prefsDir.listFiles()?.forEach { it.deleteRecursively() }

                    // Clear app-specific external files
                    context.getExternalFilesDir(null)?.deleteRecursivelyContents()

                    // Clear code cache
                    context.codeCacheDir?.deleteRecursivelyContents()

                    // Clear external cache
                    context.externalCacheDir?.deleteRecursivelyContents()

                    Result.success(Unit)
                }.getOrElse { Result.failure(it) }
            }

        override suspend fun exportData(): Result<String> {
            return try {
                // TODO: Implement data export
                Result.success("")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun importData(path: String): Result<Unit> {
            return try {
                // TODO: Implement data import
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun getStorageStats(): Result<StorageStats> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val databaseFile = context.getDatabasePath("Otter.db")
                    val walFile = File(databaseFile.absolutePath + "-wal")
                    val shmFile = File(databaseFile.absolutePath + "-shm")
                    val databaseBytes = databaseFile.safeLength() + walFile.safeLength() + shmFile.safeLength()

                    val cacheBytes = context.cacheDir.safeDirectorySize()

                    val downloadsDir = File(context.getExternalFilesDir(null), "Downloads")
                    val downloadsBytes = downloadsDir.safeDirectorySize()

                    val downloadsOtterDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Otter")
                    val downloadsOtterBytes = if (downloadsOtterDir.exists()) downloadsOtterDir.safeDirectorySize() else 0L

                    val ytDlpDir = context.noBackupFilesDir
                    val ytDlpBytes = if (ytDlpDir.exists()) ytDlpDir.safeDirectorySize() else 0L

                    val filesDirBytes = context.filesDir.safeDirectorySize()
                    val noBackupBytes = context.noBackupFilesDir.safeDirectorySize()
                    val otherBytes = (filesDirBytes + noBackupBytes - ytDlpBytes).coerceAtLeast(0)

                    val totalAppBytes = databaseBytes + cacheBytes + downloadsBytes + downloadsOtterBytes + ytDlpBytes + otherBytes

                    val statFs = StatFs(context.filesDir.absolutePath)
                    val totalDeviceBytes = statFs.totalBytes
                    val freeDeviceBytes = statFs.availableBytes

                    StorageStats(
                        databaseBytes = databaseBytes,
                        cacheBytes = cacheBytes,
                        downloadsBytes = downloadsBytes,
                        downloadsOtterBytes = downloadsOtterBytes,
                        ytDlpBytes = ytDlpBytes,
                        otherBytes = otherBytes,
                        totalAppBytes = totalAppBytes,
                        totalDeviceBytes = totalDeviceBytes,
                        freeDeviceBytes = freeDeviceBytes,
                    )
                }
            }

        override suspend fun clearCache(): Result<Unit> =
            withContext(Dispatchers.IO) {
                runCatching {
                    context.cacheDir.deleteRecursivelyContents()
                }
            }

        override suspend fun exportDatabase(destinationUri: String): Result<Unit> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val uri = Uri.parse(destinationUri)
                    val databaseFile = context.getDatabasePath("Otter.db")
                    val walFile = File(databaseFile.absolutePath + "-wal")
                    val shmFile = File(databaseFile.absolutePath + "-shm")

                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        ZipOutputStream(out).use { zipOut ->
                            zipOut.putFileEntry("Otter.db", databaseFile)
                            zipOut.putFileEntry("Otter.db-wal", walFile)
                            zipOut.putFileEntry("Otter.db-shm", shmFile)
                        }
                    } ?: error("Unable to open destination")
                }
            }

        override suspend fun importDatabase(sourceUri: String): Result<Unit> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val uri = Uri.parse(sourceUri)
                    val databaseFile = context.getDatabasePath("Otter.db")
                    val targetDir = databaseFile.parentFile ?: context.filesDir
                    val targetDb = File(targetDir, "Otter.db")
                    val targetWal = File(targetDir, "Otter.db-wal")
                    val targetShm = File(targetDir, "Otter.db-shm")

                    database.close()

                    context.contentResolver.openInputStream(uri)?.use { raw ->
                        val inputStream = BufferedInputStream(raw)
                        val isZip = inputStream.isZipStream()
                        if (isZip) {
                            ZipInputStream(inputStream).use { zipIn ->
                                var entry = zipIn.nextEntry
                                while (entry != null) {
                                    when (entry.name) {
                                        "Otter.db" -> zipIn.copyToFile(targetDb)
                                        "Otter.db-wal" -> zipIn.copyToFile(targetWal)
                                        "Otter.db-shm" -> zipIn.copyToFile(targetShm)
                                    }
                                    zipIn.closeEntry()
                                    entry = zipIn.nextEntry
                                }
                            }
                        } else {
                            inputStream.copyToFile(targetDb)
                            targetWal.delete()
                            targetShm.delete()
                        }
                    } ?: error("Unable to open source")
                    Unit
                }
            }

        override suspend fun syncDownloadFolder(): Result<Unit> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val downloadsDir = File(context.getExternalFilesDir(null), "Downloads")
                    val downloadsOtterDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Otter")

                    val allFiles = mutableListOf<File>()

                    if (downloadsDir.exists()) {
                        downloadsDir.listFiles()?.let { allFiles.addAll(it) }
                    }

                    if (downloadsOtterDir.exists()) {
                        downloadsOtterDir.listFiles()?.let { allFiles.addAll(it) }
                    }

                    val existingTasks = database.downloadTaskDao().getAllTasks().first()
                    val filePathMap = existingTasks.associateBy { it.filePath }

                    for (file in allFiles) {
                        if (!file.isFile) continue

                        val existingTask = filePathMap[file.absolutePath]

                        if (existingTask != null) {
                            if (existingTask.status != "COMPLETED") {
                                database.downloadTaskDao().updateTask(
                                    existingTask.copy(
                                        status = "COMPLETED",
                                        totalBytes = file.length(),
                                        downloadedBytes = file.length(),
                                        progress = 100f,
                                    ),
                                )
                            }
                        } else {
                            val fileName = file.nameWithoutExtension
                            val taskId = java.util.UUID.randomUUID().toString()

                            val newTask =
                                com.Otter.app.data.database.entities.DownloadTaskEntity(
                                    id = taskId,
                                    url = "",
                                    title = fileName,
                                    thumbnailUrl = null,
                                    duration = null,
                                    progress = 100f,
                                    speed = null,
                                    eta = null,
                                    totalBytes = file.length(),
                                    downloadedBytes = file.length(),
                                    status = "COMPLETED",
                                    error = null,
                                    filePath = file.absolutePath,
                                    formatId = null,
                                    expectedSize = file.length(),
                                    addedDate = System.currentTimeMillis().toString(),
                                    artist = null,
                                    album = null,
                                    genre = null,
                                    uploadDate = null,
                                    description = null,
                                    embeddedMetadata = null,
                                )

                            database.downloadTaskDao().insertTask(newTask)
                        }
                    }

                    Unit
                }
            }

        override suspend fun clearOldestDownloads(maxSizeBytes: Long): Result<Int> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val downloadsOtterDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Otter")
                    if (!downloadsOtterDir.exists()) {
                        return@runCatching 0
                    }

                    val files = downloadsOtterDir.listFiles()?.toList() ?: emptyList()
                    if (files.isEmpty()) {
                        return@runCatching 0
                    }

                    var currentSize = files.sumOf { it.length() }
                    var deletedCount = 0

                    if (currentSize <= maxSizeBytes) {
                        return@runCatching 0
                    }

                    val sortedFiles = files.sortedBy { it.lastModified() }
                    val filesToDelete = mutableListOf<File>()

                    for (file in sortedFiles) {
                        if (currentSize <= maxSizeBytes) break
                        filesToDelete.add(file)
                        currentSize -= file.length()
                        deletedCount++
                    }

                    for (file in filesToDelete) {
                        file.delete()
                        database.downloadTaskDao().deleteTaskByPath(file.absolutePath)
                    }

                    deletedCount
                }
            }
    }

private fun File.safeLength(): Long = if (exists() && isFile) length() else 0L

private fun File.safeDirectorySize(): Long {
    if (!exists()) return 0L
    if (isFile) return length()
    val files = listFiles() ?: return 0L
    var total = 0L
    for (f in files) {
        total += f.safeDirectorySize()
    }
    return total
}

private fun File.deleteRecursivelyContents() {
    if (!exists() || !isDirectory) return
    listFiles()?.forEach { child ->
        child.deleteRecursively()
    }
}

private fun ZipOutputStream.putFileEntry(
    entryName: String,
    file: File,
) {
    if (!file.exists() || !file.isFile) return
    putNextEntry(ZipEntry(entryName))
    FileInputStream(file).use { it.copyTo(this) }
    closeEntry()
}

private fun java.io.InputStream.copyToFile(target: File) {
    target.parentFile?.mkdirs()
    target.outputStream().use { out ->
        copyTo(out)
    }
}

private fun java.io.InputStream.isZipStream(): Boolean {
    if (!markSupported()) return false
    mark(4)
    val header = ByteArray(4)
    val read = read(header)
    reset()
    if (read < 4) return false
    return header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte()
}
