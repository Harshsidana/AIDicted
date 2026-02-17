package com.ai.aidicted.data.model

/**
 * Domain model representing a news article used throughout the UI layer.
 */
data class NewsArticle(
    val id: String,
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
