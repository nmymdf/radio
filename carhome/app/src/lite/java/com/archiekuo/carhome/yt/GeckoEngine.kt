package com.archiekuo.carhome.yt

import android.content.Context
import android.view.View

/** 精簡版：沒有自帶引擎（安裝檔小很多），一律用車機內建的網頁元件 */
class GeckoEngine(ctx: Context, l: YtListener) : YtEngine {
    init { throw UnsupportedOperationException("lite") }
    override val view: View get() = throw UnsupportedOperationException()
    override val label = ""
    override fun load(url: String) {}
    override fun back() = false
    override fun play() {}
    override fun pause() {}
    override fun destroy() {}
    companion object { var textureBackend = true }
}

const val HAS_GECKO = false
