package com.example.quickpdf.data.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface RecentFileDao {
    
    @Query("SELECT * FROM recent_files ORDER BY lastAccessed DESC LIMIT 20")
    fun getAllRecentFiles(): LiveData<List<RecentFile>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentFile(recentFile: RecentFile)
    
    @Delete
    suspend fun deleteRecentFile(recentFile: RecentFile)
    
    @Query("DELETE FROM recent_files WHERE filePath = :filePath")
    suspend fun deleteRecentFileByPath(filePath: String)
    
    @Query("DELETE FROM recent_files")
    suspend fun clearAllRecentFiles()
    
    @Query("UPDATE recent_files SET lastAccessed = :timestamp WHERE filePath = :filePath")
    suspend fun updateLastAccessed(filePath: String, timestamp: Long)
}