package com.archiekuo.radio.data

import android.content.Context
import com.archiekuo.radio.model.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(val station: Station, val time: Long)

fun Station.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("name", name); put("url", url); put("country", country)
    put("cc", countryCode); put("codec", codec); put("br", bitrate); put("tags", tags)
    put("hls", hls); put("votes", votes); put("cat", categoryKey)
    put("state", state); put("lang", language); put("home", homepage); put("cur", curated)
}

fun stationFromJson(o: JSONObject) = Station(
    id = o.optString("id"), name = o.optString("name"), url = o.optString("url"),
    country = o.optString("country"), countryCode = o.optString("cc"), codec = o.optString("codec"),
    bitrate = o.optInt("br"), tags = o.optString("tags"), hls = o.optBoolean("hls"),
    votes = o.optInt("votes"), categoryKey = o.optString("cat", "hot"),
    state = o.optString("state"), language = o.optString("lang"), homepage = o.optString("home"), curated = o.optBoolean("cur"),
)

/** 收藏、播放紀錄、設定都存在手機裡 */
class LocalStore(ctx: Context) {
    private val p = ctx.getSharedPreferences("radio", Context.MODE_PRIVATE)

    private val _favorites = MutableStateFlow(readStations("fav"))
    val favorites: StateFlow<List<Station>> = _favorites.asStateFlow()

    private val _history = MutableStateFlow(readHistory())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    private val _recent = MutableStateFlow(p.getString("recent", "")!!.split('\n').filter { it.isNotBlank() })
    val recentSearches: StateFlow<List<String>> = _recent.asStateFlow()

    private val _wifiOnly = MutableStateFlow(p.getBoolean("wifiOnly", false))
    val wifiOnly: StateFlow<Boolean> = _wifiOnly.asStateFlow()

    private val _autoPlay = MutableStateFlow(p.getBoolean("autoPlay", true))
    val autoPlay: StateFlow<Boolean> = _autoPlay.asStateFlow()

    var lastStation: Station?
        get() = p.getString("last", null)?.let { runCatching { stationFromJson(JSONObject(it)) }.getOrNull() }
        set(v) { p.edit().putString("last", v?.toJson()?.toString()).apply() }

    private fun readStations(key: String): List<Station> = runCatching {
        val a = JSONArray(p.getString(key, "[]"))
        (0 until a.length()).map { stationFromJson(a.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    private fun writeStations(key: String, list: List<Station>) {
        val a = JSONArray(); list.forEach { a.put(it.toJson()) }
        p.edit().putString(key, a.toString()).apply()
    }

    private fun readHistory(): List<HistoryEntry> = runCatching {
        val a = JSONArray(p.getString("history", "[]"))
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            HistoryEntry(stationFromJson(o.getJSONObject("s")), o.getLong("t"))
        }
    }.getOrDefault(emptyList())

    private fun writeHistory(list: List<HistoryEntry>) {
        val a = JSONArray()
        list.forEach { a.put(JSONObject().put("s", it.station.toJson()).put("t", it.time)) }
        p.edit().putString("history", a.toString()).apply()
    }

    fun isFavorite(id: String) = _favorites.value.any { it.id == id }

    fun toggleFavorite(s: Station) {
        val cur = _favorites.value
        val next = if (cur.any { it.id == s.id }) cur.filterNot { it.id == s.id } else listOf(s) + cur
        _favorites.value = next; writeStations("fav", next)
    }

    fun removeFavorites(ids: Set<String>) {
        val next = _favorites.value.filterNot { it.id in ids }
        _favorites.value = next; writeStations("fav", next)
    }

    fun addHistory(s: Station) {
        val now = System.currentTimeMillis()
        val cur = _history.value
        val first = cur.firstOrNull()
        val next = if (first != null && first.station.id == s.id && now - first.time < 30 * 60_000L) {
            cur
        } else {
            (listOf(HistoryEntry(s, now)) + cur).take(100)
        }
        _history.value = next; writeHistory(next)
    }

    fun clearHistory() { _history.value = emptyList(); writeHistory(emptyList()) }

    fun addRecent(q: String) {
        val next = (listOf(q) + _recent.value.filterNot { it == q }).take(8)
        _recent.value = next; p.edit().putString("recent", next.joinToString("\n")).apply()
    }

    fun setWifiOnly(b: Boolean) { _wifiOnly.value = b; p.edit().putBoolean("wifiOnly", b).apply() }
    fun setAutoPlay(b: Boolean) { _autoPlay.value = b; p.edit().putBoolean("autoPlay", b).apply() }
}
