package com.downbadbuzor.trackpulse


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.core.content.ContextCompat.startForegroundService
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.downbadbuzor.trackpulse.model.AudioModel
import com.downbadbuzor.trackpulse.service.AudioService


object MyExoPlayer {

    private var exoPlayer: ExoPlayer? = null
    private var currentSong: MediaMetadata? = null

    private var audioList: List<AudioModel>? = null
    private var allAudioList: List<AudioModel>? = null

    private lateinit var applicationContext: Context // Add applicationContext

    private var isPlaying = false
    private var currentPlaylistPlaying = "NONE"

    fun setCurrentPlaylistPlaying(playlist: String) {
        currentPlaylistPlaying = playlist
    }

    fun getCurrentPlaylistPlaying(): String {
        return currentPlaylistPlaying
    }


    private var isServiceRunning = false
    fun getIsServiceRunning(): Boolean {
        return isServiceRunning
    }

    fun setIsServiceRunning(isRunning: Boolean) {
        isServiceRunning = isRunning
    }

    private fun startService(activity: Activity) {
        if (!this@MyExoPlayer.getIsServiceRunning()) {
            val intent = Intent(activity, AudioService::class.java) // Use activity context
            startForegroundService(activity, intent) // Start foreground service
            this@MyExoPlayer.setIsServiceRunning(true) // Update service state
        }
    }


    private var isShuffled = false

    private var sorting = "DEFAULT"
    private var pendingAudioList: List<AudioModel>? = null // Store the pending list


    //queue
    private var queue: MutableList<AudioModel>? = mutableListOf()

    private var originalPosition: Int = 0 // Store original playlist position
    private var originalIndexChanged: Boolean = false
    private var originalSequence = true // Flag for playlist interrupts

    fun getQueue(): List<AudioModel>? {
        return this@MyExoPlayer.queue

    }

    fun addToQueue(it: AudioModel) {

        if (queue?.isEmpty() == true) {
            queue?.add(it)

        } else {
            queue?.add(queue?.size!!, it)
        }

    }

    fun removeFromQueue(song: AudioModel) {
        queue?.remove(song)
    }

    fun clearQueue() {
        queue?.clear()
    }


    fun getAudioList(): List<AudioModel>? {
        return this@MyExoPlayer.audioList
    }

    fun getAllAudioList(): List<AudioModel>? {
        return this@MyExoPlayer.allAudioList
    }

    fun getPlayer(): Player {
        return exoPlayer!!
    }

    fun getNextIndex(): Int {
        return if (!originalIndexChanged) {
            exoPlayer?.nextMediaItemIndex!!
        } else {
            originalPosition
        }
    }


    fun initialize(
        context: Context,
        audioList: ArrayList<AudioModel>,
        activity: Activity,
    ) {
        applicationContext = context.applicationContext // Initialize applicationContext
        this.audioList = audioList
        this.allAudioList = audioList



        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(applicationContext).build()
            exoPlayer?.addListener(object : Player.Listener {

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        if (queue!!.isNotEmpty()) {

                            // Re-initialize ExoPlayer with the new list
                            if (exoPlayer != null) {
                                exoPlayer?.stop()
                                exoPlayer?.clearMediaItems()
                            }
                            val upNext = queue?.removeAt(0)
                            upNext?.apply {
                                val mediaMetadataUpNext = MediaMetadata.Builder()
                                    .setTitle(title)
                                    .setArtist(artist)
                                    .setArtworkUri(albumArtUri?.toUri())
                                    .setExtras(Bundle().apply { putLong("id", id) })
                                    .build()

                                val item =
                                    MediaItem.Builder().setUri(data)
                                        .setMediaMetadata(mediaMetadataUpNext)
                                        .build()

                                exoPlayer?.setMediaItem(item)
                                exoPlayer?.prepare()
                                exoPlayer?.play()
                            }
                        }
                    }


                }


                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)


                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                        playNextSong()

                    }
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                        if (exoPlayer?.repeatMode == Player.REPEAT_MODE_ALL) {
                            when (originalSequence) {
                                true -> {
                                    when (queue?.isNotEmpty()) {
                                        true -> {
                                            originalSequence = false
                                            if (!originalIndexChanged) {
                                                originalPosition =
                                                    exoPlayer?.currentMediaItemIndex!!
                                                originalIndexChanged = true
                                            }
                                            pendingAudioList = this@MyExoPlayer.audioList

                                            // Re-initialize ExoPlayer with the new list
                                            if (exoPlayer != null) {
                                                exoPlayer?.stop()
                                                exoPlayer?.clearMediaItems()
                                            }

                                            val upNext = queue?.removeAt(0)
                                            upNext?.apply {
                                                val mediaMetadataUpNext = MediaMetadata.Builder()
                                                    .setTitle(title)
                                                    .setArtist(artist)
                                                    .setArtworkUri(albumArtUri?.toUri())
                                                    .setExtras(Bundle().apply { putLong("id", id) })
                                                    .build()

                                                val item =
                                                    MediaItem.Builder().setUri(data)
                                                        .setMediaMetadata(mediaMetadataUpNext)
                                                        .build()

                                                exoPlayer?.setMediaItem(item)
                                                exoPlayer?.prepare()
                                                exoPlayer?.play()
                                            }


                                        }

                                        false -> {
                                            exoPlayer?.seekToNext()
                                            exoPlayer?.play()

                                        }

                                        null -> {
                                            exoPlayer?.seekToNext()
                                            exoPlayer?.play()

                                        }
                                    }
                                }

                                false -> {
                                    when (queue?.isNotEmpty()) {
                                        true -> {
                                            originalSequence = false
                                            if (!originalIndexChanged) {
                                                originalPosition = exoPlayer?.nextMediaItemIndex!!
                                                originalIndexChanged = true
                                            }
                                            pendingAudioList = this@MyExoPlayer.audioList

                                            // Re-initialize ExoPlayer with the new list
                                            if (exoPlayer != null) {
                                                exoPlayer?.stop()
                                                exoPlayer?.clearMediaItems()
                                            }

                                            val upNext = queue?.removeAt(0)
                                            upNext?.apply {
                                                val mediaMetadataUpNext = MediaMetadata.Builder()
                                                    .setTitle(title)
                                                    .setArtist(artist)
                                                    .setArtworkUri(albumArtUri?.toUri())
                                                    .setExtras(Bundle().apply { putLong("id", id) })
                                                    .build()

                                                val item =
                                                    MediaItem.Builder().setUri(data)
                                                        .setMediaMetadata(mediaMetadataUpNext)
                                                        .build()

                                                exoPlayer?.setMediaItem(item)
                                                exoPlayer?.prepare()
                                                exoPlayer?.play()
                                            }

                                        }

                                        false -> {
                                            if (originalPosition != -1) {
                                                // Check if there's a pending list
                                                if (pendingAudioList != null) {

                                                    // Re-initialize ExoPlayer with the new list
                                                    if (exoPlayer != null) {
                                                        exoPlayer?.stop()
                                                        exoPlayer?.clearMediaItems()
                                                    }
                                                    val mediaItems =
                                                        pendingAudioList!!.mapIndexed { _, audioModel ->
                                                            val mediaMetadata =
                                                                MediaMetadata.Builder()
                                                                    .setTitle(audioModel.title) // Set other metadata as needed
                                                                    .setArtist(audioModel.artist)
                                                                    .setArtworkUri(audioModel.albumArtUri?.toUri())
                                                                    .setExtras(Bundle().apply {
                                                                        putLong(
                                                                            "id",
                                                                            audioModel.id
                                                                        )
                                                                    }) // Store ID in extras
                                                                    .build()

                                                            MediaItem.Builder()
                                                                .setUri(audioModel.data)
                                                                .setMediaMetadata(mediaMetadata)
                                                                .build()
                                                        }


                                                    exoPlayer?.setMediaItems(mediaItems)
                                                    exoPlayer?.prepare()

                                                    exoPlayer?.seekTo(originalPosition, 0)
                                                    exoPlayer?.play()
                                                }
                                                originalSequence = true
                                                originalIndexChanged = false

                                            }
                                        }

                                        null -> {
                                            if (originalPosition != -1) {
                                                // Check if there's a pending list
                                                if (pendingAudioList != null) {

                                                    // Re-initialize ExoPlayer with the new list
                                                    if (exoPlayer != null) {
                                                        exoPlayer?.stop()
                                                        exoPlayer?.clearMediaItems()
                                                    }
                                                    val mediaItems =
                                                        pendingAudioList!!.mapIndexed { _, audioModel ->
                                                            val mediaMetadata =
                                                                MediaMetadata.Builder()
                                                                    .setTitle(audioModel.title) // Set other metadata as needed
                                                                    .setArtist(audioModel.artist)
                                                                    .setArtworkUri(audioModel.albumArtUri?.toUri())
                                                                    .setExtras(Bundle().apply {
                                                                        putLong(
                                                                            "id",
                                                                            audioModel.id
                                                                        )
                                                                    }) // Store ID in extras
                                                                    .build()

                                                            MediaItem.Builder()
                                                                .setUri(audioModel.data)
                                                                .setMediaMetadata(mediaMetadata)
                                                                .build()
                                                        }


                                                    exoPlayer?.setMediaItems(mediaItems)
                                                    exoPlayer?.prepare()

                                                    exoPlayer?.seekTo(originalPosition, 0)
                                                    exoPlayer?.play()
                                                }
                                                originalSequence = true
                                                originalIndexChanged = false

                                            }
                                            originalSequence = true
                                            originalIndexChanged = false

                                        }
                                    }
                                }
                            }
                        }

                    }


                    currentSong = mediaItem?.mediaMetadata


                }

                override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                    super.onMediaMetadataChanged(mediaMetadata)

                    currentSong = mediaMetadata

                }


                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    //giant pause play
                    //val giantplay = activity.findViewById<ImageView>(R.id.giant_play)
                    val play_pause_home = activity.findViewById<ImageView>(R.id.play_pause_home)

                    if (isPlaying) {
                        this@MyExoPlayer.isPlaying = true
                        // giantplay.setImageResource(R.drawable.giant_pause)
                        play_pause_home.setImageResource(R.drawable.round_pause_24)
                        this@MyExoPlayer.startService(activity)

                    } else {
                        // giantplay.setImageResource(R.drawable.giant_play)
                        play_pause_home.setImageResource(R.drawable.bottom_play)
                        this@MyExoPlayer.isPlaying = false
                    }


                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    super.onShuffleModeEnabledChanged(shuffleModeEnabled)
                    val shuffle = activity.findViewById<ImageView>(R.id.shuffle)
                    if (shuffleModeEnabled) {
                        this@MyExoPlayer.isShuffled = true
                        shuffle.setImageResource(R.drawable.shuffle_on)
                    } else {
                        this@MyExoPlayer.isShuffled = false
                        shuffle.setImageResource(R.drawable.shuffle)
                    }

                }

            })


            // Media Session setup

            val mediaItems = audioList.mapIndexed { _, audioModel ->
                val mediaMetadata = MediaMetadata.Builder()
                    .setTitle(audioModel.title) // Set other metadata as needed
                    .setArtist(audioModel.artist)
                    .setArtworkUri(audioModel.albumArtUri?.toUri())
                    .setExtras(Bundle().apply {
                        putLong(
                            "id",
                            audioModel.id
                        )
                    }) // Store ID in extras
                    .build()

                MediaItem.Builder()
                    .setUri(audioModel.data)
                    .setMediaMetadata(mediaMetadata)
                    .build()
            }


            exoPlayer?.setMediaItems(mediaItems)
            exoPlayer?.prepare()
            exoPlayer?.repeatMode = Player.REPEAT_MODE_ALL

        }

    }

    private fun restorePlaylist(index: Int) {
        if (index != -1) {
            // Check if there's a pending list
            if (pendingAudioList != null) {

                // Re-initialize ExoPlayer with the new list
                if (exoPlayer != null) {
                    exoPlayer?.stop()
                    exoPlayer?.clearMediaItems()
                }
                val mediaItems = pendingAudioList!!.mapIndexed { _, audioModel ->
                    val mediaMetadata = MediaMetadata.Builder()
                        .setTitle(audioModel.title) // Set other metadata as needed
                        .setArtist(audioModel.artist)
                        .setArtworkUri(audioModel.albumArtUri?.toUri())
                        .setExtras(Bundle().apply {
                            putLong(
                                "id",
                                audioModel.id
                            )
                        }) // Store ID in extras
                        .build()

                    MediaItem.Builder()
                        .setUri(audioModel.data)
                        .setMediaMetadata(mediaMetadata)
                        .build()
                }


                exoPlayer?.setMediaItems(mediaItems)
                exoPlayer?.prepare()

                exoPlayer?.seekTo(index, 0)
                exoPlayer?.seekToNext()
                exoPlayer?.play()
            }

        }
    }


    fun playFromHere(index: Int) {
        if (index != -1) {
            // Check if there's a pending list
            if (pendingAudioList != null) {
                this.audioList = pendingAudioList // Update the audioList
                pendingAudioList = null // Clear the pending list

                // Re-initialize ExoPlayer with the new list
                if (exoPlayer != null) {
                    exoPlayer?.stop()
                    exoPlayer?.clearMediaItems()
                }

                val mediaItems = this.audioList!!.mapIndexed { _, audioModel ->
                    val mediaMetadata = MediaMetadata.Builder()
                        .setTitle(audioModel.title) // Set other metadata as needed
                        .setArtist(audioModel.artist)
                        .setArtworkUri(audioModel.albumArtUri?.toUri())
                        .setExtras(Bundle().apply {
                            putLong(
                                "id",
                                audioModel.id
                            )
                        }) // Store ID in extras
                        .build()

                    MediaItem.Builder()
                        .setUri(audioModel.data)
                        .setMediaMetadata(mediaMetadata)
                        .build()
                }


                exoPlayer?.setMediaItems(mediaItems)
                exoPlayer?.prepare()
            }



            exoPlayer?.seekTo(index, 0)
            exoPlayer?.play()


        }

    }

    fun updateAudioList(newAudioList: List<AudioModel>) {
        pendingAudioList = newAudioList // Store the new list as pending
    }


    fun pause() {
        exoPlayer?.pause()
    }

    fun resume() {
        exoPlayer?.play()
    }

    fun shuffle(mode: Boolean) {
        exoPlayer?.shuffleModeEnabled = mode
    }

    fun setRepeatMode(mode: String) {
        when (mode) {
            "ALL" -> {
                exoPlayer?.repeatMode = Player.REPEAT_MODE_ALL
            }

            "OFF" -> {
                exoPlayer?.repeatMode = Player.REPEAT_MODE_OFF
            }

            "ONE" -> {
                exoPlayer?.repeatMode = Player.REPEAT_MODE_ONE
            }
        }

    }


    fun releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
            exoPlayer?.release()
            exoPlayer = null
        }
    }


    fun getNextSong(): AudioModel? {
        //val nextIndex = exoPlayer?.nextMediaItemIndex
        //return if (nextIndex != null && audioList != null && nextIndex in audioList!!.indices) {
        //audioList!![nextIndex]
        //} else {
        // null
        //}
        return if (queue?.isNotEmpty() == true) {
            queue?.get(0)
        } else {
            val nextIndex = getNextIndex()

            if (
                nextIndex != null &&
                this@MyExoPlayer.audioList != null &&
                nextIndex in this@MyExoPlayer.audioList!!.indices
            ) {
                this@MyExoPlayer.audioList?.get(nextIndex)
            } else {
                null
            }
        }

    }


    fun getIsPlaying(): Boolean {
        return this@MyExoPlayer.isPlaying
    }

    fun getIsShuffled(): Boolean {
        return this@MyExoPlayer.isShuffled
    }

    fun getCurrentSorting(): String {
        return this@MyExoPlayer.sorting
    }

    fun setCurrentSorting(sorting: String) {
        this@MyExoPlayer.sorting = sorting
    }


    fun playPreviousSong() {
        exoPlayer?.seekToPrevious()
        exoPlayer?.play()

    }

    fun playNextSong() {

        when (originalSequence) {
            true -> {
                when (queue?.isNotEmpty()) {
                    true -> {
                        originalSequence = false
                        if (!originalIndexChanged) {
                            originalPosition = exoPlayer?.currentMediaItemIndex!!
                            originalIndexChanged = true
                        }
                        pendingAudioList = this@MyExoPlayer.audioList

                        // Re-initialize ExoPlayer with the new list
                        if (exoPlayer != null) {
                            exoPlayer?.stop()
                            exoPlayer?.clearMediaItems()
                        }

                        val upNext = queue?.removeAt(0)
                        upNext?.apply {
                            val mediaMetadataUpNext = MediaMetadata.Builder()
                                .setTitle(title)
                                .setArtist(artist)
                                .setArtworkUri(albumArtUri?.toUri())
                                .setExtras(Bundle().apply { putLong("id", id) })
                                .build()

                            val item =
                                MediaItem.Builder().setUri(data)
                                    .setMediaMetadata(mediaMetadataUpNext).build()

                            exoPlayer?.setMediaItem(item)
                            exoPlayer?.prepare()
                            exoPlayer?.play()
                        }


                    }

                    false -> {
                        exoPlayer?.seekToNext()
                        exoPlayer?.play()

                    }

                    null -> {
                        exoPlayer?.seekToNext()
                        exoPlayer?.play()

                    }
                }
            }

            false -> {
                when (queue?.isNotEmpty()) {
                    true -> {
                        originalSequence = false
                        if (!originalIndexChanged) {
                            originalPosition = exoPlayer?.nextMediaItemIndex!!
                            originalIndexChanged = true
                        }
                        pendingAudioList = this@MyExoPlayer.audioList

                        // Re-initialize ExoPlayer with the new list
                        if (exoPlayer != null) {
                            exoPlayer?.stop()
                            exoPlayer?.clearMediaItems()
                        }

                        val upNext = queue?.removeAt(0)
                        upNext?.apply {
                            val mediaMetadataUpNext = MediaMetadata.Builder()
                                .setTitle(title)
                                .setArtist(artist)
                                .setArtworkUri(albumArtUri?.toUri())
                                .setExtras(Bundle().apply { putLong("id", id) })
                                .build()

                            val item =
                                MediaItem.Builder().setUri(data)
                                    .setMediaMetadata(mediaMetadataUpNext).build()

                            exoPlayer?.setMediaItem(item)
                            exoPlayer?.prepare()
                            exoPlayer?.play()
                        }

                    }

                    false -> {
                        restorePlaylist(originalPosition)
                        originalSequence = true
                        originalIndexChanged = false

                    }

                    null -> {
                        restorePlaylist(originalPosition)
                        originalSequence = true
                        originalIndexChanged = false

                    }
                }
            }
        }

    }


    fun getInstance(): ExoPlayer? {
        return exoPlayer
    }

    fun getCurrentSong(): MediaMetadata? {
        return currentSong
    }


}




