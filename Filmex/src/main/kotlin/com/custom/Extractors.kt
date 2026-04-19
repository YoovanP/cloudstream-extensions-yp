package com.custom

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.extractors.*
import com.lagradost.api.Log
import org.json.JSONObject
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import okhttp3.FormBody
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import com.google.gson.Gson
import android.annotation.SuppressLint

// --- Advanced Multi-Server Extraction Suite ---

object CommonLoader {
    suspend fun loadLinks(
        url: String, 
        referer: String?, 
        subtitleCallback: (SubtitleFile) -> Unit, 
        callback: (ExtractorLink) -> Unit
    ) {
        try {
            val domain = referer ?: getBaseUrl(url)
            when {
                url.contains("vidsrc.cc") || url.contains("vidsrccc") -> 
                    Vidsrccc().getUrl(url, domain, subtitleCallback, callback)
                url.contains("megacloud") || url.contains("rabbitstream") -> 
                    Megacloud().getUrl(url, domain, subtitleCallback, callback)
                url.contains("hubcloud") || url.contains("vcloud") -> 
                    HubCloud().getUrl(url, domain, subtitleCallback, callback)
                else -> loadExtractor(url, domain, subtitleCallback, callback)
            }
        } catch (e: Exception) {
            Log.e("CommonLoader", "Error loading links: ${e.message}")
        }
    }

    private fun getBaseUrl(url: String): String {
        return try { URI(url).let { "${it.scheme}://${it.host}" } } catch (e: Exception) { "" }
    }
}

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

class Videasy : ExtractorApi() {
    override var name = "Videasy"
    override var mainUrl = "https://db.videasy.net"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val id = url.substringAfterLast("/")
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

class Vidsrccc : ExtractorApi() {
    override var name = "Vidsrc CC"
    override var mainUrl = "https://vidsrc.cc"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val doc = app.get(url).text
        val regex = Regex("""var\s+(\w+)\s*=\s*(?:"([^"]*)"|(\w+));""")
        val variables = mutableMapOf<String, String>()
        regex.findAll(doc).forEach { variables[it.groupValues[1]] = it.groupValues[2].ifEmpty { it.groupValues[3] } }

        val movieId = variables["movieId"] ?: ""
        val userId = variables["userId"] ?: ""
        val vrf = generateVrf(movieId, userId)
        val api = "$mainUrl/api/$movieId/servers?id=$movieId&v=${variables["v"]}&vrf=$vrf"
        
        val servers = app.get(api).parsedSafe<VidsrcResponse>()?.data ?: return
        servers.forEach { server ->
            val source = app.get("$mainUrl/api/source/${server.hash}").parsedSafe<VidsrcSource>()?.data?.source ?: return@forEach
            val res = app.get(source, referer = mainUrl).text
            val m3u8 = Regex("""var\s+source\s*=\s*"([^"]+)"""\).find(res)?.groupValues?.get(1)?.replace("\\/", "/") ?: return@forEach
            M3u8Helper.generateM3u8("Vidsrc [${server.name}]", m3u8, mainUrl).forEach(callback)
        }
    }

    private fun generateVrf(movieId: String, userId: String): String {
        val key = SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(userId.toByteArray()), "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(ByteArray(16)))
        return base64Encode(cipher.doFinal(movieId.toByteArray())).replace('+', '-').replace('/', '_').replace("=", "")
    }

    data class VidsrcResponse(val data: List<VidsrcServer>)
    data class VidsrcServer(val name: String, val hash: String)
    data class VidsrcSource(val data: VidsrcData)
    data class VidsrcData(val source: String)
}

class Megacloud : Rabbitstream() {
    override var name = "Megacloud"
    override var mainUrl = "https://megacloud.club"

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        try {
            val id = url.substringAfterLast("/").substringBefore("?")
            val res = app.get(url).text
            val nonce = Regex("""\b[a-zA-Z0-9]{48}\b""").find(res)?.value ?: ""
            val key = JSONObject(app.get("https://raw.githubusercontent.com/yogesh-hacker/MegacloudKeys/refs/heads/main/keys.json").text).getString("mega")
            
            val api = "$mainUrl/embed-2/v3/e-1/getSources?id=$id&_k=$nonce"
            val sources = JSONObject(app.get(api).text).getJSONArray("sources")
            val encryptedUrl = sources.getJSONObject(0).getString("file")
            
            val decryptUrl = "https://script.google.com/macros/s/AKfycbxHbYHbrGMXYD2-bC-C43D3njIbU-wGiYQuJL61H4vyy6YVXkybMNNEPJNPPuZrD1gRVA/exec?encrypted_data=${URLEncoder.encode(encryptedUrl, "UTF-8")}&nonce=$nonce&secret=$key"
            val m3u8 = JSONObject(app.get(decryptUrl).text).getString("file")
            
            M3u8Helper.generateM3u8(name, m3u8, mainUrl).forEach(callback)
        } catch (e: Exception) {
            Log.e("Megacloud", "Extraction failed: ${e.message}")
        }
    }
}

class HubCloud : ExtractorApi() {
    override var name = "Hub-Cloud"
    override var mainUrl = "https://hubcloud.club"

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val doc = app.get(url).document
        doc.select("div.card-body h2 a.btn").forEach { el ->
            val link = el.attr("href")
            val text = el.text()
            if (text.contains("FSL") || text.contains("Direct")) {
                M3u8Helper.generateM3u8("$name [Direct]", link, mainUrl).forEach(callback)
            }
        }
    }
}
