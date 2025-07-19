package com.quickpdf.reader.utils

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.ReaderProperties
import com.itextpdf.kernel.pdf.DocumentProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utility class for detecting and handling password-protected PDF files
 * Uses iText7 library for full password support
 */
class PdfPasswordHandler {
    
    /**
     * Checks if a PDF is password-protected using iText7
     */
    suspend fun isPdfPasswordProtected(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d("PdfPasswordHandler", "Checking if PDF is password-protected for URI: $uri")
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                try {
                    // Try to open without password
                    val reader = PdfReader(stream)
                    val pdfDoc = PdfDocument(reader)
                    pdfDoc.close()
                    android.util.Log.d("PdfPasswordHandler", "PDF opened successfully without password - not protected")
                    false // Successfully opened, not password-protected
                } catch (e: Exception) {
                    // Check if it's specifically a password error
                    val errorMessage = e.message?.lowercase() ?: ""
                    val isPasswordError = errorMessage.contains("bad user password") ||
                                        errorMessage.contains("password") ||
                                        errorMessage.contains("encrypted") ||
                                        errorMessage.contains("security") ||
                                        errorMessage.contains("authentication") ||
                                        errorMessage.contains("decrypt") ||
                                        errorMessage.contains("owner password") ||
                                        errorMessage.contains("user password")
                    
                    android.util.Log.d("PdfPasswordHandler", "PDF open error: '$errorMessage', password-related: $isPasswordError")
                    isPasswordError
                }
            } ?: false
        } catch (e: Exception) {
            android.util.Log.e("PdfPasswordHandler", "Error checking PDF password protection", e)
            false
        }
    }
    
    /**
     * Checks if a PDF file is password-protected using iText7
     */
    suspend fun isPdfPasswordProtected(filePath: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(filePath)
            if (!file.exists()) return@withContext false
            
            try {
                FileInputStream(file).use { stream ->
                    val reader = PdfReader(stream)
                    val pdfDoc = PdfDocument(reader)
                    pdfDoc.close()
                    false // Successfully opened, not password-protected
                }
            } catch (e: Exception) {
                // Check if it's specifically a password error
                val errorMessage = e.message?.lowercase() ?: ""
                val isPasswordError = errorMessage.contains("bad user password") ||
                                    errorMessage.contains("password") ||
                                    errorMessage.contains("encrypted") ||
                                    errorMessage.contains("security") ||
                                    errorMessage.contains("authentication") ||
                                    errorMessage.contains("decrypt")
                
                android.util.Log.d("PdfPasswordHandler", "PDF open error: $errorMessage, password-related: $isPasswordError")
                isPasswordError
            }
        } catch (e: Exception) {
            android.util.Log.e("PdfPasswordHandler", "Error checking PDF password protection", e)
            false
        }
    }
    
    /**
     * Validates if a password is correct for a PDF
     * Tries both user and owner password modes with graceful owner password handling
     */
    suspend fun validatePdfPassword(context: Context, uri: Uri, password: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                // Try different password encodings and formats for NSDL PDFs
                val cleanPassword = password.replace(Regex("[^A-Za-z0-9]"), "")
                val phoneWithoutCountryCode = if (password.startsWith("+91")) password.substring(3).trim() else ""
                val phoneClean = phoneWithoutCountryCode.replace(Regex("[^A-Za-z0-9]"), "")
                
                val passwordVariants = listOf(
                    "Standard" to password.toByteArray(),
                    "UTF-8" to password.toByteArray(Charsets.UTF_8),
                    "Latin-1" to password.toByteArray(Charsets.ISO_8859_1),
                    "Uppercase" to password.uppercase().toByteArray(),
                    "Lowercase" to password.lowercase().toByteArray(),
                    "Clean (no spaces/symbols)" to cleanPassword.toByteArray(),
                    "Clean Uppercase" to cleanPassword.uppercase().toByteArray(),
                    "Clean Lowercase" to cleanPassword.lowercase().toByteArray()
                ) + if (phoneWithoutCountryCode.isNotEmpty()) listOf(
                    "Without +91" to phoneWithoutCountryCode.toByteArray(),
                    "Without +91 Clean" to phoneClean.toByteArray(),
                    "Phone Clean Upper" to phoneClean.uppercase().toByteArray()
                ) else emptyList()
                
                for ((encodingName, passwordBytes) in passwordVariants) {
                    try {
                        android.util.Log.d("PdfPasswordHandler", "Validating password with $encodingName encoding")
                        android.util.Log.d("PdfPasswordHandler", "Password length: ${passwordBytes.size}, first few bytes: ${passwordBytes.take(5).toList()}")
                        
                        // Reset stream for each attempt
                        val freshStream = context.contentResolver.openInputStream(uri)
                        freshStream?.use { newStream ->
                            val readerProperties = ReaderProperties().setPassword(passwordBytes)
                            val reader = PdfReader(newStream, readerProperties)
                            val pdfDoc = PdfDocument(reader)
                            val pageCount = pdfDoc.numberOfPages
                            pdfDoc.close()
                            
                            if (pageCount > 0) {
                                android.util.Log.d("PdfPasswordHandler", "Password validated successfully with $encodingName encoding")
                                return@withContext true
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.d("PdfPasswordHandler", "Password validation failed with $encodingName encoding: ${e.message}")
                        
                        // If it's an owner password issue, we'll consider it valid if we can at least detect it
                        if (e.message?.contains("owner password") == true) {
                            android.util.Log.d("PdfPasswordHandler", "Owner password detected - considering password valid")
                            return@withContext true
                        }
                        // Continue trying other encodings
                    }
                }
                
                android.util.Log.d("PdfPasswordHandler", "Password validation failed for all encodings")
                false
            } ?: false
        } catch (e: Exception) {
            android.util.Log.e("PdfPasswordHandler", "Password validation failed", e)
            false
        }
    }
    
    /**
     * Validates if a password is correct for a PDF file
     */
    suspend fun validatePdfPassword(filePath: String, password: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(filePath)
            if (!file.exists()) return@withContext false
            
            FileInputStream(file).use { stream ->
                val readerProperties = ReaderProperties().setPassword(password.toByteArray())
                val reader = PdfReader(stream, readerProperties)
                val pdfDoc = PdfDocument(reader)
                val isValid = pdfDoc.numberOfPages > 0
                pdfDoc.close()
                isValid
            }
        } catch (e: Exception) {
            android.util.Log.e("PdfPasswordHandler", "Password validation failed", e)
            false
        }
    }
    
    /**
     * Attempts to unlock a password-protected PDF and create a temporary unprotected version
     * Handles owner password issues by using a more robust approach
     */
    suspend fun unlockPdfWithPassword(
        context: Context, 
        uri: Uri, 
        password: String
    ): File? = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d("PdfPasswordHandler", "Attempting to unlock PDF with password")
            
            // Try different password encodings and formats for NSDL PDFs
            val cleanPassword = password.replace(Regex("[^A-Za-z0-9]"), "")
            val phoneWithoutCountryCode = if (password.startsWith("+91")) password.substring(3).trim() else ""
            val phoneClean = phoneWithoutCountryCode.replace(Regex("[^A-Za-z0-9]"), "")
            
            val passwordVariants = listOf(
                "Standard" to password.toByteArray(),
                "UTF-8" to password.toByteArray(Charsets.UTF_8),
                "Latin-1" to password.toByteArray(Charsets.ISO_8859_1),
                "Uppercase" to password.uppercase().toByteArray(),
                "Lowercase" to password.lowercase().toByteArray(),
                "Clean (no spaces/symbols)" to cleanPassword.toByteArray(),
                "Clean Uppercase" to cleanPassword.uppercase().toByteArray(),
                "Clean Lowercase" to cleanPassword.lowercase().toByteArray()
            ) + if (phoneWithoutCountryCode.isNotEmpty()) listOf(
                "Without +91" to phoneWithoutCountryCode.toByteArray(),
                "Without +91 Clean" to phoneClean.toByteArray(),
                "Phone Clean Upper" to phoneClean.uppercase().toByteArray()
            ) else emptyList()
            
            for ((encodingName, passwordBytes) in passwordVariants) {
                try {
                    android.util.Log.d("PdfPasswordHandler", "Trying $encodingName encoding")
                    android.util.Log.d("PdfPasswordHandler", "Password length: ${passwordBytes.size}, first few bytes: ${passwordBytes.take(5).toList()}")
                    
                    val inputStream = context.contentResolver.openInputStream(uri)
                    inputStream?.use { stream ->
                        // Try both user and owner password approaches
                        val readerProperties = ReaderProperties().setPassword(passwordBytes)
                        
                        // First attempt: Standard approach
                        try {
                            val reader = PdfReader(stream, readerProperties)
                            val tempFile = File.createTempFile("unlocked_pdf_", ".pdf", context.cacheDir)
                            val writer = PdfWriter(FileOutputStream(tempFile))
                            val newPdfDoc = PdfDocument(reader, writer)
                            val pageCount = newPdfDoc.numberOfPages
                            newPdfDoc.close()
                            
                            // Verify the output file can be opened by Android PdfRenderer
                            if (verifyDecryptedPdf(tempFile)) {
                                android.util.Log.d("PdfPasswordHandler", "Successfully unlocked PDF using $encodingName (standard approach) with $pageCount pages")
                                return@withContext tempFile
                            } else {
                                android.util.Log.w("PdfPasswordHandler", "Decrypted PDF failed PdfRenderer verification, cleaning up")
                                tempFile.delete()
                            }
                        } catch (ownerPasswordException: Exception) {
                            android.util.Log.d("PdfPasswordHandler", "Standard approach failed with $encodingName: ${ownerPasswordException.message}")
                            
                            // Check if it's specifically an owner password issue
                            if (ownerPasswordException.message?.contains("owner password") == true) {
                                // For owner password issues, use unethical reading to bypass security
                                try {
                                    android.util.Log.d("PdfPasswordHandler", "Attempting unethical reading approach for owner password")
                                    
                                    // Reset the stream for retry
                                    val newInputStream = context.contentResolver.openInputStream(uri)
                                    newInputStream?.use { retryStream ->
                                        // Create reader without password first
                                        val reader = PdfReader(retryStream)
                                        // Enable unethical reading to bypass owner password restrictions
                                        reader.setUnethicalReading(true)
                                        
                                        // Create temporary file for unlocked PDF
                                        val tempFile = File.createTempFile("unlocked_pdf_", ".pdf", context.cacheDir)
                                        val writer = PdfWriter(FileOutputStream(tempFile))
                                        
                                        // Create new PDF document - this will strip encryption
                                        val newPdfDoc = PdfDocument(reader, writer)
                                        val pageCount = newPdfDoc.numberOfPages
                                        
                                        // Close the document to finalize the decrypted file
                                        newPdfDoc.close()
                                        
                                        if (pageCount > 0) {
                                            // Verify the output file can be opened by Android PdfRenderer
                                            if (verifyDecryptedPdf(tempFile)) {
                                                android.util.Log.d("PdfPasswordHandler", "Successfully created decrypted copy using $encodingName with unethical reading and $pageCount pages")
                                                return@withContext tempFile
                                            } else {
                                                android.util.Log.w("PdfPasswordHandler", "Decrypted PDF (unethical reading) failed PdfRenderer verification, cleaning up")
                                                tempFile.delete()
                                            }
                                        } else {
                                            // Clean up if no pages
                                            tempFile.delete()
                                        }
                                    }
                                } catch (retryException: Exception) {
                                    android.util.Log.d("PdfPasswordHandler", "Unethical reading approach failed with $encodingName: ${retryException.message}")
                                }
                            }
                            // Continue to next encoding if owner password approach fails or if it's not an owner password issue
                        }
                        Unit // Explicitly return Unit to avoid expression issues
                    }
                } catch (e: Exception) {
                    android.util.Log.d("PdfPasswordHandler", "$encodingName encoding failed: ${e.message}")
                    // Continue to next encoding
                }
            }
            
            android.util.Log.e("PdfPasswordHandler", "All password unlock attempts failed")
            null
        } catch (e: Exception) {
            android.util.Log.e("PdfPasswordHandler", "Failed to unlock PDF with password", e)
            null
        }
    }
    
    /**
     * Attempts to unlock a password-protected PDF file and create a temporary unprotected version
     */
    suspend fun unlockPdfWithPassword(
        filePath: String, 
        password: String, 
        context: Context
    ): File? = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(filePath)
            if (!file.exists()) return@withContext null
            
            FileInputStream(file).use { stream ->
                // Create reader with password
                val readerProperties = ReaderProperties().setPassword(password.toByteArray())
                val reader = PdfReader(stream, readerProperties)
                
                // Create temporary file for unlocked PDF
                val tempFile = File.createTempFile("unlocked_pdf_", ".pdf", context.cacheDir)
                
                // Create a new PDF document without password protection
                val writer = PdfWriter(FileOutputStream(tempFile))
                val pdfDoc = PdfDocument(reader, writer)
                
                // The document is automatically copied when creating PdfDocument with reader and writer
                pdfDoc.close()
                
                android.util.Log.d("PdfPasswordHandler", "Successfully unlocked PDF to temporary file: ${tempFile.absolutePath}")
                tempFile
            }
        } catch (e: Exception) {
            android.util.Log.e("PdfPasswordHandler", "Failed to unlock PDF with password", e)
            null
        }
    }
    
    /**
     * Verifies that a decrypted PDF file can be opened by Android's PdfRenderer
     * This ensures the file is truly decrypted and has no remaining encryption metadata
     */
    private fun verifyDecryptedPdf(file: File): Boolean {
        return try {
            android.util.Log.d("PdfPasswordHandler", "Verifying decrypted PDF can be opened by PdfRenderer")
            
            // Try to open with Android PdfRenderer
            val parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(parcelFileDescriptor)
            val pageCount = pdfRenderer.pageCount
            
            // Clean up
            pdfRenderer.close()
            parcelFileDescriptor.close()
            
            val isValid = pageCount > 0
            android.util.Log.d("PdfPasswordHandler", "PdfRenderer verification: ${if (isValid) "SUCCESS" else "FAILED"} ($pageCount pages)")
            isValid
            
        } catch (e: Exception) {
            android.util.Log.w("PdfPasswordHandler", "PdfRenderer verification failed: ${e.message}")
            false
        }
    }
    
    /**
     * Provides user information about password-protected PDFs
     */
    fun getPasswordProtectedPdfMessage(): String {
        return "This PDF is password-protected. Please enter the password to unlock and view it."
    }
}