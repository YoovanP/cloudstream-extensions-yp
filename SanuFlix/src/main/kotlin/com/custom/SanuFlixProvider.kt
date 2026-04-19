package com.custom

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class SanuFlixProvider : MainAPI() {
    override var mainUrl = "https://sanuflix-web-v2.pages.dev"
    override var name = "SanuFlix"
    override var lang = "en"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    companion object {
        private const val TMDB_API = "https://api.themoviedb.org/3"
        private const val TMDB_KEY = "17f7b9d5b48c5446e781e13bfa980f61"
        private const val IMG_BASE = "https://image.tmdb.org/t/p/w500"
    }

    data class TmdbResult(val id: Int?, val title: String?, val name: String?, val media_type: String?, val poster_path: String?)
    data class TmdbPage(val results: List<TmdbResult>?)
    data class TmdbDetail(val id: Int?, val title: String?, val name: String?, val overview: String?, val poster_path: String?, val backdrop_path: String?, val release_date: String?, val first_air_date: String?, val genres: List<TmdbGenre>?, val seasons: List<TmdbSeason>?)
    data class TmdbGenre(val name: String?)
    data class TmdbSeason(val season_number: Int?, val episode_count: Int?, val poster_path: String?)

    private fun TmdbResult.toSearchResponse(isMovie: Boolean): SearchResponse {
        val url = if (isMovie) movieUrl(id ?: 0) else tvUrl(id ?: 0)
        return newMovieSearchResponse(title ?: name ?: "", url, if (isMovie) TvType.Movie else TvType.TvSeries) { posterUrl = poster_path?.let { "$IMG_BASE$it" } }
    }

    private fun movieUrl(id: Int) = "$mainUrl/movie/$id"
    private fun tvUrl(id: Int)    = "$mainUrl/tv/$id"
    private fun String.encodeUrl() = java.net.URLEncoder.encode(this, "UTF-8")

    override val mainPage = mainPageOf("trending/movie/week" to "Trending Movies", "trending/tv/week" to "Trending TV Shows")

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val json = app.get("$TMDB_API/${request.data}?api_key=$TMDB_KEY&page=$page").parsedSafe<TmdbPage>()
        return newHomePageResponse(request.name, json?.results?.map { it.toSearchResponse(request.data.contains("movie")) } ?: emptyList())
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val json = app.get("$TMDB_API/search/multi?api_key=$TMDB_KEY&query=${query.encodeUrl()}").parsedSafe<TmdbPage>()
        return json?.results?.mapNotNull { if (it.media_type == "person") null else it.toSearchResponse(it.media_type == "movie") } ?: emptyList()
    }

    override suspend fun load(url: String): LoadResponse? {
        val tmdbId = url.trimEnd('/').substringAfterLast("/").toIntOrNull() ?: return null
        val type = if (url.contains("/movie/")) "movie" else "tv"
        val it = app.get("$TMDB_API/$type/$tmdbId?api_key=$TMDB_KEY&append_to_response=seasons").parsedSafe<TmdbDetail>() ?: return null
        
        if (type == "movie") {
            return newMovieLoadResponse(it.title ?: "", url, TvType.Movie, tmdbId.toString()) {
                posterUrl = it.poster_path?.let { "$IMG_BASE$it" }; plot = it.overview
            }
        } else {
            val eps = it.seasons?.filter { (it.season_number ?: 0) > 0 }?.flatMap { s -> 
                List(s.episode_count ?: 0) { i -> newEpisode("$tmdbId|${s.season_number}|${i+1}") { season = s.season_number; episode = i+1 } }
            } ?: emptyList()
            return newTvSeriesLoadResponse(it.name ?: "", url, TvType.TvSeries, eps) {
                posterUrl = it.poster_path?.let { "$IMG_BASE$it" }; plot = it.overview
            }
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val parts = data.split("|"); val tmdbId = parts[0]; val s = parts.getOrNull(1); val e = parts.getOrNull(2)

        val movieEmbeds = listOf({ id -> "https://vidlink.pro/movie/$id" }, { id -> "https://vidsrc.to/embed/movie/$id" }, { id -> "https://vidsrc.me/embed/movie/$id" }, { id -> "https://player.autoembed.cc/embed/movie/$id" }, { id -> "https://multiembed.mov/?video_id=$id&tmdb=1" })
        val tvEmbeds = listOf({ id, s, e -> "https://vidlink.pro/tv/$id/$s/$e" }, { id, s, e -> "https://vidsrc.to/embed/tv/$id/$s/$e" }, { id, s, e -> "https://vidsrc.me/embed/tv/$id/$s/$e" }, { id, s, e -> "https://player.autoembed.cc/embed/tv/$id/$s/$e" }, { id, s, e -> "https://multiembed.mov/?video_id=$id&tmdb=1&s=$s&e=$e" })

        if (s == null) movieEmbeds.forEach { embed -> try { CommonLoader.loadLinks(embed(tmdbId.toInt()), "$mainUrl/", subtitleCallback, callback) } catch(_: Exception) {} }
        else tvEmbeds.forEach { embed -> try { CommonLoader.loadLinks(embed(tmdbId.toInt(), s.toInt(), e!!.toInt()), "$mainUrl/", subtitleCallback, callback) } catch(_: Exception) {} }
        return true
    }
}
