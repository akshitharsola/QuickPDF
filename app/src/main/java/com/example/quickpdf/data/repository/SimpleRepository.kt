package com.example.quickpdf.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.quickpdf.data.model.Bookmark
import com.example.quickpdf.data.model.RecentFile

/**
 * Simple in-memory repository for initial testing
 * This replaces Room database temporarily to avoid annotation processing issues
 */
class SimpleRepository {
    
    private val _recentFiles = MutableLiveData<List<RecentFile>>()
    private val recentFilesList = mutableListOf<RecentFile>()
    
    private val _bookmarks = MutableLiveData<List<Bookmark>>()
    private val bookmarksList = mutableListOf<Bookmark>()
    
    init {
        _recentFiles.value = emptyList()
        _bookmarks.value = emptyList()
    }
    
    fun getAllRecentFiles(): LiveData<List<RecentFile>> = _recentFiles
    
    suspend fun insertRecentFile(recentFile: RecentFile) {
        // Remove existing entry with same path
        recentFilesList.removeAll { it.filePath == recentFile.filePath }
        // Add to beginning of list
        recentFilesList.add(0, recentFile)
        // Keep only last 20 files
        if (recentFilesList.size > 20) {
            recentFilesList.removeAt(recentFilesList.size - 1)
        }
        _recentFiles.postValue(recentFilesList.toList())
    }
    
    suspend fun deleteRecentFile(recentFile: RecentFile) {
        recentFilesList.remove(recentFile)
        _recentFiles.postValue(recentFilesList.toList())
    }
    
    suspend fun deleteRecentFileByPath(filePath: String) {
        recentFilesList.removeAll { it.filePath == filePath }
        _recentFiles.postValue(recentFilesList.toList())
    }
    
    suspend fun clearAllRecentFiles() {
        recentFilesList.clear()
        _recentFiles.postValue(emptyList())
    }
    
    suspend fun updateLastAccessed(filePath: String, timestamp: Long = System.currentTimeMillis()) {
        val fileIndex = recentFilesList.indexOfFirst { it.filePath == filePath }
        if (fileIndex != -1) {
            val updatedFile = recentFilesList[fileIndex].copy(lastAccessed = timestamp)
            recentFilesList[fileIndex] = updatedFile
            // Move to top
            recentFilesList.removeAt(fileIndex)
            recentFilesList.add(0, updatedFile)
            _recentFiles.postValue(recentFilesList.toList())
        }
    }
    
    fun getBookmarksForFile(filePath: String): LiveData<List<Bookmark>> {
        val filteredBookmarks = MutableLiveData<List<Bookmark>>()
        filteredBookmarks.value = bookmarksList.filter { it.filePath == filePath }
        return filteredBookmarks
    }
    
    fun getAllBookmarks(): LiveData<List<Bookmark>> = _bookmarks
    
    suspend fun insertBookmark(bookmark: Bookmark): Long {
        val newId = (bookmarksList.maxOfOrNull { it.id } ?: 0) + 1
        val newBookmark = bookmark.copy(id = newId)
        bookmarksList.add(newBookmark)
        _bookmarks.postValue(bookmarksList.toList())
        return newId
    }
    
    suspend fun deleteBookmark(bookmark: Bookmark) {
        bookmarksList.remove(bookmark)
        _bookmarks.postValue(bookmarksList.toList())
    }
    
    suspend fun deleteBookmarkByPage(filePath: String, pageNumber: Int) {
        bookmarksList.removeAll { it.filePath == filePath && it.pageNumber == pageNumber }
        _bookmarks.postValue(bookmarksList.toList())
    }
    
    suspend fun isBookmarked(filePath: String, pageNumber: Int): Boolean {
        return bookmarksList.any { it.filePath == filePath && it.pageNumber == pageNumber }
    }
}