package com.Otter.app.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Otter.app.data.auth.YouTubeProfile
import com.Otter.app.data.auth.YouTubeProfileStore
import com.Otter.app.data.auth.YtDlpCookieExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class ProfilesViewModel
    @Inject
    constructor(
        private val profileStore: YouTubeProfileStore,
        private val cookieExtractor: YtDlpCookieExtractor,
    ) : ViewModel() {
        val profiles: StateFlow<List<YouTubeProfile>> =
            profileStore.profiles
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val activeProfileId: StateFlow<String?> =
            profileStore.activeProfileId
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        fun createProfile(
            label: String,
            onCreated: (String) -> Unit = {},
        ) {
            viewModelScope.launch {
                val id = profileStore.createProfile(label)
                onCreated(id)
            }
        }

        fun setActiveProfile(id: String) {
            viewModelScope.launch {
                profileStore.setActiveProfile(id)
            }
        }

        fun deleteProfile(id: String) {
            viewModelScope.launch {
                profileStore.deleteProfile(id)
            }
        }

        fun clearCookies(profileId: String) {
            viewModelScope.launch {
                profileStore.setActiveProfile(profileId)
                profileStore.clear()
            }
        }

        fun importCookiesFromFile(
            context: Context,
            profileId: String,
            uri: Uri,
        ) {
            viewModelScope.launch {
                try {
                    profileStore.setActiveProfile(profileId)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val cookiesDir = File(context.filesDir, "cookies")
                    if (!cookiesDir.exists()) {
                        cookiesDir.mkdirs()
                    }
                    val cookiesFile = File(cookiesDir, "cookies_$profileId.txt")
                    inputStream?.use { input ->
                        FileOutputStream(cookiesFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    profileStore.setCookiesFilePath(cookiesFile.absolutePath)
                    profileStore.setLoggedIn(true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
