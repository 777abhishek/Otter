package com.Otter.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.Otter.app.data.database.dao.*
import com.Otter.app.data.database.entities.*

@Database(
    entities = [
        VideoEntity::class,
        PlaylistEntity::class,
        DownloadTaskEntity::class,
        VideoProgressEntity::class,
        StudyMaterialEntity::class,
    ],
    version = 13,
    exportSchema = false,
)
abstract class OtterDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao

    abstract fun playlistDao(): PlaylistDao

    abstract fun downloadTaskDao(): DownloadTaskDao

    abstract fun videoProgressDao(): VideoProgressDao

    abstract fun studyMaterialDao(): StudyMaterialDao

    companion object {
        private const val DATABASE_NAME = "Otter.db"

        @Volatile
        private var INSTANCE: OtterDatabase? = null

        fun getDatabase(context: Context): OtterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        OtterDatabase::class.java,
                        DATABASE_NAME,
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
