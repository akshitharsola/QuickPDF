package com.example.quickpdf.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickpdf.data.model.RecentFile
import com.example.quickpdf.data.repository.SimpleRepository
import com.example.quickpdf.utils.FileUtil
import com.example.quickpdf.utils.PdfRendererUtil
import kotlinx.coroutines.launch

class MainViewModel(private val repository: SimpleRepository) : ViewModel() {
    
    val recentFiles: LiveData<List<RecentFile>> = repository.getAllRecentFiles()
    
    fun addRecentFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val fileName = FileUtil.getFileName(context, uri) ?: return@launch
                val fileSize = FileUtil.getFileSize(context, uri)
                // Store the URI directly for better reliability
                val filePath = uri.toString()
                
                // Try to get page count
                val pdfRenderer = PdfRendererUtil()
                val pageCount = if (pdfRenderer.openPdf(context, uri)) {
                    pdfRenderer.getPageCount()
                } else 0
                pdfRenderer.closePdf()
                
                val recentFile = RecentFile(
                    filePath = filePath,
                    fileName = fileName,
                    lastAccessed = System.currentTimeMillis(),
                    fileSize = fileSize,
                    pageCount = pageCount
                )
                
                repository.insertRecentFile(recentFile)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun updateLastAccessed(filePath: String) {
        viewModelScope.launch {
            repository.updateLastAccessed(filePath)
        }
    }
    
    fun deleteRecentFile(recentFile: RecentFile) {
        viewModelScope.launch {
            repository.deleteRecentFile(recentFile)
        }
    }
    
    fun clearAllRecentFiles() {
        viewModelScope.launch {
            repository.clearAllRecentFiles()
        }
    }
}