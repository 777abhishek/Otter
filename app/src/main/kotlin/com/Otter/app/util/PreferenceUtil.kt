package com.Otter.app.util

import android.content.Context

object PreferenceUtil {
    private const val PREFS_NAME = "Otter_prefs"

    private lateinit var prefs: android.content.SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        ensureDefaults()
    }

    private fun ensureDefaults() {
        if (!prefs.contains(YT_DLP_UPDATE_INTERVAL)) {
            prefs.edit().putLong(YT_DLP_UPDATE_INTERVAL, INTERVAL_WEEK).apply()
        }
        if (!prefs.contains(YT_DLP_UPDATE_CHANNEL)) {
            prefs.edit().putInt(YT_DLP_UPDATE_CHANNEL, YT_DLP_NIGHTLY).apply()
        }
    }

    fun isNetworkAvailableForDownload(): Boolean {
        // TODO: Implement network check
        return true
    }

    // Boolean preferences
    fun String.getBoolean(default: Boolean = false): Boolean = prefs.getBoolean(this, default)

    fun String.updateBoolean(value: Boolean) = prefs.edit().putBoolean(this, value).apply()

    // Int preferences
    fun String.getInt(default: Int = 0): Int = prefs.getInt(this, default)

    fun String.updateInt(value: Int) = prefs.edit().putInt(this, value).apply()

    // String preferences
    fun String.getString(default: String = ""): String = prefs.getString(this, default) ?: default

    fun String.updateString(value: String) = prefs.edit().putString(this, value).apply()

    // Long preferences
    fun String.getLong(default: Long = 0L): Long = prefs.getLong(this, default)

    fun String.updateLong(value: Long) = prefs.edit().putLong(this, value).apply()

    fun encodeString(
        key: String,
        value: String,
    ) = prefs.edit().putString(key, value).apply()

    fun encodeInt(
        key: String,
        value: Int,
    ) = prefs.edit().putInt(key, value).apply()

    fun getStringValue(
        key: String,
        default: String = "",
    ): String = prefs.getString(key, default) ?: default

    fun setStringValue(
        key: String,
        value: String,
    ) = prefs.edit().putString(key, value).apply()

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        ensureDefaults()
    }
}

// Preference keys
const val extractAudio = "extract_audio"
const val subtitle = "subtitle"
const val subtitleLanguage = "sub_lang"
const val videoClip = "video_clip"
const val mergeMultiAudioStream = "multi_audio_stream"

const val YT_DLP_VERSION = "yt-dlp_init"
const val YT_DLP_AUTO_UPDATE = "yt-dlp_update"
const val YT_DLP_UPDATE_CHANNEL = "yt-dlp_update_channel"
const val YT_DLP_UPDATE_TIME = "yt-dlp_last_update"
const val YT_DLP_UPDATE_INTERVAL = "yt-dlp_update_interval"

const val YT_DLP_STABLE = 0
const val YT_DLP_NIGHTLY = 1

// NewPipeExtractor preferences
const val NEWPIPE_VERSION = "newpipe_version"
const val NEWPIPE_UPDATE_TIME = "newpipe_last_update"

private const val INTERVAL_DAY = 86_400_000L
private const val INTERVAL_WEEK = 86_400_000L * 7
private const val INTERVAL_MONTH = 86_400_000L * 30
