package com.example.quickpdf.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_files")
data class RecentFile(
    @PrimaryKey
    val filePath: String,
    val fileName: String,
    val lastAccessed: Long,
    val fileSize: Long,
    val pageCount: Int = 0
)