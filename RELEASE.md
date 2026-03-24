# GitHub Release Notes (v2.0.0)

## Highlights
This release improves **download reliability**, **format selection clarity**, and adds a complete in-app **App Updates + Changelog** experience.

---

## Improvements

### In-app updates + changelog (new)
- Added **App Updates** screen:
  - Checks the latest GitHub release
  - Shows clear update states:
    - Up to date
    - Update available
    - Downloading
    - Ready to install
  - Full-screen **update overlay** with animated **wavy progress** indicator
  - Actions:
    - **Download APK** when available
    - **Open GitHub release page**
    - **Install downloaded APK** (with unknown-sources handling)
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
