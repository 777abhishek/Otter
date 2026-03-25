package com.Otter.app.data.ytdlp

import com.Otter.app.data.models.Channel
import com.Otter.app.data.models.Playlist
import com.Otter.app.data.models.Video
import com.Otter.app.util.FileLogger
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpParsers
    @Inject
    constructor(
        private val fileLogger: FileLogger,
    ) {
        companion object {
            private const val TAG = "YtDlpParsers"
        }

        private val gson = Gson()

        private fun extractPlaylistIdFromUrl(url: String?): String? {
            if (url.isNullOrBlank()) return null
            val listIndex = url.indexOf("list=")
            if (listIndex == -1) return null
            val start = listIndex + 5
            val end = url.indexOf('&', start).let { if (it == -1) url.length else it }
            return url.substring(start, end).takeIf { it.isNotBlank() }
        }

        private fun extractVideoIdFromUrl(url: String?): String? {
            if (url.isNullOrBlank()) return null
            val vIndex = url.indexOf("v=")
            if (vIndex != -1) {
                val start = vIndex + 2
                val end = url.indexOf('&', start).let { if (it == -1) url.length else it }
                return url.substring(start, end).takeIf { it.isNotBlank() }
            }
            val lastSlash = url.lastIndexOf('/')
            if (lastSlash != -1 && lastSlash + 1 < url.length) {
                return url.substring(lastSlash + 1).takeIf { it.isNotBlank() }
            }
            return null
        }

        fun parsePlaylistEntriesFromSingleJson(output: String): List<Video> {
            val startTime = System.currentTimeMillis()
            fileLogger.log(TAG, "Parsing playlist entries from single JSON - Output length: ${output.length}")
            return runCatching {
                val root =
                    gson.fromJson(output.trim(), Map::class.java) as? Map<String, Any>
                        ?: return@runCatching emptyList()
                val entries = root["entries"] as? List<*> ?: return@runCatching emptyList()
                fileLogger.log(TAG, "Found ${entries.size} entries in playlist JSON")

                val videos =
                    entries.mapIndexedNotNull { index, entry ->
                        val json = entry as? Map<*, *> ?: return@mapIndexedNotNull null
                        val video =
                            Video(
                                id = json["id"] as? String ?: "",
                                title = json["title"] as? String ?: "Unknown Video",
                                thumbnail =
                                    json["thumbnail"] as? String
                                        ?: (json["thumbnails"] as? List<Map<String, Any>>)?.lastOrNull()?.get("url") as? String
                                        ?: "",
                                channelName = json["channel"] as? String ?: json["uploader"] as? String ?: "",
                                channelId = json["channel_id"] as? String ?: json["uploader_id"] as? String ?: "",
                                views = json["view_count"]?.toString()?.toLongOrNull() ?: 0,
                                duration = json["duration"]?.toString()?.toIntOrNull() ?: 0,
                                uploadDate = json["upload_date"] as? String ?: "",
                                description = json["description"] as? String ?: "",
                            ).takeIf { it.id.isNotBlank() }
                        if (index < 3 && video != null) {
                            fileLogger.log(TAG, "Sample video: id=${video.id} title=${video.title} channel=${video.channelName} views=${video.views} uploadDate=${video.uploadDate}")
                        }
                        video
                    }
                val duration = System.currentTimeMillis() - startTime
                fileLogger.log(TAG, "Parsed ${videos.size} valid videos from ${entries.size} entries in ${duration}ms")
                videos
            }.getOrElse { e ->
                val duration = System.currentTimeMillis() - startTime
                fileLogger.logError(TAG, "Failed to parse playlist entries after ${duration}ms", e)
                emptyList()
            }
        }

        fun parseSingleVideoJson(output: String): Video? {
            val startTime = System.currentTimeMillis()
            return runCatching {
                val trimmed = output.trim()
                if (trimmed.isBlank()) return@runCatching null

                val json = gson.fromJson(trimmed, Map::class.java) as? Map<String, Any>
                    ?: return@runCatching null

                val id =
                    (json["id"] as? String).orEmpty().ifBlank {
                        extractVideoIdFromUrl(json["url"] as? String)
                            ?: extractVideoIdFromUrl(json["webpage_url"] as? String)
                            ?: ""
                    }

                Video(
                    id = id,
                    title = json["title"] as? String ?: "Unknown Video",
                    thumbnail =
                        json["thumbnail"] as? String
                            ?: (json["thumbnails"] as? List<Map<String, Any>>)?.lastOrNull()?.get("url") as? String
                            ?: "",
                    channelName = json["channel"] as? String ?: json["uploader"] as? String ?: "",
                    channelId = json["channel_id"] as? String ?: json["uploader_id"] as? String ?: "",
                    views = json["view_count"]?.toString()?.toLongOrNull() ?: 0,
                    duration = json["duration"]?.toString()?.toIntOrNull() ?: 0,
                    uploadDate = json["upload_date"] as? String ?: "",
                    description = json["description"] as? String ?: "",
                ).takeIf { it.id.isNotBlank() }
            }.getOrElse { e ->
                val duration = System.currentTimeMillis() - startTime
                fileLogger.logError(TAG, "Failed to parse single video JSON after ${duration}ms", e)
                null
            }
        }

        fun parseChannelList(output: String): List<Channel> {
            val startTime = System.currentTimeMillis()
            fileLogger.log(TAG, "Parsing channel list - Output lines: ${output.lines().size}")
            val channels = mutableListOf<Channel>()
            val lines = output.lines()

            for (line in lines) {
                if (line.isBlank()) continue
                try {
                    val json = gson.fromJson(line, Map::class.java) as Map<String, Any>
                    val channel =
                        Channel(
                            id = json["channel_id"] as? String ?: json["uploader_id"] as? String ?: "",
                            name = json["channel"] as? String ?: json["uploader"] as? String ?: "Unknown",
                            thumbnail =
                                json["thumbnails"]?.let {
                                    (it as? List<Map<String, Any>>)?.lastOrNull()?.get("url") as? String
                                } ?: "",
                            subscriberCount = json["subscriber_count"]?.toString()?.toLongOrNull() ?: 0,
                            videoCount = json["video_count"]?.toString()?.toIntOrNull() ?: 0,
                            description = json["description"] as? String ?: "",
                        )
                    channels.add(channel)
                } catch (e: JsonSyntaxException) {
                    fileLogger.log(TAG, "Failed to parse channel JSON line: ${line.take(100)}")
                } catch (_: Exception) {
                    // Silently skip invalid lines
                }
            }
            val duration = System.currentTimeMillis() - startTime
            fileLogger.log(TAG, "Parsed ${channels.size} channels from ${lines.size} lines in ${duration}ms")
            return channels
        }

        fun parsePlaylistList(output: String): List<Playlist> {
            val startTime = System.currentTimeMillis()
            fileLogger.log(TAG, "Parsing playlist list - Output lines: ${output.lines().size}")
            val playlists = mutableListOf<Playlist>()
            val lines = output.lines()
            var skippedCount = 0

            for (line in lines) {
                if (line.isBlank()) continue
                try {
                    val json = gson.fromJson(line, Map::class.java) as Map<String, Any>

                    // Log all available keys for first few playlists
                    if (playlists.size < 3) {
                        fileLogger.log(TAG, "JSON keys: ${json.keys.joinToString(", ")}")
                        fileLogger.log(TAG, "Full JSON: $json")
                    }

                    val rawId =
                        json["playlist_id"] as? String
                            ?: json["id"] as? String

                    val urlCandidate =
                        json["url"]?.toString()
                            ?: json["webpage_url"]?.toString()
                            ?: json["original_url"]?.toString()

                    val urlPlaylistId = extractPlaylistIdFromUrl(urlCandidate)

                    val playlistId =
                        when {
                            !rawId.isNullOrBlank() && rawId != "playlists" -> rawId
                            !urlPlaylistId.isNullOrBlank() -> {
                                if (rawId == "playlists") {
                                    fileLogger.log(TAG, "Playlist id fallback: rawId='playlists' -> list='$urlPlaylistId'")
                                }
                                urlPlaylistId
                            }
                            else -> (rawId ?: "")
                        }

                    // Log first few playlist IDs for debugging
                    if (playlists.size < 3) {
                        fileLogger.log(TAG, "Parsed playlist: id='$playlistId' title='${json["title"] as? String}'")
                    }

                    if (playlistId.isBlank()) {
                        skippedCount++
                        continue
                    }
                    val playlist =
                        Playlist(
                            id = playlistId,
                            title = json["title"] as? String ?: "Unknown Playlist",
                            thumbnail =
                                json["thumbnails"]?.let {
                                    (it as? List<Map<String, Any>>)?.lastOrNull()?.get("url") as? String
                                } ?: "",
                            videoCount =
                                json["playlist_count"]?.toString()?.toIntOrNull()
                                    ?: json["count"]?.toString()?.toIntOrNull()
                                    ?: json["n_entries"]?.toString()?.toIntOrNull()
                                    ?: 0,
                        )
                    playlists.add(playlist)
                } catch (e: JsonSyntaxException) {
                    fileLogger.log(TAG, "Failed to parse playlist JSON line: ${line.take(100)}")
                    skippedCount++
                } catch (_: Exception) {
                    skippedCount++
                }
            }
            val duration = System.currentTimeMillis() - startTime
            fileLogger.log(
                TAG,
                "Parsed ${playlists.size} playlists from ${lines.size} lines (skipped $skippedCount invalid) in ${duration}ms",
            )
            return playlists
        }

        fun parseVideoList(output: String): List<Video> {
            val startTime = System.currentTimeMillis()
            fileLogger.log(TAG, "Parsing video list - Output lines: ${output.lines().size}")
            val videos = mutableListOf<Video>()
            val lines = output.lines()
            var skippedCount = 0

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isBlank()) continue
                if (!trimmed.startsWith("{")) {
                    skippedCount++
                    continue
                }
                try {
                    val json = gson.fromJson(trimmed, Map::class.java) as Map<String, Any>
                    val id =
                        (json["id"] as? String).orEmpty().ifBlank {
                            extractVideoIdFromUrl(json["url"] as? String)
                                ?: extractVideoIdFromUrl(json["webpage_url"] as? String)
                                ?: ""
                        }
                    val video =
                        Video(
                            id = id,
                            title = json["title"] as? String ?: "Unknown Video",
                            thumbnail =
                                json["thumbnail"] as? String
                                    ?: (json["thumbnails"] as? List<Map<String, Any>>)?.lastOrNull()?.get("url") as? String
                                    ?: "",
                            channelName = json["channel"] as? String ?: json["uploader"] as? String ?: "",
                            channelId = json["channel_id"] as? String ?: json["uploader_id"] as? String ?: "",
                            views = json["view_count"]?.toString()?.toLongOrNull() ?: 0,
                            duration = json["duration"]?.toString()?.toIntOrNull() ?: 0,
                            uploadDate = json["upload_date"] as? String ?: "",
                            description = json["description"] as? String ?: "",
                        )
                    if (video.id.isNotBlank()) {
                        videos.add(video)
                    } else {
                        skippedCount++
                    }
                } catch (e: JsonSyntaxException) {
                    fileLogger.log(TAG, "Failed to parse video JSON line: ${trimmed.take(100)}")
                    skippedCount++
                } catch (_: Exception) {
                    skippedCount++
                }
            }
            val duration = System.currentTimeMillis() - startTime
            fileLogger.log(
                TAG,
                "Parsed ${videos.size} valid videos from ${lines.size} lines (skipped $skippedCount invalid) in ${duration}ms",
            )
            return videos
        }

        fun parseFormats(output: String): List<Map<String, Any>> {
            val startTime = System.currentTimeMillis()
            fileLogger.log(TAG, "Parsing formats - Output lines: ${output.lines().size}")
            val formats = mutableListOf<Map<String, Any>>()
            val lines = output.lines()
            var skippedCount = 0

            for (line in lines) {
                if (line.isBlank()) continue
                if (line.startsWith("format")) continue
                if (line.startsWith("ID")) continue

                val parts = line.trim().split(Regex("\\s+"), limit = 3)
                if (parts.size >= 2) {
                    val id = parts[0]
                    val ext = parts[1]
                    val note = parts.getOrNull(2).orEmpty()
                    formats.add(
                        mapOf(
                            "id" to id,
                            "ext" to ext,
                            "note" to note,
                        ),
                    )
                } else {
                    skippedCount++
                }
            }
            val duration = System.currentTimeMillis() - startTime
            fileLogger.log(TAG, "Parsed ${formats.size} formats from ${lines.size} lines (skipped $skippedCount invalid) in ${duration}ms")
            return formats
        }
    }
