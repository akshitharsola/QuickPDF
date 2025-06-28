package com.example.quickpdf

import android.app.Application
import android.util.Log
import com.example.quickpdf.data.repository.SimpleRepository

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
        Log.d("QuickPDFApplication", "Application started successfully")
    }
}