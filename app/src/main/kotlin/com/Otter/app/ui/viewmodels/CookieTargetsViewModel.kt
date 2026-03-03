package com.Otter.app.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Otter.app.data.auth.CookieAuthStore
import com.Otter.app.data.auth.CookieTarget
import com.Otter.app.data.auth.CookieTargetCatalog
import com.Otter.app.data.auth.CustomCookieTargetStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CookieTargetsViewModel
    @Inject
    constructor(
        private val cookieAuthStore: CookieAuthStore,
        private val targetCatalog: CookieTargetCatalog,
        private val customCookieTargetStore: CustomCookieTargetStore,
    ) : ViewModel() {
        val predefinedTargets: List<CookieTarget> = targetCatalog.predefinedTargets

        private val predefinedIds: Set<String> = predefinedTargets.map { it.id }.toSet()

        val allTargets: StateFlow<List<CookieTarget>> =
            targetCatalog.allTargets
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), predefinedTargets)

        fun isCustomTarget(targetId: String): Boolean = !predefinedIds.contains(targetId)

        fun entriesForProfile(profileId: String): StateFlow<Map<String, com.Otter.app.data.auth.CookieAuthEntry>> {
            return cookieAuthStore.entries
                .map { list ->
                    list.filter { it.profileId == profileId }.associateBy { it.targetId }
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
        }

        fun setEnabledForYtDlp(
            profileId: String,
            targetId: String,
            enabled: Boolean,
        ) {
            viewModelScope.launch {
                cookieAuthStore.setEnabledForYtDlp(profileId, targetId, enabled)
            }
        }

        fun disconnect(
            profileId: String,
            targetId: String,
        ) {
            viewModelScope.launch {
                cookieAuthStore.clear(profileId, targetId)
            }
        }

        private fun normalizeDomains(domains: String): List<String> {
            return domains
                .split(',', '\n', ' ', '\t')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { it.removePrefix("https://").removePrefix("http://") }
                .map { it.substringBefore('/') }
                .map { it.removePrefix("www.") }
                .distinct()
        }

        fun addCustomTarget(
            title: String,
            loginUrl: String,
            domains: String,
        ) {
            viewModelScope.launch {
                val t = title.trim()
                val u = loginUrl.trim()
                val ds = normalizeDomains(domains)

                if (t.isBlank() || u.isBlank() || ds.isEmpty()) return@launch

                val idBase =
                    t.lowercase()
                        .replace(Regex("[^a-z0-9]+"), "_")
                        .trim('_')
                        .ifBlank { "site" }

                val id = "custom_${idBase}_${System.currentTimeMillis()}"
                customCookieTargetStore.upsert(
                    CookieTarget(
                        id = id,
                        title = t,
                        loginUrl = u,
                        domains = ds,
                    ),
                )
            }
        }

        fun deleteCustomTarget(targetId: String) {
            if (!isCustomTarget(targetId)) return
            viewModelScope.launch {
                customCookieTargetStore.delete(targetId)
            }
        }

        private fun validateNetscapeCookiesFile(file: File) {
            if (!file.exists() || file.length() < 50L) {
                throw IllegalStateException("Imported cookies file is empty/invalid")
            }

            val head =
                runCatching { file.bufferedReader().use { it.readLines().take(25).joinToString("\n") } }
                    .getOrDefault("")

            val hasHeader = head.contains("Netscape HTTP Cookie File", ignoreCase = true)
            val hasTabSeparatedLine =
                head.lines().any { line ->
                    val t = line.trim()
                    t.isNotBlank() && !t.startsWith('#') && t.count { ch -> ch == '\t' } >= 6
                }

            if (!hasHeader && !hasTabSeparatedLine) {
                throw IllegalStateException(
                    "Selected file is not a Netscape cookies.txt (tab-separated) file. Export cookies in Netscape format (cookies.txt) and try again.",
                )
            }
        }

        fun importCookiesFromUri(
            context: Context,
            profileId: String,
            targetId: String,
            sourceUri: Uri,
        ) {
            viewModelScope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        val cookiesDir = File(context.filesDir, "cookies").apply { mkdirs() }
                        val destFile = File(cookiesDir, "cookies_${profileId}_$targetId.txt")

                        context.contentResolver.openInputStream(sourceUri)?.use { input ->
                            destFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        } ?: throw IllegalStateException("Unable to read selected file")

                        validateNetscapeCookiesFile(destFile)

                        cookieAuthStore.upsertCookiesFilePath(profileId, targetId, destFile.absolutePath)
                    }
                }
            }
        }
    }
