package com.berat.mediagrab.data.model

import java.util.Locale

/** Download progress tracking */
@Suppress("unused")
data class DownloadProgress(
        val id: String,
        val mediaInfo: MediaInfo,
        val state: DownloadState,
        val progress: Float = 0f, // 0-100
        val downloadedBytes: Long = 0,
        val totalBytes: Long? = null,
        val speed: Long? = null, // bytes per second
        val eta: Int? = null, // seconds
        val error: String? = null,
        val filePath: String? = null,
        val startTime: Long = System.currentTimeMillis(),
        val endTime: Long? = null
) {
    val formattedSpeed: String
        get() {
            if (speed == null) return "--"
            return when {
                speed < 1024 -> "$speed B/s"
                speed < 1024 * 1024 -> String.format(Locale.US, "%.1f KB/s", speed / 1024.0)
                else -> String.format(Locale.US, "%.1f MB/s", speed / (1024.0 * 1024))
            }
        }

    val formattedEta: String
        get() {
            if (eta == null) return "--:--"
            val seconds = eta
            return when {
                seconds < 60 -> "0:${seconds.toString().padStart(2, '0')}"
                seconds < 3600 -> {
                    val mins = seconds / 60
                    val secs = seconds % 60
                    "$mins:${secs.toString().padStart(2, '0')}"
                }
                else -> {
                    val hours = seconds / 3600
                    val mins = (seconds % 3600) / 60
                    "$hours:${mins.toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}"
                }
            }
        }

    val formattedDownloaded: String
        get() {
            return when {
                downloadedBytes < 1024 -> "$downloadedBytes B"
                downloadedBytes < 1024 * 1024 ->
                        String.format(Locale.US, "%.1f KB", downloadedBytes / 1024.0)
                downloadedBytes < 1024 * 1024 * 1024 ->
                        String.format(Locale.US, "%.1f MB", downloadedBytes / (1024.0 * 1024))
                else ->
                        String.format(
                                Locale.US,
                                "%.2f GB",
                                downloadedBytes / (1024.0 * 1024 * 1024)
                        )
            }
        }
}
