package com.ai.aidicted.agent

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * All Koog tools for the ReviewAgent, using the annotation-based ToolSet API.
 * Each @Tool-annotated method is discovered automatically by [ai.koog.agents.core.tools.reflect.tools].
 * Methods can be `suspend` — Koog's runtime handles coroutine dispatch.
 */
class ReviewAgentTools(
    private val fetcher: PlayStoreReviewFetcher,
    private val slackNotifier: SlackNotifier,
) : ToolSet {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    // ─── Tool 1: Fetch Play Store reviews ─────────────────────────────────────

    @Tool
    @LLMDescription("Fetches the latest user reviews from the Google Play Store for the AIDicted app. Returns a JSON array of review objects. Pass the returned JSON directly to compute_review_stats.")
    suspend fun fetch_play_reviews(
        @LLMDescription("Maximum number of reviews to fetch. Use 100 for a full daily report.")
        maxResults: Int = 100
    ): String {
        println("[Tool] fetch_play_reviews(maxResults=$maxResults)")
        val reviews = fetcher.fetchRecentReviews(maxResults.toLong())
        println("[Tool] → ${reviews.size} reviews fetched")
        if (reviews.isEmpty()) return "[]"
        return json.encodeToString(ListSerializer(PlayReview.serializer()), reviews)
    }

    // ─── Tool 2: Compute statistics ────────────────────────────────────────────

    @Tool
    @LLMDescription("Computes statistics from the JSON array returned by fetch_play_reviews. Returns a plain-text summary: total count, average rating, star-by-star breakdown, top 3 positive reviews (4-5★), and top 3 negative reviews (1-2★).")
    fun compute_review_stats(
        @LLMDescription("JSON array of reviews as returned by fetch_play_reviews.")
        reviewsJson: String
    ): String {
        println("[Tool] compute_review_stats (input length=${reviewsJson.length})")
        if (reviewsJson.isBlank() || reviewsJson == "[]") {
            return "No reviews found. The app has 0 Play Store reviews yet."
        }
        return try {
            val reviews: List<PlayReview> = json.decodeFromString(ListSerializer(PlayReview.serializer()), reviewsJson)
            val avg = reviews.map { it.rating }.average()
            val dist = (1..5).associateWith { star -> reviews.count { it.rating == star } }

            val positives = reviews.filter { it.rating >= 4 }
                .sortedByDescending { it.thumbsUpCount }.take(3)
            val negatives = reviews.filter { it.rating <= 2 }
                .sortedByDescending { it.thumbsUpCount }.take(3)

            fun fmt(list: List<PlayReview>) = if (list.isEmpty()) "  None found." else
                list.joinToString("\n") { r ->
                    "  [${r.rating}★] \"${r.text.take(120).trimEnd()}…\" — ${r.authorName} (v${r.appVersionName})"
                }

            buildString {
                appendLine("=== REVIEW STATISTICS ===")
                appendLine("Total Reviews Fetched : ${reviews.size}")
                appendLine("Average Rating        : ${"%.2f".format(avg)} / 5.00")
                appendLine()
                appendLine("Star Distribution:")
                for (s in 5 downTo 1) appendLine("  ${s}★  →  ${dist[s]} review(s)")
                appendLine()
                appendLine("Top Positive Reviews (4-5★):")
                appendLine(fmt(positives))
                appendLine()
                appendLine("Top Negative Reviews (1-2★):")
                appendLine(fmt(negatives))
            }.trimEnd()
        } catch (e: Exception) {
            "Error computing stats: ${e.message}"
        }
    }

    // ─── Tool 3: Send Slack report ─────────────────────────────────────────────

    @Tool
    @LLMDescription("Sends the completed daily report to the team's Slack channel via Incoming Webhook. Format the text using Slack mrkdwn: *bold*, _italic_, • bullets, emojis. Call this ONLY ONCE as the very last step.")
    suspend fun send_slack_report(
        @LLMDescription("The full report in Slack mrkdwn format. Must include: overall sentiment, rating breakdown, top praise points, top pain points, and dev-team recommendations. Keep under 2800 characters.")
        reportMarkdown: String
    ): String {
        println("[Tool] send_slack_report (length=${reportMarkdown.length})")
        val (success, message) = slackNotifier.send(reportMarkdown)
        return if (success) "✅ Report delivered to Slack successfully."
        else "❌ Slack delivery failed: $message"
    }
}
