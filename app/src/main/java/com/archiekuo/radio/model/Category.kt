package com.archiekuo.radio.model

import androidx.annotation.DrawableRes
import com.archiekuo.radio.R

data class Category(
    val key: String,
    val title: String,
    val tags: List<String>,
    @DrawableRes val art: Int,
    @DrawableRes val icon: Int? = null,
    val tint: Long = 0xFF2B3672,
)

object Categories {
    val hotStations = listOf(
        Category("classical", "古典音樂", listOf("classical"), R.drawable.art_classic),
        Category("jazz", "爵士音樂", listOf("jazz", "smooth jazz"), R.drawable.art_jazz),
        Category("cpop", "華語流行", listOf("mandopop", "c-pop", "chinese pop", "mandarin"), R.drawable.art_pop),
        Category("easy", "輕音樂", listOf("easy listening", "lounge", "instrumental"), R.drawable.art_light),
    )
    val music = listOf(
        Category("hot", "熱門推薦", emptyList(), R.drawable.art_radio, R.drawable.ic_cat_hot, 0xFF2B3672),
        Category("world", "國際音樂", listOf("pop", "top 40", "hits"), R.drawable.art_pop, R.drawable.ic_cat_world, 0xFF243A70),
        Category("rock", "搖滾音樂", listOf("rock", "classic rock"), R.drawable.art_jazz, R.drawable.ic_cat_rock, 0xFF3A2F70),
        Category("love", "情歌浪漫", listOf("love songs", "romantic", "ballads"), R.drawable.art_pop, R.drawable.ic_cat_love, 0xFF4B2B6C),
        Category("zen", "佛教音樂", listOf("buddhist", "buddhism", "meditation"), R.drawable.art_lake, R.drawable.ic_cat_zen, 0xFF2F396C),
        Category("nature", "自然放鬆", listOf("nature", "relax", "ambient"), R.drawable.art_lake, R.drawable.ic_cat_nature, 0xFF22406A),
    )
    val all = hotStations + music

    fun byKey(key: String): Category = all.firstOrNull { it.key == key } ?: music.first()

    /** 從電台的英文標籤猜它屬於哪一類（搜尋結果用來配圖） */
    fun guess(tags: String): String {
        val t = tags.lowercase()
        return all.firstOrNull { c -> c.tags.any { t.contains(it) } }?.key ?: "hot"
    }

    /** 中文搜尋字對應到分類，例如「爵士」→ 爵士音樂 */
    fun fromQuery(q: String): Category? =
        all.firstOrNull { q.contains(it.title.take(2)) || it.title.contains(q) && q.length >= 2 }
}
