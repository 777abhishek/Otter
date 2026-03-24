package com.Otter.app.data.models

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: String = "system",
    val useDynamicColor: Boolean = false,
    val seedColor: Long = 0xFF6750A4L,
    val pureBlack: Boolean = false,
    val monochromeTheme: Boolean = false,
    val expressive: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val downloadNotificationsEnabled: Boolean = true,
    val syncNotificationsEnabled: Boolean = true,
    val backgroundSyncEnabled: Boolean = true,
    val autoDownload: Boolean = false,
    val downloadPath: String = "App storage (Downloads)",
    val maxConcurrentDownloads: Int = 3,
    val wifiOnlyDownloads: Boolean = true,
    val analyticsEnabled: Boolean = false,
    val dataSharingEnabled: Boolean = false,
    val defaultVideoQuality: VideoQuality = VideoQuality.HD_720P,
    val audioMode: AudioMode = AudioMode.VIDEO,
    val defaultDownloadFormat: DownloadVideoFormat = DownloadVideoFormat.MP4_720P,
    val defaultAudioQuality: AudioQuality = AudioQuality.KBPS_192,
    val iconShape: IconShape = IconShape.GHOSTISH,
    val sliderStyle: SliderStyle = SliderStyle.DEFAULT,
    val playerBackgroundStyle: PlayerBackgroundStyle = PlayerBackgroundStyle.GRADIENT,
    val playerButtonsStyle: PlayerButtonsStyle = PlayerButtonsStyle.DEFAULT,
    val lyricsPosition: LyricsPosition = LyricsPosition.CENTER,
    val lyricsVerticalPosition: LyricsVerticalPosition = LyricsVerticalPosition.BOTTOM,
    val gridItemSize: GridItemSize = GridItemSize.MEDIUM,
    val slimNavBar: Boolean = false,
    val hidePlayerThumbnail: Boolean = false,
    val showLikedPlaylist: Boolean = true,
    val showDownloadedPlaylist: Boolean = true,
    val highRefreshRate: Boolean = false,
    val countryCode: String = "system",
    val powerSaverEnabled: Boolean = true,
    val lowPowerMode: Boolean = true,
    val batteryThresholdPercent: Int = 20,
    val keepScreenOn: Boolean = false,
    val crashReportingEnabled: Boolean = true,
    val webViewThirdPartyCookies: Boolean = true,
    // Streaming settings
    val streamingQuality: StreamingQuality = StreamingQuality.HD_1080P,
    val streamFormatPreference: StreamFormatPreference = StreamFormatPreference.AUTO,
    val preferredCodec: PreferredCodec = PreferredCodec.AUTO,
    val streamingDataSaver: Boolean = true,
    val streamingAudioOnly: Boolean = false,
    val streamingSubtitlesEnabled: Boolean = true,
    val streamingPreferredAudioLanguage: String = "system",
    val sponsorBlockEnabled: Boolean = true,
    val sponsorBlockCategories: Set<SponsorBlockCategory> =
        setOf(
            SponsorBlockCategory.SPONSOR,
            SponsorBlockCategory.INTRO,
            SponsorBlockCategory.OUTRO,
        ),
    val showMiniPlayerInAudioMode: Boolean = true,

    // Updates automation
    val updatesAutomationEnabled: Boolean = false,
    val updatesAutomationInterval: UpdatesAutomationInterval = UpdatesAutomationInterval.DAILY,
    val updatesAutomationNotify: Boolean = true,
    val updatesAutomationAutoDownloadApk: Boolean = false,
    val updatesAutomationAutoUpdateYtDlp: Boolean = true,
    val updatesAutomationAutoCheckNewPipe: Boolean = true,
    val updatesAutomationAutoClearCache: Boolean = false,
)

enum class UpdatesAutomationInterval {
    DAILY,
    WEEKLY,
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

enum class VideoQuality {
    SD_360P,
    SD_480P,
    HD_720P,
    HD_1080P,
    UHD_4K,
}

enum class AudioMode {
    VIDEO,
    AUDIO_ONLY,
}

enum class AudioQuality {
    KBPS_96,
    KBPS_128,
    KBPS_192,
    KBPS_256,
    KBPS_320,
}

enum class IconShape {
    CIRCLE,
    ROUNDED_RECTANGLE,
    GHOSTISH,
    SQUARE,
}

enum class SliderStyle {
    DEFAULT,
    WAVY,
}

enum class PlayerBackgroundStyle {
    GRADIENT,
    BLUR,
    SOLID,
}

enum class PlayerButtonsStyle {
    DEFAULT,
    ICON_ONLY,
}

enum class LyricsPosition {
    LEFT,
    CENTER,
    RIGHT,
}

enum class LyricsVerticalPosition {
    TOP,
    CENTER,
    BOTTOM,
}

enum class GridItemSize {
    SMALL,
    MEDIUM,
    LARGE,
}

enum class StreamingQuality {
    AUTO,
    SD_360P,
    SD_480P,
    HD_720P,
    HD_1080P,
    UHD_4K,
    HIGHEST,
}

enum class StreamFormatPreference {
    AUTO,
    HLS,
    DASH,
    PROGRESSIVE,
}

enum class PreferredCodec {
    AUTO,
    AV1,
    VP9,
    H264,
}

enum class SponsorBlockCategory {
    SPONSOR,
    INTRO,
    OUTRO,
    INTERACTION,
    SELF_PROMO,
    MUSIC_OFFTOPIC,
    PREVIEW,
    FILLER,
}

data class UserProfile(
    val id: String,
    val name: String,
    val email: String? = null,
    val avatar: String? = null,
    val watchHistory: List<WatchHistoryItem> = emptyList(),
    val likedVideos: List<String> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
)

data class WatchHistoryItem(
    val videoId: String,
    val title: String,
    val thumbnail: String,
    val watchedAt: Long,
    val progress: Float,
    val duration: Int,
)

data class SearchHistory(
    val queries: List<String> = emptyList(),
    val maxItems: Int = 50,
)

data class StorageStats(
    val databaseBytes: Long,
    val cacheBytes: Long,
    val downloadsBytes: Long,
    val downloadsOtterBytes: Long,
    val ytDlpBytes: Long,
    val otherBytes: Long,
    val totalAppBytes: Long,
    val totalDeviceBytes: Long,
    val freeDeviceBytes: Long,
)
