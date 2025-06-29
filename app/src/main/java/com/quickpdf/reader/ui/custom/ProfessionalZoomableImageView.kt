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
    
    // Zoom level constants
    private var fitToWidthScale = 1.0f
    private var originalScale = 1.0f
    
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
        setAllowParentInterceptOnEdge(false)
        
        // Advanced parent scroll interference prevention
        setOnMatrixChangeListener {
            // Completely disable parent scrolling when we're zoomed in or panning
            val isZoomedIn = scale > minimumScale + 0.05f
            
            // Request parent to not intercept touch events when zoomed
            parent?.requestDisallowInterceptTouchEvent(isZoomedIn)
            
            // Also disable grandparent (ViewPager2) if needed
            var currentParent = parent
            while (currentParent != null && isZoomedIn) {
                if (currentParent is androidx.viewpager2.widget.ViewPager2) {
                    currentParent.isUserInputEnabled = !isZoomedIn
                    break
                }
                currentParent = currentParent.parent as? android.view.ViewGroup
            }
        }
        
        // Handle touch events to provide immediate feedback
        setOnPhotoTapListener { _, _, _ ->
            // Single tap - do nothing, let parent handle
        }
        
        setOnOutsidePhotoTapListener {
            // Tap outside photo - could be used for hiding controls
        }
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        post {
            calculateScales()
            resetToFitScreen()
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
    
    fun resetToFitScreen() {
        post {
            scale = originalScale
            onZoomChanged?.invoke(scale)
        }
    }
    
    fun setZoomMode(mode: ZoomMode) {
        val targetScale = when (mode) {
            ZoomMode.FIT_TO_SCREEN -> originalScale
            ZoomMode.FIT_TO_WIDTH -> fitToWidthScale
            ZoomMode.ORIGINAL_SIZE -> 1.0f
        }
        
        post {
            setScale(targetScale, true)
            onZoomChanged?.invoke(targetScale)
        }
    }
    
    fun animateZoomTo(targetScale: Float, focusX: Float, focusY: Float) {
        post {
            setScale(targetScale, focusX, focusY, true)
            onZoomChanged?.invoke(targetScale)
        }
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