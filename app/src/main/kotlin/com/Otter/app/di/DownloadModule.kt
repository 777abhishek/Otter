package com.Otter.app.di

import com.Otter.app.data.download.Downloader
import com.Otter.app.data.download.DownloaderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DownloadModule {
    @Binds
    abstract fun bindDownloader(impl: DownloaderImpl): Downloader
}
