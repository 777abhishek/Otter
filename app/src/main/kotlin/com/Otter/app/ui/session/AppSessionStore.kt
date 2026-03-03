package com.Otter.app.ui.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appSessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_session")

@Singleton
class AppSessionStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private object Keys {
            val LAST_TOP_LEVEL_ROUTE = stringPreferencesKey("last_top_level_route")
        }

        val lastTopLevelRoute: Flow<String?> =
            context.appSessionDataStore.data.map { prefs ->
                prefs[Keys.LAST_TOP_LEVEL_ROUTE]
            }

        suspend fun setLastTopLevelRoute(route: String) {
            context.appSessionDataStore.edit { prefs ->
                prefs[Keys.LAST_TOP_LEVEL_ROUTE] = route
            }
        }
    }
