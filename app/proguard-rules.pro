# Ignore missing JDK/Desktop dependencies in Rhino/Mozilla
-dontwarn java.beans.**
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**

# Ignore missing JSoup/Re2j optional dependencies
-dontwarn org.jsoup.helper.Re2jRegex**
-dontwarn com.google.re2j.**

# Ignore OkHttp internal warnings (common in older versions)
-dontwarn okhttp3.internal.**

# Keep the Rhino/Mozilla parts you're already using
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**

# Hilt rules
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp { *; }
-keep class dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }
-keep class dagger.hilt.android.internal.lifecycle.HiltViewModelMap { *; }
-keep class dagger.hilt.android.internal.lifecycle.HiltWrapper_HiltViewModelFactory { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class com.Otter.app.ui.viewmodels.** { *; }
-keep class com.Otter.app.Otterlication { *; }

# Apache Commons Compress (ZIP) - uses registries/reflection for extra fields
-dontwarn org.apache.commons.compress.**
-keep class org.apache.commons.compress.archivers.zip.** { *; }
-keepclassmembers class org.apache.commons.compress.archivers.zip.** {
    public <init>(...);
}
