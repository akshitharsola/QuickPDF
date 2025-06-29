package com.quickpdf.reader.data.model

data class Bookmark(
    val id: Long = 0,
    val filePath: String,
    val pageNumber: Int,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)