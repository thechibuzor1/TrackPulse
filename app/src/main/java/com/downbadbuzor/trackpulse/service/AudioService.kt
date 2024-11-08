package com.downbadbuzor.trackpulse.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.downbadbuzor.trackpulse.MyExoPlayer


class AudioService : MediaSessionService() {
    private lateinit var mediaSession: MediaSession

    private lateinit var notificationManager: AudioNotificationManager

    @UnstableApi
    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSession.Builder(applicationContext, MyExoPlayer.getPlayer()).build()
        notificationManager = AudioNotificationManager(
            this,
            MyExoPlayer.getInstance()!!,
        )

        // Register broadcast receiver
        val filter = IntentFilter("UPDATE_SERVICE_DATA")
        registerReceiver(dataUpdateReceiver, filter, RECEIVER_NOT_EXPORTED)
    }

    @UnstableApi
    private val dataUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "UPDATE_SERVICE_DATA") {
                // Refresh notification
                notificationManager.startNotificationService(
                    mediaSession = mediaSession,
                    mediaSessionService = this@AudioService
                )
            }
        }
    }


    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    @UnstableApi
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        notificationManager.startNotificationService(
            mediaSession = mediaSession,
            mediaSessionService = this
        )

        return super.onStartCommand(intent, flags, startId)
    }

    @UnstableApi
    override fun onDestroy() {
        super.onDestroy()
        mediaSession.apply {
            release()
            if (player.playbackState != Player.STATE_IDLE) {
                player.seekTo(0)
                player.playWhenReady = false
                player.stop()
            }
            MyExoPlayer.releasePlayer()
        }
        // Unregister broadcast receiver
        unregisterReceiver(dataUpdateReceiver)
    }

}