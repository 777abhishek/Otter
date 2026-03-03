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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.cookieDataStore: DataStore<Preferences> by preferencesDataStore(name = "cookies")

@Singleton
class CookieStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private object Keys {
            val COOKIES_FILE_PATH = stringPreferencesKey("cookies_file_path")
            val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        }

        val cookiesFilePath: Flow<String?> =
            context.cookieDataStore.data.map { prefs -> prefs[Keys.COOKIES_FILE_PATH] }

        val isLoggedIn: Flow<Boolean> =
            context.cookieDataStore.data.map { prefs -> prefs[Keys.IS_LOGGED_IN] ?: false }

        suspend fun setCookiesFilePath(path: String?) {
            context.cookieDataStore.edit { prefs ->
                if (path == null) {
                    prefs.remove(Keys.COOKIES_FILE_PATH)
                } else {
                    prefs[Keys.COOKIES_FILE_PATH] = path
                }
            }
        }

        suspend fun setLoggedIn(value: Boolean) {
            context.cookieDataStore.edit { prefs ->
                prefs[Keys.IS_LOGGED_IN] = value
            }
        }

        suspend fun clear() {
            context.cookieDataStore.edit { prefs ->
                prefs.remove(Keys.COOKIES_FILE_PATH)
                prefs[Keys.IS_LOGGED_IN] = false
            }
        }

        suspend fun getCookiesFilePathOnce(): String? = cookiesFilePath.first()

        suspend fun isLoggedInOnce(): Boolean = isLoggedIn.first()
    }
