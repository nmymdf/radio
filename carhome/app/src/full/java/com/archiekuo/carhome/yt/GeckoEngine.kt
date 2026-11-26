package com.archiekuo.carhome.yt

import android.content.Context
import android.view.View
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.MediaSession

/** App 自帶的 Firefox 引擎（不靠車機的舊元件） */
class GeckoEngine(ctx: Context, private val l: YtListener) : YtEngine {
    private val session: GeckoSession
    private val gecko: GeckoView
    private var canBack = false
    private var media: MediaSession? = null
    override val view: View get() = gecko
    override val label = "App 自帶（Firefox 引擎）"

    init {
        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(false)
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
            .build()
        session = GeckoSession(settings)
        session.open(runtime(ctx))
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) { canBack = canGoBack }
            override fun onLoadRequest(session: GeckoSession, request: GeckoSession.NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny>? =
                if (stayInside(request.uri)) GeckoResult.fromValue(AllowOrDeny.ALLOW) else GeckoResult.fromValue(AllowOrDeny.DENY)
        }
        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) { l.onUrl(url) }
            override fun onPageStop(session: GeckoSession, success: Boolean) {}
        }
        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                if (fullScreen) { session.exitFullScreen(); l.onFullscreen(true) }
            }
        }
        // 允許影片自動播放（搬到浮動視窗後要能繼續播）
        session.permissionDelegate = object : GeckoSession.PermissionDelegate {
            override fun onContentPermissionRequest(session: GeckoSession, perm: GeckoSession.PermissionDelegate.ContentPermission): GeckoResult<Int>? =
                if (perm.permission == GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE || perm.permission == GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE)
                    GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
                else null
        }
        session.mediaSessionDelegate = object : MediaSession.Delegate {
            override fun onActivated(session: GeckoSession, mediaSession: MediaSession) { media = mediaSession }
            override fun onDeactivated(session: GeckoSession, mediaSession: MediaSession) { if (media === mediaSession) media = null; l.onPlaying(false) }
            override fun onPlay(session: GeckoSession, mediaSession: MediaSession) { media = mediaSession; l.onPlaying(true) }
            override fun onPause(session: GeckoSession, mediaSession: MediaSession) { l.onPlaying(false) }
            override fun onStop(session: GeckoSession, mediaSession: MediaSession) { l.onPlaying(false) }
        }
        gecko = GeckoView(ctx)
        // 用 TextureView 畫，搬到浮動視窗時比較不會黑掉
        if (textureBackend) runCatching { gecko.setViewBackend(GeckoView.BACKEND_TEXTURE_VIEW) }
        gecko.setSession(session)
    }

    override fun load(url: String) = session.loadUri(url)
    override fun back(): Boolean = if (canBack) { session.goBack(); true } else false
    override fun play() { media?.play() }
    override fun pause() { media?.pause() }
    override fun destroy() {
        runCatching { (gecko.parent as? android.view.ViewGroup)?.removeView(gecko) }
        runCatching { gecko.releaseSession() }
        runCatching { session.close() }
    }

    companion object {
        /** 畫面方式：true＝TextureView、false＝SurfaceView */
        var textureBackend = true
        private var rt: GeckoRuntime? = null
        fun runtime(ctx: Context): GeckoRuntime = rt ?: GeckoRuntime.create(
            ctx.applicationContext,
            GeckoRuntimeSettings.Builder().consoleOutput(false).aboutConfigEnabled(false).build(),
        ).also { rt = it }
    }
}

/** 這個版本有自帶引擎 */
const val HAS_GECKO = true
