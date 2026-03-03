# Otter Setup Guide

## 📋 Prerequisites

Before you begin, ensure you have:

- **Android Studio** - Hedgehog (2023.1.1) or newer
- **JDK 17** - Required for Android Gradle Plugin
- **Android SDK 36** - Android 16 (Baklava)
- **Git** - For version control
- **8GB+ RAM** - Recommended for smooth builds

---

## 🛠️ Development Setup

### 1. Clone the Repository

```bash
# Via HTTPS
git clone https://github.com/777abhishek/Otter.git

# Or via SSH
git clone git@github.com:777abhishek/Otter.git

cd Otter
```

### 2. Open in Android Studio

1. Launch Android Studio
2. Select **Open an Existing Project**
3. Navigate to the cloned `Otter` directory
4. Click **OK**
5. Wait for Gradle sync to complete (may take 5-10 minutes first time)

### 3. Configure Local Properties

Create or edit `local.properties` in the project root:

```properties
# Android SDK path (usually auto-generated)
sdk.dir=/path/to/Android/sdk

# Optional: Backend configuration
Otter_BACKEND_BASE_URL=https://your-backend.com
Otter_APP_API_KEY=your_api_key_here
```

### 4. Configure Signing (For Release Builds)

1. Copy the example file:
   ```bash
   cp keystore.properties.example keystore.properties
   ```

2. Create a keystore (if you don't have one):
   ```bash
   keytool -genkey -v -keystore release.keystore -alias Otter -keyalg RSA -keysize 2048 -validity 10000
   ```

3. Update `keystore.properties`:
   ```properties
   storeFile=release.keystore
   storePassword=your_store_password
   keyAlias=Otter
   keyPassword=your_key_password
   ```

---

## 🏗️ Build Variants

### Debug Build

```bash
# Command line
./gradlew assembleDebug

# Or in Android Studio:
# Build → Select Build Variant → debug
# Build → Make Project (Ctrl+F9)
```

Output: `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk`

### Release Build

```bash
# Command line
./gradlew assembleRelease

# Or in Android Studio:
# Build → Select Build Variant → release
# Build → Generate Signed Bundle/APK
```

Output: `app/build/outputs/apk/release/app-arm64-v8a-release.apk`

### Clean Build

```bash
./gradlew clean assembleDebug
```

---

## 📱 Installing on Device

### Via ADB

```bash
# Install debug build
adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk

# Install release build
adb install -r app/build/outputs/apk/release/app-arm64-v8a-release.apk
```

### Via Android Studio

1. Connect your device (USB debugging enabled)
2. Select your device in the dropdown
3. Click **Run** (Shift+F10)

---

## 🔧 Project Configuration

### Build Flavors

The project uses a single flavor configuration:

| Variant | Minify | Shrink Resources | Signing |
|---------|--------|------------------|---------|
| **Debug** | No | No | Debug key |
| **Release** | Yes | Yes | Release key |

### ABI Splits

The app is built for multiple architectures:

- `arm64-v8a` - 64-bit ARM (most modern devices)
- `armeabi-v7a` - 32-bit ARM (older devices)

Each architecture gets a separate APK, reducing download size.

---

## 🧪 Running Tests

### Unit Tests

```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.Otter.app.ui.viewmodels.HomeViewModelTest"

# With coverage
./gradlew testDebugUnitTest --coverage
```

### Instrumentation Tests

```bash
# Run on connected device
./gradlew connectedAndroidTest

# Run specific test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.Otter.app.ExampleTest
```

### Lint Checks

```bash
# KtLint (code style)
./gradlew ktlintCheck

# Auto-fix KtLint issues
./gradlew ktlintFormat

# Detekt (static analysis)
./gradlew detekt
```

---

## 📦 Dependencies

### Core Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| **Compose** | Latest | UI framework |
| **Hilt** | 2.52 | Dependency injection |
| **Room** | 2.6.1 | Local database |
| **Coil** | 3.x | Image loading |
| **Media3** | 1.4.x | Video playback |
| **yt-dlp** | Embedded | Video extraction |

### Checking for Updates

```bash
# Check for dependency updates
./gradlew dependencyUpdates
```

---

## 🐛 Debugging

### Logcat

```bash
# Filter app logs
adb logcat -s Otter

# Filter by tag
adb logcat -s SubscriptionSyncService

# Save to file
adb logcat > logs.txt
```

### Debug Build Features

Debug builds include:
- **Debuggable** - Can attach debugger
- **Logging** - Verbose logs enabled
- **Strict Mode** - Detects threading issues

### Common Debug Flags

In `Otterlication.kt`, you can enable:
```kotlin
// Strict mode for debugging
StrictMode.enableDefaults()
```

---

## 🔄 Syncing with Upstream

If you've forked the repository:

```bash
# Add upstream remote
git remote add upstream https://github.com/777abhishek/Otter.git

# Fetch upstream changes
git fetch upstream

# Merge into your branch
git checkout main
git merge upstream/main

# Or rebase (cleaner history)
git rebase upstream/main
```

---

## 🚀 Release Process

### 1. Update Version

In `app/build.gradle.kts`:
```kotlin
versionCode = 2  // Increment by 1
versionName = "1.1.0"  // Semantic versioning
```

### 2. Update Changelog

Create/update `CHANGELOG.md`:
```markdown
## [1.1.0] - 2024-XX-XX
### Added
- New feature X
### Fixed
- Bug Y
```

### 3. Build Release

```bash
./gradlew clean assembleRelease
```

### 4. Test Release Build

```bash
adb install -r app/build/outputs/apk/release/app-arm64-v8a-release.apk
```

### 5. Create Git Tag

```bash
git tag -a v1.1.0 -m "Release version 1.1.0"
git push origin v1.1.0
```

---

## 📁 Project Structure Reference

```
Otter/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/Otter/app/
│   │   │   │   ├── data/         # Data layer
│   │   │   │   ├── di/           # Dependency injection
│   │   │   │   ├── network/      # API services
│   │   │   │   ├── player/       # Player service
│   │   │   │   ├── service/      # Background services
│   │   │   │   ├── ui/           # UI layer
│   │   │   │   ├── util/         # Utilities
│   │   │   │   └── work/         # WorkManager
│   │   │   ├── res/              # Resources
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                 # Unit tests
│   │   └── androidTest/          # Instrumentation tests
│   └── build.gradle.kts
├── docs/                         # Documentation
├── gradle/                       # Gradle wrapper
├── build.gradle.kts              # Root build config
├── settings.gradle.kts           # Project settings
├── README.md                     # Project readme
├── CONTRIBUTING.md               # Contribution guide
└── LICENSE                       # MIT License
```

---

## ❓ FAQ

### Q: Gradle sync fails with "SDK location not found"
**A:** Create `local.properties` with your SDK path:
```properties
sdk.dir=/path/to/Android/sdk
```

### Q: Build fails with "OutOfMemory"
**A:** Increase Gradle memory in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -XX:+HeapDumpOnOutOfMemoryError
```

### Q: Can't install on device (INSTALL_FAILED_UPDATE_INCOMPATIBLE)
**A:** Uninstall the existing app first:
```bash
adb uninstall com.Otter.app
```

### Q: Release build fails with signing error
**A:** Check `keystore.properties` exists and has correct values.

---

## 🆘 Getting Help

- **GitHub Issues**: [Create an issue](https://github.com/777abhishek/Otter/issues)
- **Documentation**: Check the `docs/` folder
- **Code Comments**: Inline documentation in source files
