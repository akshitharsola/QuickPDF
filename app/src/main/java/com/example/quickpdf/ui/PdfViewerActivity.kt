package com.example.quickpdf.ui

import android.os.Bundle
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
import com.example.quickpdf.utils.FileUtil
import com.example.quickpdf.utils.PdfRendererUtil
import kotlinx.coroutines.launch

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding
    private lateinit var pdfRenderer: PdfRendererUtil
    private lateinit var pdfPageAdapter: PdfPageAdapter
    
    private val pdfViewerViewModel: PdfViewerViewModel by viewModels {
        ViewModelFactory((application as QuickPDFApplication).repository)
    }

    private var fileName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupPdfRenderer()
        setupViewPager()
        setupClickListeners()
        observeViewModel()
        loadPdfFromIntent()
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfPageAdapter.clearCache()
        pdfRenderer.closePdf()
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
        
        binding.viewPager.apply {
            adapter = pdfPageAdapter
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    pdfViewerViewModel.setCurrentPage(position)
                    updatePageIndicator(position + 1)
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

        // Get file path for bookmarks
        val filePath = FileUtil.getRealPathFromURI(this, uri) ?: uri.toString()
        pdfViewerViewModel.setCurrentFile(filePath)

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
}