package com.Otter.app.data.auth

import android.content.Context
import android.net.Uri
import com.Otter.app.data.ytdlp.YtDlpManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Native yt-dlp cookie extraction using yt-dlp's built-in browser cookie support.
 * This uses yt-dlp's --cookies-from-browser option to extract cookies from installed browsers.
 */
@Singleton
class YtDlpCookieExtractor
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val ytDlpManager: YtDlpManager,
        private val cookieStore: YouTubeProfileStore,
        private val cookieAuthStore: CookieAuthStore,
    ) {
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

        private fun hasUsableYouTubeLoginCookies(file: File): Boolean {
            if (!file.exists() || file.length() < 50L) return false

            val nowEpochSeconds = System.currentTimeMillis() / 1000L

            fun looksLikeYouTubeDomain(domain: String): Boolean {
                val d = domain.trim().lowercase()
                return d == "youtube.com" || d.endsWith(".youtube.com") ||
                    d == "google.com" || d.endsWith(".google.com")
            }

            val requiredCookieNames =
                setOf(
                    "SID",
                    "HSID",
                    "SSID",
                    "APISID",
                    "SAPISID",
                    "__Secure-1PSID",
                    "__Secure-3PSID",
                    "LOGIN_INFO",
                )

            var foundAny = false
            file.forEachLine { rawLine ->
                val line = rawLine.trim()
                if (line.isBlank() || line.startsWith('#')) return@forEachLine
                val parts = line.split('\t')
                if (parts.size < 7) return@forEachLine

                val domain = parts[0]
                if (!looksLikeYouTubeDomain(domain)) return@forEachLine

                val expires = parts[4].toLongOrNull() ?: 0L
                if (expires in 1..nowEpochSeconds) return@forEachLine

                val name = parts[5]
                if (name in requiredCookieNames) {
                    foundAny = true
                }
            }
            return foundAny
        }

        private suspend fun persistForYtDlpIfPossible(cookiesFile: File) {
            val profileId = cookieAuthStore.getActiveProfileIdOrNull() ?: return
            val targetId = CookieTargetCatalog.TARGET_YOUTUBE
            cookieAuthStore.upsertCookiesFilePath(profileId, targetId, cookiesFile.absolutePath)
            cookieAuthStore.setEnabledForYtDlp(profileId, targetId, true)
        }

        /**
         * Extract cookies from specified browser using yt-dlp native method.
         * Supported browsers: chrome, firefox, safari, edge, opera, brave, vivaldi, etc.
         */
        suspend fun extractCookiesFromBrowser(browser: SupportedBrowser): Result<File> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val cookiesDir = File(context.filesDir, "cookies").apply { mkdirs() }
                    val cookiesFile = File(cookiesDir, "${browser.name.lowercase()}_cookies.txt")

                    // Use yt-dlp to extract cookies from browser
                    val result =
                        ytDlpManager.extractCookiesFromBrowser(
                            browser = browser.name.lowercase(),
                            outputFile = cookiesFile.absolutePath,
                        )

                    result.getOrThrow()

                    validateNetscapeCookiesFile(cookiesFile)
                    val loggedIn = hasUsableYouTubeLoginCookies(cookiesFile)

                    cookieStore.setCookiesFilePath(cookiesFile.absolutePath)
                    cookieStore.setLoggedIn(loggedIn)
                    persistForYtDlpIfPossible(cookiesFile)

                    if (!loggedIn) {
                        throw IllegalStateException(
                            "Cookies extracted, but no valid YouTube login session cookies were found. Make sure you're logged into YouTube in that browser profile.",
                        )
                    }

                    cookiesFile
                }
            }

        /**
         * Import cookies from a user-provided cookies.txt file.
         */
        suspend fun importCookiesFile(sourceFile: File): Result<File> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val cookiesDir = File(context.filesDir, "cookies").apply { mkdirs() }
                    val destFile = File(cookiesDir, "imported_cookies.txt")

                    sourceFile.copyTo(destFile, overwrite = true)

                    validateNetscapeCookiesFile(destFile)
                    val loggedIn = hasUsableYouTubeLoginCookies(destFile)

                    cookieStore.setCookiesFilePath(destFile.absolutePath)
                    cookieStore.setLoggedIn(loggedIn)
                    persistForYtDlpIfPossible(destFile)

                    if (!loggedIn) {
                        throw IllegalStateException(
                            "Cookies imported, but no valid YouTube login session cookies were found. Export cookies while logged into YouTube, then try again.",
                        )
                    }

                    destFile
                }
            }

        /**
         * Import cookies from a Storage Access Framework Uri (e.g. content://...).
         */
        suspend fun importCookiesUri(sourceUri: Uri): Result<File> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val cookiesDir = File(context.filesDir, "cookies").apply { mkdirs() }
                    val destFile = File(cookiesDir, "imported_cookies.txt")

                    context.contentResolver.openInputStream(sourceUri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw IllegalStateException("Unable to read selected file")

                    validateNetscapeCookiesFile(destFile)
                    val loggedIn = hasUsableYouTubeLoginCookies(destFile)

                    cookieStore.setCookiesFilePath(destFile.absolutePath)
                    cookieStore.setLoggedIn(loggedIn)
                    persistForYtDlpIfPossible(destFile)

                    if (!loggedIn) {
                        throw IllegalStateException(
                            "Cookies imported, but no valid YouTube login session cookies were found. Export cookies while logged into YouTube, then try again.",
                        )
                    }

                    destFile
                }
            }

        suspend fun importCookiesText(netscapeCookiesText: String): Result<File> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val cookiesDir = File(context.filesDir, "cookies").apply { mkdirs() }
                    val destFile = File(cookiesDir, "pasted_cookies.txt")

                    val normalized = netscapeCookiesText.trimStart()
                    destFile.writeText(
                        if (normalized.startsWith(
                                "# Netscape HTTP Cookie File",
                            )
                        ) {
                            normalized
                        } else {
                            "# Netscape HTTP Cookie File\n$normalized"
                        },
                    )

                    validateNetscapeCookiesFile(destFile)
                    val loggedIn = hasUsableYouTubeLoginCookies(destFile)

                    cookieStore.setCookiesFilePath(destFile.absolutePath)
                    cookieStore.setLoggedIn(loggedIn)
                    persistForYtDlpIfPossible(destFile)

                    if (!loggedIn) {
                        throw IllegalStateException(
                            "Cookies pasted, but no valid YouTube login session cookies were found. Paste/export cookies while logged into YouTube, then try again.",
                        )
                    }

                    destFile
                }
            }

        /**
         * Clear all cookies and reset login state.
         */
        suspend fun clearAllCookies() {
            cookieStore.clear()

            val profileId = cookieAuthStore.getActiveProfileIdOrNull() ?: return
            cookieAuthStore.clear(profileId, CookieTargetCatalog.TARGET_YOUTUBE)

            // Delete cookie files
            withContext(Dispatchers.IO) {
                val cookiesDir = File(context.filesDir, "cookies")
                cookiesDir.listFiles()?.forEach { it.delete() }
            }
        }

        enum class SupportedBrowser {
            CHROME,
            FIREFOX,
            EDGE,
            BRAVE,
            OPERA,
            VIVALDI,
            SAFARI,
        }
    }
