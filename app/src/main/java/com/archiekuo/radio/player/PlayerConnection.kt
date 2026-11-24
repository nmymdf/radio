package com.archiekuo.radio.player

import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.archiekuo.radio.data.LocalStore
import com.archiekuo.radio.data.stationFromJson
import com.archiekuo.radio.data.toJson
import com.archiekuo.radio.model.Categories
import com.archiekuo.radio.model.Station
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

data class PlayerState(
    val station: Station? = null,
    val isPlaying: Boolean = false,
    val buffering: Boolean = false,
    val error: String? = null,
    val nowPlaying: String? = null,
    val sleepMinutes: Int = 0,
    val sleepEndAt: Long = 0L,
)

/** 畫面這一端的播放器遙控器：透過 MediaController 控制背景的 PlaybackService */
class PlayerConnection(private val ctx: Context, private val store: LocalStore) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null
    private var connecting = false
    private val pending = ArrayList<(MediaController) -> Unit>()
    private var errorCount = 0
    private var sleepJob: Job? = null

    private val _state = MutableStateFlow(PlayerState(station = store.lastStation))
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying, error = if (isPlaying) null else it.error) }
            if (isPlaying) errorCount = 0
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update { it.copy(buffering = playbackState == Player.STATE_BUFFERING) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val s = mediaItem?.let { stationOf(it) } ?: return
            errorCount = 0
            _state.update { it.copy(station = s, nowPlaying = null, error = null) }
            store.lastStation = s
            store.addHistory(s)
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val t = mediaMetadata.title?.toString()?.trim()
            val name = _state.value.station?.name
            _state.update { it.copy(nowPlaying = if (t.isNullOrBlank() || t == name) null else t) }
        }

        override fun onPlayerError(error: PlaybackException) {
            errorCount++
            val msg = if (errorCount >= 5) "這個電台目前無法播放，請換一台試試" else "連線不穩，正在重新連線…"
            _state.update { it.copy(error = msg, buffering = errorCount < 5) }
        }
    }

    fun connect() {
        if (controller != null || connecting) return
        connecting = true
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        future.addListener({
            connecting = false
            val c = runCatching { future.get() }.getOrNull() ?: return@addListener
            controller = c
            c.addListener(listener)
            c.currentMediaItem?.let { item ->
                stationOf(item)?.let { s -> _state.update { it.copy(station = s, isPlaying = c.isPlaying) } }
            }
            val todo = pending.toList(); pending.clear()
            todo.forEach { it(c) }
        }, ContextCompat.getMainExecutor(ctx))
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else { pending += block; connect() }
    }

    private fun stationOf(item: MediaItem): Station? =
        item.mediaMetadata.extras?.getString("st")?.let { runCatching { stationFromJson(JSONObject(it)) }.getOrNull() }

    private fun isWifi(): Boolean {
        val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    /** 播放某個電台；list 是同一份清單，上一台／下一台會在這份清單裡切換 */
    fun play(station: Station, list: List<Station> = listOf(station)) {
        if (store.wifiOnly.value && !isWifi()) {
            _state.update { it.copy(station = station, error = "目前設定為「只用 Wi‑Fi 播放」，可到設定關閉") }
            return
        }
        val l = if (list.any { it.id == station.id }) list else listOf(station)
        val idx = l.indexOfFirst { it.id == station.id }.coerceAtLeast(0)
        errorCount = 0
        _state.update { it.copy(station = station, error = null, buffering = true, nowPlaying = null) }
        store.lastStation = station
        withController { c ->
            c.setMediaItems(l.map { it.toMediaItem(ctx) }, idx, 0L)
            c.prepare()
            c.play()
        }
    }

    fun toggle() {
        withController { c ->
            if (c.isPlaying || c.playWhenReady && c.playbackState == Player.STATE_BUFFERING) {
                c.pause()
            } else if (c.mediaItemCount == 0) {
                _state.value.station?.let { play(it) }
            } else {
                // 直播：重新接到最新的即時內容
                c.seekToDefaultPosition()
                c.prepare()
                c.play()
            }
        }
    }

    fun next() = withController { c -> if (c.mediaItemCount > 1) { c.seekToNextMediaItem(); c.prepare(); c.play() } }
    fun previous() = withController { c -> if (c.mediaItemCount > 1) { c.seekToPreviousMediaItem(); c.prepare(); c.play() } }

    /** 有上次電台而且目前沒在播，就自動接著播 */
    fun autoResume() = withController { c ->
        if (!c.isPlaying && c.mediaItemCount == 0) store.lastStation?.let { play(it) }
    }

    fun setSleep(minutes: Int) {
        sleepJob?.cancel()
        if (minutes <= 0) {
            _state.update { it.copy(sleepMinutes = 0, sleepEndAt = 0L) }
            return
        }
        val end = System.currentTimeMillis() + minutes * 60_000L
        _state.update { it.copy(sleepMinutes = minutes, sleepEndAt = end) }
        sleepJob = scope.launch {
            delay(minutes * 60_000L)
            controller?.pause()
            _state.update { it.copy(sleepMinutes = 0, sleepEndAt = 0L) }
        }
    }
}
