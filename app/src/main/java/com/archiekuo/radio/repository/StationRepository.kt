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

    suspend fun forCategory(c: Category, force: Boolean = false): List<Station> {
        if (!force) cache[c.key]?.let { return it }
        val raw = if (c.tags.isEmpty()) {
            api.top(c.key)
        } else coroutineScope {
            val parts = c.tags.map { t -> async { runCatching { api.byTag(t, c.key) } } }.awaitAll()
            if (parts.all { it.isFailure }) throw parts.first().exceptionOrNull() ?: Exception()
            parts.flatMap { it.getOrDefault(emptyList()) }
        }
        val res = clean(raw).take(80)
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
        clean(b.getOrDefault(emptyList()) + a.getOrDefault(emptyList())).take(80)
    }
}
