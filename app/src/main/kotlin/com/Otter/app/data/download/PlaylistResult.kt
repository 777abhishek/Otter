package com.Otter.app.data.download

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistResult(
    val id: String = "",
    val title: String = "",
    val uploader: String = "",
    val channel: String = "",
    val webpageUrl: String = "",
    val thumbnail: String = "",
    val entryCount: Int = 0,
    val entries: List<PlaylistEntry>? = null,
)

@Serializable
data class PlaylistEntry(
    val id: String = "",
    val title: String = "",
    val duration: Double = 0.0,
    val uploader: String = "",
    val channel: String = "",
    val url: String? = null,
    val thumbnails: List<Thumbnail>? = null,
    val index: Int = 0,
)

@Serializable
data class Thumbnail(
    val url: String = "",
    val width: Int? = null,
    val height: Int? = null,
    val resolution: String? = null,
)
