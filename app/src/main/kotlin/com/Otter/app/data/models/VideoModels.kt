package com.Otter.app.data.models

data class Video(
    val id: String,
    val title: String,
    val thumbnail: String,
    val channelName: String,
    val channelId: String,
    val views: Long,
    val duration: Int, // in seconds
    val uploadDate: String,
    val description: String,
    val isLiked: Boolean = false,
    val isWatched: Boolean = false,
    val streamUrl: String? = null,
    val audioStreamUrl: String? = null,
)

data class VideoFormat(
    val id: String,
    val quality: String,
    val resolution: String,
    val fps: Int,
    val codec: String,
    val fileSize: Long,
    val url: String,
)

data class Channel(
    val id: String,
    val name: String,
    val thumbnail: String,
    val subscriberCount: Long,
    val videoCount: Int,
    val description: String,
)

data class Playlist(
    val id: String,
    val title: String,
    val thumbnail: String,
    val videoCount: Int,
    val videos: List<Video> = emptyList(),
)

data class VideoProgress(
    val videoId: String,
    val title: String,
    val thumbnail: String,
    val watchedAt: Long,
    val progress: Float,
    val duration: Int,
    val watchedDuration: Int = 0,
    val isCompleted: Boolean = false,
)
