package com.archiekuo.carhome.yt

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/**
 * YouTube 播放器的網頁引擎。
 * 車機內建的網頁元件（WebView）太舊時，改用 App 自帶的 Firefox 引擎（GeckoView）。
 * 作者：ArchieKUO
 */
interface YtEngine {
    val view: View
    /** 給設定頁顯示用 */
    val label: String
    fun load(url: String)
    /** 回上一頁；沒有上一頁回傳 false */
    fun back(): Boolean
    fun play()
    fun pause()
    fun destroy()
}

/** 引擎回報給控制器的事 */
interface YtListener {
    fun onUrl(url: String)
    fun onPlaying(playing: Boolean)
    /** 影片按了「全螢幕」 */
    fun onFullscreen(on: Boolean)
}

const val YT_HOME = "https://m.youtube.com/"

/** 判斷一個網址能不能留在播放器裡（其他像 intent://、vnd.youtube: 一律擋掉，不跳出去開 YouTube App） */
fun stayInside(url: String): Boolean = url.startsWith("https://") || url.startsWith("http://") || url.startsWith("about:") || url.startsWith("data:") || url.startsWith("blob:")

/**
 * 每一頁一開始就執行：
 * 1. 讓網頁以為自己一直「看得到」，縮小、浮動、換位置時影片才不會被 YouTube 自己暫停
 * 2. 每秒回報影片是不是正在播
 */
internal val YT_SCRIPT = """
(function(){
  if (window.__carhome) return; window.__carhome = 1;
  try {
    Object.defineProperty(document, 'hidden', { get: function(){ return false; } });
    Object.defineProperty(document, 'webkitHidden', { get: function(){ return false; } });
    Object.defineProperty(document, 'visibilityState', { get: function(){ return 'visible'; } });
    Object.defineProperty(document, 'webkitVisibilityState', { get: function(){ return 'visible'; } });
  } catch (e) {}
  var stop = function(e){ e.stopImmediatePropagation(); };
  document.addEventListener('visibilitychange', stop, true);
  document.addEventListener('webkitvisibilitychange', stop, true);
  window.addEventListener('pagehide', stop, true);
  window.addEventListener('blur', stop, true);
  var last = null;
  setInterval(function(){
    var v = document.querySelector('video');
    var p = !!(v && !v.paused && !v.ended && v.readyState > 2);
    if (p !== last) { last = p; try { CarHome.playing(p); } catch (e) {} }
  }, 1000);
})();
""".trimIndent()

/** 車機內建的網頁元件 */
@SuppressLint("SetJavaScriptEnabled")
class SystemEngine(ctx: Context, private val l: YtListener) : YtEngine {
    private val main = Handler(Looper.getMainLooper())
    private val web = WebView(ctx)
    override val view: View get() = web
    override val label = "車機內建（WebView ${webViewMajor(ctx)}）"
    private val docStart = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)

    init {
        web.setBackgroundColor(Color.BLACK)
        val s = web.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.mediaPlaybackRequiresUserGesture = false
        s.loadWithOverviewMode = true
        s.useWideViewPort = true
        s.setSupportMultipleWindows(false)
        // 不要讓 YouTube 認出是 App 裡的網頁（不然會一直叫你去開 YouTube App）
        s.userAgentString = s.userAgentString.replace("; wv", "").replace(Regex("Version/\\d+\\.\\d+ "), "")
        CookieManager.getInstance().setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= 21) CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
        web.addJavascriptInterface(object {
            @JavascriptInterface fun playing(p: Boolean) { main.post { l.onPlaying(p) } }
        }, "CarHome")
        if (docStart) runCatching { WebViewCompat.addDocumentStartJavaScript(web, YT_SCRIPT, setOf("*")) }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = !stayInside(request.url.toString())
            @Deprecated("old api")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = !stayInside(url)
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                if (!docStart) view.evaluateJavascript(YT_SCRIPT, null)
                l.onUrl(url)
            }
            override fun onPageFinished(view: WebView, url: String) {
                if (!docStart) view.evaluateJavascript(YT_SCRIPT, null)
                l.onUrl(url)
            }
            override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) { l.onUrl(url) }
        }
        web.webChromeClient = object : WebChromeClient() {
            // 影片右下角的「全螢幕」：改成放大播放器（不用系統的全螢幕）
            override fun onShowCustomView(v: View, callback: CustomViewCallback) {
                callback.onCustomViewHidden()
                l.onFullscreen(true)
            }
        }
    }

    override fun load(url: String) = web.loadUrl(url)
    override fun back(): Boolean = if (web.canGoBack()) { web.goBack(); true } else false
    override fun play() = web.evaluateJavascript("(function(){var v=document.querySelector('video'); if(v&&v.paused) v.play();})()", null)
    override fun pause() = web.evaluateJavascript("(function(){var v=document.querySelector('video'); if(v&&!v.paused) v.pause();})()", null)
    override fun destroy() {
        runCatching { (web.parent as? android.view.ViewGroup)?.removeView(web) }
        runCatching { web.loadUrl("about:blank"); web.destroy() }
    }
}

/** 車機內建網頁元件的大版本號（例如 74、120）；讀不到回傳 0 */
fun webViewMajor(ctx: Context): Int {
    val pi = runCatching {
        if (Build.VERSION.SDK_INT >= 26) WebView.getCurrentWebViewPackage()
        else listOf("com.google.android.webview", "com.android.webview", "com.android.chrome")
            .firstNotNullOfOrNull { p -> runCatching { ctx.packageManager.getPackageInfo(p, 0) }.getOrNull() }
    }.getOrNull()
    return pi?.versionName?.substringBefore('.')?.toIntOrNull() ?: 0
}

/** 內建元件夠新（100 版以上）才用；不然用自帶的 */
const val WEBVIEW_OK = 100
