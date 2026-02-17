package com.ai.aidicted.deeplink

import android.content.Intent
import android.net.Uri

/**
 * Utility for creating and parsing deep links.
 *
 * Supported formats:
 *   - https://aidicted.app/article/{articleId}
 *   - aidicted://article/{articleId}
 */
object DeepLinkHelper {

    private const val HTTPS_HOST = "aidicted.app"
    private const val CUSTOM_SCHEME = "aidicted"
    private const val ARTICLE_PATH = "article"

    // Play Store link for fallback when app is not installed
    private const val PLAY_STORE_PACKAGE = "com.ai.aidicted"
    private const val PLAY_STORE_URL =
        "https://play.google.com/store/apps/details?id=$PLAY_STORE_PACKAGE"

    /**
     * Build a shareable deep link URL for an article.
     * Uses the custom scheme for automatic app opening without manual setup.
     */
    fun buildArticleDeepLink(articleId: String): String {
        return "$CUSTOM_SCHEME://$ARTICLE_PATH/${Uri.encode(articleId)}"
    }

    /**
     * Build the full share text for an article including the deep link.
     */
    fun buildShareText(title: String, summary: String, articleId: String): String {
        val deepLink = buildArticleDeepLink(articleId)
        return "$title\n\n$summary\n\nRead on AIDicted:\n$deepLink"
    }

    /**
     * Extract the article ID from an incoming deep link intent.
     * Returns null if the intent doesn't contain a valid article deep link.
     */
    fun extractArticleId(intent: Intent?): String? {
        val data: Uri = intent?.data ?: return null

        return when {
            // https://aidicted.app/article/{id}
            (data.scheme == "https" || data.scheme == "http") && data.host == HTTPS_HOST -> {
                val segments = data.pathSegments
                if (segments.size >= 2 && segments[0] == ARTICLE_PATH) {
                    segments[1]
                } else null
            }

            // aidicted://article/{id}
            data.scheme == CUSTOM_SCHEME && data.host == ARTICLE_PATH -> {
                data.pathSegments.firstOrNull()
            }

            else -> null
        }
    }

    /**
     * Build an intent that opens the article in the app,
     * or falls back to the Play Store if the app isn't installed.
     */
    fun buildPlayStoreFallbackIntent(): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL))
    }
}
