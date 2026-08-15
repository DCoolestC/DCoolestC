package com.isokovibe.musicplayer.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Keeps music playing while the app is backgrounded, and exposes the
 * player to the system (lock screen, notification, Bluetooth, Android Auto)
 * through a [MediaSession].
 */
class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        AudioEngine.attachPlayer(player)
        // The audio session id isn't necessarily known at build time and can
        // change during the player's life, so bind the effects from the
        // event rather than reading it once and hoping.
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(
                eventTime: AnalyticsListener.EventTime,
                audioSessionId: Int
            ) {
                AudioEngine.attachSession(audioSessionId)
            }
        })
        if (player.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            AudioEngine.attachSession(player.audioSessionId)
        }

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        // Stop playback and let the service die once the app is swiped away
        // and nothing is actively playing, matching most users' expectations.
        val session = mediaSession ?: return
        if (!session.player.playWhenReady || session.player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        AudioEngine.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
