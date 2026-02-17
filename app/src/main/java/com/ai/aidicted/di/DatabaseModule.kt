package com.ai.aidicted.di

import android.content.Context
import androidx.room.Room
import com.ai.aidicted.data.local.AppDatabase
import com.ai.aidicted.data.local.NewsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "aidicted_db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideNewsDao(database: AppDatabase): NewsDao = database.newsDao()
}
