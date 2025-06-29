package com.example.quickpdf.ui.adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.quickpdf.ui.custom.ProfessionalZoomableImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.quickpdf.R
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
    private val viewHolders = mutableMapOf<Int, PdfPageViewHolder>()
    
    // Callback for zoom level changes
    var onZoomChanged: ((Float) -> Unit)? = null

    fun setPageCount(count: Int) {
        pageCount = count
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = pageCount

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfPageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pdf_page, parent, false)
        return PdfPageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PdfPageViewHolder, position: Int) {
        viewHolders[position] = holder
        holder.bind(position)
    }

    override fun onViewRecycled(holder: PdfPageViewHolder) {
        super.onViewRecycled(holder)
        // Remove from viewHolders map
        val position = viewHolders.entries.find { it.value == holder }?.key
        position?.let { viewHolders.remove(it) }
        holder.cleanup()
    }

    inner class PdfPageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        val imageViewPage: ProfessionalZoomableImageView = itemView.findViewById(R.id.imageViewPage)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val textViewError: TextView = itemView.findViewById(R.id.textViewError)

        fun bind(pageIndex: Int) {
            android.util.Log.d("PdfPageAdapter", "Binding page $pageIndex")
            progressBar.visibility = View.VISIBLE
            textViewError.visibility = View.GONE
            imageViewPage.setImageBitmap(null)

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
                    android.util.Log.d("PdfPageAdapter", "Starting render for page $pageIndex")
                    val bitmap = withContext(Dispatchers.IO) {
                        val displayMetrics = itemView.context.resources.displayMetrics
                        val screenWidth = displayMetrics.widthPixels
                        val screenHeight = displayMetrics.heightPixels
                        
                        android.util.Log.d("PdfPageAdapter", "Screen dimensions: ${screenWidth}x$screenHeight")
                        
                        // Use simple fit-to-width rendering for now
                        pdfRenderer.renderPageWithAspectRatio(pageIndex, screenWidth, screenHeight)
                    }

                    if (bitmap != null && !bitmap.isRecycled) {
                        android.util.Log.d("PdfPageAdapter", "Page $pageIndex rendered successfully: ${bitmap.width}x${bitmap.height}")
                        pageCache[pageIndex] = bitmap
                        displayBitmap(bitmap)
                    } else {
                        android.util.Log.e("PdfPageAdapter", "Page $pageIndex rendering failed - bitmap is null or recycled")
                        showError()
                    }
                } catch (e: SecurityException) {
                    android.util.Log.e("PdfPageAdapter", "Security exception rendering page $pageIndex", e)
                    showError()
                } catch (e: OutOfMemoryError) {
                    android.util.Log.e("PdfPageAdapter", "Out of memory rendering page $pageIndex", e)
                    showError()
                } catch (e: Exception) {
                    android.util.Log.e("PdfPageAdapter", "Error rendering page $pageIndex: ${e.javaClass.simpleName}: ${e.message}", e)
                    showError()
                }
            }
        }

        private fun displayBitmap(bitmap: Bitmap) {
            progressBar.visibility = View.GONE
            textViewError.visibility = View.GONE
            imageViewPage.setImageBitmap(bitmap)
            
            // Set up zoom change callback
            imageViewPage.onZoomChanged = { zoomLevel ->
                onZoomChanged?.invoke(zoomLevel)
            }
        }

        private fun showError() {
            progressBar.visibility = View.GONE
            textViewError.visibility = View.VISIBLE
            imageViewPage.setImageBitmap(null)
        }

        fun cleanup() {
            imageViewPage.setImageBitmap(null)
            imageViewPage.resetToFitScreen()
            imageViewPage.onZoomChanged = null
        }
    }

    fun clearCache() {
        pageCache.values.forEach { bitmap ->
            bitmap?.takeIf { !it.isRecycled }?.recycle()
        }
        pageCache.clear()
    }

    
    fun getCurrentZoomableView(position: Int): ProfessionalZoomableImageView? {
        return viewHolders[position]?.imageViewPage
    }

    fun updateViewMode() {
        // Clear cache and reload pages with new view mode
        clearCache()
        notifyDataSetChanged()
    }
}