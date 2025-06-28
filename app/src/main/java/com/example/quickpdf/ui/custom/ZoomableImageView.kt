package com.example.quickpdf.ui.custom

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.sqrt

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val matrix = Matrix()
    private val savedMatrix = Matrix()
    
    private var mode = NONE
    private val start = PointF()
    private val mid = PointF()
    private var oldDist = 1f
    
    private var minScale = 0.5f
    private var maxScale = 4.0f
    private var currentScale = 1.0f
    
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, GestureListener())
    
    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }

    init {
        scaleType = ScaleType.MATRIX
        setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            handleTouch(event)
            true
        }
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        resetZoom()
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                start.set(event.x, event.y)
                mode = DRAG
            }
            
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                if (oldDist > 10f) {
                    savedMatrix.set(matrix)
                    midPoint(mid, event)
                    mode = ZOOM
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    matrix.set(savedMatrix)
                    val dx = event.x - start.x
                    val dy = event.y - start.y
                    matrix.postTranslate(dx, dy)
                } else if (mode == ZOOM && event.pointerCount == 2) {
                    val newDist = spacing(event)
                    if (newDist > 10f) {
                        matrix.set(savedMatrix)
                        val scale = newDist / oldDist
                        matrix.postScale(scale, scale, mid.x, mid.y)
                    }
                }
                
                checkAndSetMatrix()
            }
        }
        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            currentScale *= scaleFactor
            currentScale = Math.max(minScale, Math.min(currentScale, maxScale))
            
            matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            checkAndSetMatrix()
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val targetScale = if (currentScale > 1.5f) 1.0f else 2.0f
            val scaleFactor = targetScale / currentScale
            currentScale = targetScale
            
            matrix.postScale(scaleFactor, scaleFactor, e.x, e.y)
            checkAndSetMatrix()
            return true
        }
    }

    private fun checkAndSetMatrix() {
        val values = FloatArray(9)
        matrix.getValues(values)
        
        val scaleX = values[Matrix.MSCALE_X]
        val scaleY = values[Matrix.MSCALE_Y]
        val transX = values[Matrix.MTRANS_X]
        val transY = values[Matrix.MTRANS_Y]
        
        currentScale = scaleX
        
        // Constrain scale
        if (scaleX < minScale || scaleY < minScale) {
            val scale = minScale / Math.min(scaleX, scaleY)
            matrix.postScale(scale, scale)
            currentScale = minScale
        } else if (scaleX > maxScale || scaleY > maxScale) {
            val scale = maxScale / Math.max(scaleX, scaleY)
            matrix.postScale(scale, scale)
            currentScale = maxScale
        }
        
        // Constrain translation
        val drawable = drawable
        if (drawable != null) {
            val drawableWidth = drawable.intrinsicWidth * currentScale
            val drawableHeight = drawable.intrinsicHeight * currentScale
            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()
            
            var deltaX = 0f
            var deltaY = 0f
            
            if (drawableWidth <= viewWidth) {
                deltaX = (viewWidth - drawableWidth) / 2 - transX
            } else {
                if (transX > 0) deltaX = -transX
                else if (transX < -(drawableWidth - viewWidth)) {
                    deltaX = -(drawableWidth - viewWidth) - transX
                }
            }
            
            if (drawableHeight <= viewHeight) {
                deltaY = (viewHeight - drawableHeight) / 2 - transY
            } else {
                if (transY > 0) deltaY = -transY
                else if (transY < -(drawableHeight - viewHeight)) {
                    deltaY = -(drawableHeight - viewHeight) - transY
                }
            }
            
            matrix.postTranslate(deltaX, deltaY)
        }
        
        imageMatrix = matrix
    }

    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }

    fun resetZoom() {
        matrix.reset()
        currentScale = 1.0f
        imageMatrix = matrix
    }

    fun setNightMode(enabled: Boolean) {
        colorFilter = if (enabled) {
            android.graphics.ColorMatrixColorFilter(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        } else {
            null
        }
    }
}