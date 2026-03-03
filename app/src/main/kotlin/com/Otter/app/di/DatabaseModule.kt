package com.Otter.app.di

import android.content.Context
import androidx.room.Room
import com.Otter.app.data.database.OtterDatabase
import com.Otter.app.data.database.dao.*
import com.Otter.app.data.repositories.*
import com.Otter.app.service.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideOtterDatabase(
        @ApplicationContext context: Context,
    ): OtterDatabase {
        return Room.databaseBuilder(
            context,
            OtterDatabase::class.java,
            "Otter.db",
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideVideoDao(database: OtterDatabase): VideoDao {
        return database.videoDao()
    }

    @Provides
    fun providePlaylistDao(database: OtterDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun provideDownloadTaskDao(database: OtterDatabase): DownloadTaskDao {
        return database.downloadTaskDao()
    }

    @Provides
    fun provideVideoProgressDao(database: OtterDatabase): VideoProgressDao {
        return database.videoProgressDao()
    }

    @Provides
    fun provideStudyMaterialDao(database: OtterDatabase): StudyMaterialDao {
        return database.studyMaterialDao()
    }
}
