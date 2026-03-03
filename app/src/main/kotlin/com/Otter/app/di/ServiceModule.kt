package com.Otter.app.di

import com.Otter.app.service.*
import com.Otter.app.service.impl.DownloadServiceImpl
import com.Otter.app.service.impl.SettingsServiceImpl
import com.Otter.app.service.impl.StorageServiceImpl
import com.Otter.app.service.impl.VideoServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    // TODO: Re-enable service bindings once compilation issues are resolved

    @Binds
    @Singleton
    abstract fun bindVideoService(videoService: VideoServiceImpl): VideoService

    @Binds
    @Singleton
    abstract fun bindDownloadService(downloadService: DownloadServiceImpl): DownloadService

    @Binds
    @Singleton
    abstract fun bindSettingsService(settingsService: SettingsServiceImpl): SettingsService

    @Binds
    @Singleton
    abstract fun bindStorageService(storageService: StorageServiceImpl): StorageService
}
