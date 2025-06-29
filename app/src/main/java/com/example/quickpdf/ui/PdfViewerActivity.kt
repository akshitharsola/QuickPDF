package com.example.quickpdf.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.quickpdf.QuickPDFApplication
import com.example.quickpdf.R
import com.example.quickpdf.databinding.ActivityPdfViewerBinding
import com.example.quickpdf.ui.adapters.PdfPageAdapter
import com.example.quickpdf.ui.custom.ProfessionalZoomableImageView
import com.example.quickpdf.utils.FileUtil
import com.example.quickpdf.utils.PdfRendererUtil
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding
    private lateinit var pdfRenderer: PdfRendererUtil
    private lateinit var pdfPageAdapter: PdfPageAdapter
    
    private val pdfViewerViewModel: PdfViewerViewModel by viewModels {
        ViewModelFactory((application as QuickPDFApplication).repository)
    }

    private var fileName: String = ""
    private var zoomControlsVisible = false
    private val zoomControlsHandler = Handler(Looper.getMainLooper())
    private var zoomControlsHideRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupPdfRenderer()
        setupViewPager()
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
    }

    private fun setupPdfRenderer() {
        pdfRenderer = PdfRendererUtil()
    }

    private fun setupViewPager() {
        pdfPageAdapter = PdfPageAdapter(pdfRenderer, this, pdfViewerViewModel)
        
        // Set up zoom change callback
        pdfPageAdapter.onZoomChanged = { zoomLevel ->
            updateZoomLevelDisplay()
            showZoomControlsTemporarily()
        }
        
        binding.viewPager.apply {
            adapter = pdfPageAdapter
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            
            // Reduce sensitivity to prevent accidental page turns during zooming
            (getChildAt(0) as? androidx.recyclerview.widget.RecyclerView)?.let { recyclerView ->
                recyclerView.addOnItemTouchListener(object : androidx.recyclerview.widget.RecyclerView.SimpleOnItemTouchListener() {
                    override fun onInterceptTouchEvent(rv: androidx.recyclerview.widget.RecyclerView, e: android.view.MotionEvent): Boolean {
                        // Check if any page is zoomed in - if so, disable ViewPager swiping
                        val currentView = getCurrentZoomableView()
                        val isZoomedIn = currentView?.let { view ->
                            view.getCurrentScale() > (view.getOriginalScale() + 0.1f)
                        } ?: false
                        
                        // When zoomed in, prevent ViewPager from intercepting touch events
                        return isZoomedIn
                    }
                })
            }
            
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    pdfViewerViewModel.setCurrentPage(position)
                    updatePageIndicator(position + 1)
                    // Update zoom level display for new page
                    if (zoomControlsVisible) {
                        updateZoomLevelDisplay()
                    }
                }
            })
        }
    }

    private fun setupClickListeners() {
        binding.buttonPreviousPage.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem > 0) {
                binding.viewPager.currentItem = currentItem - 1
            }
        }

        binding.buttonNextPage.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem < pdfPageAdapter.itemCount - 1) {
                binding.viewPager.currentItem = currentItem + 1
            }
        }
        
        binding.buttonToggleZoomControls.setOnClickListener {
            toggleZoomControls()
        }
    }
    
    private fun setupZoomControls() {
        binding.buttonZoomIn.setOnClickListener {
            getCurrentZoomableView()?.zoomIn()
            showZoomControlsTemporarily()
        }
        
        binding.buttonZoomOut.setOnClickListener {
            getCurrentZoomableView()?.zoomOut()
            showZoomControlsTemporarily()
        }
        
        binding.buttonFitToWidth.setOnClickListener {
            getCurrentZoomableView()?.setZoomMode(ProfessionalZoomableImageView.ZoomMode.FIT_TO_WIDTH)
            showZoomControlsTemporarily()
        }
        
        binding.buttonFitToScreen.setOnClickListener {
            getCurrentZoomableView()?.setZoomMode(ProfessionalZoomableImageView.ZoomMode.FIT_TO_SCREEN)
            showZoomControlsTemporarily()
        }
    }

    private fun observeViewModel() {
        pdfViewerViewModel.currentPage.observe(this) { currentPage ->
            updatePageIndicator(currentPage + 1)
            updateNavigationButtons(currentPage)
        }

        pdfViewerViewModel.totalPages.observe(this) { totalPages ->
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
                binding.layoutPageControls.visibility = View.GONE
                
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
                    binding.layoutPageControls.visibility = View.VISIBLE
                    
                    updatePageIndicator(1)
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

    private fun updatePageIndicator(currentPage: Int) {
        val totalPages = pdfViewerViewModel.totalPages.value ?: 0
        binding.textViewPageIndicator.text = "Page $currentPage of $totalPages"
    }

    private fun updateNavigationButtons(currentPage: Int) {
        val totalPages = pdfViewerViewModel.totalPages.value ?: 0
        binding.buttonPreviousPage.isEnabled = currentPage > 0
        binding.buttonNextPage.isEnabled = currentPage < totalPages - 1
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.textViewError.visibility = View.VISIBLE
        binding.textViewError.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    private fun toggleZoomControls() {
        zoomControlsVisible = !zoomControlsVisible
        binding.layoutZoomControls.visibility = if (zoomControlsVisible) View.VISIBLE else View.GONE
        
        if (zoomControlsVisible) {
            updateZoomLevelDisplay()
            showZoomControlsTemporarily()
        } else {
            zoomControlsHideRunnable?.let { zoomControlsHandler.removeCallbacks(it) }
        }
    }
    
    private fun showZoomControlsTemporarily() {
        if (!zoomControlsVisible) {
            binding.layoutZoomControls.visibility = View.VISIBLE
            zoomControlsVisible = true
        }
        
        updateZoomLevelDisplay()
        
        // Remove any existing hide callback
        zoomControlsHideRunnable?.let { zoomControlsHandler.removeCallbacks(it) }
        
        // Schedule hide after 3 seconds
        zoomControlsHideRunnable = Runnable {
            binding.layoutZoomControls.visibility = View.GONE
            zoomControlsVisible = false
        }
        zoomControlsHandler.postDelayed(zoomControlsHideRunnable!!, 3000)
    }
    
    private fun updateZoomLevelDisplay() {
        getCurrentZoomableView()?.let { zoomView ->
            val zoomPercent = (zoomView.getCurrentScale() * 100).roundToInt()
            binding.textViewZoomLevel.text = "$zoomPercent%"
            
            // Update zoom button states based on current scale
            val currentScale = zoomView.getCurrentScale()
            binding.buttonZoomIn.isEnabled = currentScale < zoomView.getMaxScale() - 0.01f
            binding.buttonZoomOut.isEnabled = currentScale > zoomView.getMinScale() + 0.01f
        }
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
    
    
    override fun onDestroy() {
        super.onDestroy()
        zoomControlsHideRunnable?.let { zoomControlsHandler.removeCallbacks(it) }
        pdfPageAdapter.clearCache()
        pdfRenderer.closePdf()
    }
}