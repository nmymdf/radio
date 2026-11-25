package com.archiekuo.radio.model

/** 一個網路電台（資料來自 Radio Browser 公開資料庫） */
data class Station(
    val id: String,
    val name: String,
    val url: String,
    val country: String = "",
    val countryCode: String = "",
    val codec: String = "",
    val bitrate: Int = 0,
    val tags: String = "",
    val hls: Boolean = false,
    val votes: Int = 0,
    val categoryKey: String = "hot",
    val state: String = "",
    val language: String = "",
    val homepage: String = "",
    /** 列在精選名單裡的電台 */
    val curated: Boolean = false,
    /** 電台自己的台標網址（有的電台沒有） */
    val favicon: String = "",
    /** 收聽次數（熱門排序用） */
    val clicks: Int = 0,
) {
    val countryZh: String get() = Names.country(countryCode, country)
    val languageZh: String get() = Names.language(language)

    /** 地區：國家 ・ 城市 */
    val place: String get() = listOf(countryZh, state.trim()).filter { it.isNotBlank() }.distinct().joinToString(" ・ ")

    /** 音質：128 kbps ・ MP3 */
    val quality: String get() = listOf(if (bitrate > 0) "$bitrate kbps" else "", codec.uppercase()).filter { it.isNotBlank() }.joinToString(" ・ ")

    /** 資料來源 */
    val sourceText: String get() = if (curated) "精選電台（已實測可播放）" else "Radio Browser 公開資料庫"

    /** 清單裡的第二行：台灣 ・ 國語 ・ 128 kbps */
    val subtitle: String
        get() = listOf(countryZh, languageZh, if (bitrate > 0) "$bitrate kbps" else "").filter { it.isNotBlank() }
            .ifEmpty { listOf(Categories.byKey(categoryKey).title) }.joinToString(" ・ ")

    /** 迷你播放列：直播中 ・ 台灣 ・ 國語 */
    val liveLine: String get() = (listOf("直播中") + listOf(countryZh, languageZh).filter { it.isNotBlank() }).joinToString(" ・ ")
}
