# OtterKotlin Kotlin Source Structure Review (com/Otter/app)

## Problems Found (High Impact)

1. **Duplicate/inconsistent utility packages**
   - `util/` and `utils/` both exist.

2. **Overlapping “network/service” naming across layers**
   - Root `network/` overlaps with `data/network/`.
   - Root `service/` overlaps with `data/services/`.

3. **Player-related code split across multiple packages**
   - `player/` (service/connection) vs `ui/player/` (composables) vs `ui/screens/player/` (screen components).

4. **ViewModel naming collision**
   - `ui/download/DownloadViewModel.kt` and `ui/viewmodels/DownloadViewModel.kt`.

5. **Mixed organization style inside UI**
   - `ui/screens/*` (screen-level) but also `ui/page/*` and `ui/download/*`.

This document proposes a clean, GitHub-friendly structure and provides an **old path → new path** mapping.

---

## Recommended Target Structure (Proposed)

```
com/Otter/app/
  core/
    app/
    di/
    logging/
    util/

  data/
    auth/
    download/
    model/
    newpipe/
    remote/
    repository/
    sync/
    ytdlp/
    local/
      db/
      prefs/

  playback/
    service/

  downloads/
    service/
    worker/

  ui/
    components/
    navigation/
    screens/
      crash/
      download/
      home/
      login/
      player/
      playlist/
      settings/
    theme/
    viewmodel/
```

Notes:
- `core/*` is for app-wide infra (Application, DI modules, shared utils/logging).
- `data/*` is for data sources (remote/local), repos, and data models.
- `playback/*` is dedicated to player/service connection.
- `downloads/*` is dedicated to foreground service, notifications, and workers.
- `ui/*` is purely UI + viewmodels.

---

## Old → New Mapping (Folders + Files)

### Root package files

- `Otterlication.kt` → `core/app/Otterlication.kt`
- `MainActivity.kt` → `core/app/MainActivity.kt`

### di/

- `di/` → `core/di/`
- `di/CoilModule.kt` → `core/di/CoilModule.kt`
- `di/DatabaseModule.kt` → `core/di/DatabaseModule.kt`
- `di/DownloadModule.kt` → `core/di/DownloadModule.kt`
- `di/RepositoryModule.kt` → `core/di/RepositoryModule.kt`
- `di/ServiceModule.kt` → `core/di/ServiceModule.kt`

### util/ and utils/

- `util/` → `core/util/`
- `util/LocaleUtil.kt` → `core/util/LocaleUtil.kt`
- `util/PreferenceUtil.kt` → `core/util/PreferenceUtil.kt`
- `util/StringExtensions.kt` → `core/util/StringExtensions.kt`
- `util/SubtitleFormat.kt` → `core/util/SubtitleFormat.kt`
- `util/UpdateUtil.kt` → `core/util/UpdateUtil.kt`

- `utils/` → `core/logging/` (merge into logging)
- `utils/CrashReportManager.kt` → `core/logging/CrashReportManager.kt`
- `utils/FileLogger.kt` → `core/logging/FileLogger.kt`

### network/

- `network/` → `data/remote/telemetry/` (or keep under `data/remote/`)
- `network/AnalyticsService.kt` → `data/remote/telemetry/AnalyticsService.kt`
- `network/CrashReportService.kt` → `data/remote/telemetry/CrashReportService.kt`
- `network/PrivacySettingsService.kt` → `data/remote/settings/PrivacySettingsService.kt`

### data/

- `data/` → `data/`

#### data/auth/
- `data/auth/CookieExporter.kt` → `data/auth/CookieExporter.kt`
- `data/auth/CookieStore.kt` → `data/auth/CookieStore.kt`
- `data/auth/YouTubeProfile.kt` → `data/auth/YouTubeProfile.kt`
- `data/auth/YouTubeProfileStore.kt` → `data/auth/YouTubeProfileStore.kt`
- `data/auth/YtDlpCookieExtractor.kt` → `data/auth/YtDlpCookieExtractor.kt`

#### data/database/
- `data/database/` → `data/local/db/`
- `data/database/OtterDatabase.kt` → `data/local/db/OtterDatabase.kt`
- `data/database/dao/Daos.kt` → `data/local/db/dao/Daos.kt`
- `data/database/entities/Entities.kt` → `data/local/db/entities/Entities.kt`

#### data/download/
- `data/download/CommandTemplate.kt` → `data/download/CommandTemplate.kt`
- `data/download/DownloadEngine.kt` → `data/download/DownloadEngine.kt`
- `data/download/DownloadPreferences.kt` → `data/local/prefs/DownloadPreferences.kt`
- `data/download/DownloadQueueManager.kt` → `data/download/DownloadQueueManager.kt`
- `data/download/Downloader.kt` → `data/download/Downloader.kt`
- `data/download/DownloaderImpl.kt` → `data/download/DownloaderImpl.kt`
- `data/download/Format.kt` → `data/download/Format.kt`
- `data/download/PlaylistResult.kt` → `data/download/PlaylistResult.kt`
- `data/download/Task.kt` → `data/download/Task.kt`
- `data/download/TaskBackupStore.kt` → `data/download/TaskBackupStore.kt`
- `data/download/TaskFactory.kt` → `data/download/TaskFactory.kt`
- `data/download/VideoInfo.kt` → `data/download/VideoInfo.kt`

#### data/models/
- `data/models/` → `data/model/`
- `data/models/AppModels.kt` → `data/model/AppModels.kt`
- `data/models/DownloadModels.kt` → `data/model/DownloadModels.kt`
- `data/models/VideoModels.kt` → `data/model/VideoModels.kt`

#### data/network/
- `data/network/NetworkClient.kt` → `data/remote/NetworkClient.kt`

#### data/newpipe/
- `data/newpipe/OkHttpNewPipeDownloader.kt` → `data/newpipe/OkHttpNewPipeDownloader.kt`

#### data/repositories/
- `data/repositories/` → `data/repository/`
- `data/repositories/Repositories.kt` → `data/repository/Repositories.kt`
- `data/repositories/StreamRepository.kt` → `data/repository/StreamRepository.kt`

#### data/services/
- `data/services/` → `data/remote/service/` (interfaces) + `data/remote/service/impl/` (implementations)
- `data/services/DownloadService.kt` → `data/remote/service/DownloadService.kt`
- `data/services/SettingsService.kt` → `data/remote/service/SettingsService.kt`
- `data/services/VideoService.kt` → `data/remote/service/VideoService.kt`
- `data/services/impl/DownloadServiceImpl.kt` → `data/remote/service/impl/DownloadServiceImpl.kt`
- `data/services/impl/SettingsServiceImpl.kt` → `data/remote/service/impl/SettingsServiceImpl.kt`
- `data/services/impl/VideoServiceImpl.kt` → `data/remote/service/impl/VideoServiceImpl.kt`

#### data/sync/
- `data/sync/Metadata.kt` → `data/sync/Metadata.kt`
- `data/sync/SubscriptionSyncService.kt` → `data/sync/SubscriptionSyncService.kt`
- `data/sync/base.kt` → `data/sync/base.kt`
- `data/sync/format.kt` → `data/sync/format.kt`

#### data/ytdlp/
- `data/ytdlp/DownloadUtil.kt` → `data/ytdlp/DownloadUtil.kt`
- `data/ytdlp/YtDlpCookieSupport.kt` → `data/ytdlp/YtDlpCookieSupport.kt`
- `data/ytdlp/YtDlpCore.kt` → `data/ytdlp/YtDlpCore.kt`
- `data/ytdlp/YtDlpManager.kt` → `data/ytdlp/YtDlpManager.kt`
- `data/ytdlp/YtDlpMediaClient.kt` → `data/ytdlp/YtDlpMediaClient.kt`
- `data/ytdlp/YtDlpParsers.kt` → `data/ytdlp/YtDlpParsers.kt`
- `data/ytdlp/YtDlpSyncClient.kt` → `data/ytdlp/YtDlpSyncClient.kt`
- `data/ytdlp/YtDlpUtils.kt` → `data/ytdlp/YtDlpUtils.kt`

### playback (player service/connection)

- `player/` → `playback/service/`
- `player/PlayerConnectionManager.kt` → `playback/service/PlayerConnectionManager.kt`
- `player/PlayerService.kt` → `playback/service/PlayerService.kt`

### downloads (foreground services + worker)

- `service/` → `downloads/service/`
- `service/DownloadForegroundService.kt` → `downloads/service/DownloadForegroundService.kt`
- `service/DownloadNotificationReceiver.kt` → `downloads/service/DownloadNotificationReceiver.kt`
- `service/NotificationManager.kt` → `downloads/service/NotificationManager.kt`

- `work/` → `downloads/worker/`
- `work/DownloadWorker.kt` → `downloads/worker/DownloadWorker.kt`

### ui/

- `ui/` → `ui/`

#### ui/common/
- `ui/common/ThemeObjects.kt` → `ui/theme/ThemeObjects.kt`

#### ui/components/
- `ui/components/CommonComponents.kt` → `ui/components/CommonComponents.kt`
- `ui/components/FormatComponents.kt` → `ui/components/FormatComponents.kt`
- `ui/components/LoadingSpinner.kt` → `ui/components/LoadingSpinner.kt`
- `ui/components/NewActionButton.kt` → `ui/components/NewActionButton.kt`
- `ui/components/PlaylistCardItem.kt` → `ui/components/PlaylistCardItem.kt`
- `ui/components/PreferenceEntry.kt` → `ui/components/PreferenceEntry.kt`
- `ui/components/SearchBar.kt` → `ui/components/SearchBar.kt`
- `ui/components/SettingCard.kt` → `ui/components/SettingCard.kt`
- `ui/components/SettingComponents.kt` → `ui/components/SettingComponents.kt`
- `ui/components/ShimmerEffect.kt` → `ui/components/ShimmerEffect.kt`
- `ui/components/SyncProgressSnackbar.kt` → `ui/components/SyncProgressSnackbar.kt`
- `ui/components/VideoCardItem.kt` → `ui/components/VideoCardItem.kt`

#### ui/navigation/
- `ui/navigation/OtterBottomNavigation.kt` → `ui/navigation/OtterBottomNavigation.kt`

#### ui/theme/
- `ui/theme/Color.kt` → `ui/theme/Color.kt`
- `ui/theme/ColorScheme.kt` → `ui/theme/ColorScheme.kt`
- `ui/theme/OtterColors.kt` → `ui/theme/OtterColors.kt`
- `ui/theme/OtterTheme.kt` → `ui/theme/OtterTheme.kt`
- `ui/theme/Shapes.kt` → `ui/theme/Shapes.kt`
- `ui/theme/ThemeUtils.kt` → `ui/theme/ThemeUtils.kt`
- `ui/theme/Type.kt` → `ui/theme/Type.kt`
- `ui/theme/Typography.kt` → `ui/theme/Typography.kt`

#### ui/viewmodels/ (rename to viewmodel)
- `ui/viewmodels/` → `ui/viewmodel/`
- `ui/viewmodels/DownloadViewModel.kt` → `ui/viewmodel/DownloadViewModel.kt`
- `ui/viewmodels/HomeViewModel.kt` → `ui/viewmodel/HomeViewModel.kt`
- `ui/viewmodels/LikedViewModel.kt` → `ui/viewmodel/LikedViewModel.kt`
- `ui/viewmodels/LoginViewModel.kt` → `ui/viewmodel/LoginViewModel.kt`
- `ui/viewmodels/PlayerViewModel.kt` → `ui/viewmodel/PlayerViewModel.kt`
- `ui/viewmodels/PlaylistViewModel.kt` → `ui/viewmodel/PlaylistViewModel.kt`
- `ui/viewmodels/ProfilesViewModel.kt` → `ui/viewmodel/ProfilesViewModel.kt`
- `ui/viewmodels/SettingsViewModel.kt` → `ui/viewmodel/SettingsViewModel.kt`
- `ui/viewmodels/StorageViewModel.kt` → `ui/viewmodel/StorageViewModel.kt`
- `ui/viewmodels/SyncViewModel.kt` → `ui/viewmodel/SyncViewModel.kt`

#### ui/player/ (mini player widgets)
- `ui/player/` → `ui/screens/player/components/`
- `ui/player/AudioMiniPlayer.kt` → `ui/screens/player/components/AudioMiniPlayer.kt`
- `ui/player/AudioPlayer.kt` → `ui/screens/player/components/AudioPlayer.kt`
- `ui/player/AudioPlayerController.kt` → `ui/screens/player/components/AudioPlayerController.kt`

#### ui/download/
- `ui/download/` → `ui/screens/download/components/`
- `ui/download/TaskCard.kt` → `ui/screens/download/components/TaskCard.kt`

- `ui/download/configure/` → `ui/screens/download/configure/`
- `ui/download/configure/ConfigureFormatsSheet.kt` → `ui/screens/download/configure/ConfigureFormatsSheet.kt`
- `ui/download/configure/DownloadDialog.kt` → `ui/screens/download/configure/DownloadDialog.kt`
- `ui/download/configure/DownloadDialogViewModel.kt` → `ui/screens/download/configure/DownloadDialogViewModel.kt`
- `ui/download/configure/FormatDialogs.kt` → `ui/screens/download/configure/FormatDialogs.kt`
- `ui/download/configure/FormatPage.kt` → `ui/screens/download/configure/FormatPage.kt`
- `ui/download/configure/FormatTabs.kt` → `ui/screens/download/configure/FormatTabs.kt`
- `ui/download/configure/PlaylistSelectionPage.kt` → `ui/screens/download/configure/PlaylistSelectionPage.kt`

- `ui/download/DownloadViewModel.kt` → `ui/screens/download/DownloadScreenViewModel.kt` (rename to avoid collision)

#### ui/page/ (should be merged)
- `ui/page/` → `ui/components/` (or move into the owning screen)
- `ui/page/YtdlpUpdater.kt` → `ui/screens/settings/components/YtdlpUpdater.kt`
- `ui/page/download/VideoClipComponents.kt` → `ui/screens/download/components/VideoClipComponents.kt`
- `ui/page/settings/general/DialogComponents.kt` → `ui/screens/settings/components/DialogComponents.kt`

#### ui/screens/
- `ui/screens/CrashScreen.kt` → `ui/screens/crash/CrashScreen.kt`
- `ui/screens/DownloadScreen.kt` → `ui/screens/download/DownloadScreen.kt`
- `ui/screens/HomeScreen.kt` → `ui/screens/home/HomeScreen.kt`
- `ui/screens/PlayerScreen.kt` → `ui/screens/player/PlayerScreen.kt`
- `ui/screens/PlaylistDetailScreen.kt` → `ui/screens/playlist/PlaylistDetailScreen.kt`
- `ui/screens/WebViewLoginScreen.kt` → `ui/screens/login/WebViewLoginScreen.kt`

##### ui/screens/player/
- `ui/screens/player/PlaybackSettingsSheet.kt` → `ui/screens/player/PlaybackSettingsSheet.kt`
- `ui/screens/player/PlayerAudioSheet.kt` → `ui/screens/player/PlayerAudioSheet.kt`
- `ui/screens/player/PlayerControlsOverlay.kt` → `ui/screens/player/PlayerControlsOverlay.kt`
- `ui/screens/player/PlayerQualitySheet.kt` → `ui/screens/player/PlayerQualitySheet.kt`
- `ui/screens/player/PlayerQueueSheets.kt` → `ui/screens/player/PlayerQueueSheets.kt`
- `ui/screens/player/VideoPlayerView.kt` → `ui/screens/player/VideoPlayerView.kt`

##### ui/screens/settings/
- `ui/screens/settings/AboutSettings.kt` → `ui/screens/settings/AboutSettings.kt`
- `ui/screens/settings/AppearanceSettingsScreen.kt` → `ui/screens/settings/AppearanceSettingsScreen.kt`
- `ui/screens/settings/BackupAndRestore.kt` → `ui/screens/settings/BackupAndRestore.kt`
- `ui/screens/settings/ContentSettings.kt` → `ui/screens/settings/ContentSettings.kt`
- `ui/screens/settings/DiagnosticsSettings.kt` → `ui/screens/settings/DiagnosticsSettings.kt`
- `ui/screens/settings/Integrations.kt` → `ui/screens/settings/Integrations.kt`
- `ui/screens/settings/NotificationSettings.kt` → `ui/screens/settings/NotificationSettings.kt`
- `ui/screens/settings/PowerSaverSettings.kt` → `ui/screens/settings/PowerSaverSettings.kt`
- `ui/screens/settings/PrivacySettings.kt` → `ui/screens/settings/PrivacySettings.kt`
- `ui/screens/settings/ProfilesSettings.kt` → `ui/screens/settings/ProfilesSettings.kt`
- `ui/screens/settings/SettingsListContent.kt` → `ui/screens/settings/SettingsListContent.kt`
- `ui/screens/settings/SettingsScreen.kt` → `ui/screens/settings/SettingsScreen.kt`
- `ui/screens/settings/StorageSettings.kt` → `ui/screens/settings/StorageSettings.kt`
- `ui/screens/settings/UpdatesSettings.kt` → `ui/screens/settings/UpdatesSettings.kt`
- `ui/screens/settings/YtdlpUpdateDialog.kt` → `ui/screens/settings/YtdlpUpdateDialog.kt`

---

## What I did NOT change

This is only a **structure proposal + mapping**. No Kotlin package declarations, imports, or references were updated yet.

## Next Step (if you want)

If you confirm you like this target structure, I can:
1. Apply the file/folder moves.
2. Update all Kotlin `package` declarations + imports.
3. Update any references (e.g. `R` imports, Hilt modules, manifests if needed).
