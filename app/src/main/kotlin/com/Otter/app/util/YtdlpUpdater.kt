package com.Otter.app.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.Otter.app.util.PreferenceUtil.getBoolean
import com.Otter.app.util.PreferenceUtil.getLong
import com.Otter.app.util.PreferenceUtil.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun YtdlpUpdater() {
    LaunchedEffect(Unit) {
        // Check if auto update is enabled and conditions are met
        if (!YT_DLP_AUTO_UPDATE.getBoolean() && YT_DLP_VERSION.getString().isNotEmpty()) {
            return@LaunchedEffect
        }

        if (!PreferenceUtil.isNetworkAvailableForDownload()) {
            return@LaunchedEffect
        }

        val lastUpdateTime = YT_DLP_UPDATE_TIME.getLong()
        val currentTime = System.currentTimeMillis()

        if (currentTime < lastUpdateTime + YT_DLP_UPDATE_INTERVAL.getLong()) {
            return@LaunchedEffect
        }

        runCatching {
            withContext(Dispatchers.IO) {
                UpdateUtil.updateYtDlpWithNotification()
            }
        }.onFailure { it.printStackTrace() }
    }
}
