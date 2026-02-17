package com.ai.aidicted.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ai.aidicted.data.local.NewsDao
import com.ai.aidicted.data.remote.RssNewsSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker that periodically fetches new AI news articles
 * and sends a notification if new ones are found.
 *
 * Scheduled via WorkManager in AIDictedApp.
 */
@HiltWorker
class NewsRefreshWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val rssNewsSource: RssNewsSource,
    private val newsDao: NewsDao
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "news_refresh_work"
        private const val TAG = "NewsRefreshWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting background news refresh...")

            // Get current article count before refresh
            val existingIds = newsDao.getAllArticleIds()

            // Fetch fresh articles from RSS feeds
            val freshArticles = rssNewsSource.fetchArticles()

            if (freshArticles.isEmpty()) {
                Log.d(TAG, "No articles fetched, skipping")
                return Result.success()
            }

            // Preserve favorite status and insert
            val updatedArticles = freshArticles.map { newArticle ->
                val existing = newsDao.getArticleById(newArticle.id)
                newArticle.copy(isFavorite = existing?.isFavorite ?: false)
            }
            newsDao.clearNonFavoriteArticles()
            newsDao.insertArticles(updatedArticles)

            // Count genuinely new articles (IDs we haven't seen before)
            val newIds = freshArticles.map { it.id }.toSet()
            val brandNewCount = newIds.count { it !in existingIds }

            Log.d(TAG, "Refresh complete: $brandNewCount new articles")

            // Send notification if there are new articles
            if (brandNewCount > 0) {
                val latestTitle = freshArticles.firstOrNull()?.title
                NotificationHelper.sendNewArticlesNotification(
                    context,
                    brandNewCount,
                    latestTitle
                )
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background refresh failed", e)
            Result.retry()
        }
    }
}
