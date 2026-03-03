package com.Otter.app.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.youtubeProfilesDataStore: DataStore<Preferences> by preferencesDataStore(name = "cookies")

@Singleton
class YouTubeProfileStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private object Keys {
            val PROFILES_JSON = stringPreferencesKey("yt_profiles_json")
            val ACTIVE_PROFILE_ID = stringPreferencesKey("yt_active_profile_id")

            // Legacy CookieStore keys for migration
            val LEGACY_COOKIES_FILE_PATH = stringPreferencesKey("cookies_file_path")
            val LEGACY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        }

        private val json =
            Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

        private suspend fun ensureMigratedIfNeeded() {
            val prefs = context.youtubeProfilesDataStore.data.first()
            if (prefs[Keys.PROFILES_JSON] != null) return

            val legacyPath = prefs[Keys.LEGACY_COOKIES_FILE_PATH]
            val legacyLoggedIn = prefs[Keys.LEGACY_IS_LOGGED_IN] ?: false

            // If there is no legacy data either, initialize with an empty state.
            if (legacyPath.isNullOrBlank() && !legacyLoggedIn) {
                context.youtubeProfilesDataStore.edit { e ->
                    if (e[Keys.PROFILES_JSON] == null) {
                        e[Keys.PROFILES_JSON] = json.encodeToString(emptyList<YouTubeProfile>())
                    }
                }
                return
            }

            val id = UUID.randomUUID().toString()
            val profile =
                YouTubeProfile(
                    id = id,
                    label = "Profile 1",
                    cookiesFilePath = legacyPath,
                    isLoggedIn = legacyLoggedIn,
                )

            context.youtubeProfilesDataStore.edit { e ->
                e[Keys.PROFILES_JSON] = json.encodeToString(listOf(profile))
                e[Keys.ACTIVE_PROFILE_ID] = id
            }
        }

        private val dataFlow = context.youtubeProfilesDataStore.data

        val profiles: Flow<List<YouTubeProfile>> =
            flow {
                ensureMigratedIfNeeded()
                emitAll(
                    dataFlow.map { prefs ->
                        val raw = prefs[Keys.PROFILES_JSON]
                        if (raw.isNullOrBlank()) {
                            emptyList()
                        } else {
                            runCatching {
                                json.decodeFromString<List<YouTubeProfile>>(
                                    raw,
                                )
                            }.getOrDefault(emptyList())
                        }
                    },
                )
            }

        val activeProfileId: Flow<String?> =
            flow {
                ensureMigratedIfNeeded()
                emitAll(
                    dataFlow.map { prefs ->
                        prefs[Keys.ACTIVE_PROFILE_ID]
                    },
                )
            }

        val activeProfile: Flow<YouTubeProfile?> =
            flow {
                ensureMigratedIfNeeded()
                emitAll(
                    dataFlow.map { prefs ->
                        val raw = prefs[Keys.PROFILES_JSON]
                        val list =
                            if (raw.isNullOrBlank()) {
                                emptyList()
                            } else {
                                runCatching {
                                    json.decodeFromString<List<YouTubeProfile>>(raw)
                                }.getOrDefault(emptyList())
                            }
                        val activeId = prefs[Keys.ACTIVE_PROFILE_ID]
                        activeId?.let { id -> list.firstOrNull { it.id == id } }
                    },
                )
            }

        // Compatibility API (matches old CookieStore usage)
        val cookiesFilePath: Flow<String?> = activeProfile.map { it?.cookiesFilePath }
        val isLoggedIn: Flow<Boolean> = activeProfile.map { it?.isLoggedIn ?: false }

        suspend fun getCookiesFilePathOnce(): String? {
            ensureMigratedIfNeeded()
            return cookiesFilePath.first()
        }

        suspend fun isLoggedInOnce(): Boolean {
            ensureMigratedIfNeeded()
            return isLoggedIn.first()
        }

        suspend fun setCookiesFilePath(path: String?) {
            ensureMigratedIfNeeded()
            context.youtubeProfilesDataStore.edit { prefs ->
                val raw = prefs[Keys.PROFILES_JSON].orEmpty()
                val list = runCatching { json.decodeFromString<List<YouTubeProfile>>(raw) }.getOrDefault(emptyList())
                val activeId = prefs[Keys.ACTIVE_PROFILE_ID]

                val ensured = ensureActiveProfile(list, activeId)
                val updated =
                    ensured.profiles.map { p ->
                        if (p.id == ensured.activeId) {
                            p.copy(
                                cookiesFilePath = path,
                                updatedAtEpochMs = System.currentTimeMillis(),
                            )
                        } else {
                            p
                        }
                    }

                prefs[Keys.PROFILES_JSON] = json.encodeToString(updated)
                prefs[Keys.ACTIVE_PROFILE_ID] = ensured.activeId
            }
        }

        suspend fun setLoggedIn(value: Boolean) {
            ensureMigratedIfNeeded()
            context.youtubeProfilesDataStore.edit { prefs ->
                val raw = prefs[Keys.PROFILES_JSON].orEmpty()
                val list = runCatching { json.decodeFromString<List<YouTubeProfile>>(raw) }.getOrDefault(emptyList())
                val activeId = prefs[Keys.ACTIVE_PROFILE_ID]

                val ensured = ensureActiveProfile(list, activeId)
                val updated =
                    ensured.profiles.map { p ->
                        if (p.id == ensured.activeId) {
                            p.copy(
                                isLoggedIn = value,
                                updatedAtEpochMs = System.currentTimeMillis(),
                            )
                        } else {
                            p
                        }
                    }

                prefs[Keys.PROFILES_JSON] = json.encodeToString(updated)
                prefs[Keys.ACTIVE_PROFILE_ID] = ensured.activeId
            }
        }

        suspend fun clear() {
            ensureMigratedIfNeeded()
            context.youtubeProfilesDataStore.edit { prefs ->
                val raw = prefs[Keys.PROFILES_JSON].orEmpty()
                val list = runCatching { json.decodeFromString<List<YouTubeProfile>>(raw) }.getOrDefault(emptyList())
                val activeId = prefs[Keys.ACTIVE_PROFILE_ID]

                val ensured = ensureActiveProfile(list, activeId)
                val updated =
                    ensured.profiles.map { p ->
                        if (p.id == ensured.activeId) {
                            p.copy(
                                cookiesFilePath = null,
                                isLoggedIn = false,
                                updatedAtEpochMs = System.currentTimeMillis(),
                            )
                        } else {
                            p
                        }
                    }

                prefs[Keys.PROFILES_JSON] = json.encodeToString(updated)
                prefs[Keys.ACTIVE_PROFILE_ID] = ensured.activeId
            }
        }

        suspend fun createProfile(label: String): String {
            ensureMigratedIfNeeded()
            val id = UUID.randomUUID().toString()
            context.youtubeProfilesDataStore.edit { prefs ->
                val raw = prefs[Keys.PROFILES_JSON].orEmpty()
                val list = runCatching { json.decodeFromString<List<YouTubeProfile>>(raw) }.getOrDefault(emptyList())
                val now = System.currentTimeMillis()
                val newProfile =
                    YouTubeProfile(
                        id = id,
                        label = label,
                        createdAtEpochMs = now,
                        updatedAtEpochMs = now,
                    )
                prefs[Keys.PROFILES_JSON] = json.encodeToString(list + newProfile)
                prefs[Keys.ACTIVE_PROFILE_ID] = id
            }
            return id
        }

        suspend fun setActiveProfile(id: String): Result<Unit> {
            ensureMigratedIfNeeded()
            return runCatching {
                context.youtubeProfilesDataStore.edit { prefs ->
                    val raw = prefs[Keys.PROFILES_JSON].orEmpty()
                    val list = runCatching { json.decodeFromString<List<YouTubeProfile>>(raw) }.getOrDefault(emptyList())
                    if (list.none { it.id == id }) {
                        throw IllegalArgumentException("Profile not found")
                    }
                    prefs[Keys.ACTIVE_PROFILE_ID] = id
                }
            }
        }

        suspend fun deleteProfile(id: String): Result<Unit> {
            ensureMigratedIfNeeded()
            return runCatching {
                context.youtubeProfilesDataStore.edit { prefs ->
                    val raw = prefs[Keys.PROFILES_JSON].orEmpty()
                    val list = runCatching { json.decodeFromString<List<YouTubeProfile>>(raw) }.getOrDefault(emptyList())
                    val filtered = list.filterNot { it.id == id }
                    prefs[Keys.PROFILES_JSON] = json.encodeToString(filtered)

                    val activeId = prefs[Keys.ACTIVE_PROFILE_ID]
                    if (activeId == id) {
                        prefs.remove(Keys.ACTIVE_PROFILE_ID)
                    }
                }
            }
        }

        private data class EnsuredActiveProfile(val profiles: List<YouTubeProfile>, val activeId: String)

        private fun ensureActiveProfile(
            list: List<YouTubeProfile>,
            activeId: String?,
        ): EnsuredActiveProfile {
            if (list.isEmpty()) {
                val id = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                val profile =
                    YouTubeProfile(
                        id = id,
                        label = "Profile 1",
                        createdAtEpochMs = now,
                        updatedAtEpochMs = now,
                    )
                return EnsuredActiveProfile(listOf(profile), id)
            }

            val id = activeId?.takeIf { act -> list.any { it.id == act } } ?: list.first().id
            return EnsuredActiveProfile(list, id)
        }
    }
