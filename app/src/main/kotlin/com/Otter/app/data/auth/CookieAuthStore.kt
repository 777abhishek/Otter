package com.Otter.app.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.cookieAuthDataStore: DataStore<Preferences> by preferencesDataStore(name = "cookie_auth")

@Singleton
class CookieAuthStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val profileStore: YouTubeProfileStore,
        private val targetCatalog: CookieTargetCatalog,
    ) {
        private object Keys {
            val ENTRIES_JSON = stringPreferencesKey("cookie_auth_entries_json")
        }

        private val json =
            Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

        private val dataFlow = context.cookieAuthDataStore.data

        val entries: Flow<List<CookieAuthEntry>> =
            dataFlow.map { prefs ->
                val raw = prefs[Keys.ENTRIES_JSON]
                if (raw.isNullOrBlank()) {
                    emptyList()
                } else {
                    runCatching { json.decodeFromString<List<CookieAuthEntry>>(raw) }.getOrDefault(emptyList())
                }
            }

        suspend fun getActiveProfileIdOrNull(): String? = profileStore.activeProfileId.first()

        suspend fun upsertCookiesFilePath(
            profileId: String,
            targetId: String,
            path: String?,
        ) {
            context.cookieAuthDataStore.edit { prefs ->
                val raw = prefs[Keys.ENTRIES_JSON]
                val list =
                    if (raw.isNullOrBlank()) {
                        emptyList()
                    } else {
                        runCatching {
                            json.decodeFromString<List<CookieAuthEntry>>(raw)
                        }.getOrDefault(emptyList())
                    }

                val now = System.currentTimeMillis()
                val updated = list.toMutableList()
                val idx = updated.indexOfFirst { it.profileId == profileId && it.targetId == targetId }

                val newEntry =
                    if (idx >= 0) {
                        updated[idx].copy(cookiesFilePath = path, updatedAtEpochMs = now)
                    } else {
                        CookieAuthEntry(
                            profileId = profileId,
                            targetId = targetId,
                            cookiesFilePath = path,
                            enabledForYtDlp = false,
                            createdAtEpochMs = now,
                            updatedAtEpochMs = now,
                        )
                    }

                if (idx >= 0) updated[idx] = newEntry else updated.add(newEntry)
                prefs[Keys.ENTRIES_JSON] = json.encodeToString(updated)
            }
        }

        suspend fun setEnabledForYtDlp(
            profileId: String,
            targetId: String,
            enabled: Boolean,
        ) {
            context.cookieAuthDataStore.edit { prefs ->
                val raw = prefs[Keys.ENTRIES_JSON]
                val list =
                    if (raw.isNullOrBlank()) {
                        emptyList()
                    } else {
                        runCatching {
                            json.decodeFromString<List<CookieAuthEntry>>(raw)
                        }.getOrDefault(emptyList())
                    }

                val now = System.currentTimeMillis()
                val updated = list.toMutableList()
                val idx = updated.indexOfFirst { it.profileId == profileId && it.targetId == targetId }

                val newEntry =
                    if (idx >= 0) {
                        updated[idx].copy(enabledForYtDlp = enabled, updatedAtEpochMs = now)
                    } else {
                        CookieAuthEntry(
                            profileId = profileId,
                            targetId = targetId,
                            cookiesFilePath = null,
                            enabledForYtDlp = enabled,
                            createdAtEpochMs = now,
                            updatedAtEpochMs = now,
                        )
                    }

                if (idx >= 0) updated[idx] = newEntry else updated.add(newEntry)
                prefs[Keys.ENTRIES_JSON] = json.encodeToString(updated)
            }
        }

        suspend fun clear(
            profileId: String,
            targetId: String,
        ) {
            context.cookieAuthDataStore.edit { prefs ->
                val raw = prefs[Keys.ENTRIES_JSON]
                val list =
                    if (raw.isNullOrBlank()) {
                        emptyList()
                    } else {
                        runCatching {
                            json.decodeFromString<List<CookieAuthEntry>>(raw)
                        }.getOrDefault(emptyList())
                    }

                val now = System.currentTimeMillis()
                val updated =
                    list.map { e ->
                        if (e.profileId == profileId && e.targetId == targetId) {
                            e.copy(cookiesFilePath = null, enabledForYtDlp = false, updatedAtEpochMs = now)
                        } else {
                            e
                        }
                    }

                prefs[Keys.ENTRIES_JSON] = json.encodeToString(updated)
            }
        }

        suspend fun getCookiesFilePathOnceForUrl(
            url: String,
            requireEnabled: Boolean = true,
        ): String? {
            val targetId = targetCatalog.matchTargetIdForUrl(url) ?: return null
            val profileId = getActiveProfileIdOrNull() ?: return null
            return getCookiesFilePathOnce(profileId, targetId, requireEnabled)
        }

        suspend fun getCookiesFilePathOnce(
            profileId: String,
            targetId: String,
            requireEnabled: Boolean = true,
        ): String? {
            val list = entries.first()
            val e = list.firstOrNull { it.profileId == profileId && it.targetId == targetId } ?: return null
            if (requireEnabled && !e.enabledForYtDlp) return null
            return e.cookiesFilePath
        }
    }
