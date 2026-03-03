package com.Otter.app.util

import kotlinx.serialization.Serializable

@Serializable
data class SubtitleFormat(
    val ext: String,
    val url: String,
    val name: String? = null,
    val protocol: String? = null,
)
