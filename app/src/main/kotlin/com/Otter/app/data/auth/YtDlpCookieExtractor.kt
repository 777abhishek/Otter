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

                    // Save the cookies file path
                    cookieStore.setCookiesFilePath(cookiesFile.absolutePath)
                    cookieStore.setLoggedIn(true)

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

                    cookieStore.setCookiesFilePath(destFile.absolutePath)
                    cookieStore.setLoggedIn(true)

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

                    cookieStore.setCookiesFilePath(destFile.absolutePath)
                    cookieStore.setLoggedIn(true)

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

                    cookieStore.setCookiesFilePath(destFile.absolutePath)
                    cookieStore.setLoggedIn(true)

                    destFile
                }
            }

        /**
         * Clear all cookies and reset login state.
         */
        suspend fun clearAllCookies() {
            cookieStore.clear()

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
