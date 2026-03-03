# Otter Features Guide

## 🎬 Video Downloading

### Supported Sites
Otter supports downloading from 1000+ websites via yt-dlp:

- **YouTube** - Videos, shorts, playlists, live streams
- **Instagram** - Reels, stories, posts
- **Twitter/X** - Videos, GIFs
- **Reddit** - Videos with audio
- **TikTok** - Videos without watermark
- **Vimeo, Dailymotion, Twitch** - And many more

### Download Options

#### Video Quality
- **4K (2160p)** - Highest quality, large file size
- **1080p Full HD** - Recommended for most users
- **720p HD** - Good quality, smaller size
- **480p SD** - Smaller file size
- **360p** - Smallest video files

#### Audio Only
- **MP3** - Most compatible
- **M4A/AAC** - Better quality, smaller size
- **Opus/WEBM** - Best quality-to-size ratio

### Download Features

- **Background Downloads** - Continue using your phone while downloading
- **Pause/Resume** - Pause downloads and resume later
- **Queue Management** - Multiple downloads with priority control
- **Auto-Retry** - Automatically retry failed downloads
- **Smart Naming** - Automatic file naming based on video title

---

## 📋 Playlist Management

### Synced Content

Otter can sync and manage:

| Type | Description | Requires Auth |
|------|-------------|---------------|
| **Playlists** | Your saved YouTube playlists | Optional |
| **Watch Later** | Videos saved for later | Yes |
| **Liked Videos** | Your liked videos list | Yes |
| **Subscriptions** | Channel subscriptions | Yes |

### Sync Features

- **Automatic Sync** - Scheduled background sync
- **Manual Refresh** - Pull-to-refresh on home screen
- **Offline Access** - View synced playlists without internet
- **Progress Tracking** - See sync progress in real-time

### Playlist Operations

- **Download All** - Download entire playlist
- **Selective Download** - Choose specific videos
- **Sort & Filter** - By date, duration, title
- **Search** - Find videos within playlists

---

## 👤 Multi-Profile Support

### What Are Profiles?

Profiles allow you to:
- Switch between different YouTube accounts
- Keep separate cookies per account
- Download from membership channels
- Access age-restricted content

### Profile Management

```
Settings → Profiles → Add Profile
```

Each profile has:
- Unique name/label
- Separate cookie storage
- Independent settings

### Cookie Targets

For each profile, you can enable cookies for:

| Target | Use Case |
|--------|----------|
| **YouTube** | Watch Later, Liked Videos, Members-only |
| **Instagram** | Private accounts you follow |
| **Twitter/X** | Private tweets |
| **Reddit** | Saved posts |
| **Custom** | Any website you want |

### How to Connect

1. **Via WebView** (Recommended)
   - Opens login page in-app
   - Automatically extracts cookies
   - Works for most sites

2. **Import cookies.txt**
   - Export from browser extension
   - Netscape format supported
   - Good for advanced users

---

## 🎥 Video Streaming

### Built-in Player

Otter includes a full-featured video player:

- **Adaptive Streaming** - Auto-adjusts quality
- **Subtitle Support** - Load external subtitles
- **Playback Speed** - 0.5x to 2.0x
- **Background Play** - Audio continues when screen off
- **Picture-in-Picture** - Watch while using other apps

### Streaming vs Downloading

| Streaming | Downloading |
|-----------|-------------|
| Instant playback | Watch offline |
| Uses data each time | Download once |
| Quality auto-adjusts | Choose quality |
| Requires internet | No internet needed |

---

## 🎨 UI Features

### Material Design 3

- **Dynamic Colors** - Theme matches your wallpaper (Android 12+)
- **Dark Mode** - Automatic or manual
- **Expressive Components** - Modern UI elements

### Navigation

- **Bottom Navigation** - Quick access to main screens
- **Gesture Navigation** - Swipe to go back
- **Deep Links** - Share videos directly to app

### Home Screen

- **Playlists Grid** - Visual playlist cards
- **Watch Later** - Quick access to saved videos
- **Liked Videos** - Your liked content
- **Pull to Refresh** - Sync all playlists

---

## ⚙️ Settings

### Download Settings

| Setting | Options | Default |
|---------|---------|---------|
| **Download Location** | Internal/SD Card | Internal |
| **Video Quality** | 360p-4K | 1080p |
| **Audio Quality** | 128-320kbps | 192kbps |
| **WiFi Only** | On/Off | Off |
| **Concurrent Downloads** | 1-3 | 2 |

### Player Settings

| Setting | Options | Default |
|---------|---------|---------|
| **Default Quality** | Auto/360p-4K | Auto |
| **Background Play** | On/Off | On |
| **Skip Silence** | On/Off | Off |
| **Default Speed** | 0.5x-2.0x | 1.0x |

### App Settings

- **Theme** - System/Light/Dark
- **Language** - System default or choose
- **Notifications** - Download complete, errors
- **Crash Reporting** - On/Off

---

## 🔒 Privacy & Security

### Data Storage

- **Local Only** - All data stays on device
- **Encrypted Storage** - Cookies stored securely
- **No Account Required** - Use without signing up

### Permissions Required

| Permission | Why Needed |
|------------|------------|
| **Storage** | Save downloaded videos |
| **Network** | Download and stream |
| **Foreground Service** | Background downloads |
| **Notifications** | Download status |

### What We Don't Do

- ❌ No tracking or analytics (unless enabled)
- ❌ No data collection
- ❌ No third-party sharing
- ❌ No account required

---

## 🚀 Performance

### Optimizations

- **Parallel Downloads** - Multiple downloads simultaneously
- **Efficient Sync** - Only fetch changed playlists
- **Smart Caching** - Reduce redundant network calls
- **Lightweight Player** - Hardware-accelerated decoding

### Battery Efficiency

- **Batch Operations** - Group network requests
- **Doze Compatible** - Respects battery saver
- **Smart Wake Locks** - Only when needed

---

## 🆘 Troubleshooting

### Common Issues

#### "Video unavailable"
- Check if video is region-restricted
- Try enabling cookies for the site
- Video may have been deleted

#### "Download failed"
- Check internet connection
- Try different quality/format
- Site may have changed

#### "Can't access Watch Later"
- Need to connect cookies first
- Go to Profiles → Cookie Targets → YouTube → Connect

#### "App is slow"
- Clear app cache
- Reduce concurrent downloads
- Check available storage

### Getting Help

1. Check existing issues on GitHub
2. Include device info and Android version
3. Attach relevant logs from Settings → Logs
4. Describe steps to reproduce
