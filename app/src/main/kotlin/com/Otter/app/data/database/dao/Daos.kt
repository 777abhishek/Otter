package com.Otter.app.data.database.dao

import androidx.room.*
import com.Otter.app.data.database.entities.*
import kotlinx.coroutines.flow.Flow

data class VideoFlagsProjection(
    val id: String,
    val isDownloaded: Boolean,
    val filePath: String?,
    val isLiked: Boolean,
    val isWatchLater: Boolean,
    val addedDate: String?,
)

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getVideoById(id: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getVideosByPlaylist(playlistId: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE playlistId = :playlistId")
    suspend fun getVideosByPlaylistOnce(playlistId: String): List<VideoEntity>

    @Query(
        "SELECT id, isDownloaded, filePath, isLiked, isWatchLater, addedDate FROM videos WHERE playlistId = :playlistId",
    )
    suspend fun getVideoFlagsByPlaylistOnce(playlistId: String): List<VideoFlagsProjection>

    @Query("SELECT * FROM videos WHERE playlistId = 'LL' ORDER BY position ASC")
    fun getLikedVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE playlistId = 'WL' ORDER BY position ASC")
    fun getWatchLaterVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isDownloaded = 1 ORDER BY addedDate DESC")
    fun getDownloadedVideos(): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Delete
    suspend fun deleteVideo(video: VideoEntity)

    @Query("DELETE FROM videos WHERE playlistId = :playlistId")
    suspend fun deleteVideosByPlaylist(playlistId: String)

    @Query("SELECT id FROM videos WHERE playlistId = :playlistId")
    suspend fun getVideoIdsByPlaylist(playlistId: String): List<String>

    @Query("DELETE FROM videos WHERE playlistId = :playlistId AND id NOT IN (:ids)")
    suspend fun deleteVideosByPlaylistNotIn(
        playlistId: String,
        ids: List<String>,
    )

    @Query("UPDATE videos SET isLiked = :isLiked WHERE id = :id")
    suspend fun updateLikeStatus(
        id: String,
        isLiked: Boolean,
    )

    @Query("UPDATE videos SET isWatchLater = :isWatchLater WHERE id = :id")
    suspend fun updateWatchLaterStatus(
        id: String,
        isWatchLater: Boolean,
    )

    @Query("UPDATE videos SET isLiked = 0")
    suspend fun clearLikedFlags()

    @Query("UPDATE videos SET isWatchLater = 0")
    suspend fun clearWatchLaterFlags()

    @Query("UPDATE videos SET isDownloaded = :isDownloaded, filePath = :filePath WHERE id = :id")
    suspend fun updateDownloadStatus(
        id: String,
        isDownloaded: Boolean,
        filePath: String?,
    )
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observePlaylistById(id: String): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists ORDER BY lastUpdated DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE isLiked = 1")
    fun getLikedPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE isWatchLater = 1")
    fun getWatchLaterPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(playlists: List<PlaylistEntity>)

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id NOT IN (:ids) AND id NOT IN ('LL', 'WL')")
    suspend fun deletePlaylistsNotIn(ids: List<String>)
}

@Dao
interface DownloadTaskDao {
    @Query("SELECT * FROM download_tasks WHERE id = :id")
    suspend fun getTaskById(id: String): DownloadTaskEntity?

    @Query("SELECT * FROM download_tasks ORDER BY addedDate DESC")
    fun getAllTasks(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE status = :status ORDER BY addedDate DESC")
    fun getTasksByStatus(status: String): Flow<List<DownloadTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DownloadTaskEntity)

    @Update
    suspend fun updateTask(task: DownloadTaskEntity)

    @Delete
    suspend fun deleteTask(task: DownloadTaskEntity)

    @Query("DELETE FROM download_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("DELETE FROM download_tasks WHERE status = 'COMPLETED'")
    suspend fun clearCompletedTasks()

    @Query("DELETE FROM download_tasks WHERE filePath = :filePath")
    suspend fun deleteTaskByPath(filePath: String)

    @Query("DELETE FROM download_tasks")
    suspend fun clearAllTasks()
}

@Dao
interface VideoProgressDao {
    @Query("SELECT * FROM video_progress WHERE videoId = :videoId")
    suspend fun getProgress(videoId: String): VideoProgressEntity?

    @Query("SELECT * FROM video_progress ORDER BY lastWatched DESC")
    fun getAllProgress(): Flow<List<VideoProgressEntity>>

    @Query("SELECT * FROM video_progress WHERE lastWatched > :timestamp ORDER BY lastWatched DESC")
    fun getRecentProgress(timestamp: Long): Flow<List<VideoProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: VideoProgressEntity)

    @Update
    suspend fun updateProgress(progress: VideoProgressEntity)

    @Delete
    suspend fun deleteProgress(progress: VideoProgressEntity)

    @Query("DELETE FROM video_progress WHERE videoId = :videoId")
    suspend fun deleteProgressById(videoId: String)

    @Query("DELETE FROM video_progress WHERE lastWatched < :timestamp")
    suspend fun deleteOldProgress(timestamp: Long)
}

@Dao
interface StudyMaterialDao {
    @Query("SELECT * FROM study_materials WHERE videoId = :videoId")
    suspend fun getStudyMaterial(videoId: String): StudyMaterialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyMaterial(material: StudyMaterialEntity)

    @Update
    suspend fun updateStudyMaterial(material: StudyMaterialEntity)

    @Delete
    suspend fun deleteStudyMaterial(material: StudyMaterialEntity)
}
