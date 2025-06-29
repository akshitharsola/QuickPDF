package com.quickpdf.reader

import android.app.Application
import android.util.Log
import com.quickpdf.reader.data.repository.SimpleRepository

class QuickPDFApplication : Application() {
    
    val repository by lazy { 
        try {
            SimpleRepository()
        } catch (e: Exception) {
            Log.e("QuickPDFApplication", "Error initializing repository", e)
            SimpleRepository() // Fallback
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Store the default handler before setting our custom one
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        // Set up global exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e("QuickPDFApplication", "Uncaught exception in thread ${thread.name}", exception)
            
            // You could also save the error to a file or send it to crash reporting service
            // For now, just log it and let the system handle it
            defaultHandler?.uncaughtException(thread, exception)
        }
        
        Log.d("QuickPDFApplication", "Application started successfully")
    }
}