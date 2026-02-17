package com.ai.aidicted.data.repository

import com.ai.aidicted.data.local.NewsDao
import com.ai.aidicted.data.model.NewsArticle
import com.ai.aidicted.data.model.toDomain
import com.ai.aidicted.data.remote.RssNewsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor(
    private val rssNewsSource: RssNewsSource,
    private val newsDao: NewsDao
) {

    fun getAllArticles(): Flow<List<NewsArticle>> =
        newsDao.getAllArticles().map { entities -> entities.map { it.toDomain() } }

    fun getFavoriteArticles(): Flow<List<NewsArticle>> =
        newsDao.getFavoriteArticles().map { entities -> entities.map { it.toDomain() } }

    fun searchArticles(query: String): Flow<List<NewsArticle>> =
        newsDao.searchArticles(query).map { entities -> entities.map { it.toDomain() } }

    /**
     * Fetches articles from RSS feeds and caches them locally.
     * Returns true if successful, false if failed (cached data still available via Flow).
     */
    suspend fun refreshArticles(): Boolean {
        return try {
            val articles = rssNewsSource.fetchArticles()
            if (articles.isNotEmpty()) {
                // Preserve favorite status for existing articles
                val updatedArticles = articles.map { newArticle ->
                    val existing = newsDao.getArticleById(newArticle.id)
                    newArticle.copy(isFavorite = existing?.isFavorite ?: false)
                }
                // Clear old non-favorite articles to get fresh data with images
                newsDao.clearNonFavoriteArticles()
                newsDao.insertArticles(updatedArticles)
            }
            articles.isNotEmpty()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun toggleFavorite(articleId: String) {
        val article = newsDao.getArticleById(articleId)
        article?.let {
            newsDao.updateFavoriteStatus(articleId, !it.isFavorite)
        }
    }
}
