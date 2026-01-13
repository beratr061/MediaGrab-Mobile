package com.berat.mediagrab.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DownloadService : Service() {

    companion object {
        private const val TAG = "DownloadService"
        const val CHANNEL_ID = "download_channel"
        const val NOTIFICATION_ID = 1

        // Actions
        const val ACTION_START_DOWNLOAD = "com.berat.mediagrab.START_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.berat.mediagrab.CANCEL_DOWNLOAD"
        const val ACTION_CANCEL_ALL = "com.berat.mediagrab.CANCEL_ALL"

        // Extras
        const val EXTRA_DOWNLOAD_ID = "download_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_PROGRESS = "progress"

        // Active downloads tracking
        private val activeDownloads = ConcurrentHashMap<Long, DownloadState>()

        fun isDownloadActive(downloadId: Long): Boolean = activeDownloads.containsKey(downloadId)

        fun getActiveDownloadCount(): Int = activeDownloads.size

        fun startDownload(context: Context, downloadId: Long, title: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                putExtra(EXTRA_TITLE, title)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancelDownload(context: Context, downloadId: Long) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startService(intent)
        }

        fun cancelAllDownloads(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL_ALL
            }
            context.startService(intent)
        }
    }

    data class DownloadState(
        val downloadId: Long,
        val title: String,
        var progress: Float = 0f,
        var job: Job? = null,
        var isCancelled: Boolean = false
    )

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
        Log.d(TAG, "DownloadService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "İndirme"
                if (downloadId != -1L) {
                    startDownloadInternal(downloadId, title)
                }
            }
            ACTION_CANCEL_DOWNLOAD -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
                if (downloadId != -1L) {
                    cancelDownloadInternal(downloadId)
                }
            }
            ACTION_CANCEL_ALL -> {
                cancelAllDownloadsInternal()
            }
        }

        // Start foreground with current state notification
        startForeground(NOTIFICATION_ID, createNotification())

        // Stop service if no active downloads
        if (activeDownloads.isEmpty()) {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun startDownloadInternal(downloadId: Long, title: String) {
        if (activeDownloads.containsKey(downloadId)) {
            Log.w(TAG, "Download $downloadId already active")
            return
        }

        val state = DownloadState(downloadId, title)
        activeDownloads[downloadId] = state

        Log.d(TAG, "Starting download: $downloadId - $title")
        updateNotification()

        // Note: Actual download logic is in HomeViewModel
        // This service just manages foreground state and cancellation
    }

    private fun cancelDownloadInternal(downloadId: Long) {
        val state = activeDownloads[downloadId]
        if (state != null) {
            Log.d(TAG, "Cancelling download: $downloadId")
            state.isCancelled = true
            state.job?.cancel()
            activeDownloads.remove(downloadId)
            updateNotification()

            if (activeDownloads.isEmpty()) {
                stopSelf()
            }
        }
    }

    private fun cancelAllDownloadsInternal() {
        Log.d(TAG, "Cancelling all downloads")
        activeDownloads.values.forEach { state ->
            state.isCancelled = true
            state.job?.cancel()
        }
        activeDownloads.clear()
        stopSelf()
    }

    fun updateProgress(downloadId: Long, progress: Float) {
        activeDownloads[downloadId]?.let { state ->
            state.progress = progress
            updateNotification()
        }
    }

    fun completeDownload(downloadId: Long) {
        activeDownloads.remove(downloadId)
        updateNotification()
        if (activeDownloads.isEmpty()) {
            stopSelf()
        }
    }

    fun failDownload(downloadId: Long) {
        activeDownloads.remove(downloadId)
        updateNotification()
        if (activeDownloads.isEmpty()) {
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        activeDownloads.clear()
        Log.d(TAG, "DownloadService destroyed")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "İndirmeler",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Video indirme bildirimleri"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun createNotification(): Notification {
        val activeCount = activeDownloads.size
        val (title, text, progress) = when {
            activeCount == 0 -> Triple("MediaGrab", "İndirme tamamlandı", -1)
            activeCount == 1 -> {
                val state = activeDownloads.values.first()
                Triple(
                    state.title.take(30),
                    "İndiriliyor... ${(state.progress * 100).toInt()}%",
                    (state.progress * 100).toInt()
                )
            }
            else -> {
                val avgProgress = activeDownloads.values.map { it.progress }.average()
                Triple(
                    "$activeCount indirme aktif",
                    "İndiriliyor... ${(avgProgress * 100).toInt()}%",
                    (avgProgress * 100).toInt()
                )
            }
        }

        val cancelIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_CANCEL_ALL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this, 0, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(activeCount > 0)
            .setOnlyAlertOnce(true)
            .apply {
                if (progress >= 0) {
                    setProgress(100, progress, false)
                }
                if (activeCount > 0) {
                    addAction(
                        android.R.drawable.ic_delete,
                        "İptal",
                        cancelPendingIntent
                    )
                }
            }
            .build()
    }
}
