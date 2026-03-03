package com.Otter.app.data.download

import com.Otter.app.util.PreferenceUtil.getBoolean
import com.Otter.app.util.PreferenceUtil.getInt
import com.Otter.app.util.PreferenceUtil.getString
import kotlinx.serialization.Serializable

@Serializable
data class DownloadPreferences(
    val extractAudio: Boolean = false,
    val createThumbnail: Boolean = false,
    val downloadPlaylist: Boolean = false,
    val subdirectoryExtractor: Boolean = false,
    val subdirectoryPlaylistTitle: Boolean = false,
    val commandDirectory: String = "",
    val downloadSubtitle: Boolean = false,
    val embedSubtitle: Boolean = false,
    val keepSubtitle: Boolean = false,
    val subtitleLanguage: String = "",
    val autoSubtitle: Boolean = false,
    val autoTranslatedSubtitles: Boolean = false,
    val convertSubtitle: Int = 0,
    val concurrentFragments: Int = 0,
    val sponsorBlock: Boolean = false,
    val sponsorBlockCategory: String = "",
    val sponsorBlockMarkCategories: String = "",
    val sponsorBlockRemoveCategories: String = "",
    val sponsorBlockChapterTitle: String = "",
    val sponsorBlockApiUrl: String = "",
    val noSponsorBlock: Boolean = false,
    val cookies: Boolean = false,
    val aria2c: Boolean = false,
    val useCustomAudioPreset: Boolean = false,
    val audioFormat: Int = 0,
    val audioQuality: Int = 0,
    val convertAudio: Boolean = false,
    val formatSorting: Boolean = false,
    val sortingFields: String = "",
    val audioConvertFormat: Int = 0,
    val videoFormat: Int = 0,
    val formatIdString: String = "",
    val videoResolution: Int = 0,
    val privateMode: Boolean = false,
    val rateLimit: Boolean = false,
    val maxDownloadRate: String = "",
    val privateDirectory: Boolean = false,
    val cropArtwork: Boolean = false,
    val sdcard: Boolean = false,
    val sdcardUri: String = "",
    val customDownloadPath: String = "",
    val embedThumbnail: Boolean = false,
    val videoClips: List<VideoClip> = emptyList(),
    val splitByChapter: Boolean = false,
    val debug: Boolean = false,
    val proxy: Boolean = false,
    val proxyUrl: String = "",
    val newTitle: String = "",
    val userAgentString: String = "",
    val outputTemplate: String = "",
    val useDownloadArchive: Boolean = false,
    val embedMetadata: Boolean = false,
    val restrictFilenames: Boolean = false,
    val supportAv1HardwareDecoding: Boolean = false,
    val forceIpv4: Boolean = false,
    val mergeAudioStream: Boolean = false,
    val mergeToMkv: Boolean = false,
) {
    companion object {
        val EMPTY = DownloadPreferences()

        fun createFromPreferences(): DownloadPreferences {
            val downloadSubtitle = SUBTITLE.getBoolean()
            val embedSubtitle = EMBED_SUBTITLE.getBoolean()
            return DownloadPreferences(
                extractAudio = EXTRACT_AUDIO.getBoolean(),
                createThumbnail = THUMBNAIL.getBoolean(),
                downloadPlaylist = PLAYLIST.getBoolean(),
                subdirectoryExtractor = SUBDIRECTORY_EXTRACTOR.getBoolean(),
                subdirectoryPlaylistTitle = SUBDIRECTORY_PLAYLIST_TITLE.getBoolean(),
                commandDirectory = COMMAND_DIRECTORY.getString(),
                downloadSubtitle = downloadSubtitle,
                embedSubtitle = embedSubtitle,
                keepSubtitle = KEEP_SUBTITLE_FILES.getBoolean(),
                subtitleLanguage = SUBTITLE_LANGUAGE.getString(),
                autoSubtitle = AUTO_SUBTITLE.getBoolean(),
                autoTranslatedSubtitles = AUTO_TRANSLATED_SUBTITLES.getBoolean(),
                convertSubtitle = CONVERT_SUBTITLE.getInt(),
                concurrentFragments = CONCURRENT_FRAGMENTS.getInt(),
                sponsorBlock = SPONSORBLOCK.getBoolean(),
                sponsorBlockCategory = SPONSORBLOCK_CATEGORIES.getString(),
                sponsorBlockMarkCategories = SPONSORBLOCK_MARK_CATEGORIES.getString(),
                sponsorBlockRemoveCategories = SPONSORBLOCK_REMOVE_CATEGORIES.getString(),
                sponsorBlockChapterTitle = SPONSORBLOCK_CHAPTER_TITLE.getString(),
                sponsorBlockApiUrl = SPONSORBLOCK_API_URL.getString(),
                noSponsorBlock = NO_SPONSORBLOCK.getBoolean(),
                cookies = COOKIES.getBoolean(),
                aria2c = ARIA2C.getBoolean(),
                useCustomAudioPreset = USE_CUSTOM_AUDIO_PRESET.getBoolean(),
                audioFormat = AUDIO_FORMAT.getInt(),
                audioQuality = AUDIO_QUALITY.getInt(),
                convertAudio = AUDIO_CONVERT.getBoolean(),
                formatSorting = FORMAT_SORTING.getBoolean(),
                sortingFields = SORTING_FIELDS.getString(),
                audioConvertFormat = AUDIO_CONVERSION_FORMAT.getInt(),
                videoFormat = VIDEO_FORMAT.getInt(),
                formatIdString = "",
                videoResolution = VIDEO_QUALITY.getInt(),
                privateMode = PRIVATE_MODE.getBoolean(),
                rateLimit = RATE_LIMIT.getBoolean(),
                maxDownloadRate = MAX_RATE.getString(),
                privateDirectory = PRIVATE_DIRECTORY.getBoolean(),
                cropArtwork = CROP_ARTWORK.getBoolean(),
                sdcard = SDCARD_DOWNLOAD.getBoolean(),
                sdcardUri = SDCARD_URI.getString(),
                customDownloadPath = CUSTOM_DOWNLOAD_PATH.getString(),
                embedThumbnail = EMBED_THUMBNAIL.getBoolean(),
                videoClips = emptyList(),
                splitByChapter = false,
                debug = DEBUG.getBoolean(),
                proxy = PROXY.getBoolean(),
                proxyUrl = PROXY_URL.getString(),
                newTitle = "",
                userAgentString = USER_AGENT_STRING.getString(),
                outputTemplate = OUTPUT_TEMPLATE.getString(),
                useDownloadArchive = DOWNLOAD_ARCHIVE.getBoolean(),
                embedMetadata = EMBED_METADATA.getBoolean(),
                restrictFilenames = RESTRICT_FILENAMES.getBoolean(),
                supportAv1HardwareDecoding = AV1_HARDWARE_ACCELERATED.getBoolean(),
                forceIpv4 = FORCE_IPV4.getBoolean(),
                mergeAudioStream = MERGE_MULTI_AUDIO_STREAM.getBoolean(),
                mergeToMkv = (downloadSubtitle && embedSubtitle) || MERGE_OUTPUT_MKV.getBoolean(),
            )
        }
    }
}

@Serializable
data class VideoClip(
    val start: Int,
    val end: Int,
)

// Constants for audio format
const val M4A = 0
const val OPUS = 1

// Constants for audio quality
const val HIGH = 0
const val MEDIUM = 1
const val LOW = 2

// Constants for audio conversion format
const val CONVERT_MP3 = 0
const val CONVERT_M4A = 1

// Constants for video format
const val FORMAT_COMPATIBILITY = 0
const val FORMAT_QUALITY = 1

// Constants for subtitle conversion
const val CONVERT_ASS = 0
const val CONVERT_SRT = 1
const val CONVERT_VTT = 2
const val CONVERT_LRC = 3

// Resolution constants
const val RES_2160P = 1
const val RES_1440P = 2
const val RES_1080P = 3
const val RES_720P = 4
const val RES_480P = 5
const val RES_360P = 6
const val RES_LOWEST = 7

// Preference keys (Seal-compatible)
const val CONCURRENT_FRAGMENTS = "concurrent_fragments"
const val EXTRACT_AUDIO = "extract_audio"
const val THUMBNAIL = "create_thumbnail"
const val DEBUG = "debug"
const val AUDIO_CONVERT = "audio_convert"
const val AUDIO_CONVERSION_FORMAT = "audio_convert_format"
const val AUDIO_FORMAT = "audio_format_preferred"
const val AUDIO_QUALITY = "audio_quality"
const val VIDEO_FORMAT = "video_format"
const val VIDEO_QUALITY = "quality"
const val FORMAT_SORTING = "format_sorting"
const val SORTING_FIELDS = "sorting_fields"
const val COMMAND_DIRECTORY = "command_directory"
const val SDCARD_DOWNLOAD = "sdcard_download"
const val SDCARD_URI = "sd_card_uri"
const val SUBDIRECTORY_EXTRACTOR = "sub-directory"
const val SUBDIRECTORY_PLAYLIST_TITLE = "subdirectory_playlist_title"
const val PLAYLIST = "playlist"
const val SUBTITLE = "subtitle"
const val EMBED_SUBTITLE = "embed_subtitle"
const val KEEP_SUBTITLE_FILES = "keep_subtitle"
const val SUBTITLE_LANGUAGE = "sub_lang"
const val AUTO_SUBTITLE = "auto_subtitle"
const val CONVERT_SUBTITLE = "convert_subtitle"
const val AUTO_TRANSLATED_SUBTITLES = "translated_subs"
const val SPONSORBLOCK = "sponsorblock"
const val SPONSORBLOCK_CATEGORIES = "sponsorblock_categories"
const val SPONSORBLOCK_MARK_CATEGORIES = "sponsorblock_mark_categories"
const val SPONSORBLOCK_REMOVE_CATEGORIES = "sponsorblock_remove_categories"
const val SPONSORBLOCK_CHAPTER_TITLE = "sponsorblock_chapter_title"
const val SPONSORBLOCK_API_URL = "sponsorblock_api_url"
const val NO_SPONSORBLOCK = "no_sponsorblock"
const val ARIA2C = "aria2c"
const val COOKIES = "cookies"
const val USER_AGENT_STRING = "user_agent_string"
const val PRIVATE_MODE = "private_mode"
const val RATE_LIMIT = "rate_limit"
const val MAX_RATE = "max_rate"
const val PRIVATE_DIRECTORY = "private_directory"
const val CROP_ARTWORK = "crop_artwork"
const val EMBED_THUMBNAIL = "embed_thumbnail"
const val PROXY = "proxy"
const val PROXY_URL = "proxy_url"
const val OUTPUT_TEMPLATE = "output_template"
const val DOWNLOAD_ARCHIVE = "download_archive"
const val EMBED_METADATA = "embed_metadata"
const val RESTRICT_FILENAMES = "restrict_filenames"
const val AV1_HARDWARE_ACCELERATED = "av1_hardware_accelerated"
const val FORCE_IPV4 = "force_ipv4"
const val CUSTOM_DOWNLOAD_PATH = "custom_download_path"
const val MERGE_OUTPUT_MKV = "merge_to_mkv"
const val USE_CUSTOM_AUDIO_PRESET = "custom_audio_preset"
const val MERGE_MULTI_AUDIO_STREAM = "multi_audio_stream"
