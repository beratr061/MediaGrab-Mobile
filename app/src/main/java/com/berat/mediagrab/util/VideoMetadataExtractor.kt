package com.berat.mediagrab.util

import android.media.MediaMetadataRetriever
import java.io.File

/** Video metadata extracted from file */
data class VideoMetadata(
        val width: Int?,
        val height: Int?,
        val duration: Long?, // in milliseconds
        val bitrate: Int?,
        val frameRate: Float?,
        val rotation: Int?,
        val mimeType: String?,
        val fileSize: Long
) {
    val resolution: String
        get() = if (width != null && height != null) "${width}x${height}" else "Bilinmiyor"

    val formattedBitrate: String
        get() =
                when {
                    bitrate == null -> "Bilinmiyor"
                    bitrate >= 1_000_000 -> String.format("%.1f Mbps", bitrate / 1_000_000.0)
                    bitrate >= 1_000 -> String.format("%.0f Kbps", bitrate / 1_000.0)
                    else -> "$bitrate bps"
                }

    val formattedFps: String
        get() =
                if (frameRate != null && frameRate > 0) String.format("%.1f fps", frameRate)
                else "Bilinmiyor"

    val formattedDuration: String
        get() {
            val d = duration ?: return "Bilinmiyor"
            val totalSeconds = d / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }

    val formattedFileSize: String
        get() =
                when {
                    fileSize >= 1_073_741_824 ->
                            String.format("%.2f GB", fileSize / 1_073_741_824.0)
                    fileSize >= 1_048_576 -> String.format("%.1f MB", fileSize / 1_048_576.0)
                    fileSize >= 1024 -> String.format("%.1f KB", fileSize / 1024.0)
                    else -> "$fileSize B"
                }
}

/** Utility class to extract metadata from video files */
object VideoMetadataExtractor {

    /** Extract metadata from a video file */
    fun extractMetadata(filePath: String): VideoMetadata? {
        val file = File(filePath)
        if (!file.exists()) return null

        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(filePath)

            val width =
                    retriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                            ?.toIntOrNull()
            val height =
                    retriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                            ?.toIntOrNull()
            val duration =
                    retriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLongOrNull()
            val bitrate =
                    retriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                            ?.toIntOrNull()
            val rotation =
                    retriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                            ?.toIntOrNull()
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)

            // Frame rate - may not be available on all devices
            val frameRate =
                    try {
                        retriever
                                .extractMetadata(
                                        MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE
                                )
                                ?.toFloatOrNull()
                                ?: retriever.extractMetadata(
                                                MediaMetadataRetriever
                                                        .METADATA_KEY_VIDEO_FRAME_COUNT
                                        )
                                        ?.let { frameCount ->
                                            if (duration != null && duration > 0) {
                                                (frameCount.toFloat() / (duration / 1000f))
                                            } else null
                                        }
                    } catch (e: Exception) {
                        null
                    }

            VideoMetadata(
                    width = width,
                    height = height,
                    duration = duration,
                    bitrate = bitrate,
                    frameRate = frameRate,
                    rotation = rotation,
                    mimeType = mimeType,
                    fileSize = file.length()
            )
        } catch (e: Exception) {
            // If extraction fails, return basic info
            VideoMetadata(
                    width = null,
                    height = null,
                    duration = null,
                    bitrate = null,
                    frameRate = null,
                    rotation = null,
                    mimeType = null,
                    fileSize = file.length()
            )
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore release errors
            }
        }
    }
}
