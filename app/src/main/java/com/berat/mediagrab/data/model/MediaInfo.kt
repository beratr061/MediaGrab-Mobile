package com.berat.mediagrab.data.model

import java.util.Locale

/** Represents an available video/audio quality option */
data class VideoQuality(
        val formatId: String,
        val label: String, // e.g., "1080p", "720p", "360p", "320kbps"
        val height: Int? = null, // For video: 1080, 720, etc.
        val fps: Int? = null, // For video: 30, 60, etc.
        val bitrate: Int? = null, // For audio: kbps
        val filesize: Long? = null,
        val isVideo: Boolean = true,
        val hasAudio: Boolean = true,
        val ext: String = "mp4"
)

/** Media information fetched from a URL */
data class MediaInfo(
        val id: String = "",
        val title: String,
        val url: String,
        val thumbnail: String? = null,
        val duration: Double? = null,
        val uploader: String? = null,
        val uploaderUrl: String? = null,
        val description: String? = null,
        val viewCount: Long? = null,
        val likeCount: Long? = null,
        val uploadDate: String? = null,
        val filesize: Long? = null,
        val extractor: String? = null,
        val availableQualities: List<VideoQuality> = emptyList()
) {
    val formattedDuration: String
        get() {
            if (duration == null) return "--:--"
            val totalSeconds = duration.toInt()
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60

            return if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%d:%02d", minutes, seconds)
            }
        }

    @Suppress("unused")
    val formattedFilesize: String
        get() {
            if (filesize == null) return "Unknown size"
            val bytes = filesize
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
                bytes < 1024 * 1024 * 1024 ->
                        String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
                else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024 * 1024))
            }
        }
}
