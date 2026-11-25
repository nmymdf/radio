package com.archiekuo.radio.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.archiekuo.radio.RadioApp
import com.archiekuo.radio.model.Categories
import com.archiekuo.radio.model.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface Load {
    data object Loading : Load
    data class Ok(val list: List<Station>) : Load
    data class Err(val msg: String) : Load
}

private fun friendly(e: Throwable): String {
    val m = e.message ?: ""
    return if (m.contains("找不到")) m else "連不上電台資料庫，請確認網路後再試一次"
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val ra = app as RadioApp
    val store = ra.store
    private val repo = ra.repo
    private val player = ra.player

    val playerState = player.state
    val favorites = store.favorites
    val history = store.history
    val recentSearches = store.recentSearches
    val wifiOnly = store.wifiOnly
    val autoPlay = store.autoPlay

    private val _cats = MutableStateFlow<Map<String, Load>>(emptyMap())
    val cats: StateFlow<Map<String, Load>> = _cats.asStateFlow()

    private val _search = MutableStateFlow<Load?>(null)
    val search: StateFlow<Load?> = _search.asStateFlow()
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun load(key: String, force: Boolean = false) {
        val cur = _cats.value[key]
        if (!force && (cur is Load.Ok || cur is Load.Loading)) return
        _cats.update { it + (key to Load.Loading) }
        viewModelScope.launch {
            val r = runCatching { repo.forCategory(Categories.byKey(key), force) }
            _cats.update { it + (key to r.fold({ l -> Load.Ok(l) }, { e -> Load.Err(friendly(e)) })) }
        }
    }

    /** 這次打開 App 後，使用者有沒有選過電台（選過才顯示下方播放器） */
    private val _opened = MutableStateFlow(false)
    val opened: StateFlow<Boolean> = _opened

    /** 熱門排行（存在 cats 裡，key＝hot-國碼） */
    fun loadHot(cc: String, force: Boolean = false) {
        val key = "hot-$cc"
        val cur = _cats.value[key]
        if (!force && (cur is Load.Ok || cur is Load.Loading)) return
        _cats.update { it + (key to Load.Loading) }
        viewModelScope.launch {
            val r = runCatching { repo.hot(cc, force) }
            _cats.update { it + (key to r.fold({ l -> Load.Ok(l) }, { e -> Load.Err(friendly(e)) })) }
        }
    }

    fun setQuery(q: String) { _query.value = q }

    fun runSearch(q: String = _query.value) {
        val t = q.trim()
        if (t.isEmpty()) return
        _query.value = t
        store.addRecent(t)
        _search.value = Load.Loading
        viewModelScope.launch {
            val r = runCatching { repo.search(t) }
            _search.value = r.fold(
                { l -> if (l.isEmpty()) Load.Err("找不到「$t」相關的電台，換個關鍵字試試") else Load.Ok(l) },
                { e -> Load.Err(friendly(e)) },
            )
        }
    }

    fun play(s: Station, list: List<Station> = listOf(s)) { _opened.value = true; player.play(s, list) }
    fun toggle() = player.toggle()
    fun next() = player.next()
    fun previous() = player.previous()
    fun setSleep(min: Int) = player.setSleep(min)

    /** 首頁大圖的「開始播放」：key 為空就接著播上次的電台（沒有就播熱門第一名），否則播該分類第一名 */
    fun playBanner(key: String? = null) {
        _opened.value = true
        val st = playerState.value
        if (key == null) {
            if (st.isPlaying) return
            val s = st.station ?: store.lastStation
            if (s != null) { player.play(s, currentListFor(s)); return }
        }
        val k = key ?: "hot"
        viewModelScope.launch {
            runCatching { repo.forCategory(Categories.byKey(k)) }.getOrNull()?.let { l ->
                _cats.update { it + (k to Load.Ok(l)) }
                l.firstOrNull()?.let { player.play(it, l) }
            }
        }
    }

    private fun currentListFor(s: Station): List<Station> =
        (_cats.value[s.categoryKey] as? Load.Ok)?.list?.takeIf { l -> l.any { it.id == s.id } } ?: listOf(s)

    fun toggleFavorite(s: Station) = store.toggleFavorite(s)
    fun removeFavorites(ids: Set<String>) = store.removeFavorites(ids)
    fun clearHistory() = store.clearHistory()
    fun setWifiOnly(b: Boolean) = store.setWifiOnly(b)
    fun setAutoPlay(b: Boolean) = store.setAutoPlay(b)
    val screenMode = store.screenMode
    fun setScreenMode(m: String) = store.setScreenMode(m)
}
