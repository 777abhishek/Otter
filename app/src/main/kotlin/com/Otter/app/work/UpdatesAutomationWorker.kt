package com.Otter.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.Otter.app.service.SettingsService
import com.Otter.app.service.StorageService
import com.Otter.app.util.AppUpdateUtil
import com.Otter.app.util.UpdateUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@HiltWorker
class UpdatesAutomationWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val settingsService: SettingsService,
        private val storageService: StorageService,
    ) : CoroutineWorker(context, params) {

        companion object {
            const val UNIQUE_WORK_NAME = "updates_automation"
        }

        override suspend fun doWork(): Result {
            val settings = settingsService.getSettings().first()
            if (!settings.updatesAutomationEnabled) return Result.success()

            return runCatching {
                // 1) Otter app update check
                val latestRelease = withContext(Dispatchers.IO) { AppUpdateUtil.fetchLatestRelease() }
                if (settings.updatesAutomationNotify && latestRelease != null) {
                    val currentVersion = normalizeVersion(com.Otter.app.BuildConfig.VERSION_NAME)
                    val latestVersion = normalizeVersion(latestRelease.tagName)
                    if (latestVersion.isNotBlank() && compareVersions(latestVersion, currentVersion) > 0) {
                        UpdateUtil.postUpdateAvailableNotification(
                            title = "Otter update available",
                            text = "New version ${latestRelease.tagName} is available",
                        )

                        if (settings.updatesAutomationAutoDownloadApk) {
                            val asset = AppUpdateUtil.pickApkAsset(latestRelease)
                            if (asset != null) {
                                AppUpdateUtil.downloadAssetToCache(applicationContext, asset) { _, _ -> }
                            }
                        }
                    }
                }

                // 2) yt-dlp auto update
                if (settings.updatesAutomationAutoUpdateYtDlp) {
                    UpdateUtil.updateYtDlpWithNotification()
                }

                // 3) NewPipe auto check
                if (settings.updatesAutomationAutoCheckNewPipe) {
                    val latest = UpdateUtil.checkNewPipeUpdates()
                    if (latest != null && settings.updatesAutomationNotify) {
                        UpdateUtil.postUpdateAvailableNotification(
                            title = "NewPipe extractor update",
                            text = "Latest version: $latest",
                        )
                    }
                }

                // 4) Cache cleanup
                if (settings.updatesAutomationAutoClearCache) {
                    storageService.clearCache()
                }

                Unit
            }.fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() },
            )
        }

        private fun normalizeVersion(v: String): String = v.trim().removePrefix("v").removePrefix("V")

        private fun compareVersions(a: String, b: String): Int {
            val partsA = a.split(".").map { it.toIntOrNull() ?: 0 }
            val partsB = b.split(".").map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(partsA.size, partsB.size)) {
                val pa = partsA.getOrElse(i) { 0 }
                val pb = partsB.getOrElse(i) { 0 }
                if (pa != pb) return pa.compareTo(pb)
            }
            return 0
        }
    }
