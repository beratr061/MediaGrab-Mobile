package com.berat.mediagrab.ui.viewmodel

import android.app.Application
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.berat.mediagrab.data.database.DownloadEntity
import com.berat.mediagrab.data.model.MediaInfo
import com.berat.mediagrab.data.repository.DownloadRepository
import com.berat.mediagrab.python.YtdlpWrapper
import com.berat.mediagrab.util.FFmpegExecutor
import com.berat.mediagrab.util.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
        val url: String = "",
        val selectedFormat: String = "video-mp4",
        val selectedQuality: String = "best",
        val isLoading: Boolean = false,
        val isDownloading: Boolean = false,
        val downloadProgress: Float = 0f,
        val downloadSpeed: Long? = null, // bytes per second
        val statusMessage: String? = null,
        val errorMessage: String? = null,
        val mediaInfo: MediaInfo? = null,
        val wifiOnlyEnabled: Boolean = false
)

@HiltViewModel
class HomeViewModel
@Inject
constructor(
        application: Application,
        private val ytdlpWrapper: YtdlpWrapper,
        private val downloadRepository: DownloadRepository
) : AndroidViewModel(application) {

        private val _uiState = MutableStateFlow(HomeUiState())
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        // Recent downloads from database
        val recentDownloads: StateFlow<List<DownloadEntity>> =
                downloadRepository
                        .getRecentDownloads(5)
                        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        private val tag = "HomeViewModel"

        private var currentDownloadId: Long? = null
        private var fetchJob: Job? = null
        private var downloadJob: Job? = null
        private var isCancelled = false

        fun updateUrl(url: String) {
                _uiState.value =
                        _uiState.value.copy(
                                url = url,
                                errorMessage = null,
                                mediaInfo = null // Reset media info when URL changes
                        )

                // Auto-fetch video info when URL looks valid
                if (url.isNotBlank() && (url.contains("http://") || url.contains("https://"))) {
                        fetchVideoInfo(url)
                }
        }

        private fun fetchVideoInfo(url: String) {
                // Cancel previous fetch job if any
                fetchJob?.cancel()

                fetchJob =
                        viewModelScope.launch {
                                // Small delay to avoid fetching on every keystroke
                                kotlinx.coroutines.delay(500)

                                Log.d(tag, "Auto-fetching video info for: $url")
                                _uiState.value = _uiState.value.copy(isLoading = true)

                                try {
                                        val result = ytdlpWrapper.getVideoInfo(url)
                                        result.fold(
                                                onSuccess = { mediaInfo ->
                                                        Log.d(
                                                                tag,
                                                                "Video info fetched: ${mediaInfo.title}, qualities: ${mediaInfo.availableQualities.size}"
                                                        )
                                                        _uiState.value =
                                                                _uiState.value.copy(
                                                                        isLoading = false,
                                                                        mediaInfo = mediaInfo,
                                                                        statusMessage = null
                                                                )
                                                },
                                                onFailure = { error ->
                                                        Log.e(
                                                                tag,
                                                                "Failed to fetch video info",
                                                                error
                                                        )
                                                        _uiState.value =
                                                                _uiState.value.copy(
                                                                        isLoading = false,
                                                                        statusMessage = null
                                                                )
                                                }
                                        )
                                } catch (e: Exception) {
                                        Log.e(tag, "Error fetching video info", e)
                                        _uiState.value = _uiState.value.copy(isLoading = false)
                                }
                        }
        }

        fun updateFormat(format: String) {
                _uiState.value = _uiState.value.copy(selectedFormat = format)
        }

        fun updateQuality(quality: String) {
                _uiState.value = _uiState.value.copy(selectedQuality = quality)
        }

        fun setWifiOnly(enabled: Boolean) {
                _uiState.value = _uiState.value.copy(wifiOnlyEnabled = enabled)
        }

        fun cancelDownload() {
                Log.d(tag, "Cancelling download...")
                isCancelled = true
                downloadJob?.cancel()
                downloadJob = null
                
                currentDownloadId?.let { id ->
                        viewModelScope.launch {
                                downloadRepository.markFailed(id, "İndirme iptal edildi")
                        }
                }
                currentDownloadId = null
                
                _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        isLoading = false,
                        downloadProgress = 0f,
                        statusMessage = "İndirme iptal edildi",
                        errorMessage = null
                )
        }

        fun startDownload() {
                val url = _uiState.value.url
                if (url.isBlank()) return

                // Check network availability
                val context = getApplication<Application>()
                if (!NetworkUtils.isNetworkAvailable(context)) {
                        _uiState.value = _uiState.value.copy(
                                errorMessage = "İnternet bağlantısı yok"
                        )
                        return
                }

                // Check WiFi-only setting
                if (_uiState.value.wifiOnlyEnabled && !NetworkUtils.isWifiConnected(context)) {
                        _uiState.value = _uiState.value.copy(
                                errorMessage = "WiFi bağlantısı gerekli (Ayarlardan değiştirilebilir)"
                        )
                        return
                }

                val format = _uiState.value.selectedFormat
                val quality = _uiState.value.selectedQuality

                // Reset cancellation flag
                isCancelled = false

                downloadJob = viewModelScope.launch {
                        _uiState.value =
                                _uiState.value.copy(
                                        isLoading = true,
                                        isDownloading = false,
                                        downloadProgress = 0f,
                                        errorMessage = null,
                                        statusMessage = "Video bilgisi alınıyor..."
                                )

                        try {
                                // Check if cancelled
                                if (isCancelled) return@launch

                                // First fetch video info
                                Log.d(tag, "Fetching video info for URL")
                                val infoResult = ytdlpWrapper.getVideoInfo(url)

                                infoResult.fold(
                                        onSuccess = { mediaInfo ->
                                                if (isCancelled) return@fold
                                                Log.d(tag, "Video info fetched: ${mediaInfo.title}")
                                                _uiState.value =
                                                        _uiState.value.copy(
                                                                isLoading = false,
                                                                isDownloading = true,
                                                                mediaInfo = mediaInfo,
                                                                statusMessage =
                                                                        "İndiriliyor: ${mediaInfo.title}"
                                                        )

                                                // Create download record in database
                                                val downloadEntity =
                                                        DownloadEntity(
                                                                videoId = mediaInfo.id,
                                                                title = mediaInfo.title,
                                                                url = url,
                                                                thumbnail = mediaInfo.thumbnail,
                                                                duration = mediaInfo.duration,
                                                                uploader = mediaInfo.uploader,
                                                                format = format,
                                                                quality = quality,
                                                                filePath = null,
                                                                fileSize = mediaInfo.filesize,
                                                                status =
                                                                        DownloadEntity
                                                                                .STATUS_DOWNLOADING
                                                        )

                                                currentDownloadId =
                                                        downloadRepository.createDownload(
                                                                downloadEntity
                                                        )
                                                Log.d(
                                                        tag,
                                                        "Download record created: $currentDownloadId"
                                                )

                                                // Get download path
                                                val downloadDir = getDownloadDirectory()
                                                val safeTitle =
                                                        mediaInfo
                                                                .title
                                                                .replace(
                                                                        Regex(
                                                                                "[^a-zA-Z0-9\\-_\\s]"
                                                                        ),
                                                                        ""
                                                                )
                                                                .take(50)
                                                val extension =
                                                        if (format.startsWith("audio")) "mp3"
                                                        else "mp4"
                                                val outputPath =
                                                        File(
                                                                        downloadDir,
                                                                        "${safeTitle}.${extension}"
                                                                )
                                                                .absolutePath

                                                Log.d(
                                                        tag,
                                                        "Starting actual download to: $outputPath"
                                                )

                                                // Determine yt-dlp format string
                                                // Using pre-merged formats only (no ffmpeg needed)
                                                // Android security blocks executing external
                                                // binaries
                                                // 'b' = best pre-merged format with video+audio
                                                // 'bv*' = best video only (no audio) for highest
                                                // quality
                                                // Parse quality to extract height and fps
                                                val qualityMatch =
                                                        Regex("(\\d+)p(\\d+)?").find(quality)
                                                val targetHeight =
                                                        qualityMatch
                                                                ?.groupValues
                                                                ?.get(1)
                                                                ?.toIntOrNull()
                                                @Suppress("UNUSED_VARIABLE")
                                                val targetFps =
                                                        qualityMatch
                                                                ?.groupValues
                                                                ?.get(2)
                                                                ?.toIntOrNull()

                                                // Check if platform has low-quality DASH audio
                                                // (Instagram, TikTok, Twitter, Facebook etc.)
                                                // For these platforms, use hybrid download:
                                                // - Video: from DASH streams (60fps)
                                                // - Audio: from muxed format (good quality)
                                                val extractor =
                                                        mediaInfo.extractor?.lowercase() ?: ""
                                                val useHybridDownload =
                                                        extractor.contains("instagram") ||
                                                                extractor.contains("tiktok") ||
                                                                extractor.contains("twitter") ||
                                                                extractor.contains("facebook") ||
                                                                extractor.contains("fb")

                                                // For video formats: download video+audio
                                                // separately
                                                // then merge for highest quality (including 60fps)
                                                val isVideoFormat =
                                                        !format.startsWith("audio") &&
                                                                !format.startsWith("video-only")

                                                // Result variable for download
                                                var downloadResult: Result<String>

                                                if (isVideoFormat && useHybridDownload) {
                                                        // === HYBRID DOWNLOAD STRATEGY ===
                                                        // For Instagram, TikTok, Twitter, Facebook:
                                                        // - Download 60fps video from DASH streams
                                                        // - Download muxed format for good audio
                                                        // - Merge them with FFmpeg
                                                        Log.d(
                                                                tag,
                                                                "Using hybrid download: 60fps DASH video + muxed audio for $extractor"
                                                        )
                                                        _uiState.value =
                                                                _uiState.value.copy(
                                                                        statusMessage =
                                                                                "60fps video + yüksek kalite ses indiriliyor..."
                                                                )

                                                        // Paths for temporary files
                                                        val videoPath = "${outputPath}_video.mp4"
                                                        val audioPath = "${outputPath}_audio.m4a"

                                                        // Video format: Best quality with FFmpeg
                                                        // (supports all codecs)
                                                        // Prioritize 60fps, then best quality
                                                        val videoFormat =
                                                                when {
                                                                        targetHeight != null ->
                                                                                "bv*[height<=${targetHeight}][fps>=60]/bv*[height<=${targetHeight}]/bv*"
                                                                        quality == "best" ||
                                                                                quality.contains(
                                                                                        "60"
                                                                                ) ->
                                                                                "bv*[fps>=60]/bv*"
                                                                        else -> "bv*"
                                                                }

                                                        Log.d(tag, "Video format: $videoFormat")

                                                        // Download video
                                                        val videoDownload =
                                                                ytdlpWrapper.download(
                                                                        url = url,
                                                                        outputPath = videoPath,
                                                                        format = videoFormat
                                                                ) { progress, _, _, speed ->
                                                                        viewModelScope.launch(
                                                                                kotlinx.coroutines
                                                                                        .Dispatchers
                                                                                        .Main
                                                                                        .immediate
                                                                        ) {
                                                                                _uiState.value =
                                                                                        _uiState.value
                                                                                                .copy(
                                                                                                        downloadProgress =
                                                                                                                progress /
                                                                                                                        250f, // 0-40% for video
                                                                                                        downloadSpeed =
                                                                                                                speed,
                                                                                                        statusMessage =
                                                                                                                "Video indiriliyor... ${progress.toInt()}%"
                                                                                                )
                                                                        }
                                                                        currentDownloadId?.let { id
                                                                                ->
                                                                                viewModelScope
                                                                                        .launch {
                                                                                                downloadRepository
                                                                                                        .updateProgress(
                                                                                                                id,
                                                                                                                progress /
                                                                                                                        250f
                                                                                                        )
                                                                                        }
                                                                        }
                                                                }

                                                        if (videoDownload.isSuccess) {
                                                                // Download audio source
                                                                // For hybrid download: use muxed
                                                                // format
                                                                // which has better audio quality
                                                                val audioFormat =
                                                                        if (useHybridDownload) {
                                                                                "b[height<=1920]/b" // Best muxed format
                                                                        } else {
                                                                                "ba[ext=m4a][abr>=192]/ba[ext=m4a]/ba[abr>=192]/ba"
                                                                        }
                                                                val audioDownload =
                                                                        ytdlpWrapper.download(
                                                                                url = url,
                                                                                outputPath =
                                                                                        audioPath,
                                                                                format = audioFormat
                                                                        ) { progress, _, _, speed ->
                                                                                viewModelScope
                                                                                        .launch(
                                                                                                kotlinx.coroutines
                                                                                                        .Dispatchers
                                                                                                        .Main
                                                                                                        .immediate
                                                                                        ) {
                                                                                                _uiState.value =
                                                                                                        _uiState.value
                                                                                                                .copy(
                                                                                                                        downloadProgress =
                                                                                                                                0.40f +
                                                                                                                                        progress /
                                                                                                                                                333f, // 40-70% for audio
                                                                                                                        downloadSpeed =
                                                                                                                                speed,
                                                                                                                        statusMessage =
                                                                                                                                "Ses indiriliyor... ${progress.toInt()}%"
                                                                                                                )
                                                                                        }
                                                                        }

                                                                if (audioDownload.isSuccess) {
                                                                        // Merge with FFmpegExecutor
                                                                        // (bundled FFmpeg binary)
                                                                        _uiState.value =
                                                                                _uiState.value.copy(
                                                                                        downloadProgress =
                                                                                                0.70f,
                                                                                        statusMessage =
                                                                                                "Video ve ses birleştiriliyor..."
                                                                                )

                                                                        // Initialize FFmpeg if not
                                                                        // already done
                                                                        if (!FFmpegExecutor
                                                                                        .isAvailable()
                                                                        ) {
                                                                                FFmpegExecutor
                                                                                        .initialize(
                                                                                                getApplication<
                                                                                                        Application>()
                                                                                        )
                                                                        }

                                                                        val mergeResult =
                                                                                FFmpegExecutor
                                                                                        .mergeVideoAudio(
                                                                                                videoPath =
                                                                                                        videoPath,
                                                                                                audioPath =
                                                                                                        audioPath,
                                                                                                outputPath =
                                                                                                        outputPath,
                                                                                                onProgress = {
                                                                                                        mergeProgress:
                                                                                                                Float
                                                                                                        ->
                                                                                                        viewModelScope
                                                                                                                .launch(
                                                                                                                        kotlinx.coroutines
                                                                                                                                .Dispatchers
                                                                                                                                .Main
                                                                                                                                .immediate
                                                                                                                ) {
                                                                                                                        _uiState.value =
                                                                                                                                _uiState.value
                                                                                                                                        .copy(
                                                                                                                                                downloadProgress =
                                                                                                                                                        0.70f +
                                                                                                                                                                (mergeProgress *
                                                                                                                                                                        0.30f) // 70-100% for merge
                                                                                                                                        )
                                                                                                                }
                                                                                                },
                                                                                                context =
                                                                                                        getApplication<Application>()
                                                                                        )

                                                                        // Cleanup temp files
                                                                        FFmpegExecutor
                                                                                .cleanupTempFiles(
                                                                                        videoPath,
                                                                                        audioPath
                                                                                )

                                                                        if (mergeResult.isSuccess) {
                                                                                downloadResult =
                                                                                        Result.success(
                                                                                                outputPath
                                                                                        )
                                                                                Log.d(
                                                                                        tag,
                                                                                        "Merge successful: $outputPath"
                                                                                )
                                                                        } else {
                                                                                downloadResult =
                                                                                        Result.failure(
                                                                                                mergeResult
                                                                                                        .exceptionOrNull()
                                                                                                        ?: Exception(
                                                                                                                "Merge failed"
                                                                                                        )
                                                                                        )
                                                                                Log.e(
                                                                                        tag,
                                                                                        "Merge failed",
                                                                                        mergeResult
                                                                                                .exceptionOrNull()
                                                                                )
                                                                        }
                                                                } else {
                                                                        // Audio download failed,
                                                                        // cleanup video
                                                                        FFmpegExecutor
                                                                                .cleanupTempFiles(
                                                                                        videoPath
                                                                                )
                                                                        downloadResult =
                                                                                Result.failure(
                                                                                        audioDownload
                                                                                                .exceptionOrNull()
                                                                                                ?: Exception(
                                                                                                        "Audio download failed"
                                                                                                )
                                                                                )
                                                                }
                                                        } else {
                                                                // Video download failed
                                                                Log.e(
                                                                        tag,
                                                                        "Video download failed",
                                                                        videoDownload
                                                                                .exceptionOrNull()
                                                                )
                                                                downloadResult =
                                                                        Result.failure(
                                                                                videoDownload
                                                                                        .exceptionOrNull()
                                                                                        ?: Exception(
                                                                                                "Video download failed"
                                                                                        )
                                                                        )
                                                        }
                                                } else if (isVideoFormat && !useHybridDownload) {
                                                        // === YOUTUBE/Standard platforms ===
                                                        // These platforms have good DASH audio
                                                        // Download video+audio separately and merge
                                                        Log.d(
                                                                tag,
                                                                "Using standard separate download for $extractor"
                                                        )
                                                        _uiState.value =
                                                                _uiState.value.copy(
                                                                        statusMessage =
                                                                                "En yüksek kalite için video+audio ayrı indiriliyor..."
                                                                )

                                                        // Paths for temporary files
                                                        val videoPath = "${outputPath}_video.mp4"
                                                        val audioPath = "${outputPath}_audio.m4a"

                                                        // Video format: Best quality
                                                        val videoFormat =
                                                                when {
                                                                        targetHeight != null ->
                                                                                "bv*[height<=${targetHeight}][fps>=60]/bv*[height<=${targetHeight}]/bv*"
                                                                        quality == "best" ||
                                                                                quality.contains(
                                                                                        "60"
                                                                                ) ->
                                                                                "bv*[fps>=60]/bv*"
                                                                        else -> "bv*"
                                                                }

                                                        Log.d(
                                                                tag,
                                                                "Video format: $videoFormat (standard)"
                                                        )

                                                        // Download video
                                                        val videoDownload =
                                                                ytdlpWrapper.download(
                                                                        url = url,
                                                                        outputPath = videoPath,
                                                                        format = videoFormat
                                                                ) { progress, _, _, _ ->
                                                                        viewModelScope.launch(
                                                                                kotlinx.coroutines
                                                                                        .Dispatchers
                                                                                        .Main
                                                                                        .immediate
                                                                        ) {
                                                                                _uiState.value =
                                                                                        _uiState.value
                                                                                                .copy(
                                                                                                        downloadProgress =
                                                                                                                progress /
                                                                                                                        250f,
                                                                                                        statusMessage =
                                                                                                                "Video indiriliyor... ${progress.toInt()}%"
                                                                                                )
                                                                        }
                                                                        currentDownloadId?.let { id
                                                                                ->
                                                                                viewModelScope
                                                                                        .launch {
                                                                                                downloadRepository
                                                                                                        .updateProgress(
                                                                                                                id,
                                                                                                                progress /
                                                                                                                        250f
                                                                                                        )
                                                                                        }
                                                                        }
                                                                }

                                                        if (videoDownload.isSuccess) {
                                                                // Download audio (good quality DASH
                                                                // audio for YouTube etc.)
                                                                val audioDownload =
                                                                        ytdlpWrapper.download(
                                                                                url = url,
                                                                                outputPath =
                                                                                        audioPath,
                                                                                format =
                                                                                        "ba[ext=m4a][abr>=192]/ba[ext=m4a]/ba[abr>=192]/ba"
                                                                        ) { progress, _, _, _ ->
                                                                                viewModelScope
                                                                                        .launch(
                                                                                                kotlinx.coroutines
                                                                                                        .Dispatchers
                                                                                                        .Main
                                                                                                        .immediate
                                                                                        ) {
                                                                                                _uiState.value =
                                                                                                        _uiState.value
                                                                                                                .copy(
                                                                                                                        downloadProgress =
                                                                                                                                0.40f +
                                                                                                                                        progress /
                                                                                                                                                500f,
                                                                                                                        statusMessage =
                                                                                                                                "Ses indiriliyor... ${progress.toInt()}%"
                                                                                                                )
                                                                                        }
                                                                                currentDownloadId
                                                                                        ?.let { id
                                                                                                ->
                                                                                                viewModelScope
                                                                                                        .launch {
                                                                                                                downloadRepository
                                                                                                                        .updateProgress(
                                                                                                                                id,
                                                                                                                                0.40f +
                                                                                                                                        progress /
                                                                                                                                                500f
                                                                                                                        )
                                                                                                        }
                                                                                        }
                                                                        }

                                                                if (audioDownload.isSuccess) {
                                                                        // Merge
                                                                        _uiState.value =
                                                                                _uiState.value.copy(
                                                                                        downloadProgress =
                                                                                                0.60f,
                                                                                        statusMessage =
                                                                                                "Video ve ses birleştiriliyor..."
                                                                                )

                                                                        FFmpegExecutor.initialize(
                                                                                getApplication<
                                                                                        Application>()
                                                                        )
                                                                        val mergeResult =
                                                                                FFmpegExecutor
                                                                                        .mergeVideoAudio(
                                                                                                videoPath,
                                                                                                audioPath,
                                                                                                outputPath,
                                                                                                context = getApplication<Application>()
                                                                                        )

                                                                        if (mergeResult.isSuccess) {
                                                                                _uiState.value =
                                                                                        _uiState.value
                                                                                                .copy(
                                                                                                        downloadProgress =
                                                                                                                1f
                                                                                                )
                                                                                viewModelScope
                                                                                        .launch(
                                                                                                kotlinx.coroutines
                                                                                                        .Dispatchers
                                                                                                        .Main
                                                                                                        .immediate
                                                                                        ) {
                                                                                                File(
                                                                                                                videoPath
                                                                                                        )
                                                                                                        .delete()
                                                                                                        .also {
                                                                                                                Log.d(
                                                                                                                        tag,
                                                                                                                        "Deleted temp video: $it"
                                                                                                                )
                                                                                                        }
                                                                                                File(
                                                                                                                audioPath
                                                                                                        )
                                                                                                        .delete()
                                                                                                        .also {
                                                                                                                Log.d(
                                                                                                                        tag,
                                                                                                                        "Deleted temp audio: $it"
                                                                                                                )
                                                                                                        }
                                                                                        }
                                                                                downloadResult =
                                                                                        Result.success(
                                                                                                outputPath
                                                                                        )
                                                                        } else {
                                                                                downloadResult =
                                                                                        Result.failure(
                                                                                                mergeResult
                                                                                                        .exceptionOrNull()
                                                                                                        ?: Exception(
                                                                                                                "Merge failed"
                                                                                                        )
                                                                                        )
                                                                        }
                                                                } else {
                                                                        downloadResult =
                                                                                Result.failure(
                                                                                        audioDownload
                                                                                                .exceptionOrNull()
                                                                                                ?: Exception(
                                                                                                        "Audio download failed"
                                                                                                )
                                                                                )
                                                                }
                                                        } else {
                                                                downloadResult =
                                                                        Result.failure(
                                                                                videoDownload
                                                                                        .exceptionOrNull()
                                                                                        ?: Exception(
                                                                                                "Video download failed"
                                                                                        )
                                                                        )
                                                        }
                                                } else {
                                                        // === AUDIO-ONLY or VIDEO-ONLY: Direct
                                                        // download ===
                                                        val ytdlpFormat =
                                                                when {
                                                                        format.startsWith(
                                                                                "audio"
                                                                        ) ->
                                                                                "ba[ext=m4a]/ba[ext=mp3]/ba/b"
                                                                        format.startsWith(
                                                                                "video-only"
                                                                        ) ->
                                                                                "bv*[vcodec^=avc1]/bv*[vcodec^=vp9]/bv*"
                                                                        else -> "b/w" // Fallback
                                                                }

                                                        Log.d(
                                                                tag,
                                                                "Direct download with format: $ytdlpFormat"
                                                        )

                                                        downloadResult =
                                                                ytdlpWrapper.download(
                                                                        url = url,
                                                                        outputPath = outputPath,
                                                                        format = ytdlpFormat
                                                                ) { progress, _, _, speed ->
                                                                        viewModelScope.launch(
                                                                                kotlinx.coroutines
                                                                                        .Dispatchers
                                                                                        .Main
                                                                                        .immediate
                                                                        ) {
                                                                                _uiState.value =
                                                                                        _uiState.value
                                                                                                .copy(
                                                                                                        downloadProgress =
                                                                                                                progress /
                                                                                                                        100f,
                                                                                                        downloadSpeed =
                                                                                                                speed
                                                                                                )
                                                                        }
                                                                        currentDownloadId?.let { id
                                                                                ->
                                                                                viewModelScope
                                                                                        .launch {
                                                                                                downloadRepository
                                                                                                        .updateProgress(
                                                                                                                id,
                                                                                                                progress /
                                                                                                                        100f
                                                                                                        )
                                                                                        }
                                                                        }
                                                                }
                                                }

                                                downloadResult.fold(
                                                        onSuccess = { filePath ->
                                                                Log.d(
                                                                        tag,
                                                                        "Download completed: $filePath"
                                                                )
                                                                currentDownloadId?.let { id ->
                                                                        downloadRepository
                                                                                .markCompleted(
                                                                                        id,
                                                                                        filePath
                                                                                )
                                                                }

                                                                // Scan file so it appears in device
                                                                // storage
                                                                android.media.MediaScannerConnection
                                                                        .scanFile(
                                                                                getApplication(),
                                                                                arrayOf(filePath),
                                                                                arrayOf("video/mp4")
                                                                        ) { path, uri ->
                                                                                Log.d(
                                                                                        tag,
                                                                                        "MediaScanner scanned: $path -> $uri"
                                                                                )
                                                                        }

                                                                _uiState.value =
                                                                        _uiState.value.copy(
                                                                                isDownloading =
                                                                                        false,
                                                                                downloadProgress =
                                                                                        1f,
                                                                                statusMessage =
                                                                                        "İndirme tamamlandı!",
                                                                                url = "" // Clear
                                                                                // URL
                                                                                // after
                                                                                // successful
                                                                                // download
                                                                                )
                                                        },
                                                        onFailure = { error ->
                                                                Log.e(tag, "Download failed", error)
                                                                currentDownloadId?.let { id ->
                                                                        downloadRepository
                                                                                .markFailed(
                                                                                        id,
                                                                                        error.message
                                                                                                ?: "Bilinmeyen hata"
                                                                                )
                                                                }
                                                                _uiState.value =
                                                                        _uiState.value.copy(
                                                                                isDownloading =
                                                                                        false,
                                                                                errorMessage =
                                                                                        "İndirme başarısız: ${error.message}"
                                                                        )
                                                        }
                                                )
                                        },
                                        onFailure = { error ->
                                                Log.e(tag, "Failed to get video info", error)
                                                _uiState.value =
                                                        _uiState.value.copy(
                                                                isLoading = false,
                                                                errorMessage =
                                                                        "Video bilgisi alınamadı: ${error.message}"
                                                        )
                                        }
                                )
                        } catch (e: Exception) {
                                Log.e(tag, "Exception during download", e)
                                _uiState.value =
                                        _uiState.value.copy(
                                                isLoading = false,
                                                isDownloading = false,
                                                errorMessage = "Hata: ${e.message}"
                                        )
                        }
                }
        }

        private fun getDownloadDirectory(): File {
                val downloadDir =
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS
                        )
                val mediaGrabDir = File(downloadDir, "MediaGrab")
                if (!mediaGrabDir.exists()) {
                        mediaGrabDir.mkdirs()
                }
                return mediaGrabDir
        }

        @Suppress("unused")
        fun clearError() {
                _uiState.value = _uiState.value.copy(errorMessage = null)
        }

        @Suppress("unused")
        fun clearStatus() {
                _uiState.value = _uiState.value.copy(statusMessage = null)
        }

        fun deleteDownload(downloadId: Long) {
                viewModelScope.launch { downloadRepository.deleteDownload(downloadId) }
        }
}
