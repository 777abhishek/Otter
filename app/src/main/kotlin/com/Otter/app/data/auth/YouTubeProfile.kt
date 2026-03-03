package com.Otter.app.data.auth

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeProfile(
    val id: String,
    val label: String,
    val cookiesFilePath: String? = null,
    val isLoggedIn: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
)
