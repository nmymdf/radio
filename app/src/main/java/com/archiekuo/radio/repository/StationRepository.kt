package com.archiekuo.radio.repository

import com.archiekuo.radio.data.RadioBrowserApi
import com.archiekuo.radio.model.Categories
import com.archiekuo.radio.model.Category
import com.archiekuo.radio.model.Station
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class StationRepository(private val api: RadioBrowserApi) {
    private val cache = HashMap<String, List<Station>>()

    private fun clean(list: List<Station>) = list
        .sortedByDescending { it.votes }
        .distinctBy { it.name.lowercase().trim() }

    /** 精選名單裡的電台排在最前面，並標記「精選」 */
    private fun prioritize(list: List<Station>, c: Category): List<Station> {
        if (c.priority.isEmpty()) return list
        fun rank(s: Station): Int {
            val i = c.priority.indexOfFirst { p -> s.name.contains(p, ignoreCase = true) }
            return if (i < 0) Int.MAX_VALUE else i
        }
        return list.map { if (rank(it) != Int.MAX_VALUE) it.copy(curated = true) else it }
            .sortedWith(compareBy<Station> { rank(it) }.thenByDescending { it.votes })
    }

    fun clear(key: String) { cache.remove(key) }

    /** 把精選大台放最前面，資料庫裡重複的那一筆拿掉 */
    private fun withFeatured(feat: List<Station>, rest: List<Station>, key: String): List<Station> {
        if (feat.isEmpty()) return rest
        val f = feat.map { it.copy(categoryKey = key) }
        val names = f.map { it.name.lowercase() }.toSet()
        return f + rest.filter { s -> f.none { it.id == s.id || it.url == s.url } && s.name.lowercase() !in names }
    }

    /** 熱門排行：精選大台（實測可播）＋ 該國實際收聽次數最多的 */
    suspend fun hot(cc: String, force: Boolean = false): List<Station> {
        val key = "hot-$cc"
        if (!force) cache[key]?.let { return it }
        val feat = Categories.featured[cc].orEmpty()
        val rb = runCatching { api.popular(cc) }
        if (rb.isFailure && feat.isEmpty()) throw rb.exceptionOrNull() ?: Exception()
        val rest = rb.getOrDefault(emptyList()).distinctBy { it.name.lowercase().trim() }
        val res = withFeatured(feat, rest, "hot").take(80)
        if (res.isEmpty()) throw Exception("這個國家目前找不到電台")
        cache[key] = res
        return res
    }

    suspend fun forCategory(c: Category, force: Boolean = false): List<Station> {
        if (!force) cache[c.key]?.let { return it }
        val specs = c.specs.ifEmpty { listOf(mapOf("top" to "1")) }
        val parts = coroutineScope { specs.map { s -> async { runCatching { api.bySpec(s, c.key) } } }.awaitAll() }
        // 「熱門推薦」和「依地區」：該國的精選大台放最前面
        val cc = when {
            c.key == "hot" -> "TW"
            c.group == "region" -> c.specs.firstNotNullOfOrNull { it["countrycode"] }
            else -> null
        }
        val feat = cc?.let { Categories.featured[it] }.orEmpty()
        if (parts.all { it.isFailure } && feat.isEmpty()) throw parts.first().exceptionOrNull() ?: Exception()
        val res = withFeatured(feat, prioritize(clean(parts.flatMap { it.getOrDefault(emptyList()) }), c), c.key).take(100)
        if (res.isEmpty()) throw Exception("這個分類目前找不到電台")
        cache[c.key] = res
        return res
    }

    suspend fun search(q: String): List<Station> = coroutineScope {
        val byName = async { runCatching { api.byName(q) } }
        val cat = Categories.fromQuery(q)
        val byCat = async { if (cat != null) runCatching { forCategory(cat) } else Result.success(emptyList()) }
        val a = byName.await(); val b = byCat.await()
        if (a.isFailure && b.isFailure) throw a.exceptionOrNull() ?: Exception()
        clean(b.getOrDefault(emptyList()) + a.getOrDefault(emptyList())).take(100)
    }
}
