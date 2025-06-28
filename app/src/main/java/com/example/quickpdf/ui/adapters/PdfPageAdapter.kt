package com.example.quickpdf.ui.adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.quickpdf.databinding.ItemPdfPageBinding
import com.example.quickpdf.ui.PdfViewerViewModel
import com.example.quickpdf.utils.PdfRendererUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PdfPageAdapter(
    private val pdfRenderer: PdfRendererUtil,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: PdfViewerViewModel
) : RecyclerView.Adapter<PdfPageAdapter.PdfPageViewHolder>() {

    private var pageCount = 0
    private val pageCache = mutableMapOf<Int, Bitmap?>()

    fun setPageCount(count: Int) {
        pageCount = count
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = pageCount

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfPageViewHolder {
        val binding = ItemPdfPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PdfPageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PdfPageViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun onViewRecycled(holder: PdfPageViewHolder) {
        super.onViewRecycled(holder)
        holder.cleanup()
    }

    inner class PdfPageViewHolder(
        private val binding: ItemPdfPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pageIndex: Int) {
            binding.progressBar.visibility = View.VISIBLE
            binding.textViewError.visibility = View.GONE
            binding.imageViewPage.setImageBitmap(null)

            // Check cache first
            pageCache[pageIndex]?.let { cachedBitmap ->
                if (!cachedBitmap.isRecycled) {
                    displayBitmap(cachedBitmap)
                    return
                }
            }

            // Load page asynchronously
            lifecycleOwner.lifecycleScope.launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        val displayMetrics = binding.root.context.resources.displayMetrics
                        val screenWidth = displayMetrics.widthPixels
                        val screenHeight = displayMetrics.heightPixels
                        
                        when (viewModel.viewMode.value) {
                            PdfViewerViewModel.ViewMode.FIT_WIDTH -> {
                                pdfRenderer.renderPageWithAspectRatio(pageIndex, screenWidth, screenHeight)
                            }
                            PdfViewerViewModel.ViewMode.FIT_PAGE -> {
                                pdfRenderer.renderPageWithAspectRatio(pageIndex, screenWidth, screenHeight)
                            }
                            PdfViewerViewModel.ViewMode.ORIGINAL_SIZE -> {
                                val dimensions = pdfRenderer.getPageDimensions(pageIndex)
                                if (dimensions != null) {
                                    pdfRenderer.renderPage(pageIndex, dimensions.first, dimensions.second)
                                } else {
                                    pdfRenderer.renderPageWithAspectRatio(pageIndex, screenWidth, screenHeight)
                                }
                            }
                            else -> pdfRenderer.renderPageWithAspectRatio(pageIndex, screenWidth, screenHeight)
                        }
                    }

                    if (bitmap != null) {
                        pageCache[pageIndex] = bitmap
                        displayBitmap(bitmap)
                    } else {
                        showError()
                    }
                } catch (e: Exception) {
                    showError()
                }
            }
        }

        private fun displayBitmap(bitmap: Bitmap) {
            binding.progressBar.visibility = View.GONE
            binding.textViewError.visibility = View.GONE
            binding.imageViewPage.setImageBitmap(bitmap)
            
            // Apply night mode if enabled
            viewModel.isNightMode.value?.let { isNightMode ->
                binding.imageViewPage.setNightMode(isNightMode)
            }
        }

        private fun showError() {
            binding.progressBar.visibility = View.GONE
            binding.textViewError.visibility = View.VISIBLE
            binding.imageViewPage.setImageBitmap(null)
        }

        fun cleanup() {
            binding.imageViewPage.setImageBitmap(null)
        }
    }

    fun clearCache() {
        pageCache.values.forEach { bitmap ->
            bitmap?.takeIf { !it.isRecycled }?.recycle()
        }
        pageCache.clear()
    }

    fun updateNightMode(isNightMode: Boolean) {
        // This will be handled by the ViewHolder when displaying bitmaps
        notifyDataSetChanged()
    }

    fun updateViewMode() {
        // Clear cache and reload pages with new view mode
        clearCache()
        notifyDataSetChanged()
    }
}