package com.example.quickpdf.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RecentFile::class, Bookmark::class],
    version = 1,
    exportSchema = false
)
abstract class QuickPDFDatabase : RoomDatabase() {
    
    abstract fun recentFileDao(): RecentFileDao
    abstract fun bookmarkDao(): BookmarkDao
    
    companion object {
        @Volatile
        private var INSTANCE: QuickPDFDatabase? = null
        
        fun getDatabase(context: Context): QuickPDFDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuickPDFDatabase::class.java,
                    "quickpdf_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}