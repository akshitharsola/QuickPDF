package com.quickpdf.reader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quickpdf.reader.data.repository.SimpleRepository

class ViewModelFactory(private val repository: SimpleRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(PdfViewerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PdfViewerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}