package com.ai.aidicted.agent

import kotlinx.serialization.Serializable

// ─── Play Store data models ────────────────────────────────────────────────

@Serializable
data class PlayReview(
    val reviewId: String,
    val authorName: String,
    val rating: Int,          // 1–5
    val text: String,
    val submittedAt: String,  // ISO-8601
    val thumbsUpCount: Int = 0,
    val appVersionName: String = "unknown",
)

// ─── Slack data models ──────────────────────────────────────────────────────

@Serializable
data class SlackBlock(
    val type: String,          // "section", "header", "divider"
    val text: SlackText? = null,
)

@Serializable
data class SlackText(
    val type: String,          // "mrkdwn" or "plain_text"
    val text: String,
    val emoji: Boolean? = null,
)

@Serializable
data class SlackPayload(
    val text: String,          // fallback / notification text
    val blocks: List<SlackBlock>,
)

// ─── Agent report model ─────────────────────────────────────────────────────

data class ReviewReport(
    val totalReviews: Int,
    val averageRating: Double,
    val oneStar: Int,
    val twoStar: Int,
    val threeStar: Int,
    val fourStar: Int,
    val fiveStar: Int,
    val topPositive: List<PlayReview>,
    val topNegative: List<PlayReview>,
    val aiSummary: String,
)
