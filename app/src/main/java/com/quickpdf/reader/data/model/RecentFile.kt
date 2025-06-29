package com.quickpdf.reader.data.model

data class RecentFile(
    val filePath: String,
    val fileName: String,
    val lastAccessed: Long,
    val fileSize: Long,
    val pageCount: Int = 0
)