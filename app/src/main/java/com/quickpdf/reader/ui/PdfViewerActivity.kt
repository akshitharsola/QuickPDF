package com.quickpdf.reader.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.quickpdf.reader.QuickPDFApplication
import com.quickpdf.reader.R
import com.quickpdf.reader.databinding.ActivityPdfViewerBinding
import com.quickpdf.reader.ui.adapters.PdfPageAdapter
import com.quickpdf.reader.ui.custom.ProfessionalZoomableImageView
import com.quickpdf.reader.utils.FileUtil
import com.quickpdf.reader.utils.PdfRendererUtil
import kotlinx.coroutines.launch

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding
    private lateinit var pdfRenderer: PdfRendererUtil
    private lateinit var pdfPageAdapter: PdfPageAdapter
    
    private val pdfViewerViewModel: PdfViewerViewModel by viewModels {
        ViewModelFactory((application as QuickPDFApplication).repository)
    }

    private var fileName: String = ""
    private var toolbarVisible = true
    private val toolbarHandler = Handler(Looper.getMainLooper())
    private var toolbarHideRunnable: Runnable? = null
    private var savedCurrentPage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Restore saved page position on orientation change
        savedCurrentPage = savedInstanceState?.getInt("current_page", 0) ?: 0

        setupToolbar()
        setupPdfRenderer()
        setupViewPager()
        setupGestureDetector()
        setupClickListeners()
        setupZoomControls()
        observeViewModel()
        loadPdfFromIntent()
    }


    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // Controls will be initially visible, auto-hide after PDF loads
    }

    private fun setupPdfRenderer() {
        pdfRenderer = PdfRendererUtil()
    }

    private fun setupViewPager() {
        pdfPageAdapter = PdfPageAdapter(pdfRenderer, this, pdfViewerViewModel)
        
        // Set up single tap callback for toolbar toggle
        pdfPageAdapter.onSingleTap = {
            android.util.Log.d("PdfViewerActivity", "Single tap detected from page")
            toggleToolbar()
        }
        
        binding.viewPager.apply {
            adapter = pdfPageAdapter
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            
            // Optimized touch handling - let ProfessionalZoomableImageView handle this
            
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    pdfViewerViewModel.setCurrentPage(position)
                    updatePageIndicator(position + 1)
                    // Save current page for orientation changes
                    savedCurrentPage = position
                    // Trigger preloading of adjacent pages for smoother swipes
                    pdfPageAdapter.preloadAdjacentPages(position)
                }
            })
        }
    }

    private fun setupGestureDetector() {
        // No longer needed - tap detection is handled by individual page views
        // ViewPager2 will handle swipes naturally
    }
    
    private fun setupClickListeners() {
        // Controls overlay will be handled by tap gestures
    }
    
    private fun setupZoomControls() {
        // Zoom controls removed - using native pinch-to-zoom
    }

    private fun observeViewModel() {
        pdfViewerViewModel.currentPage.observe(this) { currentPage ->
            updatePageIndicator(currentPage + 1)
        }

        pdfViewerViewModel.totalPages.observe(this) { _ ->
            updatePageIndicator(pdfViewerViewModel.currentPage.value?.plus(1) ?: 1)
        }
    }

    private fun loadPdfFromIntent() {
        val uri = intent.data
        if (uri == null) {
            android.util.Log.e("PdfViewerActivity", "No PDF file URI provided")
            showError("No PDF file provided")
            return
        }

        android.util.Log.d("PdfViewerActivity", "Loading PDF from URI: $uri")
        
        fileName = FileUtil.getFileName(this, uri) ?: "Document"
        binding.toolbar.title = fileName
        android.util.Log.d("PdfViewerActivity", "PDF file name: $fileName")

        // Store URI for consistent file identification
        pdfViewerViewModel.setCurrentFile(uri.toString())

        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.textViewError.visibility = View.GONE
                binding.toolbar.visibility = View.VISIBLE
                
                android.util.Log.d("PdfViewerActivity", "Attempting to open PDF...")
                
                // Check if we can access the URI first
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val available = inputStream?.available() ?: 0
                    inputStream?.close()
                    android.util.Log.d("PdfViewerActivity", "URI accessible, estimated size: $available bytes")
                    
                    if (available == 0) {
                        android.util.Log.w("PdfViewerActivity", "File appears to be empty")
                        // Don't return here, continue to try opening - some files report 0 size initially
                    }
                } catch (e: SecurityException) {
                    android.util.Log.e("PdfViewerActivity", "Security exception accessing URI", e)
                    showError("Permission denied accessing file. Please grant storage permissions and try again.")
                    return@launch
                } catch (e: Exception) {
                    android.util.Log.e("PdfViewerActivity", "Cannot access URI: ${e.javaClass.simpleName}: ${e.message}", e)
                    showError("Cannot access the selected file: ${e.message}")
                    return@launch
                }
                
                val success = pdfRenderer.openPdf(this@PdfViewerActivity, uri)
                
                if (success) {
                    val pageCount = pdfRenderer.getPageCount()
                    android.util.Log.d("PdfViewerActivity", "PDF opened successfully with $pageCount pages")
                    
                    if (pageCount <= 0) {
                        android.util.Log.e("PdfViewerActivity", "PDF has no pages")
                        showError("This PDF file appears to be empty or corrupted")
                        return@launch
                    }
                    
                    pdfViewerViewModel.setTotalPages(pageCount)
                    pdfPageAdapter.setPageCount(pageCount)
                    
                    binding.progressBar.visibility = View.GONE
                    binding.toolbar.visibility = View.VISIBLE
                    
                    updatePageIndicator(1)
                    
                    // Restore saved page position
                    if (savedCurrentPage > 0 && savedCurrentPage < pageCount) {
                        binding.viewPager.setCurrentItem(savedCurrentPage, false)
                        updatePageIndicator(savedCurrentPage + 1)
                        android.util.Log.d("PdfViewerActivity", "Restored to page: ${savedCurrentPage + 1}")
                    }
                    
                    // Start with toolbar visible, then auto-hide after delay
                    showToolbarTemporarily()
                    android.util.Log.d("PdfViewerActivity", "PDF viewer setup completed successfully")
                } else {
                    android.util.Log.e("PdfViewerActivity", "Failed to open PDF file")
                    showError("Failed to open PDF file. The file may be corrupted or password-protected.")
                }
            } catch (e: SecurityException) {
                android.util.Log.e("PdfViewerActivity", "Security exception while loading PDF", e)
                showError("Permission denied. Please grant storage access and try again.")
            } catch (e: OutOfMemoryError) {
                android.util.Log.e("PdfViewerActivity", "Out of memory while loading PDF", e)
                showError("File too large. Please try a smaller PDF file.")
            } catch (e: Exception) {
                android.util.Log.e("PdfViewerActivity", "Exception while loading PDF: ${e.javaClass.simpleName}", e)
                val errorMessage = when {
                    e.message?.contains("corrupted") == true -> "The PDF file appears to be corrupted"
                    e.message?.contains("password") == true -> "This PDF is password-protected and cannot be opened"
                    e.message?.contains("permission") == true -> "Permission denied accessing the file"
                    else -> "Error loading PDF: ${e.message ?: "Unknown error"}"
                }
                showError(errorMessage)
            }
        }
    }

    private fun updatePageIndicator(@Suppress("UNUSED_PARAMETER") currentPage: Int) {
        // Page indicator removed - no UI update needed
    }


    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.textViewError.visibility = View.VISIBLE
        binding.textViewError.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    private fun toggleToolbar() {
        android.util.Log.d("PdfViewerActivity", "toggleToolbar called, current toolbarVisible: $toolbarVisible")
        if (toolbarVisible) {
            hideToolbar()
        } else {
            showToolbarTemporarily()
        }
    }
    
    private fun showToolbarTemporarily() {
        showToolbar()
        hideToolbarAfterDelay()
    }
    
    private fun showToolbar() {
        android.util.Log.d("PdfViewerActivity", "showToolbar called")
        binding.toolbar.visibility = View.VISIBLE
        toolbarVisible = true
        android.util.Log.d("PdfViewerActivity", "Toolbar shown")
    }
    
    private fun hideToolbar() {
        android.util.Log.d("PdfViewerActivity", "hideToolbar called")
        binding.toolbar.visibility = View.GONE
        toolbarVisible = false
        toolbarHideRunnable?.let { toolbarHandler.removeCallbacks(it) }
        android.util.Log.d("PdfViewerActivity", "Toolbar hidden")
    }
    
    private fun hideToolbarAfterDelay() {
        // Remove any existing hide callback
        toolbarHideRunnable?.let { toolbarHandler.removeCallbacks(it) }
        
        // Schedule hide after 3 seconds
        toolbarHideRunnable = Runnable {
            hideToolbar()
        }
        toolbarHandler.postDelayed(toolbarHideRunnable!!, 3000)
    }
    
    
    private fun getCurrentZoomableView(): ProfessionalZoomableImageView? {
        return try {
            val currentPosition = binding.viewPager.currentItem
            pdfPageAdapter.getCurrentZoomableView(currentPosition)
        } catch (e: Exception) {
            android.util.Log.e("PdfViewerActivity", "Error getting current zoomable view", e)
            null
        }
    }
    
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("current_page", binding.viewPager.currentItem)
        android.util.Log.d("PdfViewerActivity", "Saved current page: ${binding.viewPager.currentItem}")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        toolbarHideRunnable?.let { toolbarHandler.removeCallbacks(it) }
        pdfPageAdapter.clearCache()
        pdfRenderer.closePdf()
    }
}