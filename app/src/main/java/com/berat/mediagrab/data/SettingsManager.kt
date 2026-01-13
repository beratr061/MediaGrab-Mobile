package com.berat.mediagrab.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsManager @Inject constructor(@param:ApplicationContext private val context: Context) {
    companion object {
        // Download settings
        private val DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
        private val PREFERRED_FORMAT = stringPreferencesKey("preferred_format")
        private val DOWNLOAD_PATH = stringPreferencesKey("download_path")

        // App settings
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val AUTO_DOWNLOAD = booleanPreferencesKey("auto_download")
        private val SHOW_NOTIFICATIONS = booleanPreferencesKey("show_notifications")
        private val WIFI_ONLY = booleanPreferencesKey("wifi_only")

        // Cache settings
        private val MAX_CACHE_SIZE_MB = intPreferencesKey("max_cache_size_mb")
        private val AUTO_CLEAR_CACHE = booleanPreferencesKey("auto_clear_cache")
    }

    // Download Quality
    val downloadQuality: Flow<DownloadQuality> =
            context.dataStore.data.map { preferences ->
                DownloadQuality.fromString(
                        preferences[DOWNLOAD_QUALITY] ?: DownloadQuality.BEST.value
                )
            }

    suspend fun setDownloadQuality(quality: DownloadQuality) {
        context.dataStore.edit { preferences -> preferences[DOWNLOAD_QUALITY] = quality.value }
    }

    // Preferred Format
    val preferredFormat: Flow<VideoFormat> =
            context.dataStore.data.map { preferences ->
                VideoFormat.fromString(preferences[PREFERRED_FORMAT] ?: VideoFormat.MP4.value)
            }

    suspend fun setPreferredFormat(format: VideoFormat) {
        context.dataStore.edit { preferences -> preferences[PREFERRED_FORMAT] = format.value }
    }

    // Download Path
    @Suppress("unused")
    val downloadPath: Flow<String> =
            context.dataStore.data.map { preferences -> preferences[DOWNLOAD_PATH] ?: "" }

    @Suppress("unused")
    suspend fun setDownloadPath(path: String) {
        context.dataStore.edit { preferences -> preferences[DOWNLOAD_PATH] = path }
    }

    // Dark Mode
    val darkMode: Flow<Boolean> =
            context.dataStore.data.map { preferences -> preferences[DARK_MODE] ?: true }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[DARK_MODE] = enabled }
    }

    // Auto Download
    val autoDownload: Flow<Boolean> =
            context.dataStore.data.map { preferences -> preferences[AUTO_DOWNLOAD] ?: false }

    suspend fun setAutoDownload(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[AUTO_DOWNLOAD] = enabled }
    }

    // Show Notifications
    val showNotifications: Flow<Boolean> =
            context.dataStore.data.map { preferences -> preferences[SHOW_NOTIFICATIONS] ?: true }

    suspend fun setShowNotifications(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[SHOW_NOTIFICATIONS] = enabled }
    }

    // WiFi Only
    val wifiOnly: Flow<Boolean> =
            context.dataStore.data.map { preferences -> preferences[WIFI_ONLY] ?: false }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[WIFI_ONLY] = enabled }
    }

    // Max Cache Size
    val maxCacheSizeMb: Flow<Int> =
            context.dataStore.data.map { preferences -> preferences[MAX_CACHE_SIZE_MB] ?: 500 }

    suspend fun setMaxCacheSizeMb(sizeMb: Int) {
        context.dataStore.edit { preferences -> preferences[MAX_CACHE_SIZE_MB] = sizeMb }
    }

    // Auto Clear Cache
    val autoClearCache: Flow<Boolean> =
            context.dataStore.data.map { preferences -> preferences[AUTO_CLEAR_CACHE] ?: false }

    suspend fun setAutoClearCache(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[AUTO_CLEAR_CACHE] = enabled }
    }
}

enum class DownloadQuality(val value: String, val displayName: String) {
    BEST("best", "En Yüksek Kalite"),
    HIGH("high", "Yüksek (1080p)"),
    MEDIUM("medium", "Orta (720p)"),
    LOW("low", "Düşük (480p)"),
    AUDIO_ONLY("audio", "Sadece Ses");

    companion object {
        fun fromString(value: String): DownloadQuality {
            return entries.find { it.value == value } ?: BEST
        }
    }
}

enum class VideoFormat(val value: String, val displayName: String) {
    MP4("mp4", "MP4 (Video)"),
    WEBM("webm", "WebM (Video)"),
    MKV("mkv", "MKV (Video)"),
    MP3("mp3", "MP3 (Ses)"),
    M4A("m4a", "M4A (Ses)");

    companion object {
        fun fromString(value: String): VideoFormat {
            return entries.find { it.value == value } ?: MP4
        }
    }
}
