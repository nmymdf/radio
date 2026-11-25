package com.archiekuo.radio.repository

import android.content.Context
import com.archiekuo.radio.data.RadioBrowserApi
import com.archiekuo.radio.model.Categories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 分類清單：先用手機裡存的，背景再到 GitHub 抓最新版（下次打開生效） */
class CatalogRepository(private val ctx: Context, private val api: RadioBrowserApi) {
    private val prefs = ctx.getSharedPreferences("catalog", Context.MODE_PRIVATE)
    private val url = "https://raw.githubusercontent.com/nmymdf/radio/main/catalog.json"

    private fun res(name: String): Int = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)

    /** 這份清單的檢查日期（沒有精選電台的舊清單當作最舊） */
    private fun stamp(json: String?): String = runCatching {
        val o = org.json.JSONObject(json!!)
        if (o.optJSONObject("featured") == null) "" else o.optString("featuredChecked", "0")
    }.getOrDefault("")

    fun loadLocal() {
        val cached = prefs.getString("json", null)
        val bundled = runCatching { ctx.assets.open("catalog.json").bufferedReader().use { it.readText() } }.getOrNull()
        // 用比較新的那一份（舊版 App 存下來的清單沒有各國精選電台）
        val first = if (cached != null && stamp(cached) >= stamp(bundled)) cached else bundled
        if (first != null && Categories.load(first, ::res)) return
        val other = if (first === cached) bundled else cached
        if (other != null) Categories.load(other, ::res)
    }

    fun refreshRemote() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val text = runCatching { api.fetchText(url) }.getOrNull() ?: return@launch
            // 先確認內容正確才存下來
            val ok = runCatching { org.json.JSONObject(text).getJSONArray("categories").length() > 0 }.getOrDefault(false)
            if (ok && stamp(text) >= stamp(prefs.getString("json", null))) {
                prefs.edit().putString("json", text).apply()
                // 精選電台馬上生效（分類清單照舊下次打開才換，避免畫面跳動）
                if (org.json.JSONObject(text).optJSONObject("featured") != null) Categories.loadFeatured(text)
            }
        }
    }
}
