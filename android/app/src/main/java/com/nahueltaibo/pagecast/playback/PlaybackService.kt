package com.nahueltaibo.pagecast.playback

import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.nahueltaibo.pagecast.data.Library

const val SKIP_MS = 30_000L

/** Plays episodes in the background, with lock screen and Bluetooth controls, and remembers where each one stopped. */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private lateinit var library: Library
    private val handler = Handler(Looper.getMainLooper())
    private val periodicSave = object : Runnable {
        override fun run() {
            if (session?.player?.isPlaying == true) savePosition()
            handler.postDelayed(this, 5_000)
        }
    }

    @OptIn(UnstableApi::class) // The seek increment setters; stable in practice since Media3 1.0.
    override fun onCreate() {
        super.onCreate()
        library = Library(this)
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(SKIP_MS)
            .setSeekForwardIncrementMs(SKIP_MS)
            .build()
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = savePosition()

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) player.currentMediaItem?.mediaId?.let { library.savePosition(it, 0) }
            }
        })
        session = MediaSession.Builder(this, player).build()
        handler.post(periodicSave)
    }

    private fun savePosition() {
        val player = session?.player ?: return
        val id = player.currentMediaItem?.mediaId ?: return
        if (player.playbackState != Player.STATE_ENDED) library.savePosition(id, player.currentPosition)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        savePosition()
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(periodicSave)
        savePosition()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
