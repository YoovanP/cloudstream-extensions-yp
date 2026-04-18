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
        val encData = JSONObject(app.get(decryptUrl).text).getString("result")

        val apiPath = if (isMovie) "movie" else "tv"
        val epUrl = if (isMovie) "$mainUrl/api/b/$apiPath/$encData" 
                    else {
                        val s = url.substringAfter("/tv/$tmdbId/").substringBefore("/")
                        val e = url.substringAfterLast("/")
                        "$mainUrl/api/b/$apiPath/$encData/$s/$e"
                    }

        val response = app.get(epUrl, headers = mapOf("Referer" to "$mainUrl/")).text
        val streamUrl = JSONObject(response).getJSONObject("stream").getString("playlist")
        return M3u8Helper.generateM3u8(name, streamUrl, "$mainUrl/")
    }
}

class AetherHls : ExtractorApi() {
    override var name = "Aether"
    override var mainUrl = "https://tik.aether.mom"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val id = url.substringAfterLast("/")
        val isMovie = url.contains("/movie/")
        val type = if (isMovie) "movie" else "tv"
        
        // Aether serves high quality HLS directly
        val api = "$mainUrl/api/stream?id=$id&type=$type"
        val res = app.get(api).text
        val master = JSONObject(res).getString("url")
        
        return M3u8Helper.generateM3u8(name, master, "$mainUrl/")
    }
}

class Videasy : ExtractorApi() {
    override var name = "Videasy"
    override var mainUrl = "https://db.videasy.net"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val id = url.substringAfterLast("/")
        val isMovie = url.contains("/movie/")
        
        // Videasy / Cineby protocol handles sources via internal token
        val api = "$mainUrl/v3/source/$id"
        val res = app.get(api).text
        val sources = JSONObject(res).getJSONArray("sources")
        val links = mutableListOf<ExtractorLink>()
        
        for (i in 0 until sources.length()) {
            val s = sources.getJSONObject(i)
            val file = s.getString("file")
            links.addAll(M3u8Helper.generateM3u8(name, file, referer ?: ""))
        }
        return links
    }
}
