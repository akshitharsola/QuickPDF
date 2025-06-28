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
    
    suspend fun openPdf(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            closePdf()
            
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("temp_pdf", ".pdf", context.cacheDir)
            
            inputStream?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            parcelFileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(parcelFileDescriptor!!)
            true
        } catch (e: Exception) {
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
        } catch (e: IOException) {
            // Ignore
        }
    }
}