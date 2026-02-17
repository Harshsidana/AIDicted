package com.ai.aidicted.data.remote

import android.util.Log
import com.ai.aidicted.data.model.NewsArticleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Autonomous AI news fetcher that pulls from direct-source RSS feeds.
 * No API keys or backend needed — works entirely on-device.
 *
 * Uses real news outlet feeds for direct article URLs and embedded images.
 */
@Singleton
class RssNewsSource @Inject constructor(
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "RssNewsSource"
    }

    // Lightweight client with short timeout for og:image scraping
    private val imageScraperClient: OkHttpClient by lazy {
        httpClient.newBuilder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Direct-source RSS feeds from major AI/tech news outlets.
     * These provide direct article URLs (no Google redirect) and often embed images.
     */
    private val rssFeedUrls = listOf(
        // TechCrunch AI
        "https://techcrunch.com/category/artificial-intelligence/feed/",
        // The Verge AI
        "https://www.theverge.com/rss/ai-artificial-intelligence/index.xml",
        // Ars Technica Technology
        "https://feeds.arstechnica.com/arstechnica/technology-lab",
        // Wired AI
        "https://www.wired.com/feed/tag/ai/latest/rss",
        // MIT Technology Review
        "https://www.technologyreview.com/feed/",
        // VentureBeat AI
        "https://venturebeat.com/category/ai/feed/"
    )

    /**
     * Fetches AI news articles from multiple RSS feeds.
     * Returns a deduplicated list sorted by publish date.
     */
    suspend fun fetchArticles(): List<NewsArticleEntity> = withContext(Dispatchers.IO) {
        val allArticles = mutableListOf<NewsArticleEntity>()

        for (feedUrl in rssFeedUrls) {
            try {
                Log.d(TAG, "Fetching feed: $feedUrl")
                val articles = fetchFeed(feedUrl)
                Log.d(TAG, "Got ${articles.size} articles from $feedUrl")
                allArticles.addAll(articles)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch feed: $feedUrl", e)
            }
        }

        Log.d(TAG, "Total articles before dedup: ${allArticles.size}")

        // Deduplicate by title similarity and sort by date
        val deduped = allArticles
            .distinctBy { it.title.lowercase().take(60) }
            .sortedByDescending { it.publishedAt }
            .take(50)

        Log.d(TAG, "After dedup: ${deduped.size}, articles with images: ${deduped.count { !it.imageUrl.isNullOrBlank() }}")

        // Scrape og:image for articles that still need images
        enrichWithImages(deduped)
    }

    /**
     * For each article without an image, scrape the og:image from the article page.
     * Since we use direct-source feeds, URLs go straight to the article (no Google redirect).
     */
    private suspend fun enrichWithImages(
        articles: List<NewsArticleEntity>
    ): List<NewsArticleEntity> = coroutineScope {
        articles.map { article ->
            async(Dispatchers.IO) {
                if (!article.imageUrl.isNullOrBlank() && article.imageUrl.startsWith("http")) {
                    // Already has an image from RSS media tags
                    article
                } else if (!article.sourceUrl.isNullOrBlank()) {
                    // Try scraping og:image from the article page
                    try {
                        val imageUrl = scrapeOgImage(article.sourceUrl)
                        if (imageUrl != null) {
                            Log.d(TAG, "Scraped image for: ${article.title.take(40)}")
                            article.copy(imageUrl = imageUrl)
                        } else {
                            Log.d(TAG, "No og:image, using fallback for: ${article.title.take(40)}")
                            article.copy(imageUrl = getFallbackImage(article.title))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Scrape failed for: ${article.title.take(40)}", e)
                        article.copy(imageUrl = getFallbackImage(article.title))
                    }
                } else {
                    article.copy(imageUrl = getFallbackImage(article.title))
                }
            }
        }.awaitAll()
    }

    /**
     * Scrapes the og:image meta tag from an article URL.
     * Only reads the first ~20KB of HTML (the <head> section) for efficiency.
     */
    private fun scrapeOgImage(url: String): String? {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml")
                .build()

            val response = imageScraperClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.d(TAG, "HTTP ${response.code} for $url")
                response.close()
                return null
            }

            val body = response.body ?: return null

            // Read only the first ~20KB — og:image is always in the <head>
            val source = body.source()
            source.request(20_000)
            val html = source.buffer.snapshot().utf8().take(20_000)
            body.close()

            // Try og:image first (most reliable for news sites)
            extractMetaContent(html, "og:image")?.let { return it }
            // twitter:image as fallback
            extractMetaContent(html, "twitter:image")?.let { return it }
            extractMetaContent(html, "twitter:image:src")?.let { return it }

            return null
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Extracts the content attribute from a meta tag with the given property/name.
     */
    private fun extractMetaContent(html: String, property: String): String? {
        val patterns = listOf(
            Regex("""<meta[^>]+(?:property|name)=["']$property["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+(?:property|name)=["']$property["']""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(html)
            val url = match?.groupValues?.getOrNull(1)
            if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                return url
            }
        }
        return null
    }

    /**
     * Returns a unique, visually diverse Unsplash image for each article.
     * Uses a pool of 30 AI/tech-themed images selected by title hash.
     */
    private fun getFallbackImage(title: String): String {
        val imagePool = listOf(
            "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800&q=80",
            "https://images.unsplash.com/photo-1620712943543-bcc4688e7485?w=800&q=80",
            "https://images.unsplash.com/photo-1676299081847-824916de030a?w=800&q=80",
            "https://images.unsplash.com/photo-1655720828018-edd2daec9349?w=800&q=80",
            "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&q=80",
            "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=800&q=80",
            "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&q=80",
            "https://images.unsplash.com/photo-1555255707-c07966088b7b?w=800&q=80",
            "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=800&q=80",
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800&q=80",
            "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=800&q=80",
            "https://images.unsplash.com/photo-1531746790095-6c10a4031c5f?w=800&q=80",
            "https://images.unsplash.com/photo-1504639725590-34d0984388bd?w=800&q=80",
            "https://images.unsplash.com/photo-1555949963-aa79dcee981c?w=800&q=80",
            "https://images.unsplash.com/photo-1542831371-29b0f74f9713?w=800&q=80",
            "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=800&q=80",
            "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&q=80",
            "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=800&q=80",
            "https://images.unsplash.com/photo-1507413245164-6160d8298b31?w=800&q=80",
            "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?w=800&q=80",
            "https://images.unsplash.com/photo-1559526324-4b87b5e36e44?w=800&q=80",
            "https://images.unsplash.com/photo-1573804633927-bfcbcd909acd?w=800&q=80",
            "https://images.unsplash.com/photo-1633419461186-7d40a38105ec?w=800&q=80",
            "https://images.unsplash.com/photo-1488229297570-58520851e868?w=800&q=80",
            "https://images.unsplash.com/photo-1558618666-fcd25c85f82e?w=800&q=80",
            "https://images.unsplash.com/photo-1547891654-e66ed7ebb968?w=800&q=80",
            "https://images.unsplash.com/photo-1534972195531-d756b9bfa9f2?w=800&q=80",
            "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?w=800&q=80",
            "https://images.unsplash.com/photo-1530497610245-94d3c16cda28?w=800&q=80",
            "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=800&q=80"
        )
        val hash = title.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
        return imagePool[hash % imagePool.size]
    }

    private fun fetchFeed(feedUrl: String): List<NewsArticleEntity> {
        val request = Request.Builder()
            .url(feedUrl)
            .header("User-Agent", "Mozilla/5.0 (compatible; AIDicted/1.0)")
            .header("Accept", "application/rss+xml, application/xml, text/xml, application/atom+xml")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.e(TAG, "Feed request failed with code ${response.code}: $feedUrl")
            response.close()
            return emptyList()
        }
        val xml = response.body?.string() ?: return emptyList()
        return parseRss(xml, feedUrl)
    }

    /**
     * Parses RSS/Atom feed XML. Handles:
     * - Standard RSS 2.0 <item> elements
     * - Atom <entry> elements
     * - <media:content>, <media:thumbnail>, and <enclosure> for images
     */
    private fun parseRss(xml: String, feedUrl: String): List<NewsArticleEntity> {
        val articles = mutableListOf<NewsArticleEntity>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var insideItem = false
            var title = ""
            var link = ""
            var pubDate = ""
            var source = ""
            var description = ""
            var mediaUrl = ""
            var currentTag = ""
            var isAtom = false

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name ?: ""

                        // Detect RSS <item> or Atom <entry>
                        if (currentTag == "item" || currentTag == "entry") {
                            insideItem = true
                            isAtom = currentTag == "entry"
                            title = ""
                            link = ""
                            pubDate = ""
                            source = extractSourceFromFeedUrl(feedUrl)
                            description = ""
                            mediaUrl = ""
                        }

                        if (insideItem) {
                            // Atom <link> has href as attribute
                            if (currentTag == "link" && isAtom) {
                                val href = parser.getAttributeValue(null, "href")
                                val rel = parser.getAttributeValue(null, "rel")
                                if (href != null && (rel == null || rel == "alternate")) {
                                    link = href
                                }
                            }

                            // RSS <source> tag
                            if (currentTag == "source") {
                                val srcUrl = parser.getAttributeValue(null, "url")
                                if (srcUrl != null) {
                                    source = extractSourceFromUrl(srcUrl)
                                }
                            }

                            // <media:content url="..."> or <media:thumbnail url="...">
                            if (currentTag == "content" || currentTag == "thumbnail") {
                                val url = parser.getAttributeValue(null, "url")
                                if (url != null && mediaUrl.isBlank() &&
                                    (url.contains(".jpg", true) || url.contains(".jpeg", true)
                                            || url.contains(".png", true) || url.contains(".webp", true)
                                            || url.contains("image", true))) {
                                    mediaUrl = url
                                }
                            }

                            // <enclosure url="..." type="image/...">
                            if (currentTag == "enclosure") {
                                val url = parser.getAttributeValue(null, "url")
                                val type = parser.getAttributeValue(null, "type")
                                if (url != null && type != null && type.startsWith("image/")) {
                                    mediaUrl = url
                                }
                            }
                        }
                    }

                    XmlPullParser.TEXT -> {
                        if (insideItem) {
                            val text = parser.text?.trim() ?: ""
                            when (currentTag) {
                                "title" -> title = text
                                "link" -> if (!isAtom && text.startsWith("http")) link = text
                                "pubDate", "published", "updated" -> if (pubDate.isBlank()) pubDate = text
                                "source" -> source = text.ifEmpty { source }
                                "description", "summary" -> if (description.isBlank()) description = text
                                "encoded" -> if (description.isBlank()) description = text // content:encoded
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val endTag = parser.name ?: ""
                        if ((endTag == "item" || endTag == "entry") && insideItem) {
                            insideItem = false
                            if (title.isNotBlank() && link.isNotBlank()) {
                                val cleanSummary = cleanHtml(description).ifEmpty {
                                    "Tap to read the full article from ${source.ifEmpty { "the source" }}."
                                }
                                val isoDate = parseDateToIso(pubDate)
                                // Priority: RSS media tag > image in description HTML
                                val imageUrl = mediaUrl.ifBlank {
                                    extractImageFromHtml(description)
                                }

                                articles.add(
                                    NewsArticleEntity(
                                        id = generateId(title, link),
                                        title = cleanTitle(cleanHtml(title)),
                                        summary = cleanSummary.take(500),
                                        content = null,
                                        imageUrl = imageUrl.ifBlank { null },
                                        source = source.ifEmpty { extractSourceFromUrl(link) },
                                        sourceUrl = link,
                                        publishedAt = isoDate,
                                        createdAt = isoDate
                                    )
                                )
                            }
                        }
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "RSS parse error for $feedUrl", e)
        }

        return articles
    }

    /** Google News / some RSS titles end with " - SourceName". Clean that. */
    private fun cleanTitle(title: String): String {
        val dashIndex = title.lastIndexOf(" - ")
        return if (dashIndex > 20) title.substring(0, dashIndex).trim() else title
    }

    /** Strip HTML tags and decode all HTML entities from text. */
    private fun cleanHtml(html: String): String {
        return html
            // Remove HTML tags
            .replace(Regex("<[^>]*>"), "")
            // Decode numeric HTML entities (decimal): &#160; &#8230; etc.
            .replace(Regex("&#(\\d+);")) { matchResult ->
                val code = matchResult.groupValues[1].toIntOrNull()
                if (code != null) {
                    try { String(Character.toChars(code)) } catch (_: Exception) { "" }
                } else ""
            }
            // Decode hex HTML entities: &#xA0; &#x2026; etc.
            .replace(Regex("&#x([0-9a-fA-F]+);")) { matchResult ->
                val code = matchResult.groupValues[1].toIntOrNull(16)
                if (code != null) {
                    try { String(Character.toChars(code)) } catch (_: Exception) { "" }
                } else ""
            }
            // Decode common named entities
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&lsquo;", "\u2018")
            .replace("&rsquo;", "\u2019")
            .replace("&ldquo;", "\u201C")
            .replace("&rdquo;", "\u201D")
            .replace("&hellip;", "…")
            .replace("&bull;", "•")
            .replace("&copy;", "©")
            .replace("&trade;", "™")
            .replace("&reg;", "®")
            // Clean up whitespace
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /** Extract first <img src="..."> from HTML content. */
    private fun extractImageFromHtml(html: String): String {
        val imgRegex = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val match = imgRegex.find(html)
        val url = match?.groupValues?.getOrNull(1) ?: ""
        // Filter out tiny tracking pixels and icons
        return if (url.startsWith("http") && !url.contains("1x1") && !url.contains("pixel") && !url.contains("icon")) {
            url
        } else ""
    }

    /**
     * Parse various date formats to ISO 8601.
     * Handles RFC 2822 (RSS) and ISO 8601 (Atom).
     */
    private fun parseDateToIso(dateStr: String): String {
        if (dateStr.isBlank()) return nowIso()

        // Already ISO format
        if (dateStr.matches(Regex("\\d{4}-\\d{2}-\\d{2}T.*"))) {
            return dateStr.replace(Regex("\\+\\d{2}:\\d{2}$"), "Z")
        }

        // RFC 2822 format (RSS standard)
        val rssFormats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss z",
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "dd MMM yyyy HH:mm:ss z"
        )

        for (format in rssFormats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.ENGLISH)
                val date = sdf.parse(dateStr) ?: continue
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
                isoFormat.timeZone = TimeZone.getTimeZone("UTC")
                return isoFormat.format(date)
            } catch (_: Exception) { }
        }

        return nowIso()
    }

    private fun nowIso(): String {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
        isoFormat.timeZone = TimeZone.getTimeZone("UTC")
        return isoFormat.format(java.util.Date())
    }

    /** Generate a stable unique ID from title + link using MD5 hash. */
    private fun generateId(title: String, link: String): String {
        val input = "$title|$link"
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /** Extract a readable source name from a URL. */
    private fun extractSourceFromUrl(url: String): String {
        return try {
            val host = java.net.URL(url).host
            host.removePrefix("www.")
                .substringBefore(".")
                .replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            "News"
        }
    }

    /** Extract source name from feed URL. */
    private fun extractSourceFromFeedUrl(feedUrl: String): String {
        return when {
            "techcrunch" in feedUrl -> "TechCrunch"
            "theverge" in feedUrl -> "The Verge"
            "arstechnica" in feedUrl -> "Ars Technica"
            "wired" in feedUrl -> "Wired"
            "technologyreview" in feedUrl -> "MIT Tech Review"
            "venturebeat" in feedUrl -> "VentureBeat"
            else -> extractSourceFromUrl(feedUrl)
        }
    }
}
