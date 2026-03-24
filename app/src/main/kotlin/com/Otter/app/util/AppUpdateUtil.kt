package com.Otter.app.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object AppUpdateUtil {
    private const val OWNER = "777abhishek"
    private const val REPO = "Otter"

    private val client = OkHttpClient()

    data class Asset(
        val name: String,
        val downloadUrl: String,
        val sizeBytes: Long,
        val contentType: String,
    )

    data class Release(
        val tagName: String,
        val name: String,
        val body: String,
        val publishedAt: String,
        val assets: List<Asset>,
    )

    suspend fun fetchLatestRelease(): Release? =
        withContext(Dispatchers.IO) {
            try {
                val req =
                    Request.Builder()
                        .url("https://api.github.com/repos/$OWNER/$REPO/releases/latest")
                        .header("Accept", "application/vnd.github+json")
                        .header("X-GitHub-Api-Version", "2022-11-28")
                        .build()
                val resp = client.newCall(req).execute()
                if (!resp.isSuccessful) {
                    android.util.Log.e("AppUpdateUtil", "GitHub API error: ${resp.code} ${resp.message}")
                    return@withContext null
                }
                val json = resp.body?.string().orEmpty()
                if (json.isBlank()) {
                    android.util.Log.e("AppUpdateUtil", "GitHub API empty response")
                    return@withContext null
                }
                parseRelease(JSONObject(json))
            } catch (e: Exception) {
                android.util.Log.e("AppUpdateUtil", "Failed to fetch release: ${e.message}", e)
                null
            }
        }

    suspend fun fetchReleases(limit: Int = 10): List<Release> =
        withContext(Dispatchers.IO) {
            val req =
                Request.Builder()
                    .url("https://api.github.com/repos/$OWNER/$REPO/releases?per_page=$limit")
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext emptyList()
            val json = resp.body?.string().orEmpty()
            val arr = JSONArray(json)
            (0 until arr.length())
                .mapNotNull { idx ->
                    kotlin.runCatching { parseRelease(arr.getJSONObject(idx)) }.getOrNull()
                }
        }

    private fun parseRelease(obj: JSONObject): Release {
        val tag = obj.optString("tag_name")
        val name = obj.optString("name", tag)
        val body = obj.optString("body")
        val publishedAt = obj.optString("published_at")
        val assetsJson = obj.optJSONArray("assets") ?: JSONArray()
        val assets =
            (0 until assetsJson.length()).mapNotNull { idx ->
                val a = assetsJson.optJSONObject(idx) ?: return@mapNotNull null
                Asset(
                    name = a.optString("name"),
                    downloadUrl = a.optString("browser_download_url"),
                    sizeBytes = a.optLong("size"),
                    contentType = a.optString("content_type"),
                )
            }
        return Release(tagName = tag, name = name, body = body, publishedAt = publishedAt, assets = assets)
    }

    fun pickApkAsset(release: Release): Asset? {
        return release.assets
            .firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            ?: release.assets.firstOrNull { it.contentType == "application/vnd.android.package-archive" }
    }

    suspend fun downloadAssetToCache(
        context: Context,
        asset: Asset,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): File? =
        withContext(Dispatchers.IO) {
            val req = Request.Builder().url(asset.downloadUrl).build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null

            val body = resp.body ?: return@withContext null
            val total = body.contentLength().takeIf { it > 0 } ?: asset.sizeBytes

            val outFile = File(context.cacheDir, "update_${asset.name}")
            body.byteStream().use { input ->
                FileOutputStream(outFile).use { output ->
                    val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buf)
                        if (read <= 0) break
                        output.write(buf, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                    output.flush()
                }
            }

            outFile
        }

    fun buildInstallIntent(context: Context, apkFile: File): Intent {
        val uri: Uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile,
            )

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(context.contentResolver, "APK", uri)
        }
    }

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun tryStartInstall(context: Context, apkFile: File): Boolean {
        if (!canRequestPackageInstalls(context)) return false
        val intent = buildInstallIntent(context, apkFile)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    fun openUnknownSourcesSettings(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
