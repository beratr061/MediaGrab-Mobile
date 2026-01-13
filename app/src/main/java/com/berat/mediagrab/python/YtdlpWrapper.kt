package com.berat.mediagrab.python

import android.util.Log
import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.berat.mediagrab.data.model.MediaInfo
import com.berat.mediagrab.data.model.VideoQuality
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Wrapper class for yt-dlp Python library Uses Chaquopy to execute Python code on Android */
@Singleton
class YtdlpWrapper @Inject constructor() {

    companion object {
        private const val TAG = "YtdlpWrapper"
    }
    private val py: Python by lazy { Python.getInstance() }
    private val ytdlp: PyObject by lazy { py.getModule("yt_dlp") }

    /** Extract video information from URL */
    suspend fun getVideoInfo(url: String): Result<MediaInfo> =
            withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "Getting video info...")

                    // Create options dict with Android-safe settings
                    // IMPORTANT: Disable all features that use subprocess on Android
                    // NOTE: SSL certificate verification is disabled for compatibility with
                    // various video platforms. This is a known trade-off for functionality.
                    // In production, consider implementing certificate pinning for sensitive operations.
                    val ydlOpts =
                            createOptionsDict(
                                    "quiet" to true,
                                    "no_warnings" to true,
                                    "extract_flat" to false,
                                    "no_color" to true,
                                    "socket_timeout" to 30,
                                    // SSL certificate verification disabled for platform compatibility
                                    // WARNING: This reduces security but is required for some platforms
                                    "nocheckcertificate" to true,
                                    "prefer_insecure" to true,
                                    "no_check_certificate" to true,
                                    // Disable features that cause subprocess issues on Android
                                    "noprogress" to true,
                                    // Disable post-processing (ffmpeg uses subprocess)
                                    "postprocessors" to py.builtins.callAttr("list"),
                                    // Use internal HTTP downloader only
                                    "hls_prefer_native" to true,
                                    // Disable geo bypass which may cause issues
                                    "geo_bypass" to false
                            )

                    val ydl = ytdlp.callAttr("YoutubeDL", ydlOpts)
                    val info = ydl.callAttr("extract_info", url, false) // false = don't download

                    if (info == null || info.toString() == "None") {
                        return@withContext Result.failure(Exception("Video bilgisi alınamadı"))
                    }

                    // Parse available formats/qualities
                    val availableQualities = mutableListOf<VideoQuality>()
                    try {
                        // Use callAttr("get", ...) for proper Python dict access
                        var formats = info.callAttr("get", "formats")
                        Log.d(
                                TAG,
                                "Formats object type: ${formats?.javaClass?.name}, value: ${formats?.toString()?.take(100)}"
                        )

                        // If formats is None or null, try requested_formats
                        if (formats == null || formats.toString() == "None") {
                            formats = info.callAttr("get", "requested_formats")
                            Log.d(
                                    TAG,
                                    "Trying requested_formats: ${formats?.toString()?.take(100)}"
                            )
                        }

                        if (formats != null && formats.toString() != "None") {
                            val formatList = formats.asList()
                            Log.d(TAG, "Format list size: ${formatList.size}")
                            val seenQualities =
                                    mutableSetOf<String>() // Track height+fps combinations
                            val seenBitrates = mutableSetOf<Int>()

                            for (fmt in formatList) {
                                val formatId = safeGetString(fmt, "format_id") ?: continue
                                val ext = safeGetString(fmt, "ext") ?: "mp4"
                                val height = safeGetInt(fmt, "height")
                                val fps =
                                        safeGetInt(fmt, "fps") ?: safeGetFloat(fmt, "fps")?.toInt()
                                val vcodec = safeGetString(fmt, "vcodec")
                                val acodec = safeGetString(fmt, "acodec")
                                val abr = safeGetInt(fmt, "abr") // audio bitrate
                                val filesize =
                                        safeGetLong(fmt, "filesize")
                                                ?: safeGetLong(fmt, "filesize_approx")

                                Log.d(
                                        TAG,
                                        "Format: id=$formatId, height=$height, fps=$fps, vcodec=$vcodec, acodec=$acodec"
                                )

                                val hasVideo = vcodec != null && vcodec != "none"
                                val hasAudio = acodec != null && acodec != "none"

                                // Video quality (with or without audio)
                                // Track by height only (take first fps found for each height,
                                // usually highest quality)
                                if (hasVideo && height != null && height > 0) {
                                    val qualityKey = height.toString()
                                    if (!seenQualities.contains(qualityKey)) {
                                        seenQualities.add(qualityKey)
                                        // Create label with fps if available and > 30
                                        val label =
                                                if (fps != null && fps > 30) {
                                                    "${height}p${fps}"
                                                } else {
                                                    "${height}p"
                                                }
                                        availableQualities.add(
                                                VideoQuality(
                                                        formatId = formatId,
                                                        label = label,
                                                        height = height,
                                                        fps = fps,
                                                        filesize = filesize,
                                                        isVideo = true,
                                                        hasAudio = hasAudio,
                                                        ext = ext
                                                )
                                        )
                                        Log.d(TAG, "Added video quality: $label (fps=$fps)")
                                    }
                                }

                                // Audio only quality
                                if (!hasVideo &&
                                                hasAudio &&
                                                abr != null &&
                                                abr > 0 &&
                                                !seenBitrates.contains(abr)
                                ) {
                                    seenBitrates.add(abr)
                                    availableQualities.add(
                                            VideoQuality(
                                                    formatId = formatId,
                                                    label = "${abr}kbps",
                                                    bitrate = abr,
                                                    filesize = filesize,
                                                    isVideo = false,
                                                    hasAudio = true,
                                                    ext = ext
                                            )
                                    )
                                    Log.d(TAG, "Added audio quality: ${abr}kbps")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Could not parse formats", e)
                    }

                    // Sort: video by height desc, audio by bitrate desc
                    val sortedQualities =
                            availableQualities.sortedWith(
                                    compareByDescending<VideoQuality> { it.isVideo }
                                            .thenByDescending { it.height ?: 0 }
                                            .thenByDescending { it.bitrate ?: 0 }
                            )

                    val mediaInfo =
                            MediaInfo(
                                    id = safeGetString(info, "id") ?: url.hashCode().toString(),
                                    title = safeGetString(info, "title") ?: "Bilinmeyen Video",
                                    url = url,
                                    thumbnail = safeGetString(info, "thumbnail"),
                                    duration = safeGetDouble(info, "duration"),
                                    uploader = safeGetString(info, "uploader")
                                                    ?: safeGetString(info, "channel"),
                                    uploaderUrl = safeGetString(info, "uploader_url")
                                                    ?: safeGetString(info, "channel_url"),
                                    description = safeGetString(info, "description"),
                                    viewCount = safeGetLong(info, "view_count"),
                                    likeCount = safeGetLong(info, "like_count"),
                                    uploadDate = safeGetString(info, "upload_date"),
                                    filesize = safeGetLong(info, "filesize_approx")
                                                    ?: safeGetLong(info, "filesize"),
                                    extractor = safeGetString(info, "extractor")
                                                    ?: safeGetString(info, "extractor_key"),
                                    availableQualities = sortedQualities
                            )

                    Log.d(
                            TAG,
                            "Video info extracted: ${mediaInfo.title}, qualities: ${sortedQualities.size}"
                    )
                    Result.success(mediaInfo)
                } catch (e: PyException) {
                    Log.e(TAG, "Python error getting video info", e)
                    Result.failure(Exception("Python hatası: ${e.message}"))
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting video info", e)
                    Result.failure(e)
                }
            }

    /** Download video/audio from URL with retry logic and progress tracking */
    suspend fun download(
            url: String,
            outputPath: String,
            format: String,
            onProgress: (Float, Long, Long?, Long?) -> Unit // progress, downloaded, total, speed
    ): Result<String> =
            withContext(Dispatchers.IO) {
                val maxRetries = 3
                var lastException: Exception? = null

                repeat(maxRetries) { attempt ->
                    try {
                        Log.d(
                                TAG,
                                "Starting download attempt ${attempt + 1}/$maxRetries"
                        )

                        // Import progress hook module
                        val progressHookModule = py.getModule("progress_hook")
                        val progressHookClass = progressHookModule["ProgressHook"]
                        val progressHook = progressHookClass!!.call()

                        // Create progress hooks list
                        val progressHooksList = py.builtins.callAttr("list")
                        progressHooksList.callAttr("append", progressHook)

                        val ydlOpts =
                                createOptionsDict(
                                        "format" to format,
                                        "outtmpl" to outputPath,
                                        "no_warnings" to true,
                                        "no_color" to true,
                                        "nocheckcertificate" to true,
                                        "ignoreerrors" to false,
                                        "retries" to 10,
                                        "fragment_retries" to 10,
                                        "socket_timeout" to 30,
                                        "http_chunk_size" to 10485760, // 10MB chunks
                                        "extractor_retries" to 5,
                                        "file_access_retries" to 5,
                                        // Progress hooks for tracking
                                        "progress_hooks" to progressHooksList,
                                        // Android-safe settings (no ffmpeg - using pre-merged
                                        // formats)
                                        "noprogress" to false, // Allow progress
                                        "hls_prefer_native" to true,
                                        "geo_bypass" to false,
                                        "prefer_insecure" to true
                                )

                        val ydl = ytdlp.callAttr("YoutubeDL", ydlOpts)

                        // Create Python list properly
                        val pyList = py.builtins.callAttr("list")
                        pyList.callAttr("append", url)

                        // Start download in a separate thread and poll for progress
                        var downloadComplete = false
                        var downloadError: Exception? = null

                        val downloadThread = Thread {
                            try {
                                ydl.callAttr("download", pyList)
                                downloadComplete = true
                            } catch (e: Exception) {
                                downloadError = e
                            }
                        }
                        downloadThread.start()

                        // Track max progress to prevent backwards jumps (HLS estimates change)
                        var maxProgress = 0f

                        // Poll for progress while download is running
                        while (!downloadComplete && downloadError == null) {
                            try {
                                val progress = progressHook.callAttr("get_progress").toFloat()
                                // Python may return float, so convert via Double first
                                val downloaded =
                                        progressHook.callAttr("get_downloaded").toDouble().toLong()
                                val total = progressHook.callAttr("get_total").toDouble().toLong()
                                val speed = progressHook.callAttr("get_speed").toDouble().toLong()

                                // Only update if progress increased (HLS estimates can cause jumps)
                                if (progress > maxProgress) {
                                    maxProgress = progress
                                    Log.d(
                                            TAG,
                                            "Progress poll: $progress%, downloaded: $downloaded, total: $total, speed: $speed"
                                    )

                                    onProgress(
                                            progress,
                                            downloaded,
                                            if (total > 0) total else null,
                                            if (speed > 0) speed else null
                                    )
                                } else if (speed > 0) {
                                    // Still update speed even if progress didn't change
                                    onProgress(
                                            maxProgress,
                                            downloaded,
                                            if (total > 0) total else null,
                                            speed
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Progress read error", e)
                            }
                            kotlinx.coroutines.delay(500) // Poll every 500ms for balanced UI responsiveness
                        }

                        // Wait for download thread to finish
                        downloadThread.join()

                        downloadError?.let { throw it }

                        // Final progress update
                        onProgress(100f, 0, null, null)

                        Log.d(TAG, "Download completed: $outputPath")
                        return@withContext Result.success(outputPath)
                    } catch (e: PyException) {
                        Log.e(TAG, "Python error during download attempt ${attempt + 1}", e)
                        lastException = Exception("İndirme hatası: ${e.message}")
                        if (attempt < maxRetries - 1) {
                            kotlinx.coroutines.delay(2000)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during download attempt ${attempt + 1}", e)
                        lastException = e
                        if (attempt < maxRetries - 1) {
                            kotlinx.coroutines.delay(2000)
                        }
                    }
                }

                Result.failure(lastException ?: Exception("İndirme başarısız"))
            }

    /** Get available formats for a URL */
    @Suppress("unused")
    suspend fun getFormats(url: String): Result<List<FormatInfo>> =
            withContext(Dispatchers.IO) {
                try {
                    val ydlOpts = createOptionsDict("quiet" to true, "no_warnings" to true)

                    val ydl = ytdlp.callAttr("YoutubeDL", ydlOpts)
                    val info = ydl.callAttr("extract_info", url, false)
                    val formats = info?.callAttr("get", "formats")?.asList() ?: emptyList()

                    val formatList =
                            formats.mapNotNull { f ->
                                try {
                                    FormatInfo(
                                            formatId = safeGetString(f, "format_id") ?: "",
                                            ext = safeGetString(f, "ext") ?: "",
                                            resolution = safeGetString(f, "resolution"),
                                            height = safeGetInt(f, "height"),
                                            width = safeGetInt(f, "width"),
                                            filesize = safeGetLong(f, "filesize"),
                                            vcodec = safeGetString(f, "vcodec"),
                                            acodec = safeGetString(f, "acodec"),
                                            abr = safeGetFloat(f, "abr"),
                                            formatNote = safeGetString(f, "format_note")
                                    )
                                } catch (_: Exception) {
                                    null
                                }
                            }

                    Result.success(formatList)
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting formats", e)
                    Result.failure(e)
                }
            }

    // Safe helper functions
    private fun createOptionsDict(vararg pairs: Pair<String, Any?>): PyObject {
        val dict = py.builtins.callAttr("dict")
        pairs.forEach { (key, value) ->
            if (value != null) {
                dict.callAttr("__setitem__", key, value)
            }
        }
        return dict
    }

    private fun safeGetString(obj: PyObject, key: String): String? {
        return try {
            val value = obj.callAttr("get", key)
            if (value == null || value.toString() == "None") null else value.toString()
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun safeGetDouble(obj: PyObject, key: String): Double? {
        return try {
            val value = obj.callAttr("get", key)
            if (value == null || value.toString() == "None") null
            else value.toJava(Double::class.java)
        } catch (_: Exception) {
            null
        }
    }

    private fun safeGetLong(obj: PyObject, key: String): Long? {
        return try {
            val value = obj.callAttr("get", key)
            if (value == null || value.toString() == "None") null
            else value.toJava(Long::class.java)
        } catch (_: Exception) {
            null
        }
    }

    private fun safeGetInt(obj: PyObject, key: String): Int? {
        return try {
            val value = obj.callAttr("get", key)
            if (value == null || value.toString() == "None") null else value.toJava(Int::class.java)
        } catch (_: Exception) {
            null
        }
    }

    private fun safeGetFloat(obj: PyObject, key: String): Float? {
        return try {
            val value = obj.callAttr("get", key)
            if (value == null || value.toString() == "None") null
            else value.toJava(Float::class.java)
        } catch (_: Exception) {
            null
        }
    }
}

data class FormatInfo(
        val formatId: String,
        val ext: String,
        val resolution: String?,
        val height: Int?,
        val width: Int?,
        val filesize: Long?,
        val vcodec: String?,
        val acodec: String?,
        val abr: Float?,
        val formatNote: String?
) {
    @Suppress("unused")
    val isVideoOnly: Boolean
        get() = acodec == "none"

    @Suppress("unused")
    val isAudioOnly: Boolean
        get() = vcodec == "none"

    @Suppress("unused")
    val hasVideo: Boolean
        get() = vcodec != null && vcodec != "none"

    @Suppress("unused")
    val hasAudio: Boolean
        get() = acodec != null && acodec != "none"
}
