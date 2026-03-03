package com.Otter.app.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.collection.LruCache
import androidx.appcompat.content.res.AppCompatResources
import com.Otter.app.R

/**
 * Provides theme-aware app icons loaded from assets.
 *
 * Dark theme icons are stored in `IconDark/` (e.g. `IconDark/128.png`).
 * Light theme icons are stored in `IconsLight/` (e.g. `IconsLight/icon128.png`).
 *
 * Icons are transparent vector-line PNGs — no background is applied.
 */
object DynamicIconProvider {

    // Available icon sizes in assets
    private val AVAILABLE_SIZES = intArrayOf(16, 24, 32, 64, 128, 256, 512)

    // Cache up to 8 bitmaps (dark+light × a few common sizes)
    private val cache = LruCache<String, Bitmap>(8)

    /**
     * Finds the closest available size that is >= the requested size.
     * Falls back to the largest available if none is larger.
     */
    private fun closestSize(requestedSize: Int): Int {
        return AVAILABLE_SIZES.firstOrNull { it >= requestedSize }
            ?: AVAILABLE_SIZES.last()
    }

    /**
     * Resolves whether the system is currently in dark mode.
     */
    fun isSystemDarkTheme(context: Context): Boolean {
        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return uiMode == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Loads a theme-aware icon bitmap from assets.
     *
     * @param context Application context
     * @param isDark  Whether to load the dark-theme icon
     * @param size    Desired icon size in pixels (will pick the closest available)
     * @return The decoded [Bitmap], or null if loading fails
     */
    fun getIconBitmap(context: Context, isDark: Boolean, size: Int = 512): Bitmap? {
        val resolvedSize = closestSize(size)
        val key = "${if (isDark) "dark" else "light"}_$resolvedSize"

        cache[key]?.let { return it }

        return try {
            val drawableRes = if (isDark) R.drawable.icon_dark else R.drawable.icon_light
            val drawable = AppCompatResources.getDrawable(context, drawableRes)
                ?: return null
            val bitmap = Bitmap.createBitmap(resolvedSize, resolvedSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            cache.put(key, bitmap)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Loads a theme-aware icon as a Compose [ImageBitmap].
     *
     * @param context Application context
     * @param isDark  Whether to load the dark-theme icon
     * @param size    Desired icon size in pixels
     * @return The [ImageBitmap], or null if loading fails
     */
    fun getIconBitmapForCompose(context: Context, isDark: Boolean, size: Int = 512): ImageBitmap? {
        return getIconBitmap(context, isDark, size)?.asImageBitmap()
    }

}
