package com.ai.aidicted.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for cached news articles.
 */
@Entity(tableName = "news_articles")
data class NewsArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val content: String? = null,
    val imageUrl: String? = null,
    val source: String? = null,
    val sourceUrl: String? = null,
    val publishedAt: String,
    val createdAt: String,
    val isFavorite: Boolean = false
)

fun NewsArticleEntity.toDomain(): NewsArticle = NewsArticle(
    id = id,
    title = title,
    summary = summary,
    content = content,
    imageUrl = imageUrl,
    source = source,
    sourceUrl = sourceUrl,
    publishedAt = publishedAt,
    createdAt = createdAt,
    isFavorite = isFavorite
)
