package com.archiekuo.radio.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.archiekuo.radio.MainActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/** 背景播放服務：關螢幕、切到別的 App 都會繼續播；通知列、鎖定畫面、藍牙按鍵都能控制 */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private var retries = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        player.repeatMode = Player.REPEAT_MODE_ALL

        // 斷線自動重連：最多重試 5 次，間隔逐次拉長
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (retries < 5) {
                    retries++
                    handler.postDelayed({
                        player.seekToDefaultPosition()
                        player.prepare()
                        player.play()
                    }, 3000L * retries)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) retries = 0
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                retries = 0
                handler.removeCallbacksAndMessages(null)
            }
        })

        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(open)
            .setCallback(object : MediaSession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<MediaItem>,
                ): ListenableFuture<MutableList<MediaItem>> {
                    val out = mediaItems.map { item ->
                        val uri = item.requestMetadata.mediaUri ?: item.localConfiguration?.uri
                        val b = item.buildUpon().setUri(uri)
                        if (item.mediaMetadata.extras?.getBoolean("hls") == true) b.setMimeType(MimeTypes.APPLICATION_M3U8)
                        b.build()
                    }.toMutableList()
                    return Futures.immediateFuture(out)
                }
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
