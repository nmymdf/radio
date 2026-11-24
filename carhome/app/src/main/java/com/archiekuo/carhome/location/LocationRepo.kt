package com.archiekuo.carhome.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
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

data class Fix(
    val lat: Double,
    val lon: Double,
    val speedKmh: Float,
    val bearing: Float?,
    val time: Long,
)

/** GPS：位置、車速、行進方向、目前路名、是否行駛中 */
class LocationRepo(private val ctx: Context, private val isOnline: () -> Boolean) {
    private val lm = ctx.getSystemService(LocationManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _fix = MutableStateFlow<Fix?>(null)
    val fix: StateFlow<Fix?> = _fix.asStateFlow()

    /** 最後一次有效的行進方向（停車時保留） */
    private val _heading = MutableStateFlow<Float?>(null)
    val heading: StateFlow<Float?> = _heading.asStateFlow()

    private val _road = MutableStateFlow<String?>(null)
    val road: StateFlow<String?> = _road.asStateFlow()
    private val _roadStale = MutableStateFlow(false)
    val roadStale: StateFlow<Boolean> = _roadStale.asStateFlow()

    /** 行政區（給天氣顯示用），例如「新店區」 */
    private val _district = MutableStateFlow<String?>(null)
    val district: StateFlow<String?> = _district.asStateFlow()

    private val _driving = MutableStateFlow(false)
    val driving: StateFlow<Boolean> = _driving.asStateFlow()

    private var started = false
    private var lastGeoTime = 0L
    private var lastGeo: Location? = null
    private var geoBusy = false
    private var movingSince = 0L
    private var stoppedSince = 0L

    private val listener = object : LocationListener {
        override fun onLocationChanged(loc: Location) = onFix(loc)
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (started || lm == null) return
        runCatching {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper())
            if (lm.allProviders.contains(LocationManager.NETWORK_PROVIDER)) lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10_000L, 0f, listener, Looper.getMainLooper())
            started = true
            val last = listOfNotNull(
                runCatching { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull(),
                runCatching { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull(),
            ).maxByOrNull { it.time }
            last?.let { onFix(it) }
        }
    }

    private fun onFix(loc: Location) {
        val now = SystemClock.elapsedRealtime()
        // 網路定位沒有速度，GPS 有就用 GPS
        val current = _fix.value
        if (loc.provider == LocationManager.NETWORK_PROVIDER && current != null && now - current.time < 15_000) return
        val kmh = if (loc.hasSpeed()) loc.speed * 3.6f else 0f
        val bearing = if (loc.hasBearing() && kmh > 5f) loc.bearing else null
        _fix.value = Fix(loc.latitude, loc.longitude, kmh, bearing, now)
        if (bearing != null) _heading.value = bearing

        // 行駛中：時速超過 8 公里持續 3 秒；停車：低於 3 公里持續 6 秒
        if (kmh > 8f) {
            stoppedSince = 0
            if (movingSince == 0L) movingSince = now
            if (now - movingSince > 3000) _driving.value = true
        } else if (kmh < 3f) {
            movingSince = 0
            if (stoppedSince == 0L) stoppedSince = now
            if (now - stoppedSince > 6000) _driving.value = false
        }
        maybeGeocode(loc, now)
    }

    /** 每 12 秒最多查一次路名，而且要移動超過 40 公尺才查（停車時不查） */
    private fun maybeGeocode(loc: Location, now: Long) {
        if (geoBusy) return
        val moved = lastGeo?.distanceTo(loc) ?: Float.MAX_VALUE
        if (lastGeo != null && (now - lastGeoTime < 12_000 || moved < 40f)) return
        if (!isOnline()) { if (_road.value != null) _roadStale.value = true; return }
        geoBusy = true
        lastGeoTime = now
        val lat = loc.latitude; val lon = loc.longitude
        scope.launch {
            runCatching {
                val url = String.format(Locale.US, "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=%.6f&lon=%.6f&zoom=17&addressdetails=1&accept-language=zh-TW", lat, lon)
                val a = JSONObject(Http.get(url)).getJSONObject("address")
                fun pick(vararg k: String) = k.firstNotNullOfOrNull { a.optString(it).takeIf { s -> s.isNotBlank() } }
                val city = pick("city", "county", "state") ?: ""
                val dist = pick("city_district", "suburb", "town", "district", "village") ?: ""
                val road = pick("road", "pedestrian", "residential", "neighbourhood") ?: ""
                val area = (city + dist).trim()
                val text = listOf(area, road).filter { it.isNotBlank() }.joinToString(" ・ ")
                if (text.isNotBlank()) {
                    _road.value = text
                    _roadStale.value = false
                    lastGeo = loc
                }
                if (dist.isNotBlank()) _district.value = dist else if (city.isNotBlank()) _district.value = city
            }
            geoBusy = false
        }
    }
}
