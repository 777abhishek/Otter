package com.Otter.app.data.ytdlp

import com.Otter.app.data.models.Channel
import com.Otter.app.data.models.Playlist
import com.Otter.app.data.models.Video
import com.Otter.app.util.FileLogger
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpSyncClient
    @Inject
    constructor(
        private val core: YtDlpCore,
        private val cookieSupport: YtDlpCookieSupport,
        private val parsers: YtDlpParsers,
        private val fileLogger: FileLogger,
    ) {
        companion object {
            private const val TAG = "YtDlpSyncClient"
        }

        suspend fun extractSubscriptions(cookiesFilePath: String?): Result<List<Channel>> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                fileLogger.log(TAG, "Extracting subscriptions with cookies: $cookiesFilePath")
                runCatching {
                    val request =
                        YoutubeDLRequest(":ytsubs").apply {
                            addOption("--flat-playlist")
                            addOption("--dump-json")
                            addOption("--playlist-items", "1:50")
                            cookieSupport.addCookiesToRequest(this, cookiesFilePath)
                        }

                    val response = core.execute(request)
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(
                        TAG,
                        "Subscriptions response received in ${duration}ms - Output length: ${response.out.length}, Error: ${response.err.take(
                            120,
                        )}",
                    )
                    val channels = parsers.parseChannelList(response.out)
                    if (channels.isEmpty() && response.err.isNotBlank()) {
                        throw IllegalStateException(response.err.trim())
                    }
                    fileLogger.log(TAG, "Successfully extracted ${channels.size} subscriptions in ${duration}ms")
                    channels
                }
            }

        suspend fun extractPlaylists(cookiesFilePath: String?): Result<List<Playlist>> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                fileLogger.log(TAG, "Extracting playlists with cookies: $cookiesFilePath")
                runCatching {
                    val request =
                        YoutubeDLRequest("https://www.youtube.com/feed/playlists").apply {
                            addOption("--flat-playlist")
                            addOption("--dump-json")
                            cookieSupport.addCookiesToRequest(this, cookiesFilePath)
                        }

                    val response = core.execute(request)
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(
                        TAG,
                        "Playlists response received in ${duration}ms - Output length: ${response.out.length}, Error: ${response.err.take(
                            120,
                        )}",
                    )
                    val playlists = parsers.parsePlaylistList(response.out)
                    if (playlists.isEmpty() && response.err.isNotBlank()) {
                        throw IllegalStateException(response.err.trim())
                    }
                    fileLogger.log(TAG, "Successfully extracted ${playlists.size} playlists in ${duration}ms")
                    playlists
                }
            }

        suspend fun extractWatchLater(cookiesFilePath: String?): Result<List<Video>> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                fileLogger.log(TAG, "Extracting watch later with cookies: $cookiesFilePath")
                runCatching {
                    val request =
                        YoutubeDLRequest(":ytwatchlater").apply {
                            addOption("--flat-playlist")
                            addOption("--get-duration")
                            addOption("--get-id")
                            addOption("--get-title")
                            addOption("--get-thumbnail")
                            addOption("--dump-json")
                            cookieSupport.addCookiesToRequest(this, cookiesFilePath)
                        }

                    val response = core.execute(request)
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(
                        TAG,
                        "Watch later response received in ${duration}ms - Output length: ${response.out.length}, Error: ${response.err.take(
                            120,
                        )}",
                    )
                    val videos = parsers.parseVideoList(response.out)
                    if (videos.isEmpty() && response.err.isNotBlank()) {
                        throw IllegalStateException(response.err.trim())
                    }
                    fileLogger.log(TAG, "Successfully extracted ${videos.size} watch later videos in ${duration}ms")
                    videos
                }
            }

        suspend fun extractLikedVideos(cookiesFilePath: String?): Result<List<Video>> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                fileLogger.log(TAG, "Extracting liked videos with cookies: $cookiesFilePath")
                runCatching {
                    val request =
                        YoutubeDLRequest("https://www.youtube.com/playlist?list=LL").apply {
                            addOption("--flat-playlist")
                            addOption("--get-duration")
                            addOption("--get-id")
                            addOption("--get-title")
                            addOption("--get-thumbnail")
                            addOption("--dump-json")
                            cookieSupport.addCookiesToRequest(this, cookiesFilePath)
                        }

                    val response = core.execute(request)
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(
                        TAG,
                        "Liked videos response received in ${duration}ms - Output length: ${response.out.length}, Error: ${response.err.take(
                            120,
                        )}",
                    )
                    val videos = parsers.parseVideoList(response.out)
                    if (videos.isEmpty() && response.err.isNotBlank()) {
                        throw IllegalStateException(response.err.trim())
                    }
                    fileLogger.log(TAG, "Successfully extracted ${videos.size} liked videos in ${duration}ms")
                    videos
                }
            }

        suspend fun extractPlaylistVideos(
            playlistId: String,
            cookiesFilePath: String?,
        ): Result<List<Video>> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                fileLogger.log(TAG, "Extracting playlist videos: $playlistId")
                runCatching {
                    val playlistUrl = "https://www.youtube.com/playlist?list=$playlistId"

                    // Keep this fast by using flat playlist and JSON-only output (do not mix --get-* with --dump-json).
                    val request =
                        YoutubeDLRequest(playlistUrl).apply {
                            addOption("--flat-playlist")
                            addOption("--yes-playlist")
                            addOption("--dump-json")
                            addOption("--no-progress")
                            addOption("--no-warnings")
                            cookieSupport.addCookiesToRequest(this, cookiesFilePath)
                        }

                    val response = core.execute(request)
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(
                        TAG,
                        "Playlist videos response received in ${duration}ms - Output length: ${response.out.length}, Error: ${response.err.take(
                            120,
                        )}",
                    )
                    val videos = parsers.parseVideoList(response.out)
                    if (videos.isEmpty() && response.err.isNotBlank()) {
                        throw IllegalStateException(response.err.trim())
                    }
                    fileLogger.log(TAG, "Successfully extracted ${videos.size} videos from playlist in ${duration}ms")
                    videos
                }
            }

        suspend fun extractPlaylistVideosChunk(
            playlistId: String,
            cookiesFilePath: String?,
            startIndex: Int,
            endIndex: Int,
        ): Result<List<Video>> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                fileLogger.log(TAG, "Extracting playlist videos chunk: $playlistId items=$startIndex:$endIndex")
                runCatching {
                    val playlistUrl = "https://www.youtube.com/playlist?list=$playlistId"
                    val request =
                        YoutubeDLRequest(playlistUrl).apply {
                            addOption("--flat-playlist")
                            addOption("--yes-playlist")
                            addOption("--dump-json")
                            addOption("--playlist-items", "$startIndex:$endIndex")
                            addOption("--no-progress")
                            addOption("--no-warnings")
                            cookieSupport.addCookiesToRequest(this, cookiesFilePath)
                        }

                    val response = core.execute(request)
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(
                        TAG,
                        "Playlist chunk response received in ${duration}ms - Output length: ${response.out.length}, Error: ${response.err.take(
                            120,
                        )}",
                    )

                    val videos = parsers.parseVideoList(response.out)
                    if (videos.isEmpty() && response.err.isNotBlank()) {
                        throw IllegalStateException(response.err.trim())
                    }
                    fileLogger.log(TAG, "Successfully extracted ${videos.size} videos from playlist chunk in ${duration}ms")
                    videos
                }
            }

        suspend fun extractPlaylistVideosFull(
            playlistId: String,
            cookiesFilePath: String?,
        ): Result<List<Video>> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                fileLogger.log(TAG, "Extracting playlist videos with full metadata (enriched): $playlistId")
                runCatching {
                    // First get basic video list from playlist
                    val playlistUrl = "https://www.youtube.com/playlist?list=$playlistId"
                    
                    // Use --flat-playlist first to get video IDs quickly, then enrich each
                    val idRequest =
                        YoutubeDLRequest(playlistUrl).apply {
                            addOption("--ignore-config")
                            addOption("--flat-playlist")
                            addOption("--dump-json")
                            addOption("--no-progress")
                            addOption("--no-warnings")
                            cookieSupport.addCookiesToRequest(this, cookiesFilePath)
                        }

                    val idResponse = core.execute(idRequest)
                    val basicVideos = parsers.parseVideoList(idResponse.out)
                    
                    if (basicVideos.isEmpty()) {
                        return@runCatching emptyList<Video>()
                    }
                    
                    fileLogger.log(TAG, "Stage 2: Enriching ${basicVideos.size} videos with full metadata...")
                    
                    // Now enrich each video with full metadata by extracting individually
                    // Process in batches of 5 to avoid overwhelming the system
                    val enrichedVideos = mutableListOf<Video>()
                    val batchSize = 5
                    
                    basicVideos.chunked(batchSize).forEachIndexed { batchIndex, batch ->
                        fileLogger.log(TAG, "Enriching batch ${batchIndex + 1}/${(basicVideos.size + batchSize - 1) / batchSize}, size: ${batch.size}")
                        
                        val batchResults = batch.map { video ->
                            async(Dispatchers.IO) {
                                extractVideoFullMetadata(video.id, cookiesFilePath).getOrNull() ?: video
                            }
                        }.awaitAll()
                        
                        enrichedVideos.addAll(batchResults)
                        
                        // Small delay between batches to be respectful to YouTube
                        if (batchIndex < (basicVideos.size + batchSize - 1) / batchSize - 1) {
                            delay(100)
                        }
                    }
                    
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(
                        TAG,
                        "Enriched ${enrichedVideos.size} videos with full metadata in ${duration}ms"
                    )
                    enrichedVideos
                }
            }

        /**
         * Extract full metadata for a single video.
         */
        private suspend fun extractVideoFullMetadata(
            videoId: String,
            cookiesFilePath: String?,
        ): Result<Video> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val videoUrl = "https://www.youtube.com/watch?v=$videoId"
                    val request =
                        YoutubeDLRequest(videoUrl).apply {
                            addOption("--ignore-config")
                            addOption("--dump-json")
                            addOption("--skip-download")
                            addOption("--no-playlist")
                            addOption("--no-progress")
                            addOption("--no-warnings")
                            cookieSupport.addCookiesToRequest(this, cookiesFilePath)
                        }

                    val response = core.execute(request)
                    val video = parsers.parseSingleVideoJson(response.out)
                    video ?: throw IllegalStateException("No video data returned for $videoId")
                }
            }

        /**
         * Extract playlist videos chunk without --flat-playlist for full metadata.
         */
        suspend fun extractPlaylistVideosChunkFull(
            playlistId: String,
            cookiesFilePath: String?,
            startIndex: Int,
            endIndex: Int,
        ): Result<List<Video>> =
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                fileLogger.log(TAG, "Extracting playlist videos chunk (full mode): $playlistId items=$startIndex:$endIndex")
                runCatching {
                    val playlistUrl = "https://www.youtube.com/playlist?list=$playlistId"
                    val request =
                        YoutubeDLRequest(playlistUrl).apply {
                            addOption("--yes-playlist")
                            addOption("--dump-json")
                            addOption("--playlist-items", "$startIndex:$endIndex")
                            addOption("--no-progress")
                            addOption("--no-warnings")
                            cookieSupport.addCookiesToRequest(this, cookiesFilePath)
                        }

                    val response = core.execute(request)
                    val duration = System.currentTimeMillis() - startTime
                    fileLogger.log(
                        TAG,
                        "Playlist chunk (full) response received in ${duration}ms - Output length: ${response.out.length}, Error: ${response.err.take(
                            120,
                        )}",
                    )

                    val videos = parsers.parsePlaylistEntriesFromSingleJson(response.out)
                    fileLogger.log(TAG, "Successfully extracted ${videos.size} videos from playlist chunk (full mode) in ${duration}ms")
                    videos
                }
            }
    }
