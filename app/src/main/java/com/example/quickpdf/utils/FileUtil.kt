package com.example.quickpdf.utils

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileUtil {
    
    fun getFileName(context: Context, uri: Uri): String? {
        return try {
            android.util.Log.d("FileUtil", "Getting file name for URI: $uri")
            
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    // Try standard OpenableColumns.DISPLAY_NAME first
                    val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        val fileName = it.getString(displayNameIndex)
                        android.util.Log.d("FileUtil", "Found display name: $fileName")
                        if (!fileName.isNullOrEmpty()) {
                            return fileName
                        }
                    }
                    
                    // Fallback to legacy column name
                    val legacyIndex = it.getColumnIndex("_display_name")
                    if (legacyIndex != -1) {
                        val fileName = it.getString(legacyIndex)
                        android.util.Log.d("FileUtil", "Found legacy display name: $fileName")
                        if (!fileName.isNullOrEmpty()) {
                            return fileName
                        }
                    }
                }
            }
            
            // Extract from URI path as last resort
            val pathSegment = uri.lastPathSegment
            android.util.Log.d("FileUtil", "Using path segment as filename: $pathSegment")
            pathSegment
        } catch (e: Exception) {
            android.util.Log.e("FileUtil", "Error getting filename", e)
            uri.lastPathSegment
        }
    }
    
    fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            android.util.Log.d("FileUtil", "Getting file size for URI: $uri")
            
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    // Try standard OpenableColumns.SIZE first
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        val size = it.getLong(sizeIndex)
                        android.util.Log.d("FileUtil", "Found file size: $size bytes")
                        return size
                    }
                    
                    // Fallback to legacy column name
                    val legacyIndex = it.getColumnIndex("_size")
                    if (legacyIndex != -1) {
                        val size = it.getLong(legacyIndex)
                        android.util.Log.d("FileUtil", "Found legacy file size: $size bytes")
                        return size
                    }
                }
            }
            
            android.util.Log.w("FileUtil", "Could not determine file size, returning 0")
            0L
        } catch (e: Exception) {
            android.util.Log.e("FileUtil", "Error getting file size", e)
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