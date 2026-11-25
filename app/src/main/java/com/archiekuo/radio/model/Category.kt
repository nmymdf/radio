package com.archiekuo.radio.model

import androidx.annotation.DrawableRes
import com.archiekuo.radio.R
import org.json.JSONObject

/** 一個分類（四個字），內容來自 catalog.json，可以從 GitHub 遠端更新 */
data class Category(
    val key: String,
    val title: String,
    /** music 音樂、talk 講話節目、region 依地區 */
    val group: String,
    @DrawableRes val art: Int,
    @DrawableRes val icon: Int? = null,
    val tint: Long = 0xFF2B3672,
    /** hot 首頁熱門電台、music 首頁音樂分類、talk 首頁講話節目 */
    val home: String? = null,
    val specs: List<Map<String, String>> = emptyList(),
    val priority: List<String> = emptyList(),
)

object Categories {
    val groups = listOf("music" to "音樂", "talk" to "講話節目", "region" to "依地區")

    private val fallback = Category("hot", "熱門推薦", "music", R.drawable.art_radio, R.drawable.ic_cat_hot, specs = listOf(mapOf("top" to "1")))

    @Volatile
    var all: List<Category> = listOf(fallback)
        private set

    val hotStations get() = all.filter { it.home == "hot" }

    /** 熱門排行的國家（依使用者指定的順序） */
    val hotCountries = listOf("TW" to "台灣", "US" to "美國", "GB" to "英國", "KR" to "韓國", "JP" to "日本", "HK" to "香港")

    /** 各國精選大台：雲端每週實測過、確定能播的（catalog.json 的 featured） */
    @Volatile
    var featured: Map<String, List<Station>> = emptyMap()
        private set
    val music get() = all.filter { it.home == "music" }
    val talkHome get() = all.filter { it.home == "talk" }
    fun inGroup(g: String) = all.filter { it.group == g }

    fun byKey(key: String): Category = all.firstOrNull { it.key == key } ?: all.firstOrNull() ?: fallback

    /** 讀入 catalog.json；res 把圖片名稱轉成資源編號 */
    fun load(json: String, res: (String) -> Int): Boolean = runCatching {
        val arr = JSONObject(json).getJSONArray("categories")
        val list = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val specs = o.optJSONArray("specs")?.let { sa ->
                (0 until sa.length()).map { j ->
                    val so = sa.getJSONObject(j)
                    so.keys().asSequence().associateWith { so.getString(it) }
                }
            } ?: emptyList()
            val pr = o.optJSONArray("priority")?.let { pa -> (0 until pa.length()).map { pa.getString(it) } } ?: emptyList()
            val art = res(o.optString("art", "art_radio")).takeIf { it != 0 } ?: R.drawable.art_radio
            val icon = o.optString("icon", "").takeIf { it.isNotBlank() && it != "null" }?.let { res(it) }?.takeIf { it != 0 }
            val tint = runCatching { java.lang.Long.parseLong(o.optString("tint", "#2B3672").removePrefix("#"), 16) or 0xFF000000 }.getOrDefault(0xFF2B3672)
            Category(
                key = o.getString("key"), title = o.getString("title"), group = o.optString("group", "music"),
                art = art, icon = icon, tint = tint, home = o.optString("home", "").takeIf { it.isNotBlank() && it != "null" },
                specs = specs, priority = pr,
            )
        }
        if (list.isNotEmpty()) all = list
        loadFeatured(json)
        list.isNotEmpty()
    }.getOrDefault(false)

    fun loadFeatured(json: String) = runCatching {
        JSONObject(json).optJSONObject("featured")?.let { f ->
            featured = f.keys().asSequence().associateWith { cc ->
                val a = f.getJSONArray(cc)
                (0 until a.length()).mapNotNull { i ->
                    val s = a.getJSONObject(i)
                    val url = s.optString("url")
                    if (url.isBlank()) null else Station(
                        id = s.optString("uuid").ifBlank { url }, name = s.optString("name"), url = url,
                        countryCode = cc, codec = s.optString("codec"), bitrate = s.optInt("bitrate"),
                        tags = s.optString("tags"), hls = s.optBoolean("hls"), categoryKey = "hot",
                        state = s.optString("state"), language = s.optString("language"),
                        homepage = s.optString("homepage"), curated = true, favicon = s.optString("favicon"),
                    )
                }
            }
        }
        Unit
    }

    /** 從電台的英文標籤猜它屬於哪一類（搜尋結果用來配圖） */
    fun guess(tags: String): String {
        val t = tags.lowercase()
        return all.firstOrNull { c -> c.group == "music" && c.specs.any { s -> s["tag"]?.let { t.contains(it) } == true } }?.key ?: "hot"
    }

    /** 中文搜尋字對應到分類，例如「爵士」→ 爵士音樂、「台語」→ 台語歌曲 */
    fun fromQuery(q: String): Category? =
        all.firstOrNull { q.contains(it.title.take(2)) || (q.length >= 2 && it.title.contains(q)) }
}

/** 國家、語言轉成中文 */
object Names {
    private val countries = mapOf(
        "TW" to "台灣", "HK" to "香港", "MO" to "澳門", "CN" to "中國", "SG" to "新加坡", "MY" to "馬來西亞",
        "JP" to "日本", "KR" to "韓國", "US" to "美國", "GB" to "英國", "CA" to "加拿大", "AU" to "澳洲",
        "NZ" to "紐西蘭", "DE" to "德國", "FR" to "法國", "IT" to "義大利", "ES" to "西班牙", "NL" to "荷蘭",
        "CH" to "瑞士", "AT" to "奧地利", "BE" to "比利時", "SE" to "瑞典", "NO" to "挪威", "DK" to "丹麥",
        "FI" to "芬蘭", "IE" to "愛爾蘭", "PT" to "葡萄牙", "RU" to "俄羅斯", "IN" to "印度", "TH" to "泰國",
        "VN" to "越南", "PH" to "菲律賓", "ID" to "印尼", "BR" to "巴西", "MX" to "墨西哥", "AR" to "阿根廷",
        "GR" to "希臘", "PL" to "波蘭", "CZ" to "捷克", "HU" to "匈牙利", "TR" to "土耳其", "IL" to "以色列",
    )
    private val langs = listOf(
        "taiwanese" to "台語", "hokkien" to "台語", "min nan" to "台語", "minnan" to "台語",
        "cantonese" to "粵語", "hakka" to "客語", "mandarin" to "國語", "chinese" to "中文",
        "english" to "英語", "japanese" to "日語", "korean" to "韓語", "german" to "德語", "french" to "法語",
        "spanish" to "西班牙語", "italian" to "義大利語", "portuguese" to "葡萄牙語", "russian" to "俄語",
        "dutch" to "荷蘭語", "thai" to "泰語", "vietnamese" to "越南語", "malay" to "馬來語", "indonesian" to "印尼語",
    )

    fun country(code: String, fallback: String): String = countries[code.uppercase()] ?: fallback

    fun language(raw: String): String {
        val l = raw.lowercase()
        if (l.isBlank()) return ""
        val found = langs.filter { l.contains(it.first) }.map { it.second }.distinct()
        return if (found.isNotEmpty()) found.take(2).joinToString("、") else raw.split(',').first().trim().replaceFirstChar { it.uppercase() }
    }
}
