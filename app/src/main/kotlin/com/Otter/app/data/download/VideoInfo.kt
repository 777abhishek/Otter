package com.Otter.app.data.download

import com.Otter.app.util.SubtitleFormat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoInfo(
    @SerialName("original_url") val originalUrl: String = "",
    @SerialName("webpage_url") val webpageUrl: String? = null,
    val title: String = "",
    val uploader: String? = null,
    val channel: String? = null,
    @SerialName("uploader_id") val uploaderId: String? = null,
    @SerialName("view_count") val viewCount: Long = 0,
    @SerialName("extractor_key") val extractorKey: String = "",
    @SerialName("extractor") val extractor: String? = null,
    val duration: Double? = null,
    @SerialName("thumbnail") val thumbnailUrl: String? = null,
    @SerialName("filesize") val fileSize: Double? = null,
    @SerialName("filesize_approx") val fileSizeApprox: Double? = null,
    @SerialName("formats") val formats: List<Format>? = null,
    @SerialName("requested_formats") val requestedFormats: List<Format>? = null,
    @SerialName("requested_downloads") val requestedDownloads: List<RequestedDownload>? = null,
    val subtitles: Map<String, List<SubtitleFormat>> = emptyMap(),
    @SerialName("automatic_captions") val automaticCaptions: Map<String, List<SubtitleFormat>> = emptyMap(),
    val chapters: List<Chapter>? = null,
    val playlist: String? = null,
    val id: String = "",
    val vcodec: String? = null,
    val acodec: String? = null,
    val ext: String? = null,
    val format: String? = null,
    @SerialName("format_id") val formatId: String? = null,
)

@Serializable
data class RequestedDownload(
    @SerialName("requested_formats") val requestedFormats: List<Format>? = null,
    val filename: String? = null,
)

@Serializable
data class Chapter(
    @SerialName("start_time") val startTime: Double,
    @SerialName("end_time") val endTime: Double,
    val title: String? = null,
)
