package com.archiekuo.carhome.media

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RadioState(
    val installed: Boolean = false,
    val connected: Boolean = false,
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val error: Boolean = false,
    val title: String? = null,
    val subtitle: String? = null,
    val artwork: Uri? = null,
)

/** 遙控「網路收音機」App（com.archiekuo.radio） */
class RadioRemote(private val ctx: Context) {
    companion object {
        const val PKG = "com.archiekuo.radio"
        const val SERVICE = "com.archiekuo.radio.player.PlaybackService"
        const val CMD_RESUME_LAST = "com.archiekuo.radio.RESUME_LAST"
    }

    private var controller: MediaController? = null
    private var connecting = false
    private val pending = ArrayList<(MediaController) -> Unit>()
    private val _state = MutableStateFlow(RadioState())
    val state: StateFlow<RadioState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync(player)
        override fun onPlayerError(error: PlaybackException) { _state.update { it.copy(error = true) } }
    }

    fun isInstalled(): Boolean = runCatching { ctx.packageManager.getPackageInfo(PKG, 0); true }.getOrDefault(false)

    private fun sync(p: Player) {
        val md: MediaMetadata = p.mediaMetadata
        val item = p.currentMediaItem
        val station = item?.mediaMetadata?.title?.toString()
        val sub = item?.mediaMetadata?.artist?.toString()
        _state.update {
            it.copy(
                installed = true, connected = true,
                playing = p.isPlaying,
                buffering = p.playbackState == Player.STATE_BUFFERING && p.playWhenReady,
                error = if (p.isPlaying) false else it.error,
                title = station ?: md.title?.toString(),
                subtitle = sub,
                artwork = item?.mediaMetadata?.artworkUri ?: md.artworkUri,
            )
        }
    }

    fun connect() {
        val inst = isInstalled()
        _state.update { it.copy(installed = inst) }
        if (!inst || controller != null || connecting) return
        connecting = true
        val token = SessionToken(ctx, ComponentName(PKG, SERVICE))
        val f = MediaController.Builder(ctx, token).buildAsync()
        f.addListener({
            connecting = false
            val c = runCatching { f.get() }.getOrNull()
            if (c == null) { pending.clear(); return@addListener }
            controller = c
            c.addListener(listener)
            sync(c)
            val todo = pending.toList(); pending.clear(); todo.forEach { it(c) }
        }, ContextCompat.getMainExecutor(ctx))
    }

    private fun with(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null && c.isConnected) block(c) else { controller = null; pending += block; connect() }
    }

    /** 接著播上次的電台 */
    fun resumeLast() = with { c ->
        if (c.isPlaying) return@with
        c.sendCustomCommand(SessionCommand(CMD_RESUME_LAST, Bundle.EMPTY), Bundle.EMPTY)
    }

    fun toggle() = with { c -> if (c.isPlaying || (c.playWhenReady && c.playbackState == Player.STATE_BUFFERING)) c.pause() else if (c.mediaItemCount == 0) resumeLast() else { c.seekToDefaultPosition(); c.prepare(); c.play() } }
    fun pause() { controller?.let { if (it.isConnected) it.pause() } }
    fun next() = with { c -> if (c.mediaItemCount > 1) { c.seekToNextMediaItem(); c.prepare(); c.play() } }
    fun previous() = with { c -> if (c.mediaItemCount > 1) { c.seekToPreviousMediaItem(); c.prepare(); c.play() } }
    val isPlaying get() = controller?.isPlaying == true
}
