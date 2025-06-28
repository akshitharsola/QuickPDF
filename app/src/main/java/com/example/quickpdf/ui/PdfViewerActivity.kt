package com.example.quickpdf.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
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

    private var isControlsVisible = true
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

        // Toggle controls visibility on page tap
        binding.viewPager.setOnClickListener {
            toggleControlsVisibility()
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

        pdfViewerViewModel.isNightMode.observe(this) { isNightMode ->
            pdfPageAdapter.updateNightMode(isNightMode)
        }

        pdfViewerViewModel.viewMode.observe(this) { viewMode ->
            pdfPageAdapter.updateViewMode()
        }
    }

    private fun loadPdfFromIntent() {
        val uri = intent.data
        if (uri == null) {
            showError("No PDF file provided")
            return
        }

        fileName = FileUtil.getFileName(this, uri) ?: "Document"
        supportActionBar?.title = fileName

        // Get file path for bookmarks
        val filePath = FileUtil.getRealPathFromURI(this, uri) ?: uri.toString()
        pdfViewerViewModel.setCurrentFile(filePath)

        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            
            val success = pdfRenderer.openPdf(this@PdfViewerActivity, uri)
            
            if (success) {
                val pageCount = pdfRenderer.getPageCount()
                pdfViewerViewModel.setTotalPages(pageCount)
                pdfPageAdapter.setPageCount(pageCount)
                
                binding.progressBar.visibility = View.GONE
                binding.layoutPageControls.visibility = View.VISIBLE
                
                updatePageIndicator(1)
            } else {
                showError("Failed to open PDF file")
            }
        }
    }

    private fun updatePageIndicator(currentPage: Int) {
        val totalPages = pdfViewerViewModel.totalPages.value ?: 0
        binding.textViewPageIndicator.text = getString(R.string.page_indicator, currentPage, totalPages)
    }

    private fun updateNavigationButtons(currentPage: Int) {
        val totalPages = pdfViewerViewModel.totalPages.value ?: 0
        binding.buttonPreviousPage.isEnabled = currentPage > 0
        binding.buttonNextPage.isEnabled = currentPage < totalPages - 1
    }

    private fun toggleControlsVisibility() {
        isControlsVisible = !isControlsVisible
        
        val visibility = if (isControlsVisible) View.VISIBLE else View.GONE
        binding.appBarLayout.visibility = visibility
        binding.layoutPageControls.visibility = visibility
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.textViewError.visibility = View.VISIBLE
        binding.textViewError.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.pdf_viewer_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                // TODO: Implement search functionality
                Toast.makeText(this, "Search feature coming soon", Toast.LENGTH_SHORT).show()
                true
            }
            
            R.id.action_bookmark -> {
                toggleBookmark()
                true
            }
            
            R.id.action_night_mode -> {
                toggleNightMode()
                true
            }
            
            R.id.action_fit_width -> {
                pdfViewerViewModel.setViewMode(PdfViewerViewModel.ViewMode.FIT_WIDTH)
                true
            }
            
            R.id.action_fit_page -> {
                pdfViewerViewModel.setViewMode(PdfViewerViewModel.ViewMode.FIT_PAGE)
                true
            }
            
            R.id.action_original_size -> {
                pdfViewerViewModel.setViewMode(PdfViewerViewModel.ViewMode.ORIGINAL_SIZE)
                true
            }
            
            R.id.action_bookmarks -> {
                showBookmarks()
                true
            }
            
            R.id.action_thumbnails -> {
                // TODO: Implement thumbnails view
                Toast.makeText(this, "Thumbnails feature coming soon", Toast.LENGTH_SHORT).show()
                true
            }
            
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleBookmark() {
        val currentPage = pdfViewerViewModel.currentPage.value ?: 0
        lifecycleScope.launch {
            val isBookmarked = pdfViewerViewModel.isPageBookmarked(currentPage)
            
            if (isBookmarked) {
                pdfViewerViewModel.removeBookmark(currentPage)
                Toast.makeText(this@PdfViewerActivity, R.string.bookmark_removed, Toast.LENGTH_SHORT).show()
            } else {
                showAddBookmarkDialog(currentPage)
            }
        }
    }

    private fun showAddBookmarkDialog(pageNumber: Int) {
        val editText = android.widget.EditText(this)
        editText.hint = getString(R.string.enter_bookmark_title)
        editText.setText("Page ${pageNumber + 1}")

        AlertDialog.Builder(this)
            .setTitle(R.string.add_bookmark)
            .setView(editText)
            .setPositiveButton(R.string.ok) { _, _ ->
                val title = editText.text.toString().trim()
                if (title.isNotEmpty()) {
                    pdfViewerViewModel.addBookmark(pageNumber, title)
                    Toast.makeText(this, R.string.bookmark_added, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun toggleNightMode() {
        val currentNightMode = pdfViewerViewModel.isNightMode.value ?: false
        pdfViewerViewModel.setNightMode(!currentNightMode)
    }

    private fun showBookmarks() {
        pdfViewerViewModel.getBookmarksForCurrentFile()?.observe(this) { bookmarks ->
            if (bookmarks.isEmpty()) {
                Toast.makeText(this, "No bookmarks found", Toast.LENGTH_SHORT).show()
                return@observe
            }

            val bookmarkTitles = bookmarks.map { "${it.title} (Page ${it.pageNumber + 1})" }.toTypedArray()
            
            AlertDialog.Builder(this)
                .setTitle(R.string.bookmarks)
                .setItems(bookmarkTitles) { _, which ->
                    val bookmark = bookmarks[which]
                    binding.viewPager.currentItem = bookmark.pageNumber
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }
}