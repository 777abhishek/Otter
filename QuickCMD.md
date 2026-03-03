For crash = adb logcat -v time AndroidRuntime:E ActivityManager:E *:S
For normal = adb logcat -v time *:S
Export Log = adb logcat -v time *:S | Out-File -Encoding utf8 .\log_utf8.txt
compile = ./gradlew clean :app:assembleDebug --no-daemon --stacktrace --workers-5
install = adb install -r app/build/outputs/apk/debug/app-debug.apk


adb shell run-as com.Otter.app sh -c "du -d 2 files cache databases no_backup | head -n 200" | Out-File -Encoding utf8 du_utf8.txt  