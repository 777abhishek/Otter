# Changelog

All notable changes to Otter will be documented in this file.

## [2.0.4] - 2026-03-26

### Added
- Code of Conduct for community guidelines
- Contributing guidelines for contributors
- GPL-3.0 License
- Fastlane configuration for automated releases
- Multi-language support infrastructure

### Improved
- Reduced vertical spacing between video and playlist cards for better UI compactness
- Enhanced error handling with user-visible notifications

### Fixed
- Update Checker screen now correctly displays version range (e.g., "2.0.2 → 2.0.3") when an update is available

### Technical
- Added proper project documentation
- Improved code organization and structure

## [2.0.3] - 2026-03-25

### Added
- Stage 2 metadata enrichment scheduling (background enhancement after initial sync)
- Dedicated metadata notification channel for Stage 2 progress
- Expedited WorkManager jobs for faster Stage 2 execution
- Failure notifications for metadata enhancement errors

### Improved
- Enhanced notification system with separate channels for sync and metadata
- Better error handling and user feedback

### Fixed
- Hilt Context injection errors by adding @ApplicationContext annotation
- Stage 2 worker now shows notification when no videos found to enhance

## [2.0.2] - 2026-03-20

### Added
- Update checker functionality
- Automatic version checking on app launch

### Improved
- Better update notification UI
- Version comparison logic

## [2.0.1] - 2026-03-15

### Fixed
- Minor bug fixes
- Performance improvements

## [2.0.0] - 2026-03-10

### Added
- Initial public release
- YouTube playlist subscription and sync
- Video playback and management
- Offline video support
- Material Design 3 UI
- Dark/Light theme support
- Custom yt-dlp command templates
- Download history and management
- Watch later functionality
- Like/unlike videos
- Settings management
- Notification system for sync operations

### Improved
- Clean and modern UI
- Efficient sync mechanism
- Robust error handling

## [Unreleased]

### Planned
- Multi-language support translations
- Enhanced download manager
- More customization options
- Performance optimizations
