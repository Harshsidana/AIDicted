package com.ai.aidicted.agent

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Fetches reviews from the Google Play Developer API using a service account.
 *
 * 🧪 DUMMY MODE: If GOOGLE_SERVICE_ACCOUNT_JSON is not set (or USE_DUMMY_REVIEWS=true),
 * realistic fake reviews are returned so you can test the full LLM + Slack pipeline
 * without Play Console credentials.
 */
class PlayStoreReviewFetcher(
    private val packageName: String = System.getenv("APP_PACKAGE_NAME") ?: "com.ai.aidicted",
) {
    private val useDummy: Boolean =
        System.getenv("GOOGLE_SERVICE_ACCOUNT_JSON").isNullOrBlank() ||
        System.getenv("USE_DUMMY_REVIEWS") == "true"

    private val publisher: AndroidPublisher by lazy {
        val keyPath = System.getenv("GOOGLE_SERVICE_ACCOUNT_JSON")!!
        val credentials = GoogleCredentials
            .fromStream(File(keyPath).inputStream())
            .createScoped(listOf(AndroidPublisherScopes.ANDROIDPUBLISHER))
        AndroidPublisher.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            JacksonFactory.getDefaultInstance(),
            HttpCredentialsAdapter(credentials),
        ).setApplicationName("AIDicted-ReviewAgent/1.0").build()
    }

    /**
     * Fetches the most recent [maxResults] reviews.
     * Falls back to dummy data when no service account is configured.
     */
    fun fetchRecentReviews(maxResults: Long = 100): List<PlayReview> {
        if (useDummy) {
            println("[PlayStore] 🧪 DUMMY MODE — returning ${DUMMY_REVIEWS.size} fake reviews")
            return DUMMY_REVIEWS.take(maxResults.toInt())
        }
        return try {
            val response = publisher.reviews()
                .list(packageName)
                .setMaxResults(maxResults)
                .execute()
            val reviews = response.reviews ?: emptyList()
            println("[PlayStore] Fetched ${reviews.size} reviews for $packageName")
            reviews.mapNotNull { review ->
                val comment = review.comments?.firstOrNull()?.userComment ?: return@mapNotNull null
                val epochSec = comment.lastModified?.seconds ?: 0L
                PlayReview(
                    reviewId      = review.reviewId ?: "",
                    authorName    = review.authorName ?: "Anonymous",
                    rating        = comment.starRating ?: 0,
                    text          = (comment.text ?: "").trim(),
                    submittedAt   = Instant.ofEpochSecond(epochSec)
                        .atZone(ZoneId.of("UTC"))
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    thumbsUpCount = comment.thumbsUpCount ?: 0,
                    appVersionName = comment.appVersionName ?: "unknown",
                )
            }
        } catch (e: Exception) {
            System.err.println("[PlayStore] Failed to fetch reviews: ${e.message}")
            emptyList()
        }
    }

    companion object {
        /** Realistic dummy reviews for AIDicted — used when no service account is configured. */
        val DUMMY_REVIEWS = listOf(
            PlayReview("r01", "Priya Sharma",    5, "Love this app! The AI news curation is spot on. I get exactly the articles I care about without any fluff. The swipe interface is buttery smooth.", "2026-02-24T10:00:00+00:00", 12, "1.4.2"),
            PlayReview("r02", "Rahul Mehta",     5, "Finally an AI news app that doesn't feel bloated. Clean UI, fast loading, and the summaries are surprisingly accurate. 10/10 would recommend.", "2026-02-23T09:30:00+00:00", 8,  "1.4.2"),
            PlayReview("r03", "Ananya Singh",    4, "Really good app overall. The article summaries save me so much time. Would love a dark mode option though.", "2026-02-23T07:15:00+00:00", 5,  "1.4.1"),
            PlayReview("r04", "Karan Patel",     5, "Best AI news aggregator on the Play Store. The TechCrunch and Verge integration works flawlessly. Highly recommended for tech enthusiasts.", "2026-02-22T14:00:00+00:00", 15, "1.4.2"),
            PlayReview("r05", "Deepika Nair",    4, "Great concept and execution. Sometimes the articles take a bit to load on slow networks but otherwise excellent. Keep up the good work!", "2026-02-22T11:45:00+00:00", 3,  "1.4.1"),
            PlayReview("r06", "Amit Verma",      2, "App crashes every time I try to save an article to favourites. Please fix this ASAP, it's a core feature that's completely broken.", "2026-02-23T16:20:00+00:00", 22, "1.4.2"),
            PlayReview("r07", "Sneha Gupta",     1, "The app used to work fine but after the latest update it won't even open. Stuck on a white screen. Uninstalling until there's a proper fix.", "2026-02-24T08:10:00+00:00", 18, "1.4.2"),
            PlayReview("r08", "Vikram Rao",      2, "No way to filter news by topic or source. I keep seeing articles I don't care about. Needs a proper personalisation feature desperately.", "2026-02-21T13:30:00+00:00", 9,  "1.4.0"),
            PlayReview("r09", "Meera Krishnan",  3, "Decent app but the offline mode doesn't work. Articles I saved are unavailable without internet. A bit disappointing for a paid feature promise.", "2026-02-20T17:00:00+00:00", 6,  "1.4.0"),
            PlayReview("r10", "Suresh Iyer",     5, "Absolutely brilliant. I replaced three other news apps with just this one. The AI curation is leagues ahead of anything else I've tried.", "2026-02-19T09:00:00+00:00", 20, "1.3.9"),
            PlayReview("r11", "Pooja Tiwari",    4, "Love the InShorts-like swipe UI — very intuitive. Would love push notifications for breaking AI news stories. Any plans for that?", "2026-02-18T12:00:00+00:00", 4,  "1.3.9"),
            PlayReview("r12", "Nikhil Desai",    1, "Battery drain is horrible. The app kept running in the background and ate 30% of my battery in just 2 hours. Needs serious optimisation.", "2026-02-22T08:45:00+00:00", 14, "1.4.1"),
            PlayReview("r13", "Ritu Agarwal",    5, "Perfect for staying updated on AI without the noise. The article quality is consistently high. Small, fast, no annoying ads. Love it.", "2026-02-17T15:30:00+00:00", 7,  "1.3.8"),
            PlayReview("r14", "Sanjay Kapoor",   3, "Good potential but needs work. Search barely works — typing a topic returns random unrelated articles. Fix search and it'll be a 5-star app.", "2026-02-21T10:20:00+00:00", 11, "1.4.1"),
            PlayReview("r15", "Lakshmi Mohan",   4, "Really enjoying the app. Clean design, no clutter. Only wish there were more sources beyond the current 6 feeds. More diversity would be great.", "2026-02-20T19:00:00+00:00", 2,  "1.4.0"),
        )
    }
}
