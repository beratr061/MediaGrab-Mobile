package com.berat.mediagrab.data.model

/** Download configuration data class */
@Suppress("unused")
data class DownloadConfig(
        val url: String,
        val format: String, // video-mp4, video-webm, audio-mp3, etc.
        val quality: String, // best, 1080p, 720p, 480p
        val outputFolder: String = "",
        val embedSubtitles: Boolean = false,
        val cookiesFromBrowser: String? = null,
        val filenameTemplate: String? = null,
        val proxyUrl: String? = null
) {
    val isVideoFormat: Boolean
        get() = format.startsWith("video")

    val isAudioFormat: Boolean
        get() = format.startsWith("audio")

    fun toYtdlpFormat(): String {
        return when (format) {
            "video-mp4" -> "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]"
            "video-webm" -> "bestvideo[ext=webm]+bestaudio[ext=webm]/best[ext=webm]"
            "video-mkv" -> "bestvideo+bestaudio/best"
            "audio-mp3" -> "bestaudio/best"
            "audio-aac" -> "bestaudio[ext=m4a]/bestaudio"
            "audio-opus" -> "bestaudio[ext=webm]/bestaudio"
            "audio-flac" -> "bestaudio/best"
            "audio-wav" -> "bestaudio/best"
            else -> "bestvideo+bestaudio/best"
        }
    }
}
