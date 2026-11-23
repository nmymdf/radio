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
) {
    val subtitle: String
        get() {
            val parts = mutableListOf<String>()
            if (country.isNotBlank()) parts += country
            parts += Categories.byKey(categoryKey).title
            if (bitrate > 0) parts += "$bitrate kbps"
            return parts.joinToString(" ・ ")
        }
}
