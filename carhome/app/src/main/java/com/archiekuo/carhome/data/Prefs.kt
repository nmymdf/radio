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

    /** 發動後自動播網路收音機 */
    val autoRadio = BoolPref("autoRadio", true)
    /** 發動後自動開神盾 */
    val autoShield = BoolPref("autoShield", true)
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
