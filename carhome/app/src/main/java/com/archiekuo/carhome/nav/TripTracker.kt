package com.archiekuo.carhome.nav

import android.os.SystemClock
import com.archiekuo.carhome.location.Fix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.*

/** 這一趟：開了多久、跑了多遠 */
data class Trip(val startAt: Long, val meters: Double) {
    val minutes: Long get() = (SystemClock.elapsedRealtime() - startAt) / 60_000
    val km: Double get() = meters / 1000.0
    /** 平均時速（含停車等紅燈） */
    val avgKmh: Int get() { val h = (SystemClock.elapsedRealtime() - startAt) / 3_600_000.0; return if (h < 0.02) 0 else (km / h).roundToInt() }
}

/**
 * 從發動車子開始算；GPS 超過 10 分鐘沒動靜（熄火後再發動）就重新開始。
 * 作者：ArchieKUO
 */
class TripTracker(scope: CoroutineScope, fix: StateFlow<Fix?>) {
    private val _trip = MutableStateFlow(Trip(SystemClock.elapsedRealtime(), 0.0))
    val trip: StateFlow<Trip> = _trip.asStateFlow()
    private var last: Fix? = null
    private var lastAt = 0L

    fun reset() { _trip.value = Trip(SystemClock.elapsedRealtime(), 0.0); last = null }

    init {
        scope.launch {
            fix.collect { f ->
                if (f == null) return@collect
                val now = SystemClock.elapsedRealtime()
                if (lastAt != 0L && now - lastAt > 10 * 60_000) reset()
                lastAt = now
                val p = last
                last = f
                if (p == null) return@collect
                val d = meters(p.lat, p.lon, f.lat, f.lon)
                // 停著不動時 GPS 會飄，車速太低或跳太遠都不算
                if (f.speedKmh >= 3f && d < 500) _trip.value = _trip.value.copy(meters = _trip.value.meters + d)
            }
        }
    }

    private fun meters(a1: Double, o1: Double, a2: Double, o2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(a2 - a1); val dLon = Math.toRadians(o2 - o1)
        val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a1)) * cos(Math.toRadians(a2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(h))
    }
}
