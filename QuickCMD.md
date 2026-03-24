# Quick Commands for Otter Development

Quick reference for common development commands used when working on the Otter Android app.

---

## Build Commands

### Clean and Build Debug APK
```bash
./gradlew clean :app:assembleDebug --no-daemon --stacktrace --workers-5
```

### Build Release APK
```bash
./gradlew clean :app:assembleRelease --no-daemon --stacktrace --workers-5
```

---

## ADB Logcat Commands

### Filter for Crashes Only
```bash
adb logcat -v time AndroidRuntime:E ActivityManager:E *:S
```

### View Normal Logs (Minimal Output)
```bash
adb logcat -v time *:S
```

### Export Logs to File
```bash
adb logcat -v time *:S | Out-File -Encoding utf8 .\log_utf8.txt
```

---

## Installation Commands

### Install Debug APK
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Install Release APK
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## Storage Inspection

### Check App Storage Usage
```bash
adb shell run-as com.Otter.app sh -c "du -d 2 files cache databases no_backup | head -n 200" | Out-File -Encoding utf8 du_utf8.txt
```

---

## Useful Tips

- Use `--no-daemon` flag to prevent Gradle daemon issues
- The `-r` flag in `adb install` reinstalls existing apps while preserving data
- Use `-v time` in logcat for timestamped logs