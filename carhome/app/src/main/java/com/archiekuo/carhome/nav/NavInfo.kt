package com.archiekuo.carhome.nav

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 高德導航送出來的下一個轉彎資訊 */
data class NavGuide(
    val icon: Int,
    val segDist: Int,
    val nextRoad: String,
    val remainDist: Int,
    val remainSec: Int,
    val at: Long = SystemClock.elapsedRealtime(),
) {
    /** 轉彎動作（中文） */
    val action: String
        get() = when (icon) {
            2 -> "左轉"
            3 -> "右轉"
            4 -> "靠左前方"
            5 -> "靠右前方"
            6 -> "左後方轉"
            7 -> "右後方轉"
            8 -> "迴轉"
            9 -> "直走"
            10 -> "到達途經點"
            11 -> "進入圓環"
            12 -> "駛出圓環"
            13 -> "到達服務區"
            14 -> "到達收費站"
            15 -> "到達目的地"
            16 -> "進入隧道"
            else -> "繼續前進"
        }

    val actionLine: String get() = if (nextRoad.isBlank() || icon == 15) action else "$action $nextRoad"
}

fun distText(m: Int): Pair<String, String> = when {
    m < 0 -> "--" to ""
    m < 1000 -> "$m" to "公尺"
    m < 10_000 -> String.format(java.util.Locale.US, "%.1f", m / 1000f) to "公里"
    else -> "${m / 1000}" to "公里"
}

fun remainText(g: NavGuide, twoLines: Boolean = false): String {
    val (d, u) = distText(g.remainDist)
    val min = (g.remainSec + 59) / 60
    val t = if (min >= 60) "${min / 60} 小時 ${min % 60} 分" else "$min 分鐘"
    return if (g.remainDist <= 0) "" else if (twoLines) "剩 $d $u\n約 $t" else "剩 $d $u ・ 約 $t"
}

/**
 * 接收高德車機版的導航廣播（AUTONAVI_STANDARD_BROADCAST_SEND）。
 * KEY_TYPE 10001＝導航資訊，10019＝狀態（結束導航時清掉）。
 */
class NavInfo(ctx: Context, private val scope: CoroutineScope) {
    private val _guide = MutableStateFlow<NavGuide?>(null)
    val guide: StateFlow<NavGuide?> = _guide.asStateFlow()

    private fun Bundle.int(vararg keys: String): Int? {
        for (k in keys) {
            val v = runCatching { get(k) }.getOrNull() ?: continue
            when (v) {
                is Number -> return v.toInt()
                is String -> v.trim().toDoubleOrNull()?.let { return it.toInt() }
            }
        }
        return null
    }

    private fun Bundle.text(vararg keys: String): String =
        keys.firstNotNullOfOrNull { k -> runCatching { get(k)?.toString() }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() } } ?: ""

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            val b = i.extras ?: return
            when (b.int("KEY_TYPE")) {
                10001 -> _guide.value = NavGuide(
                    icon = b.int("ICON", "NEW_ICON") ?: 0,
                    segDist = b.int("SEG_REMAIN_DIS") ?: -1,
                    nextRoad = b.text("NEXT_ROAD_NAME"),
                    remainDist = b.int("ROUTE_REMAIN_DIS") ?: -1,
                    remainSec = b.int("ROUTE_REMAIN_TIME") ?: 0,
                )
                10019 -> when (b.int("EXTRA_STATE")) {
                    2, 9, 12, 39 -> _guide.value = null // 高德關閉、結束導航、結束模擬導航、到達目的地
                }
            }
        }
    }

    init {
        ContextCompat.registerReceiver(ctx, receiver, IntentFilter("AUTONAVI_STANDARD_BROADCAST_SEND"), ContextCompat.RECEIVER_EXPORTED)
        // 20 秒沒有新資訊就當作已經不在導航
        scope.launch {
            while (true) {
                delay(2000)
                val g = _guide.value
                if (g != null && SystemClock.elapsedRealtime() - g.at > 20_000) _guide.value = null
            }
        }
    }
}
