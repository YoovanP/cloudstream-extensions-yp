package com.local.shared

import com.lagradost.cloudstream3.Episode
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
import com.lagradost.cloudstream3.newSubtitleFile
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

open class TmdbCatalogProvider(
    private val siteTitle: String,
    siteUrl: String,
    private val streamReferer: String = siteUrl,
) : MainAPI() {
    override var mainUrl = siteUrl.trim().trimEnd('/')
    override var name = siteTitle
    override var lang = "en"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override val mainPage = listOf(
        mainPage("trending/movie/week", "Trending Movies"),
        mainPage("trending/tv/week", "Trending Shows"),
        mainPage("movie/popular", "Popular Movies"),
        mainPage("tv/popular", "Popular Shows"),
        mainPage("movie/top_rated", "Top Rated Movies"),
        mainPage("tv/top_rated", "Top Rated Shows"),
    )

    private val tmdbHeaders = mapOf(
        "Authorization" to "Bearer $tmdbReadToken",
        "Accept" to "application/json",
        "User-Agent" to USER_AGENT,
    )

    private val streamHeaders = mapOf(
        "Accept" to "*/*",
        "User-Agent" to USER_AGENT,
        "Origin" to "https://player.videasy.net",
        "Referer" to streamReferer.trimEnd('/').plus("/"),
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val response = tmdbGet<TmdbResults>("${request.data}?language=en-US&page=$page")
        val cards = response?.results.orEmpty().mapNotNull { it.toSearchResponse(request.data.mediaTypeFromPath()) }
        return newHomePageResponse(request.name, cards, hasNext = cards.isNotEmpty())
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return emptyList()

        val encoded = cleanQuery.urlEncode()
        return tmdbGet<TmdbResults>("search/multi?language=en-US&query=$encoded&page=1")
            ?.results
            .orEmpty()
            .mapNotNull { media ->
                if (media.media_type == "person") null else media.toSearchResponse(media.media_type)
            }
    }

    override suspend fun load(url: String): LoadResponse? {
        val data = runCatching { parseJson<TmdbData>(url) }.getOrNull() ?: return null
        val type = if (data.type == "movie") TvType.Movie else TvType.TvSeries
        val detailPath = "${data.type}/${data.id}?language=en-US&append_to_response=external_ids,videos"
        val detail = tmdbGet<TmdbDetail>(detailPath) ?: return null
        val title = detail.title ?: detail.name ?: data.title ?: return null
        val poster = detail.poster_path.tmdbImage()
        val background = detail.backdrop_path.tmdbImage("original")
        val year = (detail.release_date ?: detail.first_air_date).year()
        val imdbId = detail.external_ids?.imdb_id

        return if (type == TvType.Movie) {
            newMovieLoadResponse(
                title,
                url,
                TvType.Movie,
                TmdbStreamData(
                    type = "movie",
                    tmdbId = data.id,
                    imdbId = imdbId,
                    title = title,
                    year = year,
                ).toJson()
            ) {
                posterUrl = poster
                backgroundPosterUrl = background
                this.year = year
                plot = detail.overview
                duration = detail.runtime
            }
        } else {
            val episodes = detail.seasons
                .orEmpty()
                .filter { (it.season_number ?: 0) > 0 }
                .flatMap { season -> loadSeasonEpisodes(data.id, title, year, imdbId, season.season_number) }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                posterUrl = poster
                backgroundPosterUrl = background
                this.year = year
                plot = detail.overview
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val streamData = runCatching { parseJson<TmdbStreamData>(data) }.getOrNull() ?: return false
        var found = false

        if (invokeVideasy(streamData, subtitleCallback, callback)) found = true
        if (invokeVidlink(streamData, callback)) found = true
        if (invokeAutoembed(streamData, subtitleCallback, callback)) found = true
        if (!found && invokeVidFastPro(streamData, subtitleCallback, callback)) found = true

        return found
    }

    private suspend fun loadSeasonEpisodes(
        showId: Int?,
        showTitle: String,
        showYear: Int?,
        imdbId: String?,
        season: Int?,
    ): List<Episode> {
        if (showId == null || season == null) return emptyList()

        return tmdbGet<TmdbSeason>("tv/$showId/season/$season?language=en-US")
            ?.episodes
            .orEmpty()
            .map { episode ->
                newEpisode(
                    TmdbStreamData(
                        type = "tv",
                        tmdbId = showId,
                        imdbId = imdbId,
                        title = showTitle,
                        year = showYear ?: episode.air_date.year(),
                        season = season,
                        episode = episode.episode_number,
                    ).toJson()
                ) {
                    name = episode.name
                    this.season = season
                    this.episode = episode.episode_number
                    posterUrl = episode.still_path.tmdbImage()
                    description = episode.overview
                    runTime = episode.runtime
                }
            }
    }

    private fun TmdbMedia.toSearchResponse(fallbackType: String?): SearchResponse? {
        val type = media_type ?: fallbackType ?: return null
        val title = title ?: name ?: original_title ?: original_name ?: return null
        val payload = TmdbData(id = id ?: return null, type = type, title = title).toJson()
        val poster = poster_path.tmdbImage()

        return if (type == "tv") {
            newTvSeriesSearchResponse(title, payload, TvType.TvSeries) {
                posterUrl = poster
            }
        } else {
            newMovieSearchResponse(title, payload, TvType.Movie) {
                posterUrl = poster
            }
        }
    }

    private suspend inline fun <reified T : Any> tmdbGet(path: String): T? {
        return runCatching {
            app.get("$tmdbApi/${path.trimStart('/')}", headers = tmdbHeaders, timeout = 20_000L).parsedSafe<T>()
        }.getOrNull()
    }

    private suspend fun invokeVideasy(
        data: TmdbStreamData,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        if (data.tmdbId == null || data.title.isNullOrBlank()) return false

        val encodedTitle = data.title.urlEncode().urlEncode()
        var found = false
        val servers = if (data.season == null) videasyMovieServers else videasyTvServers

        for (server in servers) {
            val sourceUrl = if (data.season == null) {
                "$videasyApi/$server/sources-with-title?title=$encodedTitle&mediaType=movie&year=${data.year.orEmpty()}&tmdbId=${data.tmdbId}&imdbId=${data.imdbId.orEmpty()}"
            } else {
                "$videasyApi/$server/sources-with-title?title=$encodedTitle&mediaType=tv&year=${data.year.orEmpty()}&tmdbId=${data.tmdbId}&episodeId=${data.episode.orEmpty()}&seasonId=${data.season}&imdbId=${data.imdbId.orEmpty()}"
            }

            val encrypted = runCatching {
                app.get(sourceUrl, headers = streamHeaders, timeout = 20_000L).text
            }.getOrNull().orEmpty()
            if (encrypted.isBlank()) continue

            val decrypted = runCatching {
                app.post(
                    "$decryptApi/dec-videasy",
                    json = mapOf("text" to encrypted, "id" to data.tmdbId.toString()),
                    timeout = 20_000L
                ).parsedSafe<VideasyDecryptResponse>()
            }.getOrNull()?.result ?: continue

            decrypted.sources.forEach { source ->
                val url = source.url ?: return@forEach
                callback(
                    newExtractorLink(
                        source = "$siteTitle Videasy",
                        name = "$siteTitle Videasy ${server.uppercase()} ${source.quality.orEmpty()}".trim(),
                        url = url,
                        type = url.toExtractorType()
                    ) {
                        quality = getQualityFromName(source.quality ?: url).takeIf { it != Qualities.Unknown.value }
                            ?: Qualities.Unknown.value
                        headers = url.videoHeaders()
                    }
                )
                found = true
            }

            decrypted.subtitles.forEach { subtitle ->
                val url = subtitle.url ?: return@forEach
                val language = subtitle.language ?: subtitle.lang ?: "Subtitle"
                subtitleCallback(newSubtitleFile(language, url))
            }
        }

        return found
    }

    private suspend fun invokeVidlink(
        data: TmdbStreamData,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        val tmdbId = data.tmdbId ?: return false
        val encodedId = runCatching {
            app.get("$decryptApi/enc-vidlink?text=$tmdbId", timeout = 20_000L)
                .parsedSafe<VidlinkEncodeResponse>()
                ?.result
        }.getOrNull() ?: return false

        val headers = mapOf(
            "User-Agent" to USER_AGENT,
            "Connection" to "keep-alive",
            "Referer" to "$vidlinkApi/",
            "Origin" to vidlinkApi,
        )

        val streamUrl = if (data.season == null) {
            "$vidlinkApi/api/b/movie/$encodedId"
        } else {
            "$vidlinkApi/api/b/tv/$encodedId/${data.season}/${data.episode.orEmpty()}"
        }

        val playlist = runCatching {
            app.get(streamUrl, headers = headers, timeout = 20_000L)
                .parsedSafe<VidlinkResponse>()
                ?.stream
                ?.playlist
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: return false

        callback(
            newExtractorLink(
                source = "$siteTitle Vidlink",
                name = "$siteTitle Vidlink",
                url = playlist,
                type = playlist.toExtractorType()
            ) {
                quality = Qualities.Unknown.value
                this.headers = headers
            }
        )

        return true
    }

    private suspend fun invokeVidFastPro(
        data: TmdbStreamData,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        val tmdbId = data.tmdbId ?: return false
        val pageUrl = if (data.season == null) {
            "$vidfastApi/movie/$tmdbId"
        } else {
            "$vidfastApi/tv/$tmdbId/${data.season}/${data.episode.orEmpty()}"
        }
        val headers = mutableMapOf(
            "User-Agent" to USER_AGENT,
            "Referer" to "$vidfastApi/",
            "X-Requested-With" to "XMLHttpRequest",
        )

        val encodedText = runCatching {
            val page = app.get(pageUrl, headers = headers, timeout = 20_000L).text
            Regex("""\\"en\\":\\"(.*?)\\"""").find(page)?.groupValues?.get(1)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: return false

        val decoded = runCatching {
            app.get("$decryptApi/enc-vidfast?text=${encodedText.urlEncode()}&version=1", timeout = 20_000L)
                .parsedSafe<VidFastDecodeResponse>()
                ?.result
        }.getOrNull() ?: return false

        val serversUrl = decoded.servers ?: return false
        val streamBaseUrl = decoded.stream ?: return false
        val token = decoded.token ?: return false
        headers["X-CSRF-Token"] = token

        val serversEncrypted = runCatching {
            app.post(serversUrl, headers = headers, timeout = 20_000L).text
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: return false

        val servers = runCatching {
            app.post(
                "$decryptApi/dec-vidfast",
                json = mapOf("text" to serversEncrypted, "version" to "1"),
                timeout = 20_000L
            ).parsedSafe<VidFastServersResponse>()?.result.orEmpty()
        }.getOrDefault(emptyList())

        return invokeVidFastServers(
            servers = servers.prioritizedVidFastServers(isTv = data.season != null),
            streamBaseUrl = streamBaseUrl,
            headers = headers,
            subtitleCallback = subtitleCallback,
            callback = callback,
        )
    }

    private suspend fun invokeVidFastServers(
        servers: List<VidFastServerInfo>,
        streamBaseUrl: String,
        headers: Map<String, String>,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        var found = false
        servers.forEach { server ->
            val serverHash = server.data ?: return@forEach
            val encrypted = runCatching {
                app.post("$streamBaseUrl/$serverHash", headers = headers, timeout = 20_000L).text
            }.getOrNull()?.takeIf { it.isNotBlank() } ?: return@forEach

            val stream = runCatching {
                app.post(
                    "$decryptApi/dec-vidfast",
                    json = mapOf("text" to encrypted, "version" to "1"),
                    timeout = 20_000L
                ).parsedSafe<VidFastStreamResponse>()?.result
            }.getOrNull() ?: return@forEach

            stream.tracks.orEmpty().forEach { track ->
                val file = track.file ?: return@forEach
                val label = track.label ?: "Subtitle"
                subtitleCallback(newSubtitleFile(label, file))
            }

            val fileUrl = stream.url?.takeIf { it.isNotBlank() } ?: return@forEach
            callback(
                newExtractorLink(
                    source = "$siteTitle VidFast",
                    name = "$siteTitle VidFast ${server.name.orEmpty()} ${server.description.orEmpty()}".trim(),
                    url = fileUrl,
                    type = fileUrl.toExtractorType()
                ) {
                    this.headers = headers
                    quality = if (stream.`4kAvailable` == true || server.description?.contains("4K", true) == true) {
                        Qualities.P2160.value
                    } else {
                        Qualities.P1080.value
                    }
                }
            )
            found = true
        }

        return found
    }

    private suspend fun invokeAutoembed(
        data: TmdbStreamData,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        val imdbId = data.imdbId?.takeIf { it.isNotBlank() } ?: return false
        val pageUrl = if (data.season == null) {
            "$autoembedApi/embed/movie/$imdbId"
        } else {
            "$autoembedApi/embed/tv/$imdbId/${data.season}/${data.episode.orEmpty()}"
        }
        val headers = mapOf(
            "User-Agent" to USER_AGENT,
            "Referer" to "$autoembedApi/",
        )

        val page = runCatching {
            app.get(pageUrl, headers = headers, timeout = 12_000L).text
        }.getOrNull().orEmpty()
        if (page.isBlank()) return false

        val embedUrl = Regex("""var\s+embedUrlValue\s*=\s*"([^"]+)";""")
            .find(page)
            ?.groupValues
            ?.get(1)
            ?: Regex("""https?:\\?/\\?/cloudnestra\.com/rcp/[^"'<\s]+""")
                .find(page)
                ?.value
                ?.replace("\\/", "/")

        return embedUrl
            ?.takeIf { it.isNotBlank() }
            ?.let { loadExtractor(it, pageUrl, subtitleCallback, callback) }
            ?: false
    }

    private fun List<VidFastServerInfo>.prioritizedVidFastServers(isTv: Boolean): List<VidFastServerInfo> {
        val priority = if (isTv) vidfastTvServerOrder else vidfastMovieServerOrder
        val verifiedServers = filter { it.name in priority }
        val candidates = verifiedServers.takeIf { it.isNotEmpty() } ?: this
        return candidates.sortedWith(
            compareBy<VidFastServerInfo> { server ->
                val index = priority.indexOf(server.name)
                if (index == -1) Int.MAX_VALUE else index
            }.thenBy { it.name.orEmpty() }
        )
    }

    private fun String.mediaTypeFromPath(): String {
        return if (contains("/tv") || startsWith("tv")) "tv" else "movie"
    }

    private fun String?.tmdbImage(size: String = "w500"): String? {
        val path = this ?: return null
        return if (path.startsWith("http")) path else "https://image.tmdb.org/t/p/$size$path"
    }

    private fun String?.year(): Int? = this?.substringBefore("-")?.toIntOrNull()

    private fun String.urlEncode(): String {
        return URLEncoder.encode(this, StandardCharsets.UTF_8.name()).replace("+", "%20")
    }

    private fun Int?.orEmpty(): String = this?.toString().orEmpty()
    private fun String?.orEmpty(): String = this ?: ""

    private fun String.toExtractorType(): ExtractorLinkType {
        return when {
            contains(".m3u8", ignoreCase = true) -> ExtractorLinkType.M3U8
            contains(".mpd", ignoreCase = true) -> ExtractorLinkType.DASH
            contains(".mp4", ignoreCase = true) || contains(".mkv", ignoreCase = true) -> ExtractorLinkType.VIDEO
            else -> ExtractorLinkType.VIDEO
        }
    }

    private fun String.videoHeaders(): Map<String, String> {
        val headers = streamHeaders.toMutableMap()
        when {
            contains(".m3u8", ignoreCase = true) -> {
                headers["Accept"] = "application/vnd.apple.mpegurl,application/x-mpegURL,*/*"
                headers["Referer"] = "$videasyApi/"
            }
            contains(".mp4", ignoreCase = true) -> {
                headers["Accept"] = "video/mp4,*/*"
                headers["Range"] = "bytes=0-"
            }
            contains(".mkv", ignoreCase = true) -> {
                headers["Accept"] = "video/x-matroska,*/*"
                headers["Range"] = "bytes=0-"
            }
        }
        return headers
    }

    data class TmdbData(
        val id: Int? = null,
        val type: String? = null,
        val title: String? = null,
    )

    data class TmdbStreamData(
        val type: String? = null,
        val tmdbId: Int? = null,
        val imdbId: String? = null,
        val title: String? = null,
        val year: Int? = null,
        val season: Int? = null,
        val episode: Int? = null,
    )

    data class TmdbResults(
        val results: List<TmdbMedia> = emptyList(),
    )

    data class TmdbMedia(
        val id: Int? = null,
        val media_type: String? = null,
        val title: String? = null,
        val name: String? = null,
        val original_title: String? = null,
        val original_name: String? = null,
        val poster_path: String? = null,
    )

    data class TmdbDetail(
        val title: String? = null,
        val name: String? = null,
        val poster_path: String? = null,
        val backdrop_path: String? = null,
        val release_date: String? = null,
        val first_air_date: String? = null,
        val overview: String? = null,
        val runtime: Int? = null,
        val seasons: List<TmdbSeasonSummary> = emptyList(),
        val external_ids: TmdbExternalIds? = null,
    )

    data class TmdbSeasonSummary(
        val season_number: Int? = null,
    )

    data class TmdbSeason(
        val episodes: List<TmdbEpisode> = emptyList(),
    )

    data class TmdbEpisode(
        val name: String? = null,
        val overview: String? = null,
        val episode_number: Int? = null,
        val still_path: String? = null,
        val air_date: String? = null,
        val runtime: Int? = null,
    )

    data class TmdbExternalIds(
        val imdb_id: String? = null,
    )

    data class VideasyDecryptResponse(
        val result: VideasyResult? = null,
    )

    data class VideasyResult(
        val sources: List<VideasySource> = emptyList(),
        val subtitles: List<VideasySubtitle> = emptyList(),
    )

    data class VideasySource(
        val quality: String? = null,
        val url: String? = null,
    )

    data class VideasySubtitle(
        val lang: String? = null,
        val language: String? = null,
        val url: String? = null,
    )

    data class VidlinkEncodeResponse(
        val result: String? = null,
    )

    data class VidlinkResponse(
        val stream: VidlinkStream? = null,
    )

    data class VidlinkStream(
        val playlist: String? = null,
    )

    data class VidFastDecodeResponse(
        val result: VidFastDecodeResult? = null,
    )

    data class VidFastDecodeResult(
        val servers: String? = null,
        val stream: String? = null,
        val token: String? = null,
    )

    data class VidFastServersResponse(
        val result: List<VidFastServerInfo> = emptyList(),
    )

    data class VidFastServerInfo(
        val name: String? = null,
        val description: String? = null,
        val data: String? = null,
    )

    data class VidFastStreamResponse(
        val result: VidFastStream? = null,
    )

    data class VidFastStream(
        val url: String? = null,
        val tracks: List<VidFastTrack> = emptyList(),
        val `4kAvailable`: Boolean? = null,
    )

    data class VidFastTrack(
        val file: String? = null,
        val label: String? = null,
    )

    companion object {
        private const val tmdbApi = "https://api.themoviedb.org/3"
        private const val videasyApi = "https://api.videasy.net"
        private const val vidlinkApi = "https://vidlink.pro"
        private const val vidfastApi = "https://vidfast.pro"
        private const val autoembedApi = "https://player.autoembed.app"
        private const val decryptApi = "https://enc-dec.app/api"
        private const val tmdbReadToken =
            "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI1YjEwYWNhZDFhNjY3ZTQwMDEyMGVjMTc1ZDBjZTFmZCIsIm5iZiI6MTcyNDk1Mjg3MC45NDA4NDcsInN1YiI6IjY2ZDBhOTgyODQ1OWYzM2FmMjBmYjdkNSIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.ScGHs1VZTLGpUKWPG7EA-2T29OPcqW_qpJjKL5Yhrjc"
        private val videasyMovieServers = listOf(
            "cdn",
        )
        private val videasyTvServers = listOf(
            "cdn",
        )
        private val vidfastMovieServerOrder = listOf(
            "Alpha",
            "vEdge",
            "vRapid",
            "Mega",
            "vFast",
            "Max",
            "Beta",
            "Cobra",
            "Vodka",
        )
        private val vidfastTvServerOrder = listOf(
            "Alpha",
            "vEdge",
            "Mega",
            "vFast",
            "Max",
            "Beta",
            "Cobra",
            "Vodka",
        )
    }
}
