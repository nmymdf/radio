package com.archiekuo.radio.player

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import com.archiekuo.radio.data.toJson
import com.archiekuo.radio.model.Categories
import com.archiekuo.radio.model.Station

/** 車機桌面等外部程式可送來的指令：接著播上次的電台 */
const val CMD_RESUME_LAST = "com.archiekuo.radio.RESUME_LAST"

fun Station.toMediaItem(ctx: Context): MediaItem {
    val cat = Categories.byKey(categoryKey)
    val art = Uri.parse("android.resource://${ctx.packageName}/${cat.art}")
    val extras = Bundle().apply {
        putString("st", toJson().toString())
        putBoolean("hls", hls)
    }
    val md = MediaMetadata.Builder()
        .setTitle(name)
        .setArtist(listOf(countryZh, languageZh).filter { it.isNotBlank() }.joinToString(" ・ ").ifBlank { "網路收音機 ・ ${cat.title}" })
        .setArtworkUri(art)
        .setExtras(extras)
        .build()
    val b = MediaItem.Builder()
        .setMediaId(id)
        .setUri(url)
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(Uri.parse(url)).build())
        .setMediaMetadata(md)
    if (hls) b.setMimeType(MimeTypes.APPLICATION_M3U8)
    return b.build()
}
