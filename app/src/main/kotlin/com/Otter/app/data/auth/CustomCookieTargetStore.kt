package com.Otter.app.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.customCookieTargetsDataStore: DataStore<Preferences> by preferencesDataStore(name = "custom_cookie_targets")

@Singleton
class CustomCookieTargetStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private object Keys {
            val TARGETS_JSON = stringPreferencesKey("custom_cookie_targets_json")
        }

        private val json =
            Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

        val targets: Flow<List<CookieTarget>> =
            context.customCookieTargetsDataStore.data.map { prefs ->
                val raw = prefs[Keys.TARGETS_JSON]
                if (raw.isNullOrBlank()) {
                    emptyList()
                } else {
                    runCatching { json.decodeFromString<List<CookieTarget>>(raw) }.getOrDefault(emptyList())
                }
            }

        suspend fun upsert(target: CookieTarget) {
            context.customCookieTargetsDataStore.edit { prefs ->
                val raw = prefs[Keys.TARGETS_JSON]
                val list =
                    if (raw.isNullOrBlank()) {
                        emptyList()
                    } else {
                        runCatching {
                            json.decodeFromString<List<CookieTarget>>(raw)
                        }.getOrDefault(emptyList())
                    }

                val updated = list.toMutableList()
                val idx = updated.indexOfFirst { it.id == target.id }
                if (idx >= 0) updated[idx] = target else updated.add(target)
                prefs[Keys.TARGETS_JSON] = json.encodeToString(updated)
            }
        }

        suspend fun delete(targetId: String) {
            context.customCookieTargetsDataStore.edit { prefs ->
                val raw = prefs[Keys.TARGETS_JSON]
                val list =
                    if (raw.isNullOrBlank()) {
                        emptyList()
                    } else {
                        runCatching {
                            json.decodeFromString<List<CookieTarget>>(raw)
                        }.getOrDefault(emptyList())
                    }

                val updated = list.filterNot { it.id == targetId }
                prefs[Keys.TARGETS_JSON] = json.encodeToString(updated)
            }
        }
    }
