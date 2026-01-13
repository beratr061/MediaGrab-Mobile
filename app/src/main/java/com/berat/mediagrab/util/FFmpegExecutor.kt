package com.berat.mediagrab.util

import android.content.Context
import android.util.Log
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper class to execute FFmpeg commands using a bundled FFmpeg binary. Supports all codecs
 * including VP9, H.264, AV1, etc.
 *
 * FFmpeg binary is bundled in jniLibs as libffmpeg.so and accessed from nativeLibraryDir which has
 * execute permissions on Android.
 */
object FFmpegExecutor {
    private const val TAG = "FFmpegExecutor"
    private const val FFMPEG_LIB_NAME = "libffmpeg.so"

    private var ffmpegPath: String? = null
    private var isInitialized = false

    /**
     * Initialize FFmpeg by locating the binary in the native library directory. Must be called
     * before using any FFmpeg functions.
     */
    suspend fun initialize(context: Context): Boolean =
            withContext(Dispatchers.IO) {
                if (isInitialized && ffmpegPath != null && File(ffmpegPath!!).exists()) {
                    return@withContext true
                }

                try {
                    // Get the native library directory - Android allows execution from here
                    val nativeLibDir = context.applicationInfo.nativeLibraryDir
                    Log.d(TAG, "Native library dir: $nativeLibDir")

                    val ffmpegFile = File(nativeLibDir, FFMPEG_LIB_NAME)

                    if (!ffmpegFile.exists()) {
                        Log.e(TAG, "FFmpeg binary not found at: ${ffmpegFile.absolutePath}")
                        return@withContext false
                    }

                    // Verify it's executable (should be by default in nativeLibraryDir)
                    if (!ffmpegFile.canExecute()) {
                        Log.w(TAG, "FFmpeg not executable, attempting to set")
                        ffmpegFile.setExecutable(true, false)
                    }

                    Log.d(TAG, "FFmpeg canExecute: ${ffmpegFile.canExecute()}")

                    ffmpegPath = ffmpegFile.absolutePath
                    isInitialized = true

                    Log.d(TAG, "FFmpeg initialized at: $ffmpegPath")
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize FFmpeg", e)
                    false
                }
            }

    /**
     * Merge video and audio files into a single output file. Supports all codecs including VP9.
     *
     * @param videoPath Path to video-only file
     * @param audioPath Path to audio-only file
     * @param outputPath Path for merged output
     * @param onProgress Progress callback (0.0 to 1.0)
     * @param context Application context for temp directory (optional, uses outputPath parent if null)
     * @return Result with output path on success
     */
    suspend fun mergeVideoAudio(
            videoPath: String,
            audioPath: String,
            outputPath: String,
            onProgress: ((Float) -> Unit)? = null,
            context: android.content.Context? = null
    ): Result<String> =
            withContext(Dispatchers.IO) {
                if (!isInitialized || ffmpegPath == null) {
                    return@withContext Result.failure(
                            Exception("FFmpeg not initialized. Call initialize() first.")
                    )
                }

                try {
                    Log.d(TAG, "Starting merge: video=$videoPath, audio=$audioPath -> $outputPath")

                    // Validate input files
                    if (!File(videoPath).exists()) {
                        return@withContext Result.failure(
                                Exception("Video file not found: $videoPath")
                        )
                    }
                    if (!File(audioPath).exists()) {
                        return@withContext Result.failure(
                                Exception("Audio file not found: $audioPath")
                        )
                    }

                    // Delete output if exists
                    val outputFile = File(outputPath)
                    if (outputFile.exists()) {
                        outputFile.delete()
                    }

                    // Use app's cache directory for FFmpeg output to avoid permission issues
                    // Then copy to final destination
                    val tempOutputFile = if (context != null) {
                        File(context.cacheDir, "ffmpeg_output_${System.currentTimeMillis()}.mp4")
                    } else {
                        // Fallback: use same directory as input files
                        File(File(videoPath).parent, "ffmpeg_temp_${System.currentTimeMillis()}.mp4")
                    }
                    
                    val actualOutputPath = tempOutputFile.absolutePath
                    Log.d(TAG, "Using temp output path: $actualOutputPath")

                    // FFmpeg command to merge video and audio
                    // Transcode to H.264/AAC for maximum compatibility with native video players
                    val command =
                            listOf(
                                    ffmpegPath!!,
                                    "-i",
                                    videoPath,
                                    "-i",
                                    audioPath,
                                    "-c:v",
                                    "libx264",
                                    "-preset",
                                    "fast",
                                    "-crf",
                                    "18",
                                    "-c:a",
                                    "aac",
                                    "-ar",
                                    "48000",
                                    "-ac",
                                    "2",
                                    "-b:a",
                                    "320k",
                                    "-af",
                                    "aresample=async=1000",
                                    "-movflags",
                                    "+faststart",
                                    "-shortest",
                                    "-y",
                                    actualOutputPath
                            )

                    Log.d(TAG, "FFmpeg command: ${command.joinToString(" ")}")

                    val processBuilder = ProcessBuilder(command).redirectErrorStream(true)

                    val process = processBuilder.start()

                    // Read output for logging and progress
                    val output = StringBuilder()
                    process.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            output.appendLine(line)
                            Log.v(TAG, "FFmpeg: $line")
                        }
                    }

                    val exitCode = process.waitFor()

                    if (exitCode == 0 && tempOutputFile.exists() && tempOutputFile.length() > 0) {
                        Log.d(TAG, "FFmpeg merge successful, copying to final destination...")
                        
                        // Copy from temp to final destination
                        try {
                            // Generate unique filename if file already exists
                            var finalOutputFile = outputFile
                            if (finalOutputFile.exists()) {
                                val baseName = outputFile.nameWithoutExtension
                                val extension = outputFile.extension
                                val parentDir = outputFile.parentFile
                                var counter = 1
                                while (finalOutputFile.exists()) {
                                    finalOutputFile = File(parentDir, "${baseName}_$counter.$extension")
                                    counter++
                                }
                                Log.d(TAG, "File exists, using unique name: ${finalOutputFile.name}")
                            }
                            
                            // Ensure parent directory exists
                            finalOutputFile.parentFile?.mkdirs()
                            
                            // Use streams for reliable copying on Android
                            tempOutputFile.inputStream().use { input ->
                                finalOutputFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            
                            tempOutputFile.delete() // Clean up temp file
                            
                            if (finalOutputFile.exists() && finalOutputFile.length() > 0) {
                                Log.d(TAG, "Merge successful: ${finalOutputFile.absolutePath} (${finalOutputFile.length()} bytes)")
                                onProgress?.invoke(1.0f)
                                Result.success(finalOutputFile.absolutePath)
                            } else {
                                Result.failure(Exception("Failed to copy merged file to destination"))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to copy merged file", e)
                            tempOutputFile.delete()
                            Result.failure(Exception("Failed to copy merged file: ${e.message}"))
                        }
                    } else {
                        Log.e(TAG, "Merge failed with exit code $exitCode: $output")
                        tempOutputFile.delete() // Clean up on failure
                        Result.failure(
                                Exception("FFmpeg merge failed: ${output.takeLast(500)}")
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during merge", e)
                    Result.failure(e)
                }
            }

    /**
     * Execute a custom FFmpeg command.
     *
     * @param args FFmpeg arguments (without ffmpeg binary path)
     * @return Result with output on success
     */
    @Suppress("unused")
    suspend fun execute(vararg args: String): Result<String> =
            withContext(Dispatchers.IO) {
                if (!isInitialized || ffmpegPath == null) {
                    return@withContext Result.failure(
                            Exception("FFmpeg not initialized. Call initialize() first.")
                    )
                }

                try {
                    val command = listOf(ffmpegPath!!) + args.toList()
                    Log.d(TAG, "Executing: ${command.joinToString(" ")}")

                    val processBuilder = ProcessBuilder(command).redirectErrorStream(true)

                    val process = processBuilder.start()
                    val output = process.inputStream.bufferedReader().readText()
                    val exitCode = process.waitFor()

                    if (exitCode == 0) {
                        Result.success(output)
                    } else {
                        Result.failure(
                                Exception(
                                        "FFmpeg failed with exit code $exitCode: ${output.takeLast(500)}"
                                )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "FFmpeg execution failed", e)
                    Result.failure(e)
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

    /** Check if FFmpeg is available and initialized */
    fun isAvailable(): Boolean = isInitialized && ffmpegPath != null && File(ffmpegPath!!).exists()
}
