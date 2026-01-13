package com.berat.mediagrab.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper class for merging video and audio files using Android's MediaMuxer API. No external FFmpeg
 * dependency required.
 */
@Suppress("unused")
object MediaMerger {
    private const val TAG = "MediaMerger"
    private const val BUFFER_SIZE = 1024 * 1024 // 1MB buffer

    /**
     * Merge video and audio files into a single MP4 output file. Uses Android's native MediaMuxer -
     * no FFmpeg needed.
     *
     * @param videoPath Path to video-only file (MP4/WebM)
     * @param audioPath Path to audio-only file (M4A/MP3)
     * @param outputPath Path for merged output (MP4)
     * @param onProgress Progress callback (0.0 to 1.0)
     * @return Result with output path on success
     */
    @Suppress("unused")
    @android.annotation.SuppressLint("WrongConstant")
    suspend fun mergeVideoAudio(
            videoPath: String,
            audioPath: String,
            outputPath: String,
            onProgress: ((Float) -> Unit)? = null
    ): Result<String> =
            withContext(Dispatchers.IO) {
                var videoExtractor: MediaExtractor? = null
                var audioExtractor: MediaExtractor? = null
                var muxer: MediaMuxer? = null

                try {
                    Log.d(TAG, "Starting merge: video=$videoPath, audio=$audioPath -> $outputPath")

                    // Validate input files
                    val videoFile = File(videoPath)
                    val audioFile = File(audioPath)

                    if (!videoFile.exists()) {
                        return@withContext Result.failure(
                                Exception("Video file not found: $videoPath")
                        )
                    }
                    if (!audioFile.exists()) {
                        return@withContext Result.failure(
                                Exception("Audio file not found: $audioPath")
                        )
                    }

                    // Delete output if exists
                    val outputFile = File(outputPath)
                    if (outputFile.exists()) {
                        outputFile.delete()
                    }

                    // Setup extractors
                    videoExtractor = MediaExtractor().apply { setDataSource(videoPath) }
                    audioExtractor = MediaExtractor().apply { setDataSource(audioPath) }

                    // Find video track
                    var videoTrackIndex = -1
                    var videoFormat: MediaFormat? = null
                    for (i in 0 until videoExtractor.trackCount) {
                        val format = videoExtractor.getTrackFormat(i)
                        val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                        if (mime.startsWith("video/")) {
                            videoTrackIndex = i
                            videoFormat = format
                            break
                        }
                    }

                    // Find audio track
                    var audioTrackIndex = -1
                    var audioFormat: MediaFormat? = null
                    for (i in 0 until audioExtractor.trackCount) {
                        val format = audioExtractor.getTrackFormat(i)
                        val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                        if (mime.startsWith("audio/")) {
                            audioTrackIndex = i
                            audioFormat = format
                            break
                        }
                    }

                    if (videoTrackIndex < 0 || videoFormat == null) {
                        return@withContext Result.failure(
                                Exception("No video track found in: $videoPath")
                        )
                    }
                    if (audioTrackIndex < 0 || audioFormat == null) {
                        return@withContext Result.failure(
                                Exception("No audio track found in: $audioPath")
                        )
                    }

                    Log.d(TAG, "Video format: $videoFormat")
                    Log.d(TAG, "Audio format: $audioFormat")

                    // Select tracks
                    videoExtractor.selectTrack(videoTrackIndex)
                    audioExtractor.selectTrack(audioTrackIndex)

                    // Create muxer
                    muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                    // Add tracks to muxer
                    val muxerVideoTrack = muxer.addTrack(videoFormat)
                    val muxerAudioTrack = muxer.addTrack(audioFormat)

                    muxer.start()

                    // Get video duration for progress
                    val videoDuration = videoFormat.getLong(MediaFormat.KEY_DURATION)

                    // Buffer for reading samples
                    val buffer = ByteBuffer.allocate(BUFFER_SIZE)
                    val bufferInfo = MediaCodec.BufferInfo()

                    // Write video samples
                    Log.d(TAG, "Writing video track...")
                    var videoProgress = 0f
                    while (true) {
                        buffer.clear()
                        val sampleSize = videoExtractor.readSampleData(buffer, 0)
                        if (sampleSize < 0) break

                        bufferInfo.offset = 0
                        bufferInfo.size = sampleSize
                        bufferInfo.presentationTimeUs = videoExtractor.sampleTime
                        bufferInfo.flags = videoExtractor.sampleFlags

                        muxer.writeSampleData(muxerVideoTrack, buffer, bufferInfo)

                        // Update progress (0-50% for video)
                        videoProgress =
                                (bufferInfo.presentationTimeUs.toFloat() / videoDuration) * 0.5f
                        onProgress?.invoke(videoProgress.coerceIn(0f, 0.5f))

                        videoExtractor.advance()
                    }

                    // Write audio samples
                    Log.d(TAG, "Writing audio track...")
                    while (true) {
                        buffer.clear()
                        val sampleSize = audioExtractor.readSampleData(buffer, 0)
                        if (sampleSize < 0) break

                        bufferInfo.offset = 0
                        bufferInfo.size = sampleSize
                        bufferInfo.presentationTimeUs = audioExtractor.sampleTime
                        bufferInfo.flags = audioExtractor.sampleFlags

                        muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)

                        // Update progress (50-100% for audio)
                        val audioProgress =
                                0.5f +
                                        (bufferInfo.presentationTimeUs.toFloat() / videoDuration) *
                                                0.5f
                        onProgress?.invoke(audioProgress.coerceIn(0.5f, 1f))

                        audioExtractor.advance()
                    }

                    Log.d(TAG, "Merge completed: $outputPath")
                    onProgress?.invoke(1f)

                    Result.success(outputPath)
                } catch (e: Exception) {
                    Log.e(TAG, "Merge failed", e)
                    // Clean up partial output
                    try {
                        File(outputPath).delete()
                    } catch (_: Exception) {}
                    Result.failure(e)
                } finally {
                    // Release resources
                    try {
                        muxer?.stop()
                    } catch (_: Exception) {}
                    try {
                        muxer?.release()
                    } catch (_: Exception) {}
                    try {
                        videoExtractor?.release()
                    } catch (_: Exception) {}
                    try {
                        audioExtractor?.release()
                    } catch (_: Exception) {}
                }
            }

    /** Clean up temporary files */
    fun cleanupTempFiles(vararg paths: String) {
        paths.forEach { path ->
            try {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                    Log.d(TAG, "Deleted temp file: $path")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete temp file: $path", e)
            }
        }
    }
}
