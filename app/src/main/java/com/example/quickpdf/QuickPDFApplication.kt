package com.example.quickpdf

import android.app.Application
import com.example.quickpdf.data.database.QuickPDFDatabase
import com.example.quickpdf.data.repository.PdfRepository

class QuickPDFApplication : Application() {
    
    val database by lazy { QuickPDFDatabase.getDatabase(this) }
    val repository by lazy { PdfRepository(database.recentFileDao(), database.bookmarkDao()) }
}