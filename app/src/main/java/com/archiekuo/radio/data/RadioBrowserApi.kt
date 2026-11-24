package com.archiekuo.radio.data

import com.archiekuo.radio.model.Categories
import com.archiekuo.radio.model.Station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Radio Browser（radio-browser.info）免費公開的全球電台資料庫 */
class RadioBrowserApi {
    private val servers = listOf(
        "https://all.api.radio-browser.info",
        "https://de1.api.radio-browser.info",
        "https://de2.api.radio-browser.info",
        "https://fi1.api.radio-browser.info",
    )
    @Volatile private var good: String? = null

    private fun get(path: String): String {
        val order = listOfNotNull(good) + servers.filter { it != good }
        var last: Exception? = null
        for (s in order) {
            try {
                val c = URL(s + path).openConnection() as HttpURLConnection
                c.connectTimeout = 8000
                c.readTimeout = 15000
                c.setRequestProperty("User-Agent", "ArchieKUO-NetRadio/1.0")
                try {
                    val text = c.inputStream.bufferedReader().use { it.readText() }
                    good = s
                    return text
                } finally {
                    c.disconnect()
                }
            } catch (e: Exception) {
                last = e
            }
        }
        throw last ?: Exception("無法連線")
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun parse(json: String, cat: String?): List<Station> {
        val arr = JSONArray(json)
        val out = ArrayList<Station>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val url = o.optString("url_resolved").ifBlank { o.optString("url") }
            if (url.isBlank()) continue
            if (o.optInt("lastcheckok", 1) == 0) continue
            val tags = o.optString("tags")
            out += Station(
                id = o.optString("stationuuid").ifBlank { url },
                name = o.optString("name").trim().replace(Regex("\\s+"), " ").ifBlank { "未命名電台" },
                url = url,
                country = o.optString("country"),
                countryCode = o.optString("countrycode"),
                codec = o.optString("codec"),
                bitrate = o.optInt("bitrate"),
                tags = tags,
                hls = o.optInt("hls") == 1,
                votes = o.optInt("votes"),
                categoryKey = cat ?: Categories.guess(tags),
                state = o.optString("state"),
                language = o.optString("language"),
                homepage = o.optString("homepage"),
            )
        }
        return out
    }

    /** 依條件搜尋：tag、language、countrycode、name…；top=1 代表全球人氣排行 */
    suspend fun bySpec(spec: Map<String, String>, cat: String) = withContext(Dispatchers.IO) {
        val limit = spec["limit"] ?: "60"
        if (spec["top"] != null) {
            parse(get("/json/stations/topvote/$limit?hidebroken=true"), cat)
        } else {
            val q = spec.filterKeys { it != "limit" }.entries.joinToString("&") { "${it.key}=${enc(it.value)}" }
            parse(get("/json/stations/search?$q&hidebroken=true&order=votes&reverse=true&limit=$limit"), cat)
        }
    }

    suspend fun byName(q: String, limit: Int = 60) = withContext(Dispatchers.IO) {
        parse(get("/json/stations/search?name=${enc(q)}&hidebroken=true&order=votes&reverse=true&limit=$limit"), null)
    }

    /** 讀取 GitHub 上最新的分類清單 */
    suspend fun fetchText(url: String): String = withContext(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 10000
        try { c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
    }
}
