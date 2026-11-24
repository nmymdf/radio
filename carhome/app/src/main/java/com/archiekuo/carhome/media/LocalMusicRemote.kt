package com.archiekuo.carhome.media

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.archiekuo.carhome.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocalState(
    val scanned: Boolean = false,
    val scanning: Boolean = false,
    val count: Int = 0,
    val playing: Boolean = false,
    val title: String? = null,
    val artist: String? = null,
    val index: Int = 0,
    val shuffle: Boolean = false,
    val uri: android.net.Uri? = null,
    val id: String? = null,
)

/** 內部音樂：掃描歌曲、播放、記住上次聽到哪 */
class LocalMusicRemote(private val ctx: Context, private val prefs: Prefs) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null
    private var connecting = false
    private val pending = ArrayList<(MediaController) -> Unit>()
    private var songs: List<Song> = emptyList()
    private var loaded = false
    private val _state = MutableStateFlow(LocalState(shuffle = prefs.localShuffle))
    val state: StateFlow<LocalState> = _state.asStateFlow()
    private val _library = MutableStateFlow<List<Song>>(emptyList())
    /** 全部歌曲（同資料夾排在一起） */
    val library: StateFlow<List<Song>> = _library.asStateFlow()
    private val _recent = MutableStateFlow(prefs.recentSongs.value.split('\n').filter { it.isNotBlank() })
    /** 最近播放的歌（id，最新的在前面） */
    val recent: StateFlow<List<String>> = _recent.asStateFlow()
    private var lastRecorded: String? = null

    private fun record(id: String) {
        if (id == lastRecorded) return
        lastRecorded = id
        val list = (listOf(id) + _recent.value.filter { it != id }).take(40)
        _recent.value = list
        prefs.recentSongs.set(list.joinToString("\n"))
    }

    val audioSessionId: Int get() = 0

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            val item = player.currentMediaItem
            val md = item?.mediaMetadata
            _state.update {
                it.copy(playing = player.isPlaying, title = md?.title?.toString(), artist = md?.artist?.toString(),
                    index = player.currentMediaItemIndex, uri = item?.localConfiguration?.uri, id = item?.mediaId)
            }
            val id = item?.mediaId
            if (player.isPlaying && !id.isNullOrBlank()) record(id)
            if (player.mediaItemCount > 0) prefs.localIndex = player.currentMediaItemIndex
        }
    }

    init {
        // 每 10 秒記住播放位置
        scope.launch {
            while (true) {
                delay(10_000)
                controller?.let { if (it.isConnected && it.isPlaying) prefs.localPos = it.currentPosition }
            }
        }
    }

    private fun connect() {
        if (controller != null || connecting) return
        connecting = true
        val f = MediaController.Builder(ctx, SessionToken(ctx, ComponentName(ctx, LocalMusicService::class.java))).buildAsync()
        f.addListener({
            connecting = false
            val c = runCatching { f.get() }.getOrNull() ?: return@addListener
            controller = c
            c.addListener(listener)
            val todo = pending.toList(); pending.clear(); todo.forEach { it(c) }
        }, ContextCompat.getMainExecutor(ctx))
    }

    private fun with(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null && c.isConnected) block(c) else { pending += block; connect() }
    }

    fun rescan(then: (() -> Unit)? = null) {
        if (_state.value.scanning) return
        _state.update { it.copy(scanning = true) }
        scope.launch {
            songs = runCatching { LocalLibrary.scan(ctx) }.getOrDefault(emptyList())
            _library.value = songs
            loaded = false
            _state.update { it.copy(scanning = false, scanned = true, count = songs.size) }
            then?.invoke()
        }
    }

    private fun load(c: MediaController) {
        if (loaded) return
        val items = songs.map { s ->
            MediaItem.Builder().setUri(s.uri).setMediaId(s.id)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(s.title).setArtist(s.artist).build()).build()
        }
        val idx = prefs.localIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        c.setMediaItems(items, idx, prefs.localPos)
        c.shuffleModeEnabled = prefs.localShuffle
        c.prepare()
        loaded = true
    }

    fun play() {
        if (!_state.value.scanned) { rescan { play() }; return }
        if (songs.isEmpty()) return
        with { c -> load(c); c.play() }
    }

    /** 從清單點一首歌：播放那首，之後接著播下一首 */
    fun playSong(id: String) {
        val i = songs.indexOfFirst { it.id == id }
        if (i < 0) return
        with { c ->
            load(c)
            if (c.mediaItemCount != songs.size) { loaded = false; load(c) }
            c.seekTo(i, 0L)
            c.prepare()
            c.play()
        }
    }

    fun toggle() { if (_state.value.playing) pause() else play() }
    fun pause() { controller?.let { if (it.isConnected) { prefs.localPos = it.currentPosition; it.pause() } } }
    fun next() = with { c -> load(c); c.seekToNextMediaItem(); c.play() }
    fun previous() = with { c -> load(c); c.seekToPreviousMediaItem(); c.play() }
    fun toggleShuffle() {
        val v = !prefs.localShuffle
        prefs.localShuffle = v
        _state.update { it.copy(shuffle = v) }
        controller?.let { if (it.isConnected) it.shuffleModeEnabled = v }
    }
}
