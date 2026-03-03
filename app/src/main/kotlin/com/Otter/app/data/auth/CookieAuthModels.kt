package com.Otter.app.data.auth

import kotlinx.serialization.Serializable

@Serializable
data class CookieAuthEntry(
    val profileId: String,
    val targetId: String,
    val cookiesFilePath: String? = null,
    val enabledForYtDlp: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
)
