package com.Otter.app.data.download
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Format(
    @SerialName("format_id") val formatId: String = "",
    val format: String? = null,
    val url: String? = null,
    val ext: String? = null,
    val acodec: String? = null,
    val vcodec: String? = null,
    @SerialName("filesize") val fileSize: Double? = null,
    @SerialName("filesize_approx") val fileSizeApprox: Double? = null,
    val height: Int? = null,
    val width: Int? = null,
    val tbr: Double? = null,
    val protocol: String? = null,
    val name: String? = null,
) {
    fun containsVideo(): Boolean = vcodec != null && vcodec != "none"

    fun containsAudio(): Boolean = acodec != null && acodec != "none"

    fun isVideoOnly(): Boolean = containsVideo() && !containsAudio()

    fun isAudioOnly(): Boolean = !containsVideo() && containsAudio()
}
