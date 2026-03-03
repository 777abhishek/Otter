package com.Otter.app.data.download

import android.content.Context
import android.os.Build
import com.Otter.app.data.auth.CookieAuthStore
import com.Otter.app.data.ytdlp.YtDlpCookieSupport
import com.Otter.app.util.FileLogger
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadEngine
    @Inject
    constructor(
        @ApplicationContext private val appContext: Context,
        private val fileLogger: FileLogger,
        private val cookieAuthStore: CookieAuthStore,
        private val cookieSupport: YtDlpCookieSupport,
    ) {
        companion object {
            private const val TAG = "DownloadEngine"

            const val BASENAME = "%(title).200B"
            const val EXTENSION = ".%(ext)s"
            private const val ID = "[%(id)s]"
            private const val CLIP_TIMESTAMP = "%(section_start)d-%(section_end)d"

            const val OUTPUT_TEMPLATE_DEFAULT = BASENAME + EXTENSION
            const val OUTPUT_TEMPLATE_ID = "$BASENAME $ID$EXTENSION"
            private const val OUTPUT_TEMPLATE_CLIPS = "$BASENAME [$CLIP_TIMESTAMP]$EXTENSION"
            const val OUTPUT_TEMPLATE_CHAPTERS =
                "chapter:$BASENAME/%(section_number)d - %(section_title).200B$EXTENSION"
            const val OUTPUT_TEMPLATE_SPLIT = "$BASENAME/$OUTPUT_TEMPLATE_DEFAULT"
            const val PLAYLIST_TITLE_SUBDIRECTORY_PREFIX = "%(playlist_id)s/"

            private const val CROP_ARTWORK_COMMAND =
                """--ppa \"ffmpeg: -c:v mjpeg -vf crop=\"'if(gt(ih,iw),iw,ih)':'if(gt(iw,ih),ih,iw)'\"\""""
        }

        // Mutex to ensure only one yt-dlp operation runs at a time
        private val ytDlpMutex = Mutex()

        private fun isYouTubeUrl(url: String): Boolean {
            val u = url.lowercase()
            return "youtube.com" in u || "youtu.be" in u || "music.youtube.com" in u
        }

        private fun YoutubeDLRequest.applyYouTubeClientWorkaroundIfNeeded(url: String) {
            if (isYouTubeUrl(url)) {
                return
            }
        }

        private fun YoutubeDLRequest.addOptionsForVideoDownloads(preferences: DownloadPreferences): YoutubeDLRequest {
            return this.apply {
                addOption("--add-metadata")
                addOption("--no-embed-info-json")
                if (preferences.videoClips.isEmpty()) addOption("--embed-chapters")

                // Format handling - Seal's approach
                if (preferences.formatIdString.isNotEmpty()) {
                    addOption("-f", preferences.formatIdString)
                    if (preferences.mergeAudioStream) {
                        addOption("--audio-multistreams")
                    }
                } else {
                    // Use format sorter
                    applyFormatSorter(preferences, preferences.toFormatSorter())
                }

                // Subtitle support
                if (preferences.downloadSubtitle) {
                    if (preferences.autoSubtitle) {
                        addOption("--write-auto-subs")
                        if (!preferences.autoTranslatedSubtitles) {
                            addOption("--extractor-args", "youtube:skip=translated_subs")
                        }
                    }
                    preferences.subtitleLanguage.takeIf { it.isNotEmpty() }?.let { addOption("--sub-langs", it) }
                    if (preferences.embedSubtitle) {
                        addOption("--embed-subs")
                        if (preferences.keepSubtitle) {
                            addOption("--write-subs")
                        }
                    } else {
                        addOption("--write-subs")
                    }
                    // Subtitle conversion
                    when (preferences.convertSubtitle) {
                        CONVERT_ASS -> addOption("--convert-subs", "ass")
                        CONVERT_SRT -> addOption("--convert-subs", "srt")
                        CONVERT_VTT -> addOption("--convert-subs", "vtt")
                        CONVERT_LRC -> addOption("--convert-subs", "lrc")
                        else -> {}
                    }
                }

                // MKV merging
                if (preferences.mergeToMkv) {
                    addOption("--remux-video", "mkv")
                    addOption("--merge-output-format", "mkv")
                }

                // Thumbnail embedding
                if (preferences.embedThumbnail) {
                    addOption("--embed-thumbnail")
                }
            }
        }

        private fun YoutubeDLRequest.addOptionsForAudioDownloads(
            id: String,
            preferences: DownloadPreferences,
            playlistUrl: String = "",
        ): YoutubeDLRequest {
            return this.apply {
                addOption("-x")

                if (preferences.downloadSubtitle) {
                    addOption("--write-subs")
                    if (preferences.autoSubtitle) {
                        addOption("--write-auto-subs")
                        if (!preferences.autoTranslatedSubtitles) {
                            addOption("--extractor-args", "youtube:skip=translated_subs")
                        }
                    }
                    preferences.subtitleLanguage.takeIf { it.isNotEmpty() }?.let { addOption("--sub-langs", it) }
                    when (preferences.convertSubtitle) {
                        CONVERT_ASS -> addOption("--convert-subs", "ass")
                        CONVERT_SRT -> addOption("--convert-subs", "srt")
                        CONVERT_VTT -> addOption("--convert-subs", "vtt")
                        CONVERT_LRC -> addOption("--convert-subs", "lrc")
                        else -> {}
                    }
                }

                // Format handling
                if (preferences.formatIdString.isNotEmpty()) {
                    addOption("-f", preferences.formatIdString)
                    if (preferences.mergeAudioStream) {
                        addOption("--audio-multistreams")
                    }
                } else if (preferences.convertAudio) {
                    when (preferences.audioConvertFormat) {
                        CONVERT_MP3 -> addOption("--audio-format", "mp3")
                        CONVERT_M4A -> addOption("--audio-format", "m4a")
                        else -> {}
                    }
                } else {
                    applyFormatSorter(preferences, preferences.toAudioFormatSorter())
                }

                if (preferences.embedMetadata) {
                    addOption("--embed-metadata")
                    addOption("--embed-thumbnail")
                    addOption("--convert-thumbnails", "jpg")

                    if (preferences.cropArtwork) {
                        val configFile = getConfigFile(id)
                        writeContentToFile(CROP_ARTWORK_COMMAND, configFile)
                        addOption("--config", configFile.absolutePath)
                    }
                }

                // Metadata parsing for album/track
                addOption("--parse-metadata", "%(release_year,upload_date)s:%(meta_date)s")

                if (playlistUrl.isNotEmpty()) {
                    addOption("--parse-metadata", "%(album,playlist,title)s:%(meta_album)s")
                    addOption("--parse-metadata", "%(track_number,playlist_index)d:%(meta_track)s")
                } else {
                    addOption("--parse-metadata", "%(album,title)s:%(meta_album)s")
                }
            }
        }

        private fun DownloadPreferences.toAudioFormatSorter(): String {
            if (!useCustomAudioPreset) return ""
            val format =
                when (audioFormat) {
                    M4A -> "acodec:aac"
                    OPUS -> "acodec:opus"
                    else -> ""
                }
            val quality =
                when (audioQuality) {
                    HIGH -> "abr~192"
                    MEDIUM -> "abr~128"
                    LOW -> "abr~64"
                    else -> ""
                }
            return connectWithDelimiter(format, quality, delimiter = ",")
        }

        private fun DownloadPreferences.toVideoFormatSorter(): String {
            val format =
                when (videoFormat) {
                    FORMAT_COMPATIBILITY -> "proto,vcodec:h264,ext"
                    FORMAT_QUALITY -> if (supportAv1HardwareDecoding) "vcodec:av01" else "vcodec:vp9.2"
                    else -> ""
                }
            val res =
                when (videoResolution) {
                    RES_2160P -> "res:2160"
                    RES_1440P -> "res:1440"
                    RES_1080P -> "res:1080"
                    RES_720P -> "res:720"
                    RES_480P -> "res:480"
                    RES_360P -> "res:360"
                    RES_LOWEST -> "+res"
                    else -> ""
                }
            return if (videoFormat == FORMAT_COMPATIBILITY) {
                connectWithDelimiter(format, res, delimiter = ",")
            } else {
                connectWithDelimiter(res, format, delimiter = ",")
            }
        }

        private fun YoutubeDLRequest.applyFormatSorter(
            preferences: DownloadPreferences,
            sorter: String,
        ) {
            if (preferences.formatSorting && preferences.sortingFields.isNotEmpty()) {
                addOption("-S", preferences.sortingFields)
            } else if (sorter.isNotEmpty()) {
                addOption("-S", sorter)
            }
        }

        private fun DownloadPreferences.toFormatSorter(): String {
            return connectWithDelimiter(
                this.toVideoFormatSorter(),
                this.toAudioFormatSorter(),
                delimiter = ",",
            )
        }

        private fun connectWithDelimiter(
            vararg tokens: String,
            delimiter: String,
        ): String {
            return tokens.filter { it.isNotBlank() }.joinToString(delimiter)
        }

        private fun getConfigFile(id: String): File {
            val configDir = File(appContext.cacheDir, "config")
            configDir.mkdirs()
            return File(configDir, "config_$id")
        }

        private fun writeContentToFile(
            content: String,
            file: File,
        ) {
            file.writeText(content)
        }

        private fun getExternalTempDir(): String {
            val tempDir = File(appContext.externalCacheDir, "temp")
            tempDir.mkdirs()
            return tempDir.absolutePath
        }

        private fun getSdcardTempDir(id: String): File {
            val sdcardTemp = File(appContext.cacheDir, "sdcard_temp/$id")
            sdcardTemp.mkdirs()
            return sdcardTemp
        }

        private fun getArchiveFile(): File {
            val archiveDir = File(appContext.getExternalFilesDir(null), "archive")
            archiveDir.mkdirs()
            return File(archiveDir, "download_archive.txt")
        }

        private fun mapToUserFriendlyError(
            e: Throwable,
            url: String,
        ): Exception? {
            val msg = (e.message ?: "").lowercase(Locale.US)
            return when {
                e is NoVideoFormatsException -> e
                e is NetworkException -> e
                "no video formats found" in msg ->
                    NoVideoFormatsException(
                        "No video formats found for this URL. The video may be restricted, require authentication, or the site may not be supported.",
                    )
                "connection reset by peer" in msg || "errno 104" in msg ->
                    NetworkException("Connection was reset. Please check your internet connection and try again.")
                "unable to download webpage" in msg ->
                    NetworkException("Unable to access the webpage. Please check your connection or try using a proxy/VPN.")
                "unsupported url" in msg || "no suitable extractor" in msg ->
                    UnsupportedUrlException("This URL is not supported. Please check the URL or try a different video.")
                "sign in" in msg || "login" in msg || "account" in msg || "private" in msg ->
                    AccessDeniedException("This video requires authentication or is private. Please check cookies/login settings.")
                "geo" in msg || "region" in msg || "country" in msg || "not available" in msg ->
                    GeoBlockedException("This video is not available in your region. Try using a proxy/VPN.")
                "age" in msg || "confirm" in msg || "adult" in msg ->
                    AgeRestrictedException("This video is age-restricted. Cookies/login may be required.")
                else -> null
            }
        }

        // Custom exception classes for better error handling
        class NoVideoFormatsException(message: String) : Exception(message)

        class NetworkException(message: String) : Exception(message)

        class UnsupportedUrlException(message: String) : Exception(message)

        class AccessDeniedException(message: String) : Exception(message)

        class GeoBlockedException(message: String) : Exception(message)

        class AgeRestrictedException(message: String) : Exception(message)

        private fun String.isNumberInRange(
            min: Int,
            max: Int,
        ): Boolean {
            return this.toIntOrNull()?.let { it in min..max } ?: false
        }

        private fun YoutubeDLRequest.enableAria2c(): YoutubeDLRequest {
            return this.addOption("--downloader", "libaria2c.so")
        }

        private fun YoutubeDLRequest.enableCookies(
            cookiesFilePath: String?,
            userAgentString: String,
        ): YoutubeDLRequest {
            cookieSupport.addCookiesToRequest(this, cookiesFilePath)
            if (userAgentString.isNotEmpty()) {
                addOption("--add-header", "User-Agent:$userAgentString")
            }
            return this
        }

        private fun YoutubeDLRequest.enableProxy(proxyUrl: String): YoutubeDLRequest {
            return this.addOption("--proxy", proxyUrl)
        }

        private fun YoutubeDLRequest.useDownloadArchive(): YoutubeDLRequest {
            return this.addOption("--download-archive", getArchiveFile().absolutePath)
        }

        private fun buildFormatSelector(preferences: DownloadPreferences): String {
            val selected = preferences.formatIdString.trim()
            return when {
                selected.isBlank() -> "bestvideo*+bestaudio/best"
                // If the format string already contains '+' or 'best', assume audio is handled
                selected.contains("+") || selected.contains("best") -> selected
                // Audio extraction mode doesn't need video+audio merging
                preferences.extractAudio -> selected
                // Single format ID without audio — append +bestaudio for safety
                else -> "$selected+bestaudio/best"
            }
        }

        private val json =
            Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

        suspend fun fetchVideoInfo(
            url: String,
            taskKey: String?,
            preferences: DownloadPreferences,
            playlistIndex: Int? = null,
        ): Result<VideoInfo> =
            withContext(Dispatchers.IO) {
                runCatching {
                    fileLogger.log(TAG, "Fetching video info for: $url")

                    val cookiesFilePath =
                        if (preferences.cookies) {
                            cookieAuthStore.getCookiesFilePathOnceForUrl(url, requireEnabled = true)
                        } else {
                            null
                        }

                    val request =
                        YoutubeDLRequest(url).apply {
                            addOption("-o", BASENAME)
                            addOption("-R", "1")
                            addOption("--socket-timeout", "5")
                            if (playlistIndex == null) {
                                addOption("--no-playlist")
                                addOption("--dump-single-json")
                            } else {
                                addOption("--yes-playlist")
                                addOption("--playlist-items", playlistIndex)
                                addOption("--dump-single-json")
                            }

                            // Format sorting - prefer high quality formats
                            applyFormatSorter(preferences, preferences.toFormatSorter())

                            // Ignore unavailable formats warnings to prevent errors on some sites
                            addOption("--ignore-no-formats-error")
                            addOption("--no-warnings")

                            if (preferences.extractAudio) addOption("-x")
                            if (preferences.forceIpv4) addOption("-4")
                            if (preferences.restrictFilenames) addOption("--restrict-filenames")
                            if (preferences.cookies) enableCookies(cookiesFilePath, preferences.userAgentString)
                            if (preferences.proxy) enableProxy(preferences.proxyUrl)
                            if (preferences.autoSubtitle) {
                                addOption("--write-auto-subs")
                                if (!preferences.autoTranslatedSubtitles) {
                                    addOption("--extractor-args", "youtube:skip=translated_subs")
                                }
                            }

                            applyYouTubeClientWorkaroundIfNeeded(url)
                        }

                    val response =
                        ytDlpMutex.withLock {
                            YoutubeDL.getInstance().execute(request, taskKey, null)
                        }

                    // Check for errors in stderr even if execution succeeded
                    val stderr = response.err
                    if (stderr.contains("No video formats found", ignoreCase = true)) {
                        throw NoVideoFormatsException(
                            "No video formats found for this URL. The site may require authentication or the video may be restricted.",
                        )
                    }
                    if (stderr.contains("Connection reset by peer", ignoreCase = true) ||
                        stderr.contains("Errno 104", ignoreCase = true)
                    ) {
                        throw NetworkException("Connection reset by peer. Please check your internet connection and try again.")
                    }
                    if (stderr.contains("Unable to download webpage", ignoreCase = true)) {
                        throw NetworkException("Unable to access the webpage. Please check your connection or try using a proxy.")
                    }

                    val videoInfo = json.decodeFromString(VideoInfo.serializer(), response.out)

                    fileLogger.log(TAG, "Successfully fetched video info: ${videoInfo.title}")
                    videoInfo
                }.recoverCatching { e ->
                    // Log the original error and map to user-friendly exception
                    fileLogger.logError(TAG, "fetchVideoInfo failed for $url", e as? Exception ?: Exception(e))
                    throw mapToUserFriendlyError(e, url) ?: e
                }
            }

        suspend fun fetchPlaylistMetadata(
            url: String,
            taskKey: String?,
        ): Result<PlaylistResult> =
            withContext(Dispatchers.IO) {
                try {
                    fileLogger.log(TAG, "Fetching playlist metadata for: $url")

                    val request =
                        YoutubeDLRequest(url).apply {
                            addOption("-j")
                            addOption("--flat-playlist")
                            addOption("--yes-playlist")
                            addOption("--socket-timeout", "30")
                            addOption("--retries", "3")
                        }

                    val response =
                        ytDlpMutex.withLock {
                            YoutubeDL.getInstance().execute(request, taskKey, null)
                        }

                    val lines = response.out.lines().filter { it.isNotBlank() }

                    val entries =
                        lines.mapIndexed { index, line ->
                            try {
                                val entryJson = json.parseToJsonElement(line)
                                val jsonObject = entryJson.jsonObject
                                PlaylistEntry(
                                    id = jsonObject["id"]?.jsonPrimitive?.content ?: "",
                                    title = jsonObject["title"]?.jsonPrimitive?.content ?: "",
                                    duration = jsonObject["duration"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                                    uploader = jsonObject["uploader"]?.jsonPrimitive?.content ?: "",
                                    channel = jsonObject["channel"]?.jsonPrimitive?.content ?: "",
                                    url = jsonObject["url"]?.jsonPrimitive?.content,
                                    thumbnails =
                                        jsonObject["thumbnails"]?.jsonArray?.map { thumb ->
                                            val thumbObj = thumb.jsonObject
                                            Thumbnail(
                                                url = thumbObj["url"]?.jsonPrimitive?.content ?: "",
                                                width = thumbObj["width"]?.jsonPrimitive?.content?.toIntOrNull(),
                                                height = thumbObj["height"]?.jsonPrimitive?.content?.toIntOrNull(),
                                                resolution = thumbObj["resolution"]?.jsonPrimitive?.content,
                                            )
                                        },
                                    index = index + 1,
                                )
                            } catch (e: Exception) {
                                fileLogger.logError(TAG, "Failed to parse playlist entry at line $index", e)
                                null
                            }
                        }.filterNotNull()

                    val playlistResult =
                        PlaylistResult(
                            id = "",
                            title = "Playlist",
                            uploader = "",
                            channel = "",
                            webpageUrl = url,
                            thumbnail = "",
                            entryCount = entries.size,
                            entries = entries,
                        )

                    fileLogger.log(TAG, "Successfully fetched playlist metadata: ${entries.size} entries")
                    Result.success(playlistResult)
                } catch (e: Exception) {
                    val errorMessage = e.message ?: ""
                    if (errorMessage.contains("The playlist does not exist", ignoreCase = true)) {
                        fileLogger.logError(TAG, "Playlist does not exist (WL/LL likely): $url", e)
                        return@withContext Result.failure(
                            Exception(
                                "The playlist does not exist. If it's your Liked Videos or Watch Later, make sure you are logged in and the playlist is accessible.",
                            ),
                        )
                    }

                    fileLogger.logError(TAG, "fetchPlaylistMetadata failed for $url", e)
                    Result.failure(e)
                }
            }

        suspend fun download(
            task: Task,
            info: VideoInfo,
            outputDir: String,
            progressCallback: (progress: Float, text: String) -> Unit,
        ): String? =
            withContext(Dispatchers.IO) {
                val url = info.originalUrl.ifBlank { task.url }
                val selectedFormatSelector = buildFormatSelector(task.preferences)

                val cookiesFilePath =
                    if (task.preferences.cookies) {
                        cookieAuthStore.getCookiesFilePathOnceForUrl(url, requireEnabled = true)
                    } else {
                        null
                    }

                fun buildRequest(
                    formatSelector: String,
                    applyYoutubeWorkaround: Boolean,
                ): YoutubeDLRequest {
                    return YoutubeDLRequest(url).apply {
                        when (task.type) {
                            is Task.TypeInfo.Playlist -> {
                                addOption("--yes-playlist")
                                addOption("--playlist-items", (task.type as Task.TypeInfo.Playlist).index)
                            }
                            else -> {
                                if (!task.preferences.downloadPlaylist) {
                                    addOption("--no-playlist")
                                }
                            }
                        }

                        if (task.preferences.formatIdString.isBlank() && formatSelector.isNotBlank()) {
                            addOption("-f", formatSelector)
                        }
                        addOption("--newline")
                        addOption("--progress")
                        addOption("--no-mtime")
                        addOption("-P", outputDir)

                        val outputTemplate =
                            when {
                                task.preferences.splitByChapter -> OUTPUT_TEMPLATE_CHAPTERS
                                task.preferences.videoClips.isNotEmpty() -> OUTPUT_TEMPLATE_CLIPS
                                task.preferences.outputTemplate.isNotBlank() -> task.preferences.outputTemplate
                                else -> OUTPUT_TEMPLATE_DEFAULT
                            }

                        val finalTemplate =
                            if (task.preferences.downloadPlaylist && task.preferences.subdirectoryPlaylistTitle) {
                                PLAYLIST_TITLE_SUBDIRECTORY_PREFIX + outputTemplate
                            } else {
                                outputTemplate
                            }
                        addOption("-o", finalTemplate)

                        addOption("--no-update")
                        addOption("--print", "after_move:filepath")

                        // Debug mode
                        if (task.preferences.debug) {
                            addOption("-v")
                        }

                        // Download archive support
                        if (task.preferences.useDownloadArchive) {
                            val archiveFile = getArchiveFile()
                            if (archiveFile.exists() &&
                                archiveFile.readText()
                                    .contains("${info.extractorKey} ${info.id}")
                            ) {
                                throw Exception("Video already downloaded (exists in archive)")
                            }
                            useDownloadArchive()
                        }

                        // Rate limiting
                        if (task.preferences.rateLimit && task.preferences.maxDownloadRate.isNumberInRange(1, 1000000)) {
                            addOption("-r", "${task.preferences.maxDownloadRate}K")
                        }

                        // Aria2c or concurrent fragments
                        if (task.preferences.aria2c) {
                            enableAria2c()
                        } else if (task.preferences.concurrentFragments > 1) {
                            addOption("--concurrent-fragments", task.preferences.concurrentFragments)
                        }

                        // SponsorBlock support
                        if (task.preferences.noSponsorBlock) {
                            addOption("--no-sponsorblock")
                        } else if (task.preferences.sponsorBlock) {
                            val markCats = task.preferences.sponsorBlockMarkCategories.trim()
                            val removeCats = task.preferences.sponsorBlockRemoveCategories.trim()

                            if (markCats.isNotBlank()) {
                                addOption("--sponsorblock-mark", markCats)
                            }

                            val effectiveRemoveCats =
                                when {
                                    removeCats.isNotBlank() -> removeCats
                                    task.preferences.sponsorBlockCategory.isNotBlank() -> task.preferences.sponsorBlockCategory.trim()
                                    else -> "default"
                                }

                            if (effectiveRemoveCats.isNotBlank()) {
                                addOption("--sponsorblock-remove", effectiveRemoveCats)
                            }

                            val chapterTitle = task.preferences.sponsorBlockChapterTitle.trim()
                            if (chapterTitle.isNotBlank()) {
                                addOption("--sponsorblock-chapter-title", chapterTitle)
                            }

                            val apiUrl = task.preferences.sponsorBlockApiUrl.trim()
                            if (apiUrl.isNotBlank()) {
                                addOption("--sponsorblock-api", apiUrl)
                            }
                        }

                        // Video clips support
                        task.preferences.videoClips.forEach {
                            addOption("--download-sections", "*${it.start}-${it.end}")
                        }

                        // Split by chapter support
                        if (task.preferences.splitByChapter) {
                            addOption("--split-chapters")
                        }

                        // Thumbnail creation
                        if (task.preferences.createThumbnail) {
                            addOption("--write-thumbnail")
                            addOption("--convert-thumbnails", "png")
                        }

                        // Apply video or audio specific options
                        if (task.preferences.extractAudio || info.vcodec == "none") {
                            addOptionsForAudioDownloads(info.id, task.preferences)
                        } else {
                            addOptionsForVideoDownloads(task.preferences)
                        }

                        // Common options
                        if (task.preferences.cookies) enableCookies(cookiesFilePath, task.preferences.userAgentString)
                        if (task.preferences.proxy) enableProxy(task.preferences.proxyUrl)
                        if (task.preferences.forceIpv4) addOption("-4")
                        addOption("--restrict-filenames")
                        if (Build.VERSION.SDK_INT > 23) {
                            addOption("-P", "temp:${getExternalTempDir()}")
                        }

                        if (applyYoutubeWorkaround) {
                            applyYouTubeClientWorkaroundIfNeeded(url)
                        }
                    }
                }

                fun extractDownloadedPath(out: String): String? {
                    return out
                        .lineSequence()
                        .map { it.trim() }
                        .firstOrNull { it.isNotBlank() && (it.startsWith("/") || it.startsWith("content://") || it.contains(":\\")) }
                }

                suspend fun executeRequest(
                    request: YoutubeDLRequest,
                    formatSelector: String,
                ): String? {
                    fileLogger.log(TAG, "Starting download: ${task.url} with format: $formatSelector")
                    fileLogger.log(TAG, "Request options: ${request.buildCommand().take(10)}")

                    var callbackInvoked = false
                    var callbackCount = 0
                    var lastProgress = -1f

                    val response =
                        ytDlpMutex.withLock {
                            YoutubeDL.getInstance().execute(
                                request = request,
                                processId = task.id,
                                callback = { progress, _, line ->
                                    try {
                                        callbackInvoked = true
                                        callbackCount++

                                        // Try to parse progress from the line manually since library might not parse it correctly
                                        val percentMatch = Regex("""(\d+\.?\d*)%""").find(line)
                                        val parsedProgress = percentMatch?.groupValues?.get(1)?.toFloatOrNull()?.div(100f) ?: -1f

                                        val logMessage =
                                            try {
                                                buildString {
                                                    append(
                                                        "[ProgressHook] #$callbackCount - library progress: $progress, parsed progress: $parsedProgress, line: ${line.take(
                                                            100,
                                                        )}\n",
                                                    )

                                                    val percentMatch = Regex("""(\d+\.?\d*)%""").find(line)
                                                    val ofMatch = Regex("""of\s+([\d.]+\s*[KMGT]?iB)""").find(line)
                                                    val atMatch = Regex("""at\s+([\d.]+\s*[KMGT]?iB/s)""").find(line)
                                                    val etaMatch = Regex("""ETA\s+([\d:]+)""").find(line)

                                                    val downloadedStr = ofMatch?.groupValues?.get(1) ?: "?"
                                                    val totalStr = ofMatch?.groupValues?.get(1) ?: "?"
                                                    val speedStr = atMatch?.groupValues?.get(1) ?: "?"
                                                    val etaStr = etaMatch?.groupValues?.get(1) ?: "?"

                                                    append("[ProgressHook] Downloading $downloadedStr / $totalStr ")
                                                    if (percentMatch != null) {
                                                        append("(${percentMatch.value}) ")
                                                    }
                                                    append("ETA: $etaStr Speed: $speedStr")
                                                }
                                            } catch (e: Exception) {
                                                "[ProgressHook] #$callbackCount - Error parsing progress: ${e.message}, raw line: ${line.take(
                                                    100,
                                                )}"
                                            }

                                        fileLogger.log(TAG, logMessage)

                                        // Use parsed progress if available, otherwise use library progress
                                        val finalProgress = if (parsedProgress >= 0) parsedProgress else progress / 100f

                                        // Only update if progress changed significantly
                                        if (kotlin.math.abs(finalProgress - lastProgress) > 0.01f || finalProgress == 1f) {
                                            lastProgress = finalProgress
                                            progressCallback(finalProgress, line)
                                        }
                                    } catch (e: Exception) {
                                        fileLogger.logError(TAG, "Error in progress callback", e)
                                        throw e
                                    }
                                },
                            )
                        }

                    fileLogger.log(TAG, "Download finished - callback invoked: $callbackInvoked, callback count: $callbackCount")

                    if (response.err.isNotBlank()) {
                        fileLogger.log(TAG, "yt-dlp stderr (first 500 chars): ${response.err.take(500)}")
                    }
                    val downloadedPath = extractDownloadedPath(response.out)
                    fileLogger.log(TAG, "Download completed: ${task.url} -> ${downloadedPath ?: "(unknown path)"}")
                    return downloadedPath
                }

                fun parseProgressFromLine(line: String): Float {
                    val percentMatch = Regex("""(\d+\.?\d*)%""").find(line)
                    return percentMatch?.groupValues?.get(1)?.toFloatOrNull()?.div(100f) ?: -1f
                }

                fun mapToActionableMessage(t: Throwable): String? {
                    val msg = (t.message ?: "").lowercase(Locale.US)
                    return when {
                        "no address associated with hostname" in msg || "temporary failure in name resolution" in msg ->
                            "DNS resolution failed. Check internet/DNS/VPN, or try forcing IPv4 / setting a proxy."
                        "unable to resolve host" in msg ->
                            "Cannot resolve host. Check internet connection or DNS settings."
                        "network is unreachable" in msg ->
                            "Network unreachable. Check internet connection."
                        else -> null
                    }
                }

                fun shouldRetryWithYoutubeWorkaround(e: Throwable): Boolean {
                    val msg = (e.message ?: "")
                    return msg.contains("forcing SABR", ignoreCase = true) ||
                        msg.contains("missing a url", ignoreCase = true) ||
                        msg.contains("HTTP Error 403", ignoreCase = true) ||
                        msg.contains("403: Forbidden", ignoreCase = true)
                }

                try {
                    return@withContext executeRequest(
                        request = buildRequest(selectedFormatSelector, applyYoutubeWorkaround = false),
                        formatSelector = selectedFormatSelector,
                    )
                } catch (e: Throwable) {
                    val isRetryable = shouldRetryWithYoutubeWorkaround(e)
                    mapToActionableMessage(e)?.let { fileLogger.log(TAG, "Actionable error: $it") }
                    fileLogger.logError(TAG, "download failed for ${task.url}", e as? Exception ?: Exception(e))

                    if (isRetryable && isYouTubeUrl(url)) {
                        fileLogger.log(TAG, "Retrying download with YouTube client workaround")
                        try {
                            return@withContext executeRequest(
                                request = buildRequest(selectedFormatSelector, applyYoutubeWorkaround = true),
                                formatSelector = selectedFormatSelector,
                            )
                        } catch (e2: Throwable) {
                            mapToActionableMessage(e2)?.let { fileLogger.log(TAG, "Actionable error (retry): $it") }
                            if (selectedFormatSelector != "bestvideo*+bestaudio/best") {
                                val fallbackSelector = "bestvideo*+bestaudio/best"
                                fileLogger.log(TAG, "Retrying download with fallback format selector: $fallbackSelector")
                                return@withContext executeRequest(
                                    request = buildRequest(fallbackSelector, applyYoutubeWorkaround = true),
                                    formatSelector = fallbackSelector,
                                )
                            }
                            fileLogger.logError(
                                TAG,
                                "Retry with YouTube client workaround failed for ${task.url}",
                                e2 as? Exception ?: Exception(e2),
                            )
                            throw e2
                        }
                    }

                    throw e
                }
            }
    }
