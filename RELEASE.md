# Otter v2.0.3 - Sync Simplification & UI Improvements

## Download
[Download APK](../../releases/download/v2.0.3/app-release.apk)

## Overview
This release focuses on faster, more reliable playlist syncing using a flat sync approach, plus a redesigned video card with improved metadata layout.

## Key Changes

### Sync Simplification
- **Fast flat playlist sync**: Playlist syncing is optimized for speed and reliability
- **More predictable UX**: Sync completes quickly and avoids long-running enrichment attempts

### UI Enhancements
- **Redesigned video cards**:
  - Larger thumbnails (160dp × 90dp)
  - Added icons for channel name, views, and upload date
  - Better spacing and typography
  - More responsive layout

### Playlist Detail Improvements
- Like / Watch Later actions are available directly from the playlist detail video cards
- Pull-to-refresh spinner behavior is improved to avoid conflicting with sync UI

### New Policy Screens
- Added Privacy Policy screen in Settings
- Added Third Party Licenses screen in Settings
- Added Fair Use Policy screen in Settings

### Technical Improvements
- Added `--ignore-config` and `--skip-download` to yt-dlp metadata extraction
- Prevents "Requested format is not available" errors

## Technical Details

### Files Modified
- `app/src/main/kotlin/com/Otter/app/data/sync/SubscriptionSyncService.kt` - Simplified playlist sync to fast flat sync only
- `app/src/main/kotlin/com/Otter/app/ui/components/VideoCardItem.kt` - Redesigned video card
- `app/src/main/kotlin/com/Otter/app/data/ytdlp/YtDlpSyncClient.kt` - Added yt-dlp options
- `app/src/main/kotlin/com/Otter/app/ui/screens/settings/PrivacyPolicyScreen.kt` - New privacy policy screen
- `app/src/main/kotlin/com/Otter/app/ui/screens/settings/ThirdPartyLicensesScreen.kt` - New third party licenses screen
- `app/src/main/kotlin/com/Otter/app/ui/screens/settings/FairUsePolicyScreen.kt` - New fair use policy screen

---

# Otter v2.0.2 - Streaming & UI Improvements

## Download
[Download APK](../../releases/download/v2.0.2/app-release.apk)

## Overview
This release fixes critical streaming playback issues, improves the update checker, and enhances the UI with better formatting and card layouts.

## Key Fixes

### Streaming Playback
- Fixed streaming auto-start: playback now automatically starts after buffering completes
- Fixed playlist first video skipping: first video in queue now plays correctly instead of immediately advancing to next

### Update Checker
- Fixed GitHub API release fetching with proper headers
- Added scrolling support for release notes
- Added markdown formatting for release notes (strips ##, **, links, etc.)
- Added error logging for debugging fetch failures

### UI Improvements
- Commits screen now shows commit author and date in card layout
- Video description now properly strips HTML tags (<br>, <p>, etc.)
- Added clickable links in video descriptions

## Technical Details

### Files Modified
- `app/src/main/kotlin/com/Otter/app/player/PlayerService.kt` - Streaming auto-start, playlist handling
- `app/src/main/kotlin/com/Otter/app/player/PlayerConnectionManager.kt` - Removed duplicate STATE_ENDED handling
- `app/src/main/kotlin/com/Otter/app/util/AppUpdateUtil.kt` - GitHub API headers, error logging
- `app/src/main/kotlin/com/Otter/app/util/UpdateUtil.kt` - GitHub API headers, error logging
- `app/src/main/kotlin/com/Otter/app/ui/screens/settings/UpdateCheckerScreen.kt` - Scrolling, markdown formatting
- `app/src/main/kotlin/com/Otter/app/ui/screens/settings/CommitsScreen.kt` - Card layout, author/date display
- `app/src/main/kotlin/com/Otter/app/ui/screens/player/PlayerQueueSheets.kt` - HTML tag stripping

---

# GitHub Release Notes (v2.0.2)

## Release Overview
**Version**: 2.0.2  
**Date**: March 24, 2026  
**Type**: Bug Fixes & Improvements  

This release addresses critical streaming playback issues and improves the update checker functionality.

---

## Bug Fixes

### Streaming Playback Issues
- **Fixed**: Streaming playback not auto-starting after buffering
  - Added explicit `exoPlayer.play()` call when STATE_READY is reached
  - Ensures playback starts automatically after buffering completes
  - **Impact**: Critical - Fixes the main streaming issue

- **Fixed**: First video in playlist skipping to next
  - Removed duplicate STATE_ENDED handler in PlayerConnectionManager
  - Added logging for position discontinuity events
  - Disabled repeat mode to prevent auto-advancing
  - **Impact**: High - Fixes playlist playback

### Update Checker Issues
- **Fixed**: GitHub API release fetching failures
  - Added required headers: `Accept: application/vnd.github+json` and `X-GitHub-Api-Version: 2022-11-28`
  - Added error logging for debugging
  - **Impact**: High - Update checker now works correctly

---

## Improvements

### UI Enhancements
- **Scrolling on Update Checker Screen**
  - Added vertical scroll support for release notes
  - Prevents content from being cut off

- **Markdown Formatting for Release Notes**
  - Strips headers (##, ###)
  - Strips bold/italic markers (**, *, __, _)
  - Strips links and images
  - Strips inline code blocks
  - Decodes HTML entities

- **Commits Screen Card Layout**
  - Each commit now displayed in a Card component
  - Shows commit message, author name, and formatted date
  - Better visual separation between commits

- **Video Description Formatting**
  - Strips HTML tags (<br>, <p>, <div>, etc.)
  - Decodes HTML entities (&amp;, &lt;, &gt;, etc.)
  - Clickable links in descriptions

---

## Technical Details

### Core Changes
- **PlayerService**: Added explicit play() on STATE_READY, disabled repeat mode
- **PlayerConnectionManager**: Removed duplicate STATE_ENDED handler
- **AppUpdateUtil/UpdateUtil**: Added GitHub API headers and error logging
- **UpdateCheckerScreen**: Added scroll support and markdown stripping
- **CommitsScreen**: Added Card layout with author/date
- **PlayerQueueSheets**: Added HTML tag stripping for video descriptions

### Files Modified
- `app/src/main/kotlin/com/Otter/app/player/PlayerService.kt`
- `app/src/main/kotlin/com/Otter/app/player/PlayerConnectionManager.kt`
- `app/src/main/kotlin/com/Otter/app/util/AppUpdateUtil.kt`
- `app/src/main/kotlin/com/Otter/app/util/UpdateUtil.kt`
- `app/src/main/kotlin/com/Otter/app/ui/screens/settings/UpdateCheckerScreen.kt`
- `app/src/main/kotlin/com/Otter/app/ui/screens/settings/CommitsScreen.kt`
- `app/src/main/kotlin/com/Otter/app/ui/screens/player/PlayerQueueSheets.kt`
- `app/build.gradle.kts`

---

## Impact Summary
- **Critical fixes**: 1 (streaming auto-start)
- **High impact fixes**: 2 (playlist skipping, update checker)
- **UI improvements**: 4
- **Total issues resolved**: 7

---

## Release Status
**Status**: Ready for release  
**APK**: app-release.apk  
**Min Android**: 7.0 (API 24)  
**Target Android**: 16 (API 36)
- ✅ **Code pushed to `main`**
- ✅ **Tag pushed: `v2.0.1`**
- ✅ **Version updated in build.gradle.kts**
- ✅ **changelog.json created**
- ✅ **Release notes finalized**

---

## 📋 Previous Releases

### v2.0.0 (Major Release)
**Highlights**: In-app updates + changelog, download reliability improvements, format selection UX enhancements

**Key Features**:
- Complete App Updates screen with GitHub integration
- Changelog screen with categorized release notes  
- Improved format selection with Video/Audio separation
- Enhanced download engine compatibility
- Settings integration improvements

**Major Fixes**:
- Foreground service startup crash resolution
- Subscription sync retry behavior optimization

---

## Status
- **Code pushed to `main`**
- **Tag pushed: `v2.0.0`**

---

## Previous Release (v2.0.0)

## Highlights
This release improves **download reliability**, **format selection clarity**, and adds a complete in-app **App Updates + Changelog** experience.

---

## Improvements

### In-app updates + changelog (new)
- Added **App Updates** screen:
  - Checks latest GitHub release
  - Shows clear update states:
    - Up to date
    - Update available
    - Downloading
    - Ready to install
- Added **Changelog** screen:
  - Fetches GitHub releases
  - Displays release notes categorized into:
    - **Fixes**
    - **Improvements**
    - **Patches**
    - **Other**
- Added utilities:
  - `AppUpdateUtil` (GitHub release fetch + APK download/install helpers)
  - `ReleaseNotesUtil` (release notes categorization)

### Better format selection UX
- Combined formats UI is now clearer and separated into:
  - **Video (merges with audio)** (video-only choices that will merge with audio)
  - **Video + Audio (pre-merged)** (single-file formats; often lower quality)
- Improved “Suggested / Best Quality” option display (size/codec/format info behaves better, shows “Auto” where applicable)
- Improved sorting so higher resolution formats appear first

### Download engine quality + compatibility
- Compatibility mode now prefers **MP4/H.264 + M4A/AAC** when possible
- Smarter merge container selection:
  - Uses **MP4** when streams are MP4-compatible
  - Defaults to **MKV** for robust merging (VP9/AV1/Opus, fewer playback issues)
- Added ffmpeg postprocessor args to reduce timestamp/A-V sync issues in some players

### Settings integration
- About screen now includes shortcuts to:
  - **App updates**
  - **Changelog**
- Updates settings entry now opens the real App Updates screen

---

## Fixes

### Foreground service startup crash
- Fixed a potential Android crash:
  - `ForegroundServiceDidNotStartInTimeException`
- Foreground download service now calls `startForeground()` immediately on start (before any early returns)
- Downloader now uses `startForegroundService()` on Android O+ and logs failures safely

### Subscription sync retry behavior
- Worker now retries only for **transient network failures** (DNS, timeout, reset, unreachable, etc.)
- Prevents infinite retry loops for permanent/config failures
- Added file logging for subscription sync start/success/failure

---

## Patches
- More robust file logging around downloads and background sync
- Minor UI refinements and safe guards

---

## Notes
For best in-app changelog categorization in future releases, structure your release body like:
- `## Fixes`
- `## Improvements`
- `## Patches`

---

## Status
- **Code pushed to `main`**
- **Tag pushed: `v2.0.0`**
