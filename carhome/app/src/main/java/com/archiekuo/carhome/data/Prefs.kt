package com.archiekuo.carhome.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 所有設定都存在車機裡 */
class Prefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("carhome", Context.MODE_PRIVATE)

    inner class BoolPref(private val key: String, def: Boolean) {
        private val f = MutableStateFlow(p.getBoolean(key, def))
        val flow: StateFlow<Boolean> = f.asStateFlow()
        val value get() = f.value
        fun set(v: Boolean) { f.value = v; p.edit().putBoolean(key, v).apply() }
    }

    inner class StrPref(private val key: String, def: String) {
        private val f = MutableStateFlow(p.getString(key, def) ?: def)
        val flow: StateFlow<String> = f.asStateFlow()
        val value get() = f.value
        fun set(v: String) { f.value = v; p.edit().putString(key, v).apply() }
    }

    inner class IntPref(private val key: String, def: Int) {
        private val f = MutableStateFlow(p.getInt(key, def))
        val flow: StateFlow<Int> = f.asStateFlow()
        val value get() = f.value
        fun set(v: Int) { f.value = v; p.edit().putInt(key, v).apply() }
    }

    /** 時鐘指南針圓：0＝時鐘＋指南針、1＝大字時鐘、2＝大字指南針 */
    val circleMode = IntPref("circleMode", 0)
    /** 神盾和 YouTube 放左邊（時鐘和天氣換到右邊） */
    val shieldLeft = BoolPref("shieldLeft", false)
    /** 內部音樂最近播放（路徑，用換行分隔，最新的在前面） */
    val recentSongs = StrPref("recentSongs", "")
    /** 發動後自動播網路收音機 */
    val autoRadio = BoolPref("autoRadio", true)
    /** 發動後自動開神盾（v0.6 起預設不開，讓使用者自己決定） */
    val autoShield = BoolPref("autoShield06", false)
    val shieldPkg = StrPref("shieldPkg", "")
    /** 沒網路時自動切 FM */
    val autoFm = BoolPref("autoFm", true)
    val fmPkg = StrPref("fmPkg", "")
    /** 行駛中 YouTube 只播聲音 */
    val ytRestrict = BoolPref("ytRestrict", true)
    /** 跳動光條 */
    val viz = BoolPref("viz", true)
    /** 光條流動速度跟著節奏 */
    val vizTempo = BoolPref("vizTempo", true)
    /** 全黑桌面 */
    val black = BoolPref("black", false)
    /** 隱藏系統狀態列 */
    val hideStatus = BoolPref("hideStatus", true)
    /** 播放卡目前的模式：radio、local、fm */
    val mode = StrPref("mode", "radio")
    /** YouTube 平常顯示的照片：lake、cafe、taipei、zen、black，或 file:路徑 */
    val ytPhoto = StrPref("ytPhoto", "lake")
    /** YouTube 小窗開法：0＝每按一次換下一種（找方法用），1～3＝固定用第幾種 */
    val ytWinMethod = IntPref("ytWinMethod", 0)
    /** 輪流試的時候，下一次要用第幾種（1～3） */
    val ytWinNext = IntPref("ytWinNext", 1)
    /** YouTube 上次的網址 */
    val ytUrl = StrPref("ytUrl", "https://m.youtube.com")

    private val tileFlows = List(7) { i -> StrPref("tile$i", "") }
    fun tile(i: Int) = tileFlows[i]

    var localIndex: Int
        get() = p.getInt("localIndex", 0)
        set(v) { p.edit().putInt("localIndex", v).apply() }
    var localPos: Long
        get() = p.getLong("localPos", 0L)
        set(v) { p.edit().putLong("localPos", v).apply() }
    var localShuffle: Boolean
        get() = p.getBoolean("localShuffle", false)
        set(v) { p.edit().putBoolean("localShuffle", v).apply() }
}
