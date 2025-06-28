package com.example.quickpdf.utils

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileUtil {
    
    fun getFileName(context: Context, uri: Uri): String? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex("_display_name")
                    if (displayNameIndex != -1) {
                        return it.getString(displayNameIndex)
                    }
                }
            }
            uri.lastPathSegment
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }
    
    fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex("_size")
                    if (sizeIndex != -1) {
                        return it.getLong(sizeIndex)
                    }
                }
            }
            0L
        } catch (e: Exception) {
            0L
        }
    }
    
    fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024.0
            unitIndex++
        }
        
        return String.format("%.1f %s", size, units[unitIndex])
    }
    
    fun formatDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
    
    fun formatDateTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
    
    fun isPdfFile(fileName: String): Boolean {
        return fileName.lowercase(Locale.getDefault()).endsWith(".pdf")
    }
    
    fun getRealPathFromURI(context: Context, uri: Uri): String? {
        return try {
            when {
                DocumentsContract.isDocumentUri(context, uri) -> {
                    when {
                        isExternalStorageDocument(uri) -> {
                            val docId = DocumentsContract.getDocumentId(uri)
                            val split = docId.split(":")
                            if (split.size >= 2) {
                                "/storage/emulated/0/${split[1]}"
                            } else null
                        }
                        isDownloadsDocument(uri) -> {
                            val id = DocumentsContract.getDocumentId(uri)
                            if (id.startsWith("raw:")) {
                                id.replaceFirst("raw:", "")
                            } else {
                                getDataColumn(context, MediaStore.Downloads.EXTERNAL_CONTENT_URI, "_id=?", arrayOf(id))
                            }
                        }
                        isMediaDocument(uri) -> {
                            val docId = DocumentsContract.getDocumentId(uri)
                            val split = docId.split(":")
                            val contentUri = MediaStore.Files.getContentUri("external")
                            getDataColumn(context, contentUri, "_id=?", arrayOf(split[1]))
                        }
                        else -> null
                    }
                }
                "content".equals(uri.scheme, ignoreCase = true) -> {
                    getDataColumn(context, uri, null, null)
                }
                "file".equals(uri.scheme, ignoreCase = true) -> {
                    uri.path
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getDataColumn(context: Context, uri: Uri, selection: String?, selectionArgs: Array<String>?): String? {
        val cursor = context.contentResolver.query(uri, arrayOf("_data"), selection, selectionArgs, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow("_data")
                return it.getString(columnIndex)
            }
        }
        return null
    }
    
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }
    
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }
    
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }
}