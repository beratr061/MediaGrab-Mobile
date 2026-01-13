package com.berat.mediagrab.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>
    
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentDownloads(limit: Int): Flow<List<DownloadEntity>>
    
    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY createdAt DESC")
    fun getDownloadsByStatus(status: String): Flow<List<DownloadEntity>>
    
    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Long): DownloadEntity?
    
    @Query("SELECT * FROM downloads WHERE videoId = :videoId LIMIT 1")
    suspend fun getDownloadByVideoId(videoId: String): DownloadEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity): Long
    
    @Update
    suspend fun update(download: DownloadEntity)
    
    @Delete
    suspend fun delete(download: DownloadEntity)
    
    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM downloads")
    suspend fun deleteAll()

    @Query("UPDATE downloads SET status = :status, progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: Long, status: String, progress: Float)
    
    @Query("UPDATE downloads SET status = :status, filePath = :filePath, completedAt = :completedAt WHERE id = :id")
    suspend fun markCompleted(id: Long, status: String, filePath: String, completedAt: Long)
    
    @Query("UPDATE downloads SET status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun markFailed(id: Long, status: String, errorMessage: String)
    
    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'completed'")
    fun getCompletedCount(): Flow<Int>
}
