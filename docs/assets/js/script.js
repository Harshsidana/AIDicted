/* ============================================================
   AIDicted Web — Full JavaScript Engine
   Live RSS feeds via CORS proxy, Search, Favorites, Modal
   ============================================================ */

'use strict';

// ---- RSS Feed Sources (matching the Android app exactly) ----
const RSS_FEEDS = [
    { url: 'https://techcrunch.com/category/artificial-intelligence/feed/', name: 'TechCrunch' },
    { url: 'https://www.theverge.com/rss/ai-artificial-intelligence/index.xml', name: 'The Verge' },
    { url: 'https://feeds.arstechnica.com/arstechnica/technology-lab', name: 'Ars Technica' },
    { url: 'https://www.wired.com/feed/tag/ai/latest/rss', name: 'Wired' },
    { url: 'https://www.technologyreview.com/feed/', name: 'MIT Tech Review' },
    { url: 'https://venturebeat.com/category/ai/feed/', name: 'VentureBeat' },
];

// ---- CORS Proxy Strategy: try in order until one works ----
// rss2json returns pre-parsed JSON; the others return raw XML we parse ourselves.
const RSS2JSON = 'https://api.rss2json.com/v1/api.json?rss_url=';
const CORSPROXY = 'https://corsproxy.io/?';             // returns raw RSS text
const ALLORIGINS = 'https://api.allorigins.win/get?url='; // returns { contents: "..." }
const RSSBRIDGE = 'https://rss-proxy.vercel.app/api?url='; // 4th fallback

// ---- Cache config ----
const CACHE_KEY = 'aidicted_articles_cache';
const CACHE_TS_KEY = 'aidicted_cache_time';
const CACHE_TTL_MS = 30 * 60 * 1000; // 30 minutes

// ---- State ----
let allArticles = [];
let filteredArticles = [];
let favorites = JSON.parse(localStorage.getItem('aidicted_favorites') || '[]');
let currentModalArticle = null;
let activeFilter = 'all';
let currentSection = 'home';

// ---- Fallback images pool (same as Android app) ----
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

// ========== RSS JSON PARSING (rss2json returns pre-parsed JSON) ==========

function parseRss2JsonResponse(data, sourceName) {
    if (!data || !Array.isArray(data.items)) return [];

    return data.items
        .filter(item => item.title && item.link)
        .map(item => {
            // Summary: strip HTML, truncate
            const rawDesc = item.description || item.content || '';
            const summary = truncate(cleanHtml(rawDesc), 500)
                || `Read the full article from ${sourceName}.`;

            // Image: rss2json exposes item.thumbnail or image in content
            let imageUrl = item.thumbnail || '';
            if (!imageUrl || !imageUrl.startsWith('http')) {
                // Try to pull first <img> from content
                const imgMatch = rawDesc.match(/<img[^>]+src=["']([^"']+)["']/i);
                imageUrl = (imgMatch && imgMatch[1] && imgMatch[1].startsWith('http'))
                    ? imgMatch[1]
                    : getFallbackImage(item.title);
            }

            const publishedAt = item.pubDate
                ? new Date(item.pubDate).toISOString()
                : new Date().toISOString();

            const cleanedTitle = cleanTitle(cleanHtml(item.title));
            const id = simpleHash(cleanedTitle + item.link);

            return {
                id,
                title: cleanedTitle,
                summary,
                imageUrl,
                source: sourceName,
                sourceUrl: item.link,
                publishedAt,
            };
        });
}

// ========== RAW XML PARSING (fallback for corsproxy.io / allorigins) ==========

function parseRssXml(xmlText, sourceName) {
    try {
        const parser = new DOMParser();
        const xml = parser.parseFromString(xmlText, 'text/xml');
        const items = xml.querySelectorAll('item, entry');
        const articles = [];

        items.forEach(item => {
            const title = item.querySelector('title')?.textContent?.trim() || '';
            if (!title) return;

            let link = '';
            const linkEl = item.querySelector('link');
            if (linkEl) {
                link = linkEl.getAttribute('href') || linkEl.textContent?.trim() || '';
            }
            if (!link || !link.startsWith('http')) return;

            const descEl = item.querySelector('description, summary, encoded');
            const rawDesc = descEl?.textContent || '';
            const summary = truncate(cleanHtml(rawDesc), 500) || `Read the full article from ${sourceName}.`;

            const pubDateEl = item.querySelector('pubDate, published, updated');
            const pubDateStr = pubDateEl?.textContent?.trim() || '';
            const publishedAt = pubDateStr ? new Date(pubDateStr).toISOString() : new Date().toISOString();

            // Image: try media:content, media:thumbnail, enclosure, img in description
            let imageUrl = '';
            const mediaContent = item.querySelector('[url]');
            if (mediaContent) imageUrl = mediaContent.getAttribute('url') || '';
            if (!imageUrl) {
                const enclosure = item.querySelector('enclosure[type^="image"]');
                imageUrl = enclosure?.getAttribute('url') || '';
            }
            if (!imageUrl) {
                const imgMatch = rawDesc.match(/<img[^>]+src=["']([^"']+)["']/i);
                if (imgMatch?.[1]?.startsWith('http')) imageUrl = imgMatch[1];
            }
            if (!imageUrl) imageUrl = getFallbackImage(title);

            const cleanedTitle = cleanTitle(cleanHtml(title));
            const id = simpleHash(cleanedTitle + link);
            articles.push({ id, title: cleanedTitle, summary, imageUrl, source: sourceName, sourceUrl: link, publishedAt });
        });

        return articles;
    } catch (e) {
        console.warn('parseRssXml error:', e);
        return [];
    }
}

// ========== FETCH ALL FEEDS ==========

// ========== STATIC JSON LOADER (primary — served from Firebase, no CORS) ==========

async function loadFromStaticJson() {
    try {
        const res = await fetchWithTimeout('/data/articles.json');
        if (!res.ok) return null;
        const data = await res.json();
        if (!Array.isArray(data.articles) || data.articles.length === 0) return null;
        console.log(`[static-json] ✓ ${data.articles.length} articles (fetched at ${data.fetchedAt})`);
        return data.articles;
    } catch (e) {
        console.warn('[static-json] ✗', e.message);
        return null;
    }
}

// ========== FETCH ALL FEEDS ==========

async function loadNews() {
    const loadingEl = document.getElementById('loadingState');
    setRefreshSpinning(true);

    // ---- Show cached articles instantly (stale-while-revalidate) ----
    const cached = loadFromCache();
    if (cached && cached.length > 0) {
        allArticles = cached;
        filteredArticles = [...allArticles];
        loadingEl.style.display = 'none';
        document.getElementById('errorState').style.display = 'none';
        document.getElementById('articleCount').textContent = allArticles.length;
        document.getElementById('savedCount').textContent = favorites.length;
        renderNewsGrid('newsGrid', filteredArticles);
        updateProgress();
        // If cache is fresh, skip network fetch
        const cacheAge = Date.now() - (parseInt(localStorage.getItem(CACHE_TS_KEY) || '0', 10));
        if (cacheAge < CACHE_TTL_MS) {
            setRefreshSpinning(false);
            return;
        }
        // Otherwise refresh in background (no loading spinner shown)
    } else {
        // No cache — show loading spinner
        loadingEl.style.display = 'block';
        document.getElementById('errorState').style.display = 'none';
        document.getElementById('newsGrid').innerHTML = '';
    }

    try {
        // ---- Strategy 0: Static pre-fetched JSON (GitHub Actions, most reliable) ----
        let freshArticles = await loadFromStaticJson();
        let usedStaticJson = false;

        if (freshArticles && freshArticles.length > 0) {
            usedStaticJson = true;
            console.log('[loadNews] Using static JSON');
        } else {
            // ---- Fallback: CORS proxies (client-side RSS fetching) ----
            console.log('[loadNews] Static JSON empty/failed — trying CORS proxies...');
            const results = await Promise.allSettled(
                RSS_FEEDS.map(feed => fetchFeed(feed.url, feed.name))
            );
            freshArticles = [];
            results.forEach(result => {
                if (result.status === 'fulfilled' && result.value.length > 0) {
                    freshArticles.push(...result.value);
                } else if (result.status === 'rejected') {
                    console.warn('Feed failed:', result.reason);
                }
            });
        }

        if (freshArticles.length === 0) {
            // Network totally failed — if we already showed cache, keep it silently
            if (allArticles.length === 0) {
                loadingEl.style.display = 'none';
                document.getElementById('errorState').style.display = 'block';
            }
            return;
        }

        // Deduplicate by title prefix
        const seen = new Set();
        allArticles = freshArticles.filter(a => {
            const key = a.title.toLowerCase().slice(0, 60);
            if (seen.has(key)) return false;
            seen.add(key);
            return true;
        });

        // Sort by date descending and cap at 60
        allArticles.sort((a, b) => new Date(b.publishedAt) - new Date(a.publishedAt));
        allArticles = allArticles.slice(0, 60);
        filteredArticles = activeFilter === 'all'
            ? [...allArticles]
            : allArticles.filter(a => a.source === activeFilter);

        // Save to cache for next visit
        saveToCache(allArticles);

        loadingEl.style.display = 'none';
        document.getElementById('articleCount').textContent = allArticles.length;
        document.getElementById('savedCount').textContent = favorites.length;

        renderNewsGrid('newsGrid', filteredArticles);
        updateProgress();

    } catch (err) {
        console.error('loadNews error:', err);
        if (allArticles.length === 0) {
            loadingEl.style.display = 'none';
            document.getElementById('errorState').style.display = 'block';
        }
    } finally {
        setRefreshSpinning(false);
    }
}

// ---- Cache helpers ----
function saveToCache(articles) {
    try {
        localStorage.setItem(CACHE_KEY, JSON.stringify(articles));
        localStorage.setItem(CACHE_TS_KEY, Date.now().toString());
    } catch (e) { console.warn('Cache write failed:', e); }
}

function loadFromCache() {
    try {
        const raw = localStorage.getItem(CACHE_KEY);
        return raw ? JSON.parse(raw) : null;
    } catch (e) { return null; }
}

// ---- AbortController helper (proper fetch timeout) ----
function fetchWithTimeout(url, timeoutMs = 12000) {
    const ctrl = new AbortController();
    const id = setTimeout(() => ctrl.abort(), timeoutMs);
    return fetch(url, { signal: ctrl.signal }).finally(() => clearTimeout(id));
}

async function fetchFeed(feedUrl, name) {
    // ---- Strategy 1: rss2json (returns clean JSON) ----
    try {
        const url = `${RSS2JSON}${encodeURIComponent(feedUrl)}&count=15`;
        const res = await fetchWithTimeout(url);
        if (res.ok) {
            const data = await res.json();
            if (data.status === 'ok' && Array.isArray(data.items) && data.items.length > 0) {
                console.log(`[rss2json] ✓ ${name}: ${data.items.length} items`);
                return parseRss2JsonResponse(data, name);
            }
        }
    } catch (e) { console.warn(`[rss2json] ✗ ${name}:`, e.message); }

    // ---- Strategy 2: corsproxy.io (returns raw XML) ----
    try {
        const url = `${CORSPROXY}${encodeURIComponent(feedUrl)}`;
        const res = await fetchWithTimeout(url);
        if (res.ok) {
            const xml = await res.text();
            const articles = parseRssXml(xml, name);
            if (articles.length > 0) {
                console.log(`[corsproxy] ✓ ${name}: ${articles.length} items`);
                return articles;
            }
        }
    } catch (e) { console.warn(`[corsproxy] ✗ ${name}:`, e.message); }

    // ---- Strategy 3: allorigins (returns { contents: xmlString }) ----
    try {
        const url = `${ALLORIGINS}${encodeURIComponent(feedUrl)}`;
        const res = await fetchWithTimeout(url);
        if (res.ok) {
            const data = await res.json();
            const xml = data.contents || '';
            const articles = parseRssXml(xml, name);
            if (articles.length > 0) {
                console.log(`[allorigins] ✓ ${name}: ${articles.length} items`);
                return articles;
            }
        }
    } catch (e) { console.warn(`[allorigins] ✗ ${name}:`, e.message); }

    // ---- Strategy 4: rss-proxy (additional fallback) ----
    try {
        const url = `${RSSBRIDGE}${encodeURIComponent(feedUrl)}`;
        const res = await fetchWithTimeout(url);
        if (res.ok) {
            const text = await res.text();
            // Could be JSON or XML
            let articles = [];
            try {
                const json = JSON.parse(text);
                if (Array.isArray(json.items)) articles = parseRss2JsonResponse(json, name);
            } catch {
                articles = parseRssXml(text, name);
            }
            if (articles.length > 0) {
                console.log(`[rssbridge] ✓ ${name}: ${articles.length} items`);
                return articles;
            }
        }
    } catch (e) { console.warn(`[rssbridge] ✗ ${name}:`, e.message); }

    console.error(`[fetchFeed] All proxies failed for ${name}`);
    return [];
}

// ========== RENDER ==========

function renderNewsGrid(gridId, articles) {
    const grid = document.getElementById(gridId);
    if (!grid) return;

    if (articles.length === 0) {
        grid.innerHTML = '';
        return;
    }

    grid.innerHTML = articles.map(article => createCardHTML(article)).join('');

    // Observe cards for fade-in animation
    grid.querySelectorAll('.news-card').forEach((card, i) => {
        card.style.opacity = '0';
        card.style.transform = 'translateY(16px)';
        card.style.transition = `opacity 0.4s ease ${Math.min(i * 0.04, 0.5)}s, transform 0.4s ease ${Math.min(i * 0.04, 0.5)}s`;
        setTimeout(() => {
            card.style.opacity = '1';
            card.style.transform = 'translateY(0)';
        }, 50);
    });
}

function createCardHTML(article) {
    const isSaved = favorites.some(f => f.id === article.id);
    const timeAgo = formatRelativeTime(article.publishedAt);
    const saveIcon = isSaved ? '🔖' : '🤍';
    const savedClass = isSaved ? 'saved' : '';

    return `
    <article class="news-card" onclick="openModalWithEvent(event, '${escapeId(article.id)}')">
      <div class="card-img-wrap">
        <img class="card-img" src="${article.imageUrl}" 
          alt="${escapeAttr(article.title)}" loading="lazy"
          onerror="this.src='${getFallbackImage(article.title)}'">
        <div class="card-img-gradient"></div>
        <span class="card-source-badge">${escapeHtml(article.source || '')}</span>
      </div>
      <div class="card-body">
        <div class="card-meta">
          <span class="card-time">${timeAgo}</span>
          <button class="card-save-btn ${savedClass}" 
            data-id="${escapeId(article.id)}"
            onclick="toggleFavorite(event, '${escapeId(article.id)}')" 
            title="${isSaved ? 'Remove from saved' : 'Save article'}">
            ${saveIcon}
          </button>
        </div>
        <h3 class="card-title">${escapeHtml(article.title)}</h3>
        <p class="card-summary">${escapeHtml(article.summary)}</p>
        <span class="card-read-more">Read full article →</span>
      </div>
    </article>
  `;
}

// ========== MODAL ==========

function openModalWithEvent(event, articleId) {
    // Don't open modal if save button was clicked
    if (event.target.closest('.card-save-btn')) return;
    const article = findArticleById(articleId);
    if (!article) return;
    openModal(article);
}

function openModal(article) {
    currentModalArticle = article;
    const isSaved = favorites.some(f => f.id === article.id);

    document.getElementById('modalImage').src = article.imageUrl;
    document.getElementById('modalImage').onerror = function () {
        this.src = getFallbackImage(article.title);
    };
    document.getElementById('modalSource').textContent = article.source || '';
    document.getElementById('modalTime').textContent = formatRelativeTime(article.publishedAt);
    document.getElementById('modalTitle').textContent = article.title;
    document.getElementById('modalSummary').textContent = article.summary;
    document.getElementById('modalReadMore').href = article.sourceUrl || '#';
    document.getElementById('modalSaveBtn').textContent = isSaved ? '🔖 Saved' : '🤍 Save';
    document.getElementById('modalSaveBtn').className = `btn btn-save ${isSaved ? 'saved' : ''}`;

    document.getElementById('modalOverlay').classList.add('open');
    document.getElementById('articleModal').classList.add('open');
    document.body.style.overflow = 'hidden';
}

function closeModal() {
    document.getElementById('modalOverlay').classList.remove('open');
    document.getElementById('articleModal').classList.remove('open');
    document.body.style.overflow = '';
    currentModalArticle = null;
}

function toggleFavoriteFromModal() {
    if (!currentModalArticle) return;
    toggleFavoriteById(currentModalArticle.id);
    const isSaved = favorites.some(f => f.id === currentModalArticle.id);
    document.getElementById('modalSaveBtn').textContent = isSaved ? '🔖 Saved' : '🤍 Save';
    document.getElementById('modalSaveBtn').className = `btn btn-save ${isSaved ? 'saved' : ''}`;
}

// ========== FAVORITES ==========

function toggleFavorite(event, articleId) {
    event.stopPropagation();
    toggleFavoriteById(articleId);
    refreshSaveButtons(articleId);
}

function toggleFavoriteById(id) {
    const article = findArticleById(id);
    if (!article) return;

    const idx = favorites.findIndex(f => f.id === id);
    if (idx >= 0) {
        favorites.splice(idx, 1);
        showToast('Removed from saved');
    } else {
        favorites.push(article);
        showToast('✨ Article saved!');
    }

    localStorage.setItem('aidicted_favorites', JSON.stringify(favorites));
    document.getElementById('savedCount').textContent = favorites.length;

    if (currentSection === 'favorites') renderFavorites();
}

function refreshSaveButtons(articleId) {
    const isSaved = favorites.some(f => f.id === articleId);
    document.querySelectorAll(`.card-save-btn[data-id="${CSS.escape(articleId)}"]`).forEach(btn => {
        btn.textContent = isSaved ? '🔖' : '🤍';
        btn.className = `card-save-btn ${isSaved ? 'saved' : ''}`;
        btn.setAttribute('data-id', articleId);
        btn.onclick = (e) => toggleFavorite(e, articleId);
    });
}

function renderFavorites() {
    const grid = document.getElementById('favoritesGrid');
    const empty = document.getElementById('favoritesEmpty');
    if (favorites.length === 0) {
        grid.innerHTML = '';
        empty.style.display = 'block';
    } else {
        empty.style.display = 'none';
        renderNewsGrid('favoritesGrid', favorites);
    }
}

// ========== SEARCH ==========

function performSearch(query) {
    const clearBtn = document.getElementById('searchClear');
    const countEl = document.getElementById('searchResultsCount');
    const emptyEl = document.getElementById('searchEmpty');
    const placeholderEl = document.getElementById('searchPlaceholder');

    clearBtn.style.display = query ? 'block' : 'none';

    if (!query.trim()) {
        document.getElementById('searchGrid').innerHTML = '';
        countEl.style.display = 'none';
        emptyEl.style.display = 'none';
        placeholderEl.style.display = 'block';
        return;
    }

    placeholderEl.style.display = 'none';
    const q = query.toLowerCase();
    const results = allArticles.filter(a =>
        a.title.toLowerCase().includes(q) ||
        a.summary.toLowerCase().includes(q) ||
        (a.source || '').toLowerCase().includes(q)
    );

    if (results.length === 0) {
        document.getElementById('searchGrid').innerHTML = '';
        countEl.style.display = 'none';
        emptyEl.style.display = 'block';
    } else {
        emptyEl.style.display = 'none';
        countEl.style.display = 'block';
        countEl.textContent = `${results.length} result${results.length !== 1 ? 's' : ''} for "${query}"`;
        renderNewsGrid('searchGrid', results);
    }
}

function clearSearch() {
    const input = document.getElementById('searchInput');
    input.value = '';
    performSearch('');
    input.focus();
}

// ========== FILTER ==========

function filterBySource(btn, source) {
    document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
    btn.classList.add('active');
    activeFilter = source;

    filteredArticles = source === 'all'
        ? [...allArticles]
        : allArticles.filter(a => a.source === source);

    renderNewsGrid('newsGrid', filteredArticles);
}

// ========== NAVIGATION ==========

function showSection(name) {
    currentSection = name;

    document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
    document.getElementById(`section-${name}`)?.classList.add('active');

    document.querySelectorAll('.nav-link').forEach(l => {
        l.classList.remove('active');
        if (l.dataset.section === name) l.classList.add('active');
    });

    // Close mobile menu
    document.getElementById('navLinks').classList.remove('open');
    document.getElementById('hamburger').classList.remove('open');

    window.scrollTo({ top: 0, behavior: 'smooth' });

    if (name === 'favorites') renderFavorites();
}

function toggleMenu() {
    const menu = document.getElementById('navLinks');
    const btn = document.getElementById('hamburger');
    menu.classList.toggle('open');
    btn.classList.toggle('open');
}

// ========== PROGRESS BAR ==========

function updateProgress() {
    const scrollable = document.documentElement.scrollHeight - window.innerHeight;
    const progress = scrollable > 0 ? (window.scrollY / scrollable) * 100 : 100;
    document.getElementById('topProgress').style.width = `${progress}%`;

    const navbar = document.getElementById('navbar');
    navbar.classList.toggle('scrolled', window.scrollY > 20);
}

// ========== HELPERS ==========

function findArticleById(id) {
    return allArticles.find(a => a.id === id)
        || favorites.find(a => a.id === id)
        || null;
}

function formatRelativeTime(isoString) {
    try {
        const date = new Date(isoString);
        const now = Date.now();
        const diff = now - date.getTime();
        if (diff < 60_000) return 'Just now';
        if (diff < 3_600_000) return `${Math.floor(diff / 60_000)}m ago`;
        if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)}h ago`;
        if (diff < 604_800_000) return `${Math.floor(diff / 86_400_000)}d ago`;
        return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    } catch {
        return '';
    }
}

function decodeHtmlEntities(str) {
    return (str || '')
        // Named entities
        .replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>')
        .replace(/&quot;/g, '"').replace(/&#39;/g, "'").replace(/&apos;/g, "'")
        .replace(/&nbsp;/g, ' ').replace(/&mdash;/g, '—').replace(/&ndash;/g, '–')
        .replace(/&hellip;/g, '…').replace(/&lsquo;/g, "'").replace(/&rsquo;/g, "'")
        .replace(/&ldquo;/g, '"').replace(/&rdquo;/g, '"').replace(/&copy;/g, '©')
        // Decimal numeric entities e.g. &#8230;
        .replace(/&#(\d+);/g, (_, code) => String.fromCodePoint(parseInt(code, 10)))
        // Hex numeric entities e.g. &#x2026;
        .replace(/&#x([\da-fA-F]+);/g, (_, code) => String.fromCodePoint(parseInt(code, 16)));
}

function cleanHtml(html) {
    return decodeHtmlEntities(
        (html || '').replace(/<[^>]*>/g, '')  // strip tags first
    ).replace(/\s+/g, ' ').trim();
}

// Truncate at word boundary and append ellipsis if needed
function truncate(text, maxLen) {
    if (!text || text.length <= maxLen) return text;
    const cut = text.lastIndexOf(' ', maxLen);
    return (cut > maxLen * 0.7 ? text.slice(0, cut) : text.slice(0, maxLen)).trimEnd() + '…';
}

function cleanTitle(title) {
    const idx = title.lastIndexOf(' - ');
    return (idx > 20) ? title.substring(0, idx).trim() : title;
}

function escapeHtml(str) {
    return (str || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function escapeAttr(str) { return (str || '').replace(/"/g, '&quot;'); }

function escapeId(str) { return (str || '').replace(/[^a-z0-9]/gi, '_'); }

function simpleHash(str) {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
        hash = ((hash << 5) - hash) + str.charCodeAt(i);
        hash |= 0;
    }
    return Math.abs(hash).toString(36);
}

function setRefreshSpinning(spinning) {
    const btn = document.querySelector('.refresh-btn');
    if (spinning) btn.classList.add('spinning');
    else btn.classList.remove('spinning');
}

let toastTimeout;
function showToast(msg) {
    const toast = document.getElementById('toast');
    toast.textContent = msg;
    toast.classList.add('show');
    clearTimeout(toastTimeout);
    toastTimeout = setTimeout(() => toast.classList.remove('show'), 2500);
}

// ========== KEY EVENTS ==========

document.addEventListener('keydown', e => {
    if (e.key === 'Escape') closeModal();
});

window.addEventListener('scroll', updateProgress, { passive: true });

// ========== BOOT ==========

document.addEventListener('DOMContentLoaded', () => {
    loadNews();
});
