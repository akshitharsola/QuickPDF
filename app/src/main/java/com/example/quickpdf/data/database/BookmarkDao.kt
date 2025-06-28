package com.example.quickpdf.data.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface BookmarkDao {
    
    @Query("SELECT * FROM bookmarks WHERE filePath = :filePath ORDER BY pageNumber ASC")
    fun getBookmarksForFile(filePath: String): LiveData<List<Bookmark>>
    
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): LiveData<List<Bookmark>>
    
    @Insert
    suspend fun insertBookmark(bookmark: Bookmark): Long
    
    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)
    
    @Query("DELETE FROM bookmarks WHERE filePath = :filePath AND pageNumber = :pageNumber")
    suspend fun deleteBookmarkByPage(filePath: String, pageNumber: Int)
    
    @Query("SELECT COUNT(*) FROM bookmarks WHERE filePath = :filePath AND pageNumber = :pageNumber")
    suspend fun isBookmarked(filePath: String, pageNumber: Int): Int
}