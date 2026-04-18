package com.custom

import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.SubtitleFile
import org.json.JSONObject

class Vidlink : ExtractorApi() {
    override var name = "Vidlink"
    override var mainUrl = "https://vidlink.pro"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val tmdbId = url.substringAfterLast("/").substringBefore("?")
        val isMovie = url.contains("/movie/")
        
        val decryptUrl = "https://multidecrypt.megix.workers.dev/enc-vidlink?text=$tmdbId"
        val encRes = app.get(decryptUrl).text
        val encData = JSONObject(encRes).getString("result")

        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Referer" to "$mainUrl/",
            "Origin" to mainUrl
        )

        val apiPath = if (isMovie) "movie" else "tv"
        val epUrl = if (isMovie)
            "$mainUrl/api/b/$apiPath/$encData"
        else {
            val season = url.substringAfter("/tv/$tmdbId/").substringBefore("/")
            val episode = url.substringAfterLast("/")
            "$mainUrl/api/b/$apiPath/$encData/$season/$episode"
        }

        val response = app.get(epUrl, headers = headers).text
        val streamUrl = JSONObject(response).getJSONObject("stream").getString("playlist")

        return M3u8Helper.generateM3u8(
            name,
            streamUrl,
            "$mainUrl/",
            headers = headers
        )
    }
}
