package com.berat.mediagrab.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berat.mediagrab.data.DownloadQuality
import com.berat.mediagrab.data.SettingsManager
import com.berat.mediagrab.data.VideoFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(private val settingsManager: SettingsManager) :
        ViewModel() {

        val downloadQuality =
                settingsManager.downloadQuality.stateIn(
                        viewModelScope,
                        SharingStarted.WhileSubscribed(5000),
                        DownloadQuality.BEST
                )

        val preferredFormat =
                settingsManager.preferredFormat.stateIn(
                        viewModelScope,
                        SharingStarted.WhileSubscribed(5000),
                        VideoFormat.MP4
                )

        val darkMode =
                settingsManager.darkMode.stateIn(
                        viewModelScope,
                        SharingStarted.WhileSubscribed(5000),
                        true
                )

        val autoDownload =
                settingsManager.autoDownload.stateIn(
                        viewModelScope,
                        SharingStarted.WhileSubscribed(5000),
                        false
                )

        val showNotifications =
                settingsManager.showNotifications.stateIn(
                        viewModelScope,
                        SharingStarted.WhileSubscribed(5000),
                        true
                )

        val wifiOnly =
                settingsManager.wifiOnly.stateIn(
                        viewModelScope,
                        SharingStarted.WhileSubscribed(5000),
                        false
                )

        @Suppress("unused")
        val maxCacheSizeMb =
                settingsManager.maxCacheSizeMb.stateIn(
                        viewModelScope,
                        SharingStarted.WhileSubscribed(5000),
                        500
                )

        val autoClearCache =
                settingsManager.autoClearCache.stateIn(
                        viewModelScope,
                        SharingStarted.WhileSubscribed(5000),
                        false
                )

        fun setDownloadQuality(quality: DownloadQuality) {
                viewModelScope.launch { settingsManager.setDownloadQuality(quality) }
        }

        fun setPreferredFormat(format: VideoFormat) {
                viewModelScope.launch { settingsManager.setPreferredFormat(format) }
        }

        fun setDarkMode(enabled: Boolean) {
                viewModelScope.launch { settingsManager.setDarkMode(enabled) }
        }

        fun setAutoDownload(enabled: Boolean) {
                viewModelScope.launch { settingsManager.setAutoDownload(enabled) }
        }

        fun setShowNotifications(enabled: Boolean) {
                viewModelScope.launch { settingsManager.setShowNotifications(enabled) }
        }

        fun setWifiOnly(enabled: Boolean) {
                viewModelScope.launch { settingsManager.setWifiOnly(enabled) }
        }

        @Suppress("unused")
        fun setMaxCacheSizeMb(sizeMb: Int) {
                viewModelScope.launch { settingsManager.setMaxCacheSizeMb(sizeMb) }
        }

        fun setAutoClearCache(enabled: Boolean) {
                viewModelScope.launch { settingsManager.setAutoClearCache(enabled) }
        }
}
