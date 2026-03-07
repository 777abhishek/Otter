package com.Otter.app.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Otter.app.data.auth.CookieAuthStore
import com.Otter.app.data.auth.CookieExporter
import com.Otter.app.data.auth.CookieTarget
import com.Otter.app.data.auth.CookieTargetCatalog
import com.Otter.app.data.auth.YtDlpCookieExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val cookieAuthStore: CookieAuthStore,
        private val targetCatalog: CookieTargetCatalog,
        private val cookieExtractor: YtDlpCookieExtractor,
        private val cookieExporter: CookieExporter,
    ) : ViewModel() {
        suspend fun getTargetOnce(targetId: String): CookieTarget? = targetCatalog.findById(targetId)

        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading

        private val _errorMessage = MutableStateFlow<String?>(null)
        val errorMessage: StateFlow<String?> = _errorMessage

        private val _successMessage = MutableStateFlow<String?>(null)
        val successMessage: StateFlow<String?> = _successMessage

        fun extractCookiesFromBrowser(browser: YtDlpCookieExtractor.SupportedBrowser) {
            viewModelScope.launch {
                _isLoading.value = true
                _errorMessage.value = null
                _successMessage.value = null

                val result = cookieExtractor.extractCookiesFromBrowser(browser)
                result.onSuccess { _ ->
                    _successMessage.value = "Cookies extracted from ${browser.name.lowercase().replaceFirstChar { it.uppercase() }}"
                }.onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to extract cookies"
                }

                _isLoading.value = false
            }
        }

        fun importCookiesUri(uri: Uri) {
            viewModelScope.launch {
                _isLoading.value = true
                _errorMessage.value = null
                _successMessage.value = null

                val result = cookieExtractor.importCookiesUri(uri)
                result.onSuccess { _ ->
                    _successMessage.value = "Cookies imported successfully"
                }.onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to import cookies"
                }

                _isLoading.value = false
            }
        }

        fun importCookiesText(netscapeCookiesText: String) {
            viewModelScope.launch {
                _isLoading.value = true
                _errorMessage.value = null
                _successMessage.value = null

                val result = cookieExtractor.importCookiesText(netscapeCookiesText)
                result.onSuccess { _ ->
                    _successMessage.value = "Cookies pasted successfully"
                }.onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to paste cookies"
                }

                _isLoading.value = false
            }
        }

        fun importCookiesFile(file: File) {
            viewModelScope.launch {
                _isLoading.value = true
                _errorMessage.value = null
                _successMessage.value = null

                val result = cookieExtractor.importCookiesFile(file)
                result.onSuccess { _ ->
                    _successMessage.value = "Cookies imported successfully"
                }.onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to import cookies"
                }

                _isLoading.value = false
            }
        }

        fun clearCookies() {
            viewModelScope.launch {
                _isLoading.value = true
                cookieExtractor.clearAllCookies()
                cookieExporter.clearWebViewCookies()
                _successMessage.value = "Cookies cleared"
                _isLoading.value = false
            }
        }

        fun exportCookiesFromWebView(
            profileId: String?,
            targetId: String?,
            onSuccess: () -> Unit,
        ) {
            viewModelScope.launch {
                _isLoading.value = true
                _errorMessage.value = null
                _successMessage.value = null

                val pid =
                    profileId?.takeIf { it.isNotBlank() }
                        ?: cookieAuthStore.getActiveProfileIdOrNull()

                if (pid.isNullOrBlank()) {
                    _errorMessage.value = "No profile selected"
                    _isLoading.value = false
                    return@launch
                }

                val tid = targetId?.takeIf { it.isNotBlank() } ?: CookieTargetCatalog.TARGET_YOUTUBE
                val target = targetCatalog.findById(tid)
                if (target == null) {
                    _errorMessage.value = "Unknown target"
                    _isLoading.value = false
                    return@launch
                }

                val result =
                    withContext(Dispatchers.IO) {
                        cookieExporter.exportCookiesToFile(target)
                    }
                result.onSuccess { file ->
                    cookieAuthStore.upsertCookiesFilePath(pid, tid, file.absolutePath)
                    cookieAuthStore.setEnabledForYtDlp(pid, tid, true)
                    _successMessage.value = "Cookies saved"
                    onSuccess()
                }.onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to export cookies"
                }

                _isLoading.value = false
            }
        }

        fun clearMessages() {
            _errorMessage.value = null
            _successMessage.value = null
        }
    }
