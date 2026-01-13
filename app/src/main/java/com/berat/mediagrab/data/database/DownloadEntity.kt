package com.berat.mediagrab.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "downloads")
data class DownloadEntity(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val videoId: String,
        val title: String,
        val url: String,
        val thumbnail: String?,
        val duration: Double?,
        val uploader: String?,
        val format: String,
        val quality: String,
        val filePath: String?,
        val fileSize: Long?,
        val status: String, // pending, downloading, completed, failed
        val progress: Float = 0f,
        val errorMessage: String? = null,
        val createdAt: Long = System.currentTimeMillis(),
        val completedAt: Long? = null
) {
    companion object {
        @Suppress("unused") const val STATUS_PENDING = "pending"
        const val STATUS_DOWNLOADING = "downloading"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_FAILED = "failed"
    }

    val isCompleted: Boolean
        get() = status == STATUS_COMPLETED

    val isFailed: Boolean
        get() = status == STATUS_FAILED

    val isDownloading: Boolean
        get() = status == STATUS_DOWNLOADING

    val formattedDuration: String
        get() {
            val d = duration ?: return ""
            val hours = (d / 3600).toInt()
            val minutes = ((d % 3600) / 60).toInt()
            val seconds = (d % 60).toInt()
            return if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%d:%02d", minutes, seconds)
            }
        }
}
