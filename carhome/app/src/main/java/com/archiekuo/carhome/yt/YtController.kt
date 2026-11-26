package com.archiekuo.carhome.yt

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import com.archiekuo.carhome.CarApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 一個 YouTube 播放器，三種大小：
 * 桌面小格（CARD）、全畫面（FULL）、浮在所有 App 上面的小視窗（FLOAT）。
 * 切換大小時影片不中斷，同一個播放器搬來搬去。
 * 作者：ArchieKUO
 */
class YtController(private val app: CarApp) : YtListener {
    enum class Mode { IDLE, CARD, FULL, FLOAT }

    private val _mode = MutableStateFlow(Mode.IDLE)
    val mode: StateFlow<Mode> = _mode.asStateFlow()
    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing.asStateFlow()
    /** 目前用的引擎（設定頁顯示） */
    private val _engineLabel = MutableStateFlow("")
    val engineLabel: StateFlow<String> = _engineLabel.asStateFlow()

    private var engine: YtEngine? = null
    private var url = ""
    private val main = Handler(Looper.getMainLooper())
    private val float = FloatWindow(app, this)

    /** 桌面上的兩個位置（由畫面登記） */
    var cardHost: FrameLayout? = null
        set(v) { field = v; place() }
    var fullHost: FrameLayout? = null
        set(v) { field = v; place() }

    /** 浮動是自動的（離開桌面時）還是自己按的 */
    private var autoFloated = false
    private var homeVisible = true

    fun canFloat(): Boolean = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(app)

    private fun ensure(): YtEngine {
        engine?.let { return it }
        val choice = app.prefs.ytEngine.value
        val wv = webViewMajor(app)
        val wantGecko = when (choice) {
            "gecko" -> true
            "system" -> false
            else -> wv < WEBVIEW_OK
        }
        val e: YtEngine = if (wantGecko && Build.VERSION.SDK_INT >= GECKO_MIN_SDK) {
            runCatching { GeckoEngine(app, this) }.getOrElse { Log.w(TAG, "gecko failed", it); SystemEngine(app, this) }
        } else SystemEngine(app, this)
        Log.i(TAG, "engine=${e.label} webview=$wv choice=$choice")
        _engineLabel.value = e.label
        engine = e
        return e
    }

    /** 開始播放器（沒指定網址就接著上次） */
    fun start(target: Mode = Mode.CARD, url: String? = null) {
        val first = engine == null
        val e = ensure()
        if (url != null) e.load(url) else if (first) e.load(app.prefs.ytUrl.value.takeIf { it.startsWith("https://") } ?: YT_HOME)
        setMode(target)
    }

    fun search(q: String) {
        val u = "https://m.youtube.com/results?search_query=" + android.net.Uri.encode(q)
        start(if (_mode.value == Mode.IDLE) Mode.CARD else _mode.value, u)
    }

    fun home() { engine?.load(YT_HOME) ?: start(Mode.CARD, YT_HOME) }
    fun back() { if (engine?.back() != true) app.say("已經是第一頁了") }

    fun setMode(m: Mode, auto: Boolean = false) {
        if (m == Mode.FLOAT && !canFloat()) {
            app.say("請先在設定裡「允許顯示在其他 App 上層」，才能浮動")
            return
        }
        autoFloated = auto && m == Mode.FLOAT
        Log.i(TAG, "mode ${_mode.value} -> $m auto=$auto")
        _mode.value = m
        place()
    }

    /** 關掉播放器（釋放記憶體），桌面小格回到照片 */
    fun close() {
        engine?.let { runCatching { it.pause() }; it.destroy() }
        engine = null
        _playing.value = false
        _engineLabel.value = ""
        float.hide()
        _mode.value = Mode.IDLE
    }

    /** 換引擎（設定頁）：關掉目前的，下次打開用新的 */
    fun resetEngine() { if (engine != null) close() }

    /** 把播放器放到目前該在的地方 */
    private fun place() {
        val e = engine ?: run { float.hide(); return }
        val target: ViewGroup? = when (_mode.value) {
            Mode.CARD -> cardHost
            Mode.FULL -> fullHost
            Mode.FLOAT -> float.show()
            Mode.IDLE -> null
        }
        if (_mode.value != Mode.FLOAT) float.hide()
        val v = e.view
        if (target == null || v.parent === target) return
        val wasPlaying = _playing.value
        (v.parent as? ViewGroup)?.removeView(v)
        target.addView(v, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        // 搬家後如果被暫停了，幫它繼續播
        if (wasPlaying) {
            main.postDelayed({ if (!_playing.value) engine?.play() }, 700)
            main.postDelayed({ if (!_playing.value) engine?.play() }, 2000)
        }
    }

    // ---- 桌面出現／離開 ----
    fun onHomeHidden() {
        homeVisible = false
        val m = _mode.value
        if (engine != null && (m == Mode.CARD || m == Mode.FULL) && app.prefs.ytAutoFloat.value) {
            if (canFloat()) setMode(Mode.FLOAT, auto = true)
            else if (_playing.value) app.say("要浮動小視窗，請先允許「顯示在其他 App 上層」")
        }
    }

    fun onHomeShown() {
        homeVisible = true
        if (_mode.value == Mode.FLOAT && autoFloated) setMode(Mode.CARD)
        else place()
    }

    // ---- 引擎回報 ----
    override fun onUrl(url: String) {
        if (url.startsWith("https://") && url != this.url) {
            this.url = url
            app.prefs.ytUrl.set(url)
        }
    }

    override fun onPlaying(playing: Boolean) {
        if (_playing.value != playing) Log.i(TAG, "playing=$playing mode=${_mode.value}")
        _playing.value = playing
    }

    override fun onFullscreen(on: Boolean) {
        if (on && _mode.value != Mode.FULL) {
            if (homeVisible) setMode(Mode.FULL) else { setMode(Mode.FULL); app.bringHome() }
        }
    }

    companion object {
        const val TAG = "CarHomeYT"
        /** 自帶引擎需要的 Android 版本（Android 8） */
        const val GECKO_MIN_SDK = 26
    }
}
