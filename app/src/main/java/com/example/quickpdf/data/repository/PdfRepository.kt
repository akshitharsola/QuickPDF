package com.example.quickpdf.data.repository

import androidx.lifecycle.LiveData
import com.example.quickpdf.data.database.Bookmark
import com.example.quickpdf.data.database.BookmarkDao
import com.example.quickpdf.data.database.RecentFile
import com.example.quickpdf.data.database.RecentFileDao

class PdfRepository(
    private val recentFileDao: RecentFileDao,
    private val bookmarkDao: BookmarkDao
) {
    
    fun getAllRecentFiles(): LiveData<List<RecentFile>> = recentFileDao.getAllRecentFiles()
    
    suspend fun insertRecentFile(recentFile: RecentFile) {
        recentFileDao.insertRecentFile(recentFile)
    }
    
    suspend fun deleteRecentFile(recentFile: RecentFile) {
        recentFileDao.deleteRecentFile(recentFile)
    }
    
    suspend fun deleteRecentFileByPath(filePath: String) {
        recentFileDao.deleteRecentFileByPath(filePath)
    }
    
    suspend fun clearAllRecentFiles() {
        recentFileDao.clearAllRecentFiles()
    }
    
    suspend fun updateLastAccessed(filePath: String, timestamp: Long = System.currentTimeMillis()) {
        recentFileDao.updateLastAccessed(filePath, timestamp)
    }
    
    fun getBookmarksForFile(filePath: String): LiveData<List<Bookmark>> = 
        bookmarkDao.getBookmarksForFile(filePath)
    
    fun getAllBookmarks(): LiveData<List<Bookmark>> = bookmarkDao.getAllBookmarks()
    
    suspend fun insertBookmark(bookmark: Bookmark): Long = bookmarkDao.insertBookmark(bookmark)
    
    suspend fun deleteBookmark(bookmark: Bookmark) {
        bookmarkDao.deleteBookmark(bookmark)
    }
    
    suspend fun deleteBookmarkByPage(filePath: String, pageNumber: Int) {
        bookmarkDao.deleteBookmarkByPage(filePath, pageNumber)
    }
    
    suspend fun isBookmarked(filePath: String, pageNumber: Int): Boolean {
        return bookmarkDao.isBookmarked(filePath, pageNumber) > 0
    }
}