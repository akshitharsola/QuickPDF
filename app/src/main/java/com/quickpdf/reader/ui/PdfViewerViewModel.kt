package com.quickpdf.reader.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickpdf.reader.data.model.Bookmark
import com.quickpdf.reader.data.repository.SimpleRepository
import kotlinx.coroutines.launch

class PdfViewerViewModel(private val repository: SimpleRepository) : ViewModel() {
    
    private val _currentPage = MutableLiveData<Int>()
    val currentPage: LiveData<Int> = _currentPage
    
    private val _totalPages = MutableLiveData<Int>()
    val totalPages: LiveData<Int> = _totalPages
    
    private val _isNightMode = MutableLiveData<Boolean>()
    val isNightMode: LiveData<Boolean> = _isNightMode
    
    private val _viewMode = MutableLiveData<ViewMode>()
    val viewMode: LiveData<ViewMode> = _viewMode
    
    private var currentFilePath: String? = null
    
    init {
        _currentPage.value = 0
        _totalPages.value = 0
        _isNightMode.value = false
        _viewMode.value = ViewMode.FIT_WIDTH
    }
    
    fun setCurrentPage(page: Int) {
        _currentPage.value = page
    }
    
    fun setTotalPages(total: Int) {
        _totalPages.value = total
    }
    
    fun setNightMode(enabled: Boolean) {
        _isNightMode.value = enabled
    }
    
    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }
    
    fun setCurrentFile(filePath: String) {
        currentFilePath = filePath
    }
    
    fun getBookmarksForCurrentFile(): LiveData<List<Bookmark>>? {
        return currentFilePath?.let { repository.getBookmarksForFile(it) }
    }
    
    fun addBookmark(pageNumber: Int, title: String) {
        currentFilePath?.let { filePath ->
            viewModelScope.launch {
                val bookmark = Bookmark(
                    filePath = filePath,
                    pageNumber = pageNumber,
                    title = title
                )
                repository.insertBookmark(bookmark)
            }
        }
    }
    
    fun removeBookmark(pageNumber: Int) {
        currentFilePath?.let { filePath ->
            viewModelScope.launch {
                repository.deleteBookmarkByPage(filePath, pageNumber)
            }
        }
    }
    
    suspend fun isPageBookmarked(pageNumber: Int): Boolean {
        return currentFilePath?.let { filePath ->
            repository.isBookmarked(filePath, pageNumber)
        } ?: false
    }
    
    enum class ViewMode {
        FIT_WIDTH,
        FIT_PAGE,
        ORIGINAL_SIZE
    }
}