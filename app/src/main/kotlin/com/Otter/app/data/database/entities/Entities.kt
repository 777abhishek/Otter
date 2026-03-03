package com.Otter.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val channelName: String?,
    val channelId: String?,
    val viewCount: String?,
    val uploadDate: String?,
    val duration: String?,
    val thumbnailUrl: String?,
    val isShort: Boolean = false,
    val downloadProgress: Float = 0.0f,
    val playlistId: String?,
    val position: Int? = null,
    val addedDate: String?,
    val isLiked: Boolean = false,
    val isWatchLater: Boolean = false,
    val description: String?,
    val url: String?,
    val filePath: String?,
    val isDownloaded: Boolean = false,
    val streamUrl: String? = null,
    val audioStreamUrl: String? = null,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String?,
    val videoCount: Int?,
    val thumbnailUrl: String?,
    val uploader: String?,
    val uploaderUrl: String?,
    val lastUpdated: String?,
    val isLiked: Boolean = false,
    val isWatchLater: Boolean = false,
    val channel: String?,
    val channelId: String?,
    val channelUrl: String?,
    val availability: String?,
    val modifiedDate: String?,
    val viewCount: Int?,
)

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String?,
    val duration: String?,
    val progress: Float = 0.0f,
    val speed: String?,
    val eta: String?,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: String,
    val error: String?,
    val filePath: String?,
    val formatId: String?,
    val expectedSize: Long? = null,
    val addedDate: String,
    val artist: String?,
    val album: String?,
    val genre: String?,
    val uploadDate: String?,
    val description: String?,
    val embeddedMetadata: String?,
)

@Entity(tableName = "video_progress")
data class VideoProgressEntity(
    @PrimaryKey
    val videoId: String,
    val watchedDuration: Int = 0,
    val totalDuration: Int = 0,
    val lastWatched: Long = 0,
    val isCompleted: Boolean = false,
    val quality: Int = 0,
)

@Entity(tableName = "study_materials")
data class StudyMaterialEntity(
    @PrimaryKey
    val videoId: String,
    val summary: String?,
    val studyNotes: String?,
    val questions: String?,
    val quiz: String?,
    val analysis: String?,
    val transcript: String?,
    val generatedAt: String?,
    val hasApiKey: Boolean = false,
)
