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
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** 天氣代碼轉中文 */
fun weatherText(code: Int): String = when (code) {
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
fun weatherKind(code: Int): String = when (code) {
    0 -> "sun"
    1, 2 -> "partly"
    3 -> "cloud"
    45, 48 -> "fog"
    in 51..67, 80, 81, 82 -> "rain"
    in 71..77, 85, 86 -> "snow"
    95, 96, 99 -> "storm"
    else -> "cloud"
}

data class HourWeather(val label: String, val temp: Int, val code: Int, val rain: Int?)
data class DayWeather(val label: String, val max: Int, val min: Int, val code: Int, val rain: Int?)

/** 詳細天氣（點天氣卡才看） */
data class WeatherDetail(
    val feels: Int?,
    val humidity: Int?,
    val windKmh: Int?,
    val windDir: Int?,
    val uv: Double?,
    val sunrise: String?,
    val sunset: String?,
    val hours: List<HourWeather>,
    val days: List<DayWeather>,
    val aqi: Int?,
    val pm25: Int?,
)

data class Weather(val temp: Int, val code: Int, val max: Int?, val min: Int?, val rain: Int?, val detail: WeatherDetail? = null) {
    val text: String get() = weatherText(code)
    val kind: String get() = weatherKind(code)
}

private val windNames = listOf("北風", "東北風", "東風", "東南風", "南風", "西南風", "西風", "西北風")
fun windText(dir: Int?): String = if (dir == null) "" else windNames[(((dir % 360) + 360) % 360 + 22) / 45 % 8]

/** 風速（公里/小時）換成蒲福風級 */
fun windLevel(kmh: Int): Int {
    val limits = intArrayOf(1, 6, 12, 20, 29, 39, 50, 62, 75, 89, 103, 118)
    val i = limits.indexOfFirst { kmh < it }
    return if (i < 0) 12 else i
}

fun uvText(uv: Double): String = when {
    uv < 3 -> "低"
    uv < 6 -> "中等"
    uv < 8 -> "高"
    uv < 11 -> "過高"
    else -> "危險"
}

fun aqiText(aqi: Int): String = when {
    aqi <= 50 -> "良好"
    aqi <= 100 -> "普通"
    aqi <= 150 -> "敏感族群注意"
    aqi <= 200 -> "不健康"
    aqi <= 300 -> "非常不健康"
    else -> "危害"
}

/** 天氣：免費的 Open-Meteo，不用申請帳號；30 分鐘更新一次 */
class WeatherRepo {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _weather = MutableStateFlow<Weather?>(null)
    val weather: StateFlow<Weather?> = _weather.asStateFlow()
    private var lastTime = 0L
    private var lastLat = 0.0
    private var lastLon = 0.0
    private var busy = false
    private var retryJob: kotlinx.coroutines.Job? = null

    fun refresh(lat: Double, lon: Double, force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        val far = Math.abs(lat - lastLat) + Math.abs(lon - lastLon) > 0.1
        if (busy) return
        if (!force && _weather.value?.detail != null && now - lastTime < 30 * 60_000L && !far) return
        if (!force && _weather.value == null && now - lastTime < 20_000L && !far) return
        lastTime = now; lastLat = lat; lastLon = lon
        busy = true
        retryJob?.cancel()
        scope.launch {
            val w = runCatching { load(lat, lon) }
                .onFailure { android.util.Log.w("CarHome", "weather detail failed: $it") }
                .getOrNull()
                ?: runCatching { loadSimple(lat, lon) }
                    .onFailure { android.util.Log.w("CarHome", "weather simple failed: $it") }
                    .getOrNull()
            busy = false
            if (w != null) {
                _weather.value = w
                // 空氣品質另外抓，慢了也不影響天氣顯示
                runCatching { loadAir(lat, lon) }.getOrNull()?.let { (aqi, pm) ->
                    _weather.value = _weather.value?.let { cur -> cur.copy(detail = cur.detail?.copy(aqi = aqi, pm25 = pm)) }
                }
            }
            // 沒抓到詳細資料：1 分鐘後自動再試（不用等畫面觸發）
            if (w?.detail == null) {
                retryJob = scope.launch { kotlinx.coroutines.delay(60_000); refresh(lat, lon, force = true) }
            }
        }
    }

    /** 舊版的簡單查詢（詳細查詢失敗時至少顯示天氣卡） */
    private suspend fun loadSimple(lat: Double, lon: Double): Weather {
        val j = JSONObject(Http.get(String.format(Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto&forecast_days=1", lat, lon)))
        val cur = j.getJSONObject("current")
        val d = j.optJSONObject("daily")
        fun first(k: String): Int? = d?.optJSONArray(k)?.let { if (it.length() > 0 && !it.isNull(0)) Math.round(it.getDouble(0)).toInt() else null }
        return Weather(Math.round(cur.getDouble("temperature_2m")).toInt(), cur.optInt("weather_code"), first("temperature_2m_max"), first("temperature_2m_min"), first("precipitation_probability_max"))
    }

    private suspend fun loadAir(lat: Double, lon: Double): Pair<Int?, Int?> {
        val a = JSONObject(Http.get(String.format(Locale.US, "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=%.4f&longitude=%.4f&current=us_aqi,pm2_5&timezone=auto", lat, lon)))
            .getJSONObject("current")
        fun v(k: String) = if (a.has(k) && !a.isNull(k)) Math.round(a.optDouble(k)).toInt() else null
        return v("us_aqi") to v("pm2_5")
    }

    private suspend fun load(lat: Double, lon: Double): Weather {
        val url = String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
                "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m,uv_index" +
                "&hourly=temperature_2m,weather_code,precipitation_probability" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset" +
                "&timezone=auto&forecast_days=7",
            lat, lon,
        )
        val j = JSONObject(Http.get(url))
        val cur = j.getJSONObject("current")
        val daily = j.optJSONObject("daily")
        val hourly = j.optJSONObject("hourly")

        fun JSONObject?.num(k: String, i: Int): Double? =
            this?.optJSONArray(k)?.let { if (i < it.length() && !it.isNull(i)) it.optDouble(i).takeIf { d -> !d.isNaN() } else null }
        fun JSONObject?.str(k: String, i: Int): String? =
            this?.optJSONArray(k)?.let { if (i < it.length() && !it.isNull(i)) it.optString(i) else null }
        fun JSONObject.curNum(k: String): Double? = if (has(k) && !isNull(k)) optDouble(k).takeIf { !it.isNaN() } else null
        fun r(d: Double?) = d?.let { Math.round(it).toInt() }

        // 接下來 12 小時（從現在這個小時開始）
        val hours = ArrayList<HourWeather>()
        val times: JSONArray? = hourly?.optJSONArray("time")
        if (times != null) {
            val nowHour = cur.optString("time").take(13) // 2026-11-24T14
            var start = (0 until times.length()).indexOfFirst { times.optString(it).take(13) == nowHour }
            if (start < 0) start = 0
            for (i in start until minOf(start + 12, times.length())) {
                val t = times.optString(i)
                val h = t.substring(11, 13).toIntOrNull() ?: 0
                hours += HourWeather(
                    if (i == start) "現在" else "${h}時",
                    r(hourly.num("temperature_2m", i)) ?: continue,
                    r(hourly.num("weather_code", i)) ?: 3,
                    r(hourly.num("precipitation_probability", i)),
                )
            }
        }
        // 未來 7 天
        val days = ArrayList<DayWeather>()
        val dates = daily?.optJSONArray("time")
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val week = listOf("週日", "週一", "週二", "週三", "週四", "週五", "週六")
        if (dates != null) {
            for (i in 0 until minOf(7, dates.length())) {
                val label = if (i == 0) "今天" else runCatching {
                    val c = Calendar.getInstance(); c.time = fmt.parse(dates.optString(i))!!
                    week[c.get(Calendar.DAY_OF_WEEK) - 1]
                }.getOrDefault("")
                days += DayWeather(
                    label,
                    r(daily.num("temperature_2m_max", i)) ?: continue,
                    r(daily.num("temperature_2m_min", i)) ?: continue,
                    r(daily.num("weather_code", i)) ?: 3,
                    r(daily.num("precipitation_probability_max", i)),
                )
            }
        }
        val detail = WeatherDetail(
            feels = r(cur.curNum("apparent_temperature")),
            humidity = r(cur.curNum("relative_humidity_2m")),
            windKmh = r(cur.curNum("wind_speed_10m")),
            windDir = r(cur.curNum("wind_direction_10m")),
            uv = cur.curNum("uv_index"),
            sunrise = daily.str("sunrise", 0)?.takeIf { it.length >= 16 }?.substring(11, 16),
            sunset = daily.str("sunset", 0)?.takeIf { it.length >= 16 }?.substring(11, 16),
            hours = hours, days = days,
            aqi = null, pm25 = null,
        )
        return Weather(
            temp = r(cur.curNum("temperature_2m")) ?: error("no temp"),
            code = cur.optInt("weather_code"),
            max = r(daily.num("temperature_2m_max", 0)),
            min = r(daily.num("temperature_2m_min", 0)),
            rain = r(daily.num("precipitation_probability_max", 0)),
            detail = detail,
        )
    }
}
