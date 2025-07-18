package com.quickpdf.reader.ui.custom

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import com.github.chrisbanes.photoview.PhotoView
import com.github.chrisbanes.photoview.OnScaleChangedListener

class ProfessionalZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PhotoView(context, attrs, defStyleAttr) {

    // Callback for zoom level changes
    var onZoomChanged: ((Float) -> Unit)? = null
    
    // Callback for single tap
    var onSingleTap: (() -> Unit)? = null
    
    // Zoom level constants
    private var fitToWidthScale = 1.0f
    private var originalScale = 1.0f
    
    // Cache ViewPager2 reference to avoid repeated parent traversal
    private var cachedViewPager: androidx.viewpager2.widget.ViewPager2? = null
    private var lastZoomState = false
    
    init {
        // Configure PhotoView settings for optimal PDF viewing
        isZoomable = true
        maximumScale = 5.0f
        minimumScale = 0.5f
        mediumScale = 2.0f
        
        // Set up zoom change listener
        setOnScaleChangeListener(object : OnScaleChangedListener {
            override fun onScaleChange(scaleFactor: Float, focusX: Float, focusY: Float) {
                onZoomChanged?.invoke(scale)
            }
        })
        
        // Enable all gestures
        setAllowParentInterceptOnEdge(true)
        
        // Optimized parent scroll interference prevention
        setOnMatrixChangeListener {
            // More robust zoom detection - only disable when significantly zoomed beyond original
            val zoomThreshold = originalScale * 1.15f // 15% beyond original scale
            val isZoomedIn = scale > zoomThreshold
            
            // Only update if zoom state actually changed to reduce interference
            if (isZoomedIn != lastZoomState) {
                lastZoomState = isZoomedIn
                
                // Request parent to not intercept touch events when zoomed
                parent?.requestDisallowInterceptTouchEvent(isZoomedIn)
                
                // Use cached ViewPager2 reference or find it once
                if (cachedViewPager == null) {
                    cachedViewPager = findViewPager2()
                }
                
                cachedViewPager?.isUserInputEnabled = !isZoomedIn
            }
        }
        
        // Handle touch events to provide immediate feedback
        setOnPhotoTapListener { _, _, _ ->
            // Single tap on photo - notify parent
            onSingleTap?.invoke()
        }
        
        setOnOutsidePhotoTapListener {
            // Tap outside photo - also notify parent
            onSingleTap?.invoke()
        }
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        post {
            calculateScales()
            // Use fit-to-screen as default for better experience in all orientations
            setZoomMode(ZoomMode.FIT_TO_SCREEN)
        }
    }
    
    private fun calculateScales() {
        val drawable = drawable ?: return
        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        
        if (drawableWidth > 0 && drawableHeight > 0 && viewWidth > 0 && viewHeight > 0) {
            // Calculate fit-to-width scale
            fitToWidthScale = viewWidth / drawableWidth
            
            // Calculate fit-to-screen scale
            val scaleX = viewWidth / drawableWidth
            val scaleY = viewHeight / drawableHeight
            originalScale = kotlin.math.min(scaleX, scaleY)
            
            // Update PhotoView scales
            minimumScale = kotlin.math.min(0.5f, originalScale * 0.5f)
            mediumScale = kotlin.math.max(fitToWidthScale, originalScale) * 1.5f
            maximumScale = 5.0f
        }
    }
    
    private fun findViewPager2(): androidx.viewpager2.widget.ViewPager2? {
        var currentParent = parent
        while (currentParent != null) {
            if (currentParent is androidx.viewpager2.widget.ViewPager2) {
                return currentParent
            }
            currentParent = currentParent.parent as? android.view.ViewGroup
        }
        return null
    }
    
    fun resetToFitScreen() {
        scale = originalScale
        onZoomChanged?.invoke(scale)
        
        // Ensure ViewPager2 is re-enabled when resetting zoom
        if (cachedViewPager == null) {
            cachedViewPager = findViewPager2()
        }
        cachedViewPager?.isUserInputEnabled = true
    }
    
    fun setZoomMode(mode: ZoomMode) {
        val targetScale = when (mode) {
            ZoomMode.FIT_TO_SCREEN -> originalScale
            ZoomMode.FIT_TO_WIDTH -> fitToWidthScale
            ZoomMode.ORIGINAL_SIZE -> 1.0f
        }
        
        setScale(targetScale, true)
        onZoomChanged?.invoke(targetScale)
        
        // Ensure ViewPager2 is properly enabled for fit modes
        if (mode == ZoomMode.FIT_TO_SCREEN || mode == ZoomMode.FIT_TO_WIDTH) {
            if (cachedViewPager == null) {
                cachedViewPager = findViewPager2()
            }
            cachedViewPager?.isUserInputEnabled = true
        }
    }
    
    fun animateZoomTo(targetScale: Float, focusX: Float, focusY: Float) {
        setScale(targetScale, focusX, focusY, true)
        onZoomChanged?.invoke(targetScale)
    }
    
    fun getCurrentScale(): Float = scale
    
    fun getFitToWidthScale(): Float = fitToWidthScale
    
    fun getOriginalScale(): Float = originalScale
    
    fun getMinScale(): Float = minimumScale
    
    fun getMaxScale(): Float = maximumScale
    
    fun zoomIn() {
        val newScale = (scale * 1.2f).coerceAtMost(maximumScale)
        val centerX = width / 2f
        val centerY = height / 2f
        animateZoomTo(newScale, centerX, centerY)
    }
    
    fun zoomOut() {
        val newScale = (scale * 0.85f).coerceAtLeast(minimumScale)
        val centerX = width / 2f
        val centerY = height / 2f
        animateZoomTo(newScale, centerX, centerY)
    }
    
    enum class ZoomMode {
        FIT_TO_SCREEN,
        FIT_TO_WIDTH,
        ORIGINAL_SIZE
    }
}