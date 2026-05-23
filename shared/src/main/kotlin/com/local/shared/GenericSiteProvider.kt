package com.local.shared

import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPage
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI
import java.net.URLEncoder

open class GenericSiteProvider(
    private val siteTitle: String,
    siteUrl: String,
    private val searchTemplates: List<String> = defaultSearchTemplates,
) : MainAPI() {
    override var mainUrl = siteUrl.trim().trimEnd('/')
    override var name = siteTitle
    override var lang = "en"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override val mainPage = listOf(mainPage(mainUrl, "Home"))

    private val requestHeaders = mapOf(
        "User-Agent" to USER_AGENT,
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        if (page > 1) return newHomePageResponse(request, emptyList(), hasNext = false)
        val document = getDocument(request.data) ?: return newHomePageResponse(request, emptyList(), hasNext = false)
        return newHomePageResponse(request, parseSearchCards(document), hasNext = false)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return emptyList()

        val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
        for (template in searchTemplates) {
            val path = template
                .replace("{query}", encoded)
                .replace("{queryPlus}", encoded.replace("%20", "+"))
            val url = absoluteUrl(path) ?: continue
            val results = getDocument(url)?.let { parseSearchCards(it, limit = 30) }.orEmpty()
            if (results.isNotEmpty()) return results
        }

        return emptyList()
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = getDocument(url) ?: return null
        val title = document.titleFromPage() ?: return null
        val poster = document.posterFromPage()
        val plot = document.descriptionFromPage()
        val year = document.text().yearFromText()
        val episodes = document.parseEpisodes(url)
        val inferredType = inferType(url, title, document.text())

        return if (episodes.isNotEmpty() || inferredType == TvType.TvSeries) {
            val finalEpisodes = episodes.ifEmpty {
                listOf(newEpisode(url) {
                    name = "Episode 1"
                    season = 1
                    episode = 1
                })
            }
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, finalEpisodes) {
                posterUrl = poster
                this.year = year
                this.plot = plot
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                posterUrl = poster
                this.year = year
                this.plot = plot
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val pageUrl = absoluteUrl(data) ?: return false
        val document = getDocument(pageUrl) ?: return false
        var found = false

        for (url in document.extractPlayableUrls(pageUrl)) {
            if (url.isDirectVideoUrl()) {
                callback(
                    newExtractorLink(
                        source = siteTitle,
                        name = "$siteTitle ${qualityLabel(url)}".trim(),
                        url = url,
                        type = directVideoType(url)
                    ) {
                        referer = pageUrl
                        quality = getQualityFromName(url).takeIf { it != Qualities.Unknown.value }
                            ?: Qualities.Unknown.value
                        headers = requestHeaders
                    }
                )
                found = true
            } else if (loadExtractor(url, pageUrl, subtitleCallback, callback)) {
                found = true
            }
        }

        return found
    }

    private suspend fun getDocument(url: String): Document? {
        return runCatching {
            app.get(url, referer = mainUrl, headers = requestHeaders).document
        }.getOrNull()
    }

    private fun parseSearchCards(document: Document, limit: Int = 40): List<SearchResponse> {
        return document.select("a[href]")
            .asSequence()
            .mapNotNull { anchor -> anchor.toSearchResponse() }
            .distinctBy { it.url }
            .take(limit)
            .toList()
    }

    private fun Element.toSearchResponse(): SearchResponse? {
        val href = absoluteUrl(attr("href")) ?: return null
        if (!href.isLikelyContentUrl()) return null

        val image = selectFirst("img")
        val title = firstNonBlank(
            attr("title"),
            attr("aria-label"),
            image?.attr("alt"),
            selectFirst("[title]")?.attr("title"),
            text()
        )?.cleanTitle() ?: return null

        if (title.length < 2 || title.isNavigationLabel()) return null

        val poster = image?.bestImageUrl()?.let { absoluteUrl(it) }
        val type = inferType(href, title, text())

        return if (type == TvType.TvSeries) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                posterUrl = poster
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                posterUrl = poster
            }
        }
    }

    private fun Document.parseEpisodes(parentUrl: String): List<com.lagradost.cloudstream3.Episode> {
        return select("a[href]")
            .asSequence()
            .mapNotNull { anchor ->
                val href = absoluteUrl(anchor.attr("href")) ?: return@mapNotNull null
                val label = anchor.text().ifBlank { anchor.attr("title") }.cleanTitle()
                if (!href.isLikelyEpisodeUrl(label)) return@mapNotNull null

                val (season, episode) = parseSeasonEpisode("$label $href")
                newEpisode(href) {
                    name = label.ifBlank { "Episode ${episode ?: 1}" }
                    this.season = season
                    this.episode = episode
                }
            }
            .distinctBy { it.data }
            .filter { it.data != parentUrl }
            .toList()
    }

    private fun Document.extractPlayableUrls(pageUrl: String): List<String> {
        val elementUrls = select("iframe[src], embed[src], video[src], source[src], a[href]")
            .mapNotNull { element ->
                val raw = firstNonBlank(element.attr("src"), element.attr("href"))
                raw?.let { absoluteUrl(it, pageUrl) }
            }

        val scriptUrls = select("script")
            .flatMap { script -> urlRegex.findAll(script.data() + "\n" + script.html()).map { it.value.unescapeUrl() } }
            .mapNotNull { absoluteUrl(it, pageUrl) }

        return (elementUrls + scriptUrls)
            .asSequence()
            .map { it.trim() }
            .filter { it.isUsefulPlayableUrl() }
            .distinct()
            .take(40)
            .toList()
    }

    private fun inferType(url: String, title: String, context: String = ""): TvType {
        val haystack = "$url $title $context".lowercase()
        return if (
            Regex("""/title/t\d+""").containsMatchIn(haystack) ||
            listOf("/tv", "/show", "/series", "/episode", "season ", "s01e", "episodes").any { haystack.contains(it) }
        ) TvType.TvSeries else TvType.Movie
    }

    private fun parseSeasonEpisode(input: String): Pair<Int?, Int?> {
        val normalized = input.lowercase()
        Regex("""s(?:eason)?\s*(\d{1,2})\D+e(?:p(?:isode)?)?\s*(\d{1,3})""")
            .find(normalized)?.let { return it.groupValues[1].toIntOrNull() to it.groupValues[2].toIntOrNull() }
        val episode = Regex("""e(?:p(?:isode)?)?\s*(\d{1,3})""").find(normalized)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("""episode[\s-]*(\d{1,3})""").find(normalized)?.groupValues?.get(1)?.toIntOrNull()
        val season = Regex("""season[\s-]*(\d{1,2})""").find(normalized)?.groupValues?.get(1)?.toIntOrNull()
        return season to episode
    }

    private fun absoluteUrl(raw: String?, base: String = mainUrl): String? {
        val value = raw?.trim()?.removeSurrounding("\"")?.removeSurrounding("'") ?: return null
        if (value.isBlank() || value.startsWith("#") || value.startsWith("javascript:", true) || value.startsWith("mailto:", true)) {
            return null
        }

        return when {
            value.startsWith("//") -> "https:$value"
            value.startsWith("http://", true) || value.startsWith("https://", true) -> value
            value.startsWith("/") -> "${base.origin()}$value"
            else -> "${base.trimEnd('/')}/$value"
        }
    }

    private fun String.origin(): String {
        return runCatching {
            val uri = URI(this)
            "${uri.scheme}://${uri.host}"
        }.getOrDefault(mainUrl)
    }

    private fun Element.bestImageUrl(): String? {
        return firstNonBlank(
            attr("data-src"),
            attr("data-original"),
            attr("data-lazy-src"),
            attr("src"),
            attr("poster"),
            attr("srcset").split(",").lastOrNull()?.trim()?.substringBefore(" ")
        )
    }

    private fun Document.titleFromPage(): String? {
        return firstNonBlank(
            selectFirst("h1")?.text(),
            selectFirst("meta[property=og:title]")?.attr("content"),
            selectFirst("meta[name=twitter:title]")?.attr("content"),
            title()
        )?.cleanTitle()
    }

    private fun Document.posterFromPage(): String? {
        return firstNonBlank(
            selectFirst("meta[property=og:image]")?.attr("content"),
            selectFirst("meta[name=twitter:image]")?.attr("content"),
            selectFirst("img[alt*=poster], img[class*=poster], img[src*=poster]")?.bestImageUrl(),
            selectFirst("img")?.bestImageUrl()
        )?.let { absoluteUrl(it) }
    }

    private fun Document.descriptionFromPage(): String? {
        return firstNonBlank(
            selectFirst("meta[name=description]")?.attr("content"),
            selectFirst("meta[property=og:description]")?.attr("content"),
            selectFirst("p")?.text()
        )?.trim()?.takeIf { it.length > 20 }
    }

    private fun String.yearFromText(): Int? {
        return Regex("""\b(19\d{2}|20\d{2})\b""").find(this)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun String.cleanTitle(): String {
        return replace(Regex("""(?i)^image:\s*"""), "")
            .replace(Regex("""(?i)^watch\s+"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .removeSuffix("| $siteTitle")
            .trim()
    }

    private fun String.isNavigationLabel(): Boolean {
        return lowercase() in setOf(
            "home", "movies", "movie", "tv", "tv shows", "shows", "search", "settings",
            "genres", "genre", "popular", "top rated", "browse", "play", "more info", "save"
        )
    }

    private fun String.isLikelyContentUrl(): Boolean {
        val lower = lowercase()
        if (!lower.isSameSiteUrl() || lower.hasStaticAssetExtension()) return false
        val path = runCatching { URI(this).path ?: "" }.getOrDefault(lower)
        val normalized = path.trimEnd('/').lowercase()
        if (normalized in setOf("", "/movies", "/movie", "/tv", "/shows", "/series", "/search", "/settings", "/genres")) return false
        return listOf("/title/", "/movie/", "/movies/", "/tv/", "/show/", "/series/", "/watch/", "/film/", "/episode/").any {
            normalized.contains(it)
        } || normalized.count { it == '/' } >= 2
    }

    private fun String.isLikelyEpisodeUrl(label: String): Boolean {
        val lower = "$this $label".lowercase()
        return isLikelyContentUrl() && listOf("episode", "/episode", "season", "s01", "s1e", "watch").any { lower.contains(it) }
    }

    private fun String.isUsefulPlayableUrl(): Boolean {
        val lower = lowercase()
        if (lower.hasStaticAssetExtension()) return false
        return lower.isDirectVideoUrl() ||
            listOf("/embed", "embed/", "player", "stream", "watch/", "play/", "video", "server").any { lower.contains(it) } &&
            (!lower.isSameSiteUrl() || lower.contains("embed") || lower.contains("player") || lower.contains("stream"))
    }

    private fun String.isDirectVideoUrl(): Boolean {
        val lower = lowercase()
        return listOf(".m3u8", ".mp4", ".mkv", ".webm", ".mpd").any { lower.contains(it) }
    }

    private fun directVideoType(url: String): ExtractorLinkType {
        val lower = url.lowercase()
        return when {
            lower.contains(".m3u8") -> ExtractorLinkType.M3U8
            lower.contains(".mpd") -> ExtractorLinkType.DASH
            else -> ExtractorLinkType.VIDEO
        }
    }

    private fun String.hasStaticAssetExtension(): Boolean {
        return listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".svg", ".ico", ".css", ".js", ".woff", ".ttf").any {
            lowercase().substringBefore('?').endsWith(it)
        }
    }

    private fun String.isSameSiteUrl(): Boolean {
        val mainHost = runCatching { URI(mainUrl).host.removeWww() }.getOrNull()
        val host = runCatching { URI(this).host.removeWww() }.getOrNull()
        return host != null && mainHost != null && (host == mainHost || host.endsWith(".$mainHost"))
    }

    private fun String?.removeWww(): String? = this?.removePrefix("www.")

    private fun String.unescapeUrl(): String {
        return replace("\\/", "/")
            .replace("\\u002F", "/")
            .replace("&amp;", "&")
    }

    private fun qualityLabel(url: String): String {
        return Regex("""(?i)(2160p|1080p|720p|480p|360p|4k)""").find(url)?.value ?: ""
    }

    private fun firstNonBlank(vararg values: String?): String? {
        return values.firstOrNull { !it.isNullOrBlank() }
    }

    companion object {
        private val defaultSearchTemplates = listOf(
            "/search/{query}",
            "/search?query={query}",
            "/search?keyword={query}",
            "/search?q={query}",
            "/?s={queryPlus}"
        )

        private val urlRegex = Regex("""https?:\\?/\\?/[^\s"'<>\\]+""")
    }
}
