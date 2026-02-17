package com.ai.aidicted.data.local

import androidx.room.*
import com.ai.aidicted.data.model.NewsArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {

    @Query("SELECT * FROM news_articles ORDER BY publishedAt DESC")
    fun getAllArticles(): Flow<List<NewsArticleEntity>>

    @Query("SELECT * FROM news_articles WHERE isFavorite = 1 ORDER BY publishedAt DESC")
    fun getFavoriteArticles(): Flow<List<NewsArticleEntity>>

    @Query("SELECT * FROM news_articles WHERE title LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' ORDER BY publishedAt DESC")
    fun searchArticles(query: String): Flow<List<NewsArticleEntity>>

    @Query("SELECT * FROM news_articles WHERE id = :id LIMIT 1")
    suspend fun getArticleById(id: String): NewsArticleEntity?

    @Query("SELECT id FROM news_articles")
    suspend fun getAllArticleIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<NewsArticleEntity>)

    @Query("UPDATE news_articles SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)

    @Query("DELETE FROM news_articles WHERE isFavorite = 0")
    suspend fun clearNonFavoriteArticles()
}
