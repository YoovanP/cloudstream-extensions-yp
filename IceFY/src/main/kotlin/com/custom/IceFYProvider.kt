package com.custom

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class IceFYProvider : MainAPI() {
    override var mainUrl = "https://icefy.top"
    override var name = "IceFY"
    override var lang = "en"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    companion object {
        private const val TMDB_API = "https://api.themoviedb.org/3"
        private const val TMDB_KEY = "17f7b9d5b48c5446e781e13bfa980f61"
        private const val IMG_BASE = "https://image.tmdb.org/t/p/w500"
        private const val IMG_ORIG = "https://image.tmdb.org/t/p/original"

        private val EMBED_MOVIE: List<(Int) -> String> = listOf(
            { id -> "https://vidlink.pro/movie/$id" },
            { id -> "https://vidsrc.to/embed/movie/$id" },
            { id -> "https://vidsrc.me/embed/movie/$id" },
            { id -> "https://player.autoembed.cc/embed/movie/$id" },
            { id -> "https://multiembed.mov/?video_id=$id&tmdb=1" }
        )
        private val EMBED_TV: List<(Int, Int, Int) -> String> = listOf(
            { id, s, e -> "https://vidlink.pro/tv/$id/$s/$e" },
            { id, s, e -> "https://vidsrc.to/embed/tv/$id/$s/$e" },
            { id, s, e -> "https://vidsrc.me/embed/tv/$id/$s/$e" },
            { id, s, e -> "https://player.autoembed.cc/embed/tv/$id/$s/$e" },
            { id, s, e -> "https://multiembed.mov/?video_id=$id&tmdb=1&s=$s&e=$e" }
        )
    }

    // ─── Data classes ────────────────────────────────────────────────

    data class TmdbResult(
        val id: Int?,
        val title: String?,
        val name: String?,
        val media_type: String?,
        val poster_path: String?,
        val backdrop_path: String?,
    )
    data class TmdbPage(val results: List<TmdbResult>?)
    data class TmdbGenre(val id: Int?, val name: String?)
    data class TmdbSeason(
        val season_number: Int?,
        val episode_count: Int?,
        val poster_path: String?,
    )
    data class TmdbDetail(
        val id: Int?,
        val title: String?,
        val name: String?,
        val overview: String?,
        val poster_path: String?,
        val backdrop_path: String?,
        val release_date: String?,
        val first_air_date: String?,
        val genres: List<TmdbGenre>?,
        val seasons: List<TmdbSeason>?,
    )

    // ─── Helpers ─────────────────────────────────────────────────────

    private fun TmdbResult.toSearchResponse(isMovie: Boolean): SearchResponse {
        val title  = this.title ?: this.name ?: ""
        val poster = this.poster_path?.let { "${IMG_BASE}${it}" }
        val url    = if (isMovie) movieUrl(this.id ?: 0) else tvUrl(this.id ?: 0)
        return newMovieSearchResponse(title, url, if (isMovie) TvType.Movie else TvType.TvSeries) {
            posterUrl = poster
        }
    }

    private fun movieUrl(id: Int) = "$mainUrl/movie/$id"
    private fun tvUrl(id: Int)    = "$mainUrl/tv/$id"

    private fun String.encodeUrl(): String {
        return java.net.URLEncoder.encode(this, "UTF-8")
    }

    // ─── Main page ───────────────────────────────────────────────────

    override val mainPage = mainPageOf(
        "trending/movie/week" to "Trending Movies",
        "trending/tv/week"    to "Trending TV Shows",
        "movie/popular"       to "Popular Movies",
        "tv/popular"          to "Popular Shows",
        "movie/top_rated"     to "Top Rated Movies",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val endpoint = request.data
        val isMovie  = endpoint.contains("movie")
        val json = app.get("${TMDB_API}/${endpoint}?api_key=${TMDB_KEY}&page=${page}&language=en-US")
            .parsedSafe<TmdbPage>() ?: return newHomePageResponse(request.name, emptyList())
        val items = json.results?.mapNotNull { result ->
            result.id ?: return@mapNotNull null
            result.toSearchResponse(isMovie)
        } ?: emptyList()
        return newHomePageResponse(request.name, items)
    }

    // ─── Search ──────────────────────────────────────────────────────

    override suspend fun search(query: String): List<SearchResponse> {
        val json = app.get(
            "${TMDB_API}/search/multi?api_key=${TMDB_KEY}&query=${query.encodeUrl()}&language=en-US&page=1"
        ).parsedSafe<TmdbPage>() ?: return emptyList()
        return json.results?.mapNotNull { result ->
            val mediaType = result.media_type ?: "movie"
            if (mediaType == "person") return@mapNotNull null
            result.id ?: return@mapNotNull null
            result.toSearchResponse(mediaType == "movie")
        } ?: emptyList()
    }

    // ─── Load ────────────────────────────────────────────────────────

    override suspend fun load(url: String): LoadResponse? {
        val isMovie = url.contains("/movie/")
        val tmdbId  = url.trimEnd('/').substringAfterLast("/").toIntOrNull() ?: return null
        val type    = if (isMovie) "movie" else "tv"

        val detail = app.get(
            "${TMDB_API}/${type}/${tmdbId}?api_key=${TMDB_KEY}&language=en-US&append_to_response=seasons"
        ).parsedSafe<TmdbDetail>() ?: return null

        val title    = detail.title ?: detail.name ?: return null
        val poster   = detail.poster_path?.let { "${IMG_BASE}${it}" }
        val backdrop = detail.backdrop_path?.let { "${IMG_ORIG}${it}" }
        val year     = (detail.release_date ?: detail.first_air_date)?.take(4)?.toIntOrNull()
        val genres   = detail.genres?.mapNotNull { it.name }
        val plot     = detail.overview

        if (isMovie) {
            return newMovieLoadResponse(title, url, TvType.Movie, tmdbId.toString()) {
                posterUrl = poster
                backgroundPosterUrl = backdrop
                this.year = year
                tags = genres
                this.plot = plot
            }
        } else {
            val episodes = ArrayList<Episode>()
            detail.seasons?.forEach { season ->
                val sNum = season.season_number ?: return@forEach
                if (sNum == 0) return@forEach
                val ePoster = season.poster_path?.let { "${IMG_BASE}${it}" }
                repeat(season.episode_count ?: 1) { idx ->
                    val eNum = idx + 1
                    episodes += newEpisode("${tmdbId}|${sNum}|${eNum}") {
                        this.season   = sNum
                        this.episode  = eNum
                        this.posterUrl = ePoster
                    }
                }
            }
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                posterUrl = poster
                backgroundPosterUrl = backdrop
                this.year = year
                tags = genres
                this.plot = plot
            }
        }
    }

    // ─── Load links ──────────────────────────────────────────────────

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val parts  = data.split("|")
        val tmdbId = parts[0].toIntOrNull() ?: return false
        val s      = parts.getOrNull(1)?.toIntOrNull()
        val e      = parts.getOrNull(2)?.toIntOrNull()

        if (s == null) {
            EMBED_MOVIE.amap { fn ->
                try {
                    val url = fn(tmdbId)
                    loadExtractor(url, "$mainUrl/", subtitleCallback, callback)
                } catch (_: Exception) { }
            }
        } else {
            EMBED_TV.amap { fn ->
                try {
                    val url = fn(tmdbId, s, e!!)
                    loadExtractor(url, "$mainUrl/", subtitleCallback, callback)
                } catch (_: Exception) { }
            }
        }
        return true
    }
}
