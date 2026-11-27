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
class NavInfo(private val ctx: Context, private val scope: CoroutineScope) {
    private val _guide = MutableStateFlow<NavGuide?>(null)
    val guide: StateFlow<NavGuide?> = _guide.asStateFlow()

    // ---- 高德資料記錄：每一種 KEY_TYPE 留最後一筆，存到檔案（熄火重開也看得到） ----
    data class LogItem(val type: Int, val count: Int, val time: Long, val text: String)
    private val _log = MutableStateFlow<Map<Int, LogItem>>(emptyMap())
    val log: StateFlow<Map<Int, LogItem>> = _log.asStateFlow()
    private val logFile get() = java.io.File(ctx.filesDir, "amap-log.txt")
    private var logDirty = false

    private fun record(type: Int, b: Bundle) {
        val text = b.keySet().sorted().joinToString("\n") { k -> "$k = ${runCatching { b.get(k) }.getOrNull()}" }
        val old = _log.value[type]
        _log.value = _log.value + (type to LogItem(type, (old?.count ?: 0) + 1, System.currentTimeMillis(), text))
        logDirty = true
    }

    private fun saveLog() {
        runCatching {
            val o = org.json.JSONArray()
            _log.value.values.forEach { o.put(org.json.JSONObject().put("type", it.type).put("count", it.count).put("time", it.time).put("text", it.text)) }
            logFile.writeText(o.toString())
        }
    }

    private fun loadLog() {
        runCatching {
            val a = org.json.JSONArray(logFile.readText())
            _log.value = (0 until a.length()).associate { i ->
                val o = a.getJSONObject(i)
                o.getInt("type") to LogItem(o.getInt("type"), o.getInt("count"), o.getLong("time"), o.getString("text"))
            }
        }
    }

    fun clearLog() { _log.value = emptyMap(); runCatching { logFile.delete() } }

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
            val type = b.int("KEY_TYPE") ?: -1
            record(type, b)
            when (type) {
                10001 -> {
                    val g = NavGuide(
                        icon = b.int("ICON", "NEW_ICON") ?: 0,
                        segDist = b.int("SEG_REMAIN_DIS") ?: -1,
                        nextRoad = b.text("NEXT_ROAD_NAME"),
                        remainDist = b.int("ROUTE_REMAIN_DIS") ?: -1,
                        remainSec = b.int("ROUTE_REMAIN_TIME") ?: 0,
                    )
                    // 高德只是開著（巡航、沒有目的地）也會送資料：沒有剩餘距離就不算在導航
                    _guide.value = if (g.remainDist > 0) g else null
                }
                10019 -> when (b.int("EXTRA_STATE")) {
                    2, 9, 12, 39 -> _guide.value = null // 高德關閉、結束導航、結束模擬導航、到達目的地
                }
            }
        }
    }

    init {
        loadLog()
        scope.launch { while (true) { delay(10_000); if (logDirty) { logDirty = false; saveLog() } } }
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
