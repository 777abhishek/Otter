package com.Otter.app.data.ytdlp

import com.Otter.app.data.models.Channel
import com.Otter.app.data.models.Playlist
import com.Otter.app.data.models.Video
import com.Otter.app.util.FileLogger
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
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
    }
