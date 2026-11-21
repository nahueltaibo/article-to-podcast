package com.nahueltaibo.pagecast.playback

import android.content.Intent
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.PlayerMessage
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.nahueltaibo.pagecast.data.Library
import com.nahueltaibo.pagecast.data.Settings

const val SKIP_MS = 30_000L
const val CHIME_AT_MS = "chimeAtMs"
const val CHIME_FILE = "chimeFile"

/** Plays episodes in the background, with lock screen and Bluetooth controls, and remembers where each one stopped. */
// Seek increments and PlayerMessage are marked unstable, but have been unchanged since Media3 1.0.
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private lateinit var library: Library
    private lateinit var settings: Settings
    private val chimes = SoundPool.Builder()
        .setAudioAttributes(
            android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private var chimeSound = 0
    private var chimeMessages = emptyList<PlayerMessage>()
    private val handler = Handler(Looper.getMainLooper())
    private val periodicSave = object : Runnable {
        override fun run() {
            if (session?.player?.isPlaying == true) savePosition()
            handler.postDelayed(this, 5_000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        library = Library(this)
        settings = Settings(this)
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

            override fun onMediaItemTransition(item: MediaItem?, reason: Int) = scheduleChimes(player, item)

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) player.currentMediaItem?.mediaId?.let { library.savePosition(it, 0) }
            }
        })
        session = MediaSession.Builder(this, player).build()
        handler.post(periodicSave)
    }

    /** Timeline messages fire as playback crosses each position, including after seeking back. */
    private fun scheduleChimes(player: ExoPlayer, item: MediaItem?) {
        chimeMessages.forEach { it.cancel() }
        if (chimeSound != 0) chimes.unload(chimeSound)
        val extras = item?.mediaMetadata?.extras
        val file = extras?.getString(CHIME_FILE)
        val positions = extras?.getLongArray(CHIME_AT_MS)
        if (file == null || positions == null) {
            chimeSound = 0
            chimeMessages = emptyList()
            return
        }
        chimeSound = chimes.load(file, 1)
        chimeMessages = positions.map { ms ->
            player.createMessage { _, _ -> if (settings.chime) chimes.play(chimeSound, 1f, 1f, 1, 0, 1f) }
                .setLooper(Looper.getMainLooper())
                .setPosition(ms)
                .setDeleteAfterDelivery(false)
                .send()
        }
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
        chimes.release()
        super.onDestroy()
    }
}
