package com.berat.mediagrab.data.repository

import com.berat.mediagrab.data.database.DownloadDao
import com.berat.mediagrab.data.database.DownloadEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Suppress("unused")
@Singleton
class DownloadRepository @Inject constructor(private val downloadDao: DownloadDao) {

    fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    fun getRecentDownloads(limit: Int = 5): Flow<List<DownloadEntity>> =
            downloadDao.getRecentDownloads(limit)

    fun getCompletedDownloads(): Flow<List<DownloadEntity>> =
            downloadDao.getDownloadsByStatus(DownloadEntity.STATUS_COMPLETED)

    suspend fun getDownloadById(id: Long): DownloadEntity? = downloadDao.getDownloadById(id)

    suspend fun createDownload(download: DownloadEntity): Long = downloadDao.insert(download)

    suspend fun updateDownload(download: DownloadEntity) = downloadDao.update(download)

    suspend fun deleteDownload(id: Long) = downloadDao.deleteById(id)

    suspend fun delete(download: DownloadEntity) = downloadDao.delete(download)

    suspend fun deleteAllDownloads() = downloadDao.deleteAll()

    suspend fun updateProgress(id: Long, progress: Float) {
        downloadDao.updateProgress(id, DownloadEntity.STATUS_DOWNLOADING, progress)
    }

    suspend fun markCompleted(id: Long, filePath: String) {
        downloadDao.markCompleted(
                id = id,
                status = DownloadEntity.STATUS_COMPLETED,
                filePath = filePath,
                completedAt = System.currentTimeMillis()
        )
    }

    suspend fun markFailed(id: Long, errorMessage: String) {
        downloadDao.markFailed(id, DownloadEntity.STATUS_FAILED, errorMessage)
    }
}
