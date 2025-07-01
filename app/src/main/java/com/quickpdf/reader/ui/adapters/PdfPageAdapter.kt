package com.quickpdf.reader.ui.adapters

import android.graphics.Bitmap
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.quickpdf.reader.ui.custom.ProfessionalZoomableImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.quickpdf.reader.R
import com.quickpdf.reader.ui.PdfViewerViewModel
import com.quickpdf.reader.utils.PdfRendererUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PdfPageAdapter(
    private val pdfRenderer: PdfRendererUtil,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: PdfViewerViewModel
) : RecyclerView.Adapter<PdfPageAdapter.PdfPageViewHolder>() {

    private var pageCount = 0
    private val viewHolders = mutableMapOf<Int, PdfPageViewHolder>()
    private val renderJobs = mutableMapOf<Int, Job>()
    private var currentVisiblePage = 0
    
    // Cache screen dimensions to avoid repeated calculations
    private var cachedScreenWidth = 0
    private var cachedScreenHeight = 0
    private var isScreenDimensionsCached = false
    
    // LRU Cache with memory-based limits (25% of available heap)
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 4
    private val pageCache = object : LruCache<Int, Bitmap>(cacheSize) {
        override fun sizeOf(key: Int, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
        
        override fun entryRemoved(evicted: Boolean, key: Int, oldValue: Bitmap, newValue: Bitmap?) {
            if (evicted && !oldValue.isRecycled) {
                oldValue.recycle()
            }
        }
    }
    
    // Callback for single tap
    var onSingleTap: (() -> Unit)? = null

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
        // Remove from viewHolders map and cancel any pending render jobs
        val position = viewHolders.entries.find { it.value == holder }?.key
        position?.let { 
            viewHolders.remove(it)
            renderJobs[it]?.cancel()
            renderJobs.remove(it)
        }
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
            pageCache.get(pageIndex)?.let { cachedBitmap ->
                if (!cachedBitmap.isRecycled) {
                    displayBitmap(cachedBitmap)
                    return
                }
            }

            // Cancel any existing render job for this page
            renderJobs[pageIndex]?.cancel()
            
            // Load page asynchronously
            val job = lifecycleOwner.lifecycleScope.launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        // Cache screen dimensions on first use
                        if (!isScreenDimensionsCached) {
                            val displayMetrics = itemView.context.resources.displayMetrics
                            cachedScreenWidth = displayMetrics.widthPixels
                            cachedScreenHeight = displayMetrics.heightPixels
                            isScreenDimensionsCached = true
                            // Screen dimensions cached
                        }
                        
                        // Use cached dimensions
                        pdfRenderer.renderPageWithAspectRatio(pageIndex, cachedScreenWidth, cachedScreenHeight)
                    }

                    if (bitmap != null && !bitmap.isRecycled) {
                        pageCache.put(pageIndex, bitmap)
                        displayBitmap(bitmap)
                        
                        // Trigger preloading of adjacent pages
                        preloadAdjacentPages(pageIndex)
                    } else {
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
                } finally {
                    renderJobs.remove(pageIndex)
                }
            }
            renderJobs[pageIndex] = job
        }

        private fun displayBitmap(bitmap: Bitmap) {
            progressBar.visibility = View.GONE
            textViewError.visibility = View.GONE
            imageViewPage.setImageBitmap(bitmap)
            
            // Set up single tap callback
            imageViewPage.onSingleTap = {
                onSingleTap?.invoke()
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
            imageViewPage.onSingleTap = null
        }
    }

    fun clearCache() {
        pageCache.evictAll()
        // Cancel all pending render jobs
        renderJobs.values.forEach { it.cancel() }
        renderJobs.clear()
    }

    
    fun getCurrentZoomableView(position: Int): ProfessionalZoomableImageView? {
        return viewHolders[position]?.imageViewPage
    }

    fun updateViewMode() {
        // Clear cache and reload pages with new view mode
        clearCache()
        notifyDataSetChanged()
    }
    
    fun preloadAdjacentPages(currentPage: Int) {
        currentVisiblePage = currentPage
        
        // Only preload if we're not already rendering too many pages
        if (renderJobs.size >= 3) {
            android.util.Log.d("PdfPageAdapter", "Skipping preload due to concurrent renders: ${renderJobs.size}")
            return
        }
        
        // Prioritize next page over previous (forward reading)
        if (currentPage + 1 < pageCount) {
            preloadPage(currentPage + 1)
        }
        
        if (currentPage - 1 >= 0) {
            preloadPage(currentPage - 1)
        }
    }
    
    private fun preloadPage(pageIndex: Int) {
        // Skip if already cached or currently rendering
        if (pageCache.get(pageIndex) != null || renderJobs.containsKey(pageIndex)) {
            return
        }
        
        // Skip preloading if we're low on memory
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val memoryUsagePercent = (usedMemory * 100) / maxMemory
        
        if (memoryUsagePercent > 75) {
            android.util.Log.d("PdfPageAdapter", "Skipping preload due to memory pressure: ${memoryUsagePercent}%")
            return
        }
        
        // Preload with lower priority
        val job = lifecycleOwner.lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    // Use cached screen dimensions
                    if (!isScreenDimensionsCached) {
                        val displayMetrics = (lifecycleOwner as android.app.Activity).resources.displayMetrics
                        cachedScreenWidth = displayMetrics.widthPixels
                        cachedScreenHeight = displayMetrics.heightPixels
                        isScreenDimensionsCached = true
                    }
                    
                    pdfRenderer.renderPageWithAspectRatio(pageIndex, cachedScreenWidth, cachedScreenHeight)
                }
                
                if (bitmap != null && !bitmap.isRecycled) {
                    pageCache.put(pageIndex, bitmap)
                }
            } catch (e: Exception) {
                // Silently fail for preloading
            } finally {
                renderJobs.remove(pageIndex)
            }
        }
        renderJobs[pageIndex] = job
    }
}