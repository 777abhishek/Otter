package com.Otter.app.player

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.playbackResumeDataStore: DataStore<Preferences> by preferencesDataStore(name = "playback_resume")

data class PlaybackResumeState(
    val videoId: String,
    val videoUrl: String,
    val title: String,
    val thumbnailUrl: String,
    val uploaderName: String,
    val durationMs: Long,
    val positionMs: Long,
    val audioOnly: Boolean,
    val playWhenReady: Boolean,
)

@Singleton
class PlaybackResumeStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private object Keys {
            val VIDEO_ID = stringPreferencesKey("video_id")
            val VIDEO_URL = stringPreferencesKey("video_url")
            val TITLE = stringPreferencesKey("title")
            val THUMBNAIL_URL = stringPreferencesKey("thumbnail_url")
            val UPLOADER = stringPreferencesKey("uploader")
            val DURATION_MS = longPreferencesKey("duration_ms")
            val POSITION_MS = longPreferencesKey("position_ms")
            val AUDIO_ONLY = booleanPreferencesKey("audio_only")
            val PLAY_WHEN_READY = booleanPreferencesKey("play_when_ready")
        }

        val state: Flow<PlaybackResumeState?> =
            context.playbackResumeDataStore.data.map { prefs ->
                val url = prefs[Keys.VIDEO_URL] ?: return@map null
                PlaybackResumeState(
                    videoId = prefs[Keys.VIDEO_ID] ?: "",
                    videoUrl = url,
                    title = prefs[Keys.TITLE] ?: "",
                    thumbnailUrl = prefs[Keys.THUMBNAIL_URL] ?: "",
                    uploaderName = prefs[Keys.UPLOADER] ?: "",
                    durationMs = prefs[Keys.DURATION_MS] ?: 0L,
                    positionMs = prefs[Keys.POSITION_MS] ?: 0L,
                    audioOnly = prefs[Keys.AUDIO_ONLY] ?: false,
                    playWhenReady = prefs[Keys.PLAY_WHEN_READY] ?: false,
                )
            }

        suspend fun save(state: PlaybackResumeState) {
            context.playbackResumeDataStore.edit { prefs ->
                prefs[Keys.VIDEO_ID] = state.videoId
                prefs[Keys.VIDEO_URL] = state.videoUrl
                prefs[Keys.TITLE] = state.title
                prefs[Keys.THUMBNAIL_URL] = state.thumbnailUrl
                prefs[Keys.UPLOADER] = state.uploaderName
                prefs[Keys.DURATION_MS] = state.durationMs
                prefs[Keys.POSITION_MS] = state.positionMs
                prefs[Keys.AUDIO_ONLY] = state.audioOnly
                prefs[Keys.PLAY_WHEN_READY] = state.playWhenReady
            }
        }

        suspend fun clear() {
            context.playbackResumeDataStore.edit { it.clear() }
        }
    }
