package com.quickpdf.reader.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.ReaderProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class PdfRendererUtil {
    
    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var currentPage: PdfRenderer.Page? = null
    private var tempFile: File? = null
    private var unlockedTempFile: File? = null
    val passwordHandler = PdfPasswordHandler()
    
    // Performance optimizations
    private val renderMutex = Mutex()
    private val pageDimensionsCache = ConcurrentHashMap<Int, Pair<Int, Int>>()
    private val aspectRatioCache = ConcurrentHashMap<String, Float>()
    private var cachedScreenWidth = 0
    private var cachedScreenHeight = 0
    
    // Maximum bitmap dimensions to prevent OOM
    private val MAX_BITMAP_WIDTH = 2048
    private val MAX_BITMAP_HEIGHT = 2048
    
    /**
     * Checks if a PDF is password-protected using iText7
     */
    suspend fun isPdfPasswordProtected(context: Context, uri: Uri): Boolean {
        return passwordHandler.isPdfPasswordProtected(context, uri)
    }
    
    /**
     * Checks if a PDF file is password-protected
     */
    suspend fun isPdfPasswordProtected(filePath: String): Boolean {
        return passwordHandler.isPdfPasswordProtected(filePath)
    }
    
    /**
     * Opens a password-protected PDF with the provided password
     */
    suspend fun openPdfWithPassword(context: Context, uri: Uri, password: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            closePdf()
            
            android.util.Log.d("PdfRendererUtil", "Attempting to unlock PDF with provided password using iText7")
            
            // First validate that the password is correct
            val isValidPassword = passwordHandler.validatePdfPassword(context, uri, password)
            if (!isValidPassword) {
                android.util.Log.e("PdfRendererUtil", "Invalid password provided")
                return@withContext false
            }
            
            android.util.Log.d("PdfRendererUtil", "Password validated successfully")
            
            // Try to unlock with iText7
            unlockedTempFile = passwordHandler.unlockPdfWithPassword(context, uri, password)
            if (unlockedTempFile == null) {
                android.util.Log.e("PdfRendererUtil", "iText7 failed to unlock PDF - this might be an owner password issue")
                // If unlock fails but password is valid, try creating a copy with password
                try {
                    unlockedTempFile = createPasswordProtectedCopy(context, uri, password)
                    if (unlockedTempFile == null) {
                        android.util.Log.e("PdfRendererUtil", "Failed to create password-protected copy")
                        return@withContext false
                    }
                    android.util.Log.d("PdfRendererUtil", "Created password-protected copy successfully")
                } catch (e: Exception) {
                    android.util.Log.e("PdfRendererUtil", "Error creating password-protected copy", e)
                    return@withContext false
                }
            }
            
            android.util.Log.d("PdfRendererUtil", "PDF unlocked successfully, opening temporary file")
            
            // Open the unlocked PDF using existing method (but preserve the temp file reference)
            val tempFilePath = unlockedTempFile!!.absolutePath
            val tempFileToPreserve = unlockedTempFile
            unlockedTempFile = null // Temporarily clear to prevent closePdf() from deleting it
            val success = openPdf(tempFilePath)
            unlockedTempFile = tempFileToPreserve // Restore reference
            if (!success) {
                android.util.Log.e("PdfRendererUtil", "Failed to open unlocked PDF file")
                // Clean up on failure
                unlockedTempFile?.delete()
                unlockedTempFile = null
                return@withContext false
            }
            
            android.util.Log.d("PdfRendererUtil", "Password-protected PDF opened successfully")
            true
        } catch (e: Exception) {
            android.util.Log.e("PdfRendererUtil", "Error opening password-protected PDF", e)
            unlockedTempFile?.delete()
            unlockedTempFile = null
            false
        }
    }
    
    /**
     * Creates a copy of a password-protected PDF that can be accessed
     */
    private suspend fun createPasswordProtectedCopy(context: Context, uri: Uri, password: String): File? = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d("PdfRendererUtil", "Creating decrypted copy using alternative approach")
            
            // Try using PdfReader with unethical reading enabled from the start
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                try {
                    // First try: Standard reader with password, then copy to remove encryption
                    val readerProperties = ReaderProperties().setPassword(password.toByteArray())
                    val reader = PdfReader(stream, readerProperties)
                    
                    // Force unethical reading to bypass owner password restrictions
                    reader.setUnethicalReading(true)
                    
                    val tempFile = File.createTempFile("decrypted_pdf_", ".pdf", context.cacheDir)
                    val writer = PdfWriter(FileOutputStream(tempFile))
                    val pdfDoc = PdfDocument(reader, writer)
                    
                    android.util.Log.d("PdfRendererUtil", "Successfully opened PDF with ${pdfDoc.numberOfPages} pages")
                    pdfDoc.close()
                    
                    android.util.Log.d("PdfRendererUtil", "Decrypted copy created: ${tempFile.absolutePath}")
                    tempFile
                } catch (e: Exception) {
                    android.util.Log.e("PdfRendererUtil", "Alternative decryption approach failed: ${e.message}")
                    
                    // Final fallback: try without password but with unethical reading
                    try {
                        val newInputStream = context.contentResolver.openInputStream(uri)
                        newInputStream?.use { retryStream ->
                            val reader = PdfReader(retryStream)
                            reader.setUnethicalReading(true)
                            
                            val tempFile = File.createTempFile("unethical_pdf_", ".pdf", context.cacheDir)
                            val writer = PdfWriter(FileOutputStream(tempFile))
                            val pdfDoc = PdfDocument(reader, writer)
                            
                            android.util.Log.d("PdfRendererUtil", "Unethical reading approach succeeded with ${pdfDoc.numberOfPages} pages")
                            pdfDoc.close()
                            
                            tempFile
                        }
                    } catch (finalException: Exception) {
                        android.util.Log.e("PdfRendererUtil", "All decryption approaches failed: ${finalException.message}")
                        null
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PdfRendererUtil", "Error creating decrypted copy", e)
            null
        }
    }
    
    /**
     * Opens a password-protected PDF file with the provided password
     */
    suspend fun openPdfWithPassword(filePath: String, password: String, context: Context): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            closePdf()
            
            android.util.Log.d("PdfRendererUtil", "Attempting to unlock PDF file with provided password")
            
            // Try to unlock the PDF directly - if this succeeds, the password is correct
            unlockedTempFile = passwordHandler.unlockPdfWithPassword(filePath, password, context)
            if (unlockedTempFile == null) {
                android.util.Log.e("PdfRendererUtil", "Failed to unlock PDF with password - password may be incorrect")
                return@withContext false
            }
            
            android.util.Log.d("PdfRendererUtil", "PDF unlocked successfully, opening temporary file")
            
            // Open the unlocked PDF using existing method
            val success = openPdf(unlockedTempFile!!.absolutePath)
            if (!success) {
                android.util.Log.e("PdfRendererUtil", "Failed to open unlocked PDF file")
                // Clean up on failure
                unlockedTempFile?.delete()
                unlockedTempFile = null
                return@withContext false
            }
            
            android.util.Log.d("PdfRendererUtil", "Password-protected PDF opened successfully")
            true
        } catch (e: Exception) {
            android.util.Log.e("PdfRendererUtil", "Error opening password-protected PDF", e)
            unlockedTempFile?.delete()
            unlockedTempFile = null
            false
        }
    }
    
    suspend fun openPdf(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        // First, check if the PDF is password-protected using iText7
        val isPasswordProtected = passwordHandler.isPdfPasswordProtected(context, uri)
        if (isPasswordProtected) {
            android.util.Log.d("PdfRendererUtil", "PDF is password-protected, cannot open without password")
            return@withContext false
        }
        
        // Retry mechanism for PDF opening reliability
        var lastException: Exception? = null
        repeat(3) { attempt ->
            try {
                closePdf()
                android.util.Log.d("PdfRendererUtil", "Attempting to open PDF from URI (attempt ${attempt + 1}): $uri")
                
                // Validate URI accessibility first
                val isAccessible = try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.available() >= 0
                    } ?: false
                } catch (e: Exception) {
                    android.util.Log.w("PdfRendererUtil", "URI validation failed on attempt ${attempt + 1}", e)
                    false
                }
                
                if (!isAccessible) {
                    android.util.Log.w("PdfRendererUtil", "URI not accessible on attempt ${attempt + 1}, retrying...")
                    if (attempt < 2) {
                        delay(500L) // Wait before retry
                        return@repeat
                    } else {
                        lastException = Exception("URI not accessible after 3 attempts")
                        return@repeat
                    }
                }
                
                // Try direct ParcelFileDescriptor first (more efficient)
                try {
                    parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                    if (parcelFileDescriptor != null) {
                        pdfRenderer = PdfRenderer(parcelFileDescriptor!!)
                        android.util.Log.d("PdfRendererUtil", "PDF opened directly with ${pdfRenderer?.pageCount ?: 0} pages")
                        return@withContext true
                    }
                } catch (e: Exception) {
                    android.util.Log.w("PdfRendererUtil", "Direct file descriptor failed on attempt ${attempt + 1}, trying temporary file approach", e)
                    lastException = e
                }
            
                // Fallback to temporary file approach
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    android.util.Log.e("PdfRendererUtil", "Cannot open input stream for URI: $uri on attempt ${attempt + 1}")
                    lastException = Exception("Cannot open input stream")
                    if (attempt < 2) {
                        delay(500L)
                        return@repeat
                    } else {
                        return@repeat
                    }
                }
            
                // Get the file size first to check if it's reasonable
                val fileSize = try {
                    inputStream.available().toLong()
                } catch (e: Exception) {
                    android.util.Log.w("PdfRendererUtil", "Cannot determine file size on attempt ${attempt + 1}", e)
                    -1L
                }
                
                if (fileSize > 100 * 1024 * 1024) { // 100MB limit
                    android.util.Log.e("PdfRendererUtil", "File too large: $fileSize bytes")
                    inputStream.close()
                    lastException = Exception("File too large: $fileSize bytes")
                    return@repeat
                }
            
                android.util.Log.d("PdfRendererUtil", "Creating temporary file for PDF (size: $fileSize bytes) on attempt ${attempt + 1}")
                tempFile = File.createTempFile("quickpdf_${System.currentTimeMillis()}_", ".pdf", context.cacheDir)
                
                try {
                    inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(16384) // Increased buffer size
                            var bytesRead: Int
                            var totalBytes = 0L
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalBytes += bytesRead
                            }
                            output.flush() // Ensure all data is written
                            android.util.Log.d("PdfRendererUtil", "Copied $totalBytes bytes to temporary file")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PdfRendererUtil", "Error copying file to temp location on attempt ${attempt + 1}", e)
                    tempFile?.delete()
                    tempFile = null
                    lastException = e
                    if (attempt < 2) {
                        delay(1000L)
                        return@repeat
                    } else {
                        return@repeat
                    }
                }
            
                // Verify temporary file integrity
                if (tempFile?.exists() != true || tempFile?.length() == 0L) {
                    android.util.Log.e("PdfRendererUtil", "Temporary file is empty or doesn't exist on attempt ${attempt + 1}")
                    tempFile?.delete()
                    tempFile = null
                    lastException = Exception("Temporary file validation failed")
                    if (attempt < 2) {
                        delay(1000L)
                        return@repeat
                    } else {
                        return@repeat
                    }
                }
                
                android.util.Log.d("PdfRendererUtil", "Temporary file created successfully: ${tempFile?.length()} bytes")
                
                // Add small delay to ensure file system sync
                delay(100L)
                
                parcelFileDescriptor = ParcelFileDescriptor.open(tempFile!!, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(parcelFileDescriptor!!)
                
                val pageCount = pdfRenderer?.pageCount ?: 0
                if (pageCount <= 0) {
                    android.util.Log.e("PdfRendererUtil", "PDF has no pages on attempt ${attempt + 1}")
                    lastException = Exception("PDF has no readable pages")
                    if (attempt < 2) {
                        closePdf()
                        delay(1000L)
                        return@repeat
                    } else {
                        return@repeat
                    }
                }
                
                android.util.Log.d("PdfRendererUtil", "PDF opened successfully with $pageCount pages")
                return@withContext true
                
            } catch (e: Exception) {
                android.util.Log.e("PdfRendererUtil", "Error opening PDF on attempt ${attempt + 1}: ${e.javaClass.simpleName}: ${e.message}", e)
                lastException = e
                closePdf()
                if (attempt < 2) {
                    delay(1000L)
                }
            }
        }
        
        // All attempts failed
        android.util.Log.e("PdfRendererUtil", "Failed to open PDF after 3 attempts. Last error: ${lastException?.message}")
        return@withContext false
    }
    
    suspend fun openPdf(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            closePdf()
            val file = File(filePath)
            if (!file.exists()) return@withContext false
            
            // First, check if the PDF is password-protected
            val isPasswordProtected = passwordHandler.isPdfPasswordProtected(filePath)
            android.util.Log.d("PdfRendererUtil", "Opening PDF: $filePath, isPasswordProtected: $isPasswordProtected")
            if (isPasswordProtected) {
                android.util.Log.e("PdfRendererUtil", "Decrypted temp file is still detected as password-protected! This shouldn't happen.")
                android.util.Log.e("PdfRendererUtil", "File exists: ${file.exists()}, File size: ${if (file.exists()) file.length() else "N/A"}")
                return@withContext false
            }
            
            parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(parcelFileDescriptor!!)
            android.util.Log.d("PdfRendererUtil", "PDF opened successfully with ${pdfRenderer?.pageCount ?: 0} pages")
            true
        } catch (e: Exception) {
            android.util.Log.e("PdfRendererUtil", "Exception opening PDF: ${e.message}", e)
            false
        }
    }
    
    fun getPageCount(): Int = pdfRenderer?.pageCount ?: 0
    
    suspend fun renderPage(pageIndex: Int, width: Int, height: Int): Bitmap? = 
        renderMutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val renderer = pdfRenderer ?: return@withContext null
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
                    
                    // Validate dimensions
                    val safeWidth = width.coerceAtMost(MAX_BITMAP_WIDTH)
                    val safeHeight = height.coerceAtMost(MAX_BITMAP_HEIGHT)
                    
                    if (safeWidth <= 0 || safeHeight <= 0) {
                        return@withContext null
                    }
                    
                    currentPage?.close()
                    currentPage = renderer.openPage(pageIndex)
                    
                    val page = currentPage ?: return@withContext null
                    val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                } catch (e: OutOfMemoryError) {
                    System.gc()
                    null
                } catch (e: Exception) {
                    null
                }
            }
        }
    
    suspend fun renderPageWithAspectRatio(pageIndex: Int, maxWidth: Int, maxHeight: Int): Bitmap? = 
        renderMutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val renderer = pdfRenderer ?: return@withContext null
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
                    
                    // Get cached dimensions or calculate once
                    val (pageWidth, pageHeight) = pageDimensionsCache[pageIndex] ?: run {
                        currentPage?.close()
                        currentPage = renderer.openPage(pageIndex)
                        val page = currentPage ?: return@withContext null
                        val dims = Pair(page.width, page.height)
                        pageDimensionsCache[pageIndex] = dims
                        dims
                    }
                    
                    // Use cached aspect ratio if available
                    val cacheKey = "${pageWidth}x${pageHeight}_${maxWidth}x${maxHeight}"
                    val ratio = aspectRatioCache[cacheKey] ?: run {
                        val widthRatio = maxWidth.toFloat() / pageWidth
                        val heightRatio = maxHeight.toFloat() / pageHeight
                        val calculatedRatio = minOf(widthRatio, heightRatio)
                        aspectRatioCache[cacheKey] = calculatedRatio
                        calculatedRatio
                    }
                    
                    val renderWidth = (pageWidth * ratio).toInt().coerceAtMost(MAX_BITMAP_WIDTH)
                    val renderHeight = (pageHeight * ratio).toInt().coerceAtMost(MAX_BITMAP_HEIGHT)
                    
                    // Validate bitmap dimensions
                    if (renderWidth <= 0 || renderHeight <= 0) {
                        return@withContext null
                    }
                    
                    // Ensure we have the correct page open
                    if (currentPage == null) {
                        currentPage = renderer.openPage(pageIndex)
                    }
                    
                    val page = currentPage ?: return@withContext null
                    val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                } catch (e: OutOfMemoryError) {
                    System.gc()
                    null
                } catch (e: Exception) {
                    null
                }
            }
        }
    
    fun getPageDimensions(pageIndex: Int): Pair<Int, Int>? {
        return try {
            // Return cached dimensions if available
            pageDimensionsCache[pageIndex]?.let { return it }
            
            val renderer = pdfRenderer ?: return null
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
            
            val page = renderer.openPage(pageIndex)
            val dimensions = Pair(page.width, page.height)
            page.close()
            
            // Cache the dimensions
            pageDimensionsCache[pageIndex] = dimensions
            dimensions
        } catch (e: Exception) {
            null
        }
    }
    
    fun closePdf() {
        try {
            currentPage?.close()
            currentPage = null
            pdfRenderer?.close()
            pdfRenderer = null
            parcelFileDescriptor?.close()
            parcelFileDescriptor = null
            
            // Clear caches
            pageDimensionsCache.clear()
            aspectRatioCache.clear()
            
            // Clean up temporary file if it exists
            tempFile?.let { file ->
                if (file.exists()) {
                    val deleted = file.delete()
                    android.util.Log.d("PdfRendererUtil", "Temporary file cleanup: ${if (deleted) "success" else "failed"}")
                }
                tempFile = null
            }
            
            // Clean up unlocked temporary file if it exists
            unlockedTempFile?.let { file ->
                if (file.exists()) {
                    val deleted = file.delete()
                    android.util.Log.d("PdfRendererUtil", "Unlocked temporary file cleanup: ${if (deleted) "success" else "failed"}")
                }
                unlockedTempFile = null
            }
        } catch (e: IOException) {
            android.util.Log.w("PdfRendererUtil", "Error during PDF cleanup", e)
        }
    }
}