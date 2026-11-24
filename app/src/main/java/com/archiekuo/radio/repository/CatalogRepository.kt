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

    fun loadLocal() {
        val cached = prefs.getString("json", null)
        if (cached != null && Categories.load(cached, ::res)) return
        val bundled = ctx.assets.open("catalog.json").bufferedReader().use { it.readText() }
        Categories.load(bundled, ::res)
    }

    fun refreshRemote() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val text = runCatching { api.fetchText(url) }.getOrNull() ?: return@launch
            // 先確認內容正確才存下來
            val ok = runCatching { org.json.JSONObject(text).getJSONArray("categories").length() > 0 }.getOrDefault(false)
            if (ok) prefs.edit().putString("json", text).apply()
        }
    }
}
