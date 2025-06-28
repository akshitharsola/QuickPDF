package com.example.quickpdf.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class PdfRendererUtil {
    
    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var currentPage: PdfRenderer.Page? = null
    private var tempFile: File? = null
    
    suspend fun openPdf(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            closePdf()
            android.util.Log.d("PdfRendererUtil", "Attempting to open PDF from URI: $uri")
            
            // Try direct ParcelFileDescriptor first (more efficient)
            try {
                parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                if (parcelFileDescriptor != null) {
                    pdfRenderer = PdfRenderer(parcelFileDescriptor!!)
                    android.util.Log.d("PdfRendererUtil", "PDF opened directly with ${pdfRenderer?.pageCount ?: 0} pages")
                    return@withContext true
                }
            } catch (e: Exception) {
                android.util.Log.w("PdfRendererUtil", "Direct file descriptor failed, trying temporary file approach", e)
            }
            
            // Fallback to temporary file approach
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                android.util.Log.e("PdfRendererUtil", "Cannot open input stream for URI: $uri")
                return@withContext false
            }
            
            // Get the file size first to check if it's reasonable
            val fileSize = try {
                inputStream.available().toLong()
            } catch (e: Exception) {
                android.util.Log.w("PdfRendererUtil", "Cannot determine file size", e)
                -1L
            }
            
            if (fileSize > 100 * 1024 * 1024) { // 100MB limit
                android.util.Log.e("PdfRendererUtil", "File too large: $fileSize bytes")
                inputStream.close()
                return@withContext false
            }
            
            android.util.Log.d("PdfRendererUtil", "Creating temporary file for PDF (size: $fileSize bytes)")
            tempFile = File.createTempFile("quickpdf_", ".pdf", context.cacheDir)
            
            try {
                inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytes = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead
                        }
                        android.util.Log.d("PdfRendererUtil", "Copied $totalBytes bytes to temporary file")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PdfRendererUtil", "Error copying file to temp location", e)
                tempFile?.delete()
                tempFile = null
                return@withContext false
            }
            
            if (tempFile?.exists() != true || tempFile?.length() == 0L) {
                android.util.Log.e("PdfRendererUtil", "Temporary file is empty or doesn't exist")
                tempFile?.delete()
                tempFile = null
                return@withContext false
            }
            
            android.util.Log.d("PdfRendererUtil", "Temporary file created successfully: ${tempFile?.length()} bytes")
            
            parcelFileDescriptor = ParcelFileDescriptor.open(tempFile!!, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(parcelFileDescriptor!!)
            android.util.Log.d("PdfRendererUtil", "PDF opened successfully with ${pdfRenderer?.pageCount ?: 0} pages")
            
            // Store temp file reference for cleanup later (don't delete immediately)
            // The PdfRenderer needs the file to remain accessible
            android.util.Log.d("PdfRendererUtil", "Temporary file will be cleaned up when PDF is closed")
            
            true
        } catch (e: Exception) {
            android.util.Log.e("PdfRendererUtil", "Error opening PDF: ${e.javaClass.simpleName}: ${e.message}", e)
            false
        }
    }
    
    suspend fun openPdf(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            closePdf()
            val file = File(filePath)
            if (!file.exists()) return@withContext false
            
            parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(parcelFileDescriptor!!)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun getPageCount(): Int = pdfRenderer?.pageCount ?: 0
    
    suspend fun renderPage(pageIndex: Int, width: Int, height: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val renderer = pdfRenderer ?: return@withContext null
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
            
            currentPage?.close()
            currentPage = renderer.openPage(pageIndex)
            
            val page = currentPage ?: return@withContext null
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun renderPageWithAspectRatio(pageIndex: Int, maxWidth: Int, maxHeight: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val renderer = pdfRenderer ?: return@withContext null
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
            
            currentPage?.close()
            currentPage = renderer.openPage(pageIndex)
            
            val page = currentPage ?: return@withContext null
            val pageWidth = page.width
            val pageHeight = page.height
            
            val widthRatio = maxWidth.toFloat() / pageWidth
            val heightRatio = maxHeight.toFloat() / pageHeight
            val ratio = minOf(widthRatio, heightRatio)
            
            val renderWidth = (pageWidth * ratio).toInt()
            val renderHeight = (pageHeight * ratio).toInt()
            
            val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
    
    fun getPageDimensions(pageIndex: Int): Pair<Int, Int>? {
        return try {
            val renderer = pdfRenderer ?: return null
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
            
            val page = renderer.openPage(pageIndex)
            val dimensions = Pair(page.width, page.height)
            page.close()
            dimensions
        } catch (e: Exception) {
            null
        }
    }
    
    fun closePdf() {
        try {
            currentPage?.close()
            currentPage = null
            pdfRenderer?.close()
            pdfRenderer = null
            parcelFileDescriptor?.close()
            parcelFileDescriptor = null
            
            // Clean up temporary file if it exists
            tempFile?.let { file ->
                if (file.exists()) {
                    val deleted = file.delete()
                    android.util.Log.d("PdfRendererUtil", "Temporary file cleanup: ${if (deleted) "success" else "failed"}")
                }
                tempFile = null
            }
        } catch (e: IOException) {
            android.util.Log.w("PdfRendererUtil", "Error during PDF cleanup", e)
        }
    }
}