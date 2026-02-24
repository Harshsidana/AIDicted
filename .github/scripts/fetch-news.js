/* ============================================================
   Server-side RSS fetcher — runs in GitHub Actions (Node.js)
   Fetches all 6 RSS feeds and writes docs/data/articles.json
   ============================================================ */

const RSSParser = require('rss-parser');
const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');

const parser = new RSSParser({
    timeout: 15000,
    customFields: {
        item: [
            ['media:content', 'mediaContent', { keepArray: false }],
            ['media:thumbnail', 'mediaThumbnail', { keepArray: false }],
        ]
    }
});

const RSS_FEEDS = [
    { url: 'https://techcrunch.com/category/artificial-intelligence/feed/', name: 'TechCrunch' },
    { url: 'https://www.theverge.com/rss/ai-artificial-intelligence/index.xml', name: 'The Verge' },
    { url: 'https://feeds.arstechnica.com/arstechnica/technology-lab', name: 'Ars Technica' },
    { url: 'https://www.wired.com/feed/tag/ai/latest/rss', name: 'Wired' },
    { url: 'https://www.technologyreview.com/feed/', name: 'MIT Tech Review' },
    { url: 'https://venturebeat.com/category/ai/feed/', name: 'VentureBeat' },
];

const FALLBACK_IMAGES = [
    'https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800&q=80',
    'https://images.unsplash.com/photo-1620712943543-bcc4688e7485?w=800&q=80',
    'https://images.unsplash.com/photo-1676299081847-824916de030a?w=800&q=80',
    'https://images.unsplash.com/photo-1655720828018-edd2daec9349?w=800&q=80',
    'https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&q=80',
    'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=800&q=80',
    'https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&q=80',
    'https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=800&q=80',
    'https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=800&q=80',
    'https://images.unsplash.com/photo-1531746790095-6c10a4031c5f?w=800&q=80',
];

function getFallbackImage(title) {
    let hash = 0;
    for (let i = 0; i < title.length; i++) {
        hash = ((hash << 5) - hash) + title.charCodeAt(i);
        hash |= 0;
    }
    return FALLBACK_IMAGES[Math.abs(hash) % FALLBACK_IMAGES.length];
}

function simpleHash(str) {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
        hash = ((hash << 5) - hash) + str.charCodeAt(i);
        hash |= 0;
    }
    return Math.abs(hash).toString(36);
}

function cleanHtml(html) {
    return (html || '')
        .replace(/<[^>]*>/g, '')
        .replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>')
        .replace(/&quot;/g, '"').replace(/&#39;/g, "'").replace(/&nbsp;/g, ' ')
        .replace(/&mdash;/g, '—').replace(/&ndash;/g, '–').replace(/&hellip;/g, '…')
        .replace(/\s+/g, ' ').trim();
}

function cleanTitle(title) {
    const idx = title.lastIndexOf(' - ');
    return (idx > 20) ? title.substring(0, idx).trim() : title;
}

async function fetchFeed(feedUrl, sourceName) {
    try {
        const feed = await parser.parseURL(feedUrl);
        return feed.items.slice(0, 15).map(item => {
            const rawDesc = item.content || item.contentSnippet || item.summary || '';
            const summary = cleanHtml(rawDesc).slice(0, 500) || `Read the full article from ${sourceName}.`;

            // Extract image
            let imageUrl = '';
            if (item.mediaContent?.$?.url) imageUrl = item.mediaContent.$.url;
            else if (item.mediaThumbnail?.$?.url) imageUrl = item.mediaThumbnail.$.url;
            else if (item.enclosure?.url) imageUrl = item.enclosure.url;
            if (!imageUrl || !imageUrl.startsWith('http')) {
                const imgMatch = rawDesc.match(/<img[^>]+src=["']([^"']+)["']/i);
                imageUrl = (imgMatch?.[1]?.startsWith('http')) ? imgMatch[1] : getFallbackImage(item.title || '');
            }

            const title = cleanTitle(cleanHtml(item.title || ''));
            const link = item.link || item.guid || '';
            const publishedAt = item.pubDate || item.isoDate || new Date().toISOString();

            return {
                id: simpleHash(title + link),
                title,
                summary,
                imageUrl,
                source: sourceName,
                sourceUrl: link,
                publishedAt: new Date(publishedAt).toISOString(),
            };
        }).filter(a => a.title && a.sourceUrl);
    } catch (err) {
        console.warn(`[fetchFeed] Failed ${sourceName}: ${err.message}`);
        return [];
    }
}

async function main() {
    console.log('🚀 Fetching AI news from 6 sources...');

    const results = await Promise.allSettled(
        RSS_FEEDS.map(f => fetchFeed(f.url, f.name))
    );

    let allArticles = [];
    results.forEach((result, i) => {
        if (result.status === 'fulfilled' && result.value.length > 0) {
            console.log(`  ✓ ${RSS_FEEDS[i].name}: ${result.value.length} articles`);
            allArticles.push(...result.value);
        } else {
            console.warn(`  ✗ ${RSS_FEEDS[i].name}: failed`);
        }
    });

    // Deduplicate by title prefix
    const seen = new Set();
    allArticles = allArticles.filter(a => {
        const key = a.title.toLowerCase().slice(0, 60);
        if (seen.has(key)) return false;
        seen.add(key);
        return true;
    });

    // Sort by date descending, cap at 60
    allArticles.sort((a, b) => new Date(b.publishedAt) - new Date(a.publishedAt));
    allArticles = allArticles.slice(0, 60);

    console.log(`\n✅ Total: ${allArticles.length} articles`);

    // Ensure output directory exists
    const outDir = path.join(__dirname, '../../docs/data');
    fs.mkdirSync(outDir, { recursive: true });

    const outFile = path.join(outDir, 'articles.json');
    fs.writeFileSync(outFile, JSON.stringify({
        fetchedAt: new Date().toISOString(),
        count: allArticles.length,
        articles: allArticles,
    }, null, 2));

    console.log(`📄 Saved to docs/data/articles.json`);
}

main().catch(err => {
    console.error('Fatal error:', err);
    process.exit(1);
});
