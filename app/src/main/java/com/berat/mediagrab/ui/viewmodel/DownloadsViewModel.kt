package com.berat.mediagrab.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berat.mediagrab.data.database.DownloadEntity
import com.berat.mediagrab.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@HiltViewModel
class DownloadsViewModel @Inject constructor(private val downloadRepository: DownloadRepository) :
        ViewModel() {

    val downloads: Flow<List<DownloadEntity>> = downloadRepository.getAllDownloads()

    fun deleteDownload(download: DownloadEntity) {
        viewModelScope.launch {
            // Delete the file if it exists
            download.filePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
            // Delete from database
            downloadRepository.delete(download)
        }
    }
}
