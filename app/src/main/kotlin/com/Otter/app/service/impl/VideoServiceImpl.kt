package com.Otter.app.service.impl

import com.Otter.app.data.auth.CookieAuthStore
import com.Otter.app.data.models.Channel
import com.Otter.app.data.models.Video
import com.Otter.app.data.models.VideoFormat
import com.Otter.app.data.ytdlp.YtDlpManager
import com.Otter.app.service.VideoService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoServiceImpl
    @Inject
    constructor(
        private val ytDlpManager: YtDlpManager,
        private val cookieAuthStore: CookieAuthStore,
    ) : VideoService {
        private val _likedVideos = MutableStateFlow<List<Video>>(emptyList())
        private val _watchHistory = MutableStateFlow<List<Video>>(emptyList())

        override suspend fun getVideoById(id: String): Result<Video> {
            return try {
                val url = "https://www.youtube.com/watch?v=$id"
                val cookiesFilePath = cookieAuthStore.getCookiesFilePathOnceForUrl(url, requireEnabled = true)
                val info =
                    ytDlpManager.getVideoInfo(
                        url = url,
                        cookiesFilePath = cookiesFilePath,
                    )
                info.map { data ->
                    Video(
                        id = id,
                        title = data["title"] as String? ?: "",
                        thumbnail = (data["thumbnail"] as String?) ?: "",
                        channelName = data["uploader"] as String? ?: "",
                        channelId = data["channel_id"] as String? ?: "",
                        views = (data["view_count"] as String?)?.toLongOrNull() ?: 0,
                        duration = parseDuration(data["duration"] as String? ?: "0"),
                        uploadDate = data["upload_date"] as String? ?: "",
                        description = data["description"] as String? ?: "",
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun searchVideos(
            query: String,
            page: Int,
        ): Result<List<Video>> {
            return try {
                // Use yt-dlp to search
                val cookiesFilePath = cookieAuthStore.getCookiesFilePathOnceForUrl(query, requireEnabled = true)
                val info = ytDlpManager.getVideoInfo(query, cookiesFilePath = cookiesFilePath)
                info.map { data ->
                    listOf(
                        Video(
                            id = "search_${System.currentTimeMillis()}",
                            title = data["title"] as String? ?: query,
                            thumbnail = (data["thumbnail"] as String?) ?: "",
                            channelName = data["uploader"] as String? ?: "",
                            channelId = data["channel_id"] as String? ?: "",
                            views = (data["view_count"] as String?)?.toLongOrNull() ?: 0,
                            duration = parseDuration(data["duration"] as String? ?: "0"),
                            uploadDate = data["upload_date"] as String? ?: "",
                            description = data["description"] as String? ?: "",
                        ),
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun getPopularVideos(limit: Int): Result<List<Video>> {
            return try {
                val videos =
                    listOf(
                        Video(
                            id = "1",
                            title = "Introduction to Flutter Development",
                            thumbnail = "",
                            channelName = "Tech Channel",
                            channelId = "UC123",
                            views = 1200000,
                            duration = 930,
                            uploadDate = "2024-01-01",
                            description = "Learn Flutter from scratch",
                        ),
                        Video(
                            id = "2",
                            title = "Kotlin Programming Tutorial",
                            thumbnail = "",
                            channelName = "Code Academy",
                            channelId = "UC456",
                            views = 850000,
                            duration = 1365,
                            uploadDate = "2024-01-02",
                            description = "Master Kotlin programming",
                        ),
                    )
                Result.success(videos.take(limit))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun getVideoFormats(videoId: String): Result<List<VideoFormat>> {
            return try {
                val cookiesFilePath = cookieAuthStore.getCookiesFilePathOnceForUrl(videoId, requireEnabled = true)
                val formats = ytDlpManager.getFormats(videoId, cookiesFilePath = cookiesFilePath)
                formats.map { list ->
                    list.map { data ->
                        VideoFormat(
                            data["format_code"] as String? ?: "",
                            data["resolution"] as String? ?: "unknown",
                            data["resolution"] as String? ?: "unknown",
                            30,
                            "h264",
                            0L,
                            "",
                        )
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun getStreamUrl(
            videoId: String,
            formatId: String,
        ): Result<String> {
            return try {
                val url = "https://www.youtube.com/watch?v=$videoId"
                val cookiesFilePath = cookieAuthStore.getCookiesFilePathOnceForUrl(url, requireEnabled = true)
                ytDlpManager.getStreamUrl(
                    url = url,
                    formatId = formatId,
                    cookiesFilePath = cookiesFilePath,
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun getChannelById(id: String): Result<Channel> {
            return try {
                Result.success(
                    Channel(
                        id = id,
                        name = "Sample Channel",
                        thumbnail = "",
                        subscriberCount = 100000,
                        videoCount = 100,
                        description = "",
                    ),
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun getChannelVideos(
            channelId: String,
            limit: Int,
        ): Result<List<Video>> {
            return try {
                Result.success(emptyList())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override fun getLikedVideos(): Flow<List<Video>> = _likedVideos.asStateFlow()

        override suspend fun likeVideo(videoId: String): Result<Unit> {
            return try {
                _likedVideos.value = _likedVideos.value +
                    Video(
                        id = videoId,
                        title = "",
                        thumbnail = "",
                        channelName = "",
                        channelId = "",
                        views = 0,
                        duration = 0,
                        uploadDate = "",
                        description = "",
                        isLiked = true,
                    )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun unlikeVideo(videoId: String): Result<Unit> {
            return try {
                _likedVideos.value = _likedVideos.value.filter { it.id != videoId }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override fun getWatchHistory(): Flow<List<Video>> = _watchHistory.asStateFlow()

        override suspend fun addToWatchHistory(
            videoId: String,
            progress: Float,
        ): Result<Unit> {
            return try {
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun clearWatchHistory(): Result<Unit> {
            return try {
                _watchHistory.value = emptyList()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        private fun parseDuration(duration: String): Int {
            return try {
                val parts = duration.split(":")
                when (parts.size) {
                    2 -> parts[0].toInt() * 60 + parts[1].toInt()
                    3 -> parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()
                    else -> duration.toIntOrNull() ?: 0
                }
            } catch (e: Exception) {
                0
            }
        }
    }
