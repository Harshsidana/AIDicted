package com.ai.aidicted.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ai.aidicted.data.model.NewsArticleEntity

@Database(entities = [NewsArticleEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun newsDao(): NewsDao
}
