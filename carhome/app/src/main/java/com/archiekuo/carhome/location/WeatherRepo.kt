package com.archiekuo.carhome.location

import android.os.SystemClock
import com.archiekuo.carhome.net.Http
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

data class Weather(val temp: Int, val code: Int, val max: Int?, val min: Int?, val rain: Int?) {
    /** 天氣代碼轉成中文 */
    val text: String
        get() = when (code) {
            0 -> "晴天"
            1 -> "晴時多雲"
            2 -> "多雲"
            3 -> "陰天"
            45, 48 -> "有霧"
            51, 53, 55, 56, 57 -> "毛毛雨"
            61, 63, 66 -> "下雨"
            65, 67 -> "大雨"
            71, 73, 75, 77 -> "下雪"
            80, 81 -> "陣雨"
            82 -> "大陣雨"
            85, 86 -> "陣雪"
            95, 96, 99 -> "雷雨"
            else -> "多雲"
        }

    /** sun、partly、cloud、rain、storm、fog、snow */
    val kind: String
        get() = when (code) {
            0 -> "sun"
            1, 2 -> "partly"
            3 -> "cloud"
            45, 48 -> "fog"
            in 51..67, 80, 81, 82 -> "rain"
            in 71..77, 85, 86 -> "snow"
            95, 96, 99 -> "storm"
            else -> "cloud"
        }
}

/** 天氣：免費的 Open-Meteo，不用申請帳號；30 分鐘更新一次 */
class WeatherRepo {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _weather = MutableStateFlow<Weather?>(null)
    val weather: StateFlow<Weather?> = _weather.asStateFlow()
    private var lastTime = 0L
    private var lastLat = 0.0
    private var lastLon = 0.0

    fun refresh(lat: Double, lon: Double, force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        val far = Math.abs(lat - lastLat) + Math.abs(lon - lastLon) > 0.1
        if (!force && _weather.value != null && now - lastTime < 30 * 60_000L && !far) return
        lastTime = now; lastLat = lat; lastLon = lon
        scope.launch {
            runCatching {
                val url = String.format(
                    Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto&forecast_days=1",
                    lat, lon,
                )
                val j = JSONObject(Http.get(url))
                val cur = j.getJSONObject("current")
                val d = j.optJSONObject("daily")
                fun first(k: String): Double? = d?.optJSONArray(k)?.let { if (it.length() > 0 && !it.isNull(0)) it.getDouble(0) else null }
                _weather.value = Weather(
                    temp = Math.round(cur.getDouble("temperature_2m")).toInt(),
                    code = cur.optInt("weather_code"),
                    max = first("temperature_2m_max")?.let { Math.round(it).toInt() },
                    min = first("temperature_2m_min")?.let { Math.round(it).toInt() },
                    rain = first("precipitation_probability_max")?.let { Math.round(it).toInt() },
                )
            }.onFailure { lastTime = now - 29 * 60_000L }
        }
    }
}
