package com.Otter.app.service

import com.Otter.app.data.models.Channel
import com.Otter.app.data.models.Video
import com.Otter.app.data.models.VideoFormat
import kotlinx.coroutines.flow.Flow

interface VideoService {
    suspend fun getVideoById(id: String): Result<Video>

    suspend fun searchVideos(
        query: String,
        page: Int = 1,
    ): Result<List<Video>>

    suspend fun getPopularVideos(limit: Int = 20): Result<List<Video>>

    suspend fun getVideoFormats(videoId: String): Result<List<VideoFormat>>

    suspend fun getStreamUrl(
        videoId: String,
        formatId: String,
    ): Result<String>

    suspend fun getChannelById(id: String): Result<Channel>

    suspend fun getChannelVideos(
        channelId: String,
        limit: Int = 20,
    ): Result<List<Video>>

    fun getLikedVideos(): Flow<List<Video>>

    suspend fun likeVideo(videoId: String): Result<Unit>

    suspend fun unlikeVideo(videoId: String): Result<Unit>

    fun getWatchHistory(): Flow<List<Video>>

    suspend fun addToWatchHistory(
        videoId: String,
        progress: Float,
    ): Result<Unit>

    suspend fun clearWatchHistory(): Result<Unit>
}
