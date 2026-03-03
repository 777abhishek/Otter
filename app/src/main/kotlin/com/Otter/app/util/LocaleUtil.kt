package com.Otter.app.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

/**
 * Utility functions for handling app locale/language settings
 */

@Suppress("DEPRECATION")
fun setAppLocale(
    context: Context,
    locale: Locale,
) {
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}

/**
 * Sets the app's locale to the specified language code
 * @param context Application context
 * @param languageCode Language code (e.g., "en", "es", "fr") or "system" for default
 */
fun setAppLocale(
    context: Context,
    languageCode: String,
) {
    val locale =
        if (languageCode == "system") {
            Locale.getDefault()
        } else {
            Locale.forLanguageTag(languageCode)
        }
    setAppLocale(context, locale)
}

/**
 * Gets the current app locale
 * @param context Application context
 * @return Current locale
 */
fun getAppLocale(context: Context): Locale {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.resources.configuration.locales[0]
    } else {
        @Suppress("DEPRECATION")
        context.resources.configuration.locale
    }
}
