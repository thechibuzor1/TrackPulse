package com.downbadbuzor.trackpulse


import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.downbadbuzor.trackpulse.model.AudioModel

object MyExoPlayer{

    private var exoPlayer: ExoPlayer? = null
    private var currentSong : AudioModel? = null
    private var audioList : List<AudioModel>? = null
    private lateinit var applicationContext: Context // Add applicationContext

    private var isPlaying = false
    private var isShuffled = false

    private var sorting = "DEFAULT"
    private var pendingAudioList: List<AudioModel>? = null // Store the pending list





    fun initialize(context: Context, audioList: ArrayList<AudioModel>, activity: Activity) {
        applicationContext = context.applicationContext // Initialize applicationContext
        this.audioList = audioList


        if (exoPlayer == null) {
                exoPlayer = ExoPlayer.Builder(applicationContext).build()
                exoPlayer?.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        super.onPlaybackStateChanged(playbackState)


                    }


                    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                        super.onMediaMetadataChanged(mediaMetadata)

                        val currentItemIndex = exoPlayer?.currentMediaItemIndex
                        val currentTimeLine = exoPlayer?.currentTimeline

                        if (currentItemIndex != null && currentItemIndex != -1 && currentTimeLine!!.isEmpty.not()){
                            currentSong = audioList?.get(currentItemIndex!!.toInt())
                            currentSong?.apply {
                                this.albumArtUri?.let { setDiv(activity, this.title , this.artist, it) }
                            }
                        }

                    }


                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        super.onIsPlayingChanged(isPlaying)
                        //giant pause play
                        val giantplay = activity.findViewById<ImageView>(R.id.giant_play)
                        val play_pause_home = activity.findViewById<ImageView>(R.id.play_pause_home)

                        if (isPlaying) {
                            this@MyExoPlayer.isPlaying = true
                            giantplay.setImageResource(R.drawable.giant_pause)
                            play_pause_home.setImageResource(R.drawable.round_pause_24)
                        } else {
                            giantplay.setImageResource(R.drawable.giant_play)
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


                val mediaItems = audioList.map { MediaItem.fromUri(it.data) }
                exoPlayer?.setMediaItems(mediaItems, false)
                exoPlayer?.prepare()

            }

    }




    fun playFromHere(index: Int, activity: Activity) {
        if ( index != -1) {
            // Check if there's a pending list
            if (pendingAudioList != null) {
                this.audioList = pendingAudioList // Update the audioList
                pendingAudioList = null // Clear the pending list

                // Re-initialize ExoPlayer with the new list
                if (exoPlayer != null){
                    exoPlayer?.stop()
                    exoPlayer?.clearMediaItems()
                    exoPlayer?.release()
                    exoPlayer = null
                }



                initialize(applicationContext, audioList as ArrayList<AudioModel>, activity)

                val mediaItems = audioList!!.map { MediaItem.fromUri(it.data) }
                exoPlayer?.setMediaItems(mediaItems, false)
                exoPlayer?.prepare()
            }

            exoPlayer?.seekTo(index, 0)
            exoPlayer?.play()

            currentSong = audioList?.get(index)

            currentSong.apply {
                if (this != null) {
                    this.albumArtUri?.let { setDiv(activity, this.title , this.artist, it) }
                }
            }


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
    fun shuffle(mode: Boolean){
        exoPlayer?.shuffleModeEnabled = mode
    }

    fun setRepeatMode(mode: String){
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



    fun addToQueue(song: AudioModel) {
        audioList?.let {
            val newList = it.toMutableList()
            newList.add(song)
            //exoPlayer?.addMediaItem(MediaItem.fromUri(song.data))

            }
    }


    fun getNextSong(): AudioModel? {
        val nextIndex = exoPlayer?.nextMediaItemIndex
        return if (nextIndex != null && audioList != null && nextIndex in audioList!!.indices) {
            audioList!![nextIndex]
        } else {
            null
        }
    }


    fun getIsPlaying(): Boolean {
        return  this@MyExoPlayer.isPlaying
    }
    fun getIsShuffled(): Boolean {
        return  this@MyExoPlayer.isShuffled
    }
    fun getCurrentSorting(): String{
        return this@MyExoPlayer.sorting
    }
    fun setCurrentSorting(sorting: String){
        this@MyExoPlayer.sorting = sorting
    }




    fun playPreviousSong() {
        exoPlayer?.seekToPrevious()
        exoPlayer?.play()
    }

    fun playNextSong() {
        exoPlayer?.seekToNext()
        exoPlayer?.play()
    }



    fun playNextSong(activity: Activity) {
        val currentSongIndex = audioList?.indexOf(currentSong)
        val nextSongIndex = audioList?.let { (currentSongIndex?.plus(1))?.rem(it.size) }
        val nextSong = nextSongIndex?.let { audioList?.get(it) }
        currentSong = nextSong
        if (nextSong != null) {
            startPlaying(nextSong, activity)
        }
    }


    fun getInstance(): ExoPlayer? {
        return exoPlayer
    }
    fun  getCurrentSong(): AudioModel? {
        return currentSong
    }
    fun setDiv(activity: Activity, title: String, artistName: String, albumArtUri: String){
        val playing =  activity.findViewById<TextView>(R.id.playing_song_title_home)
        val artist =  activity.findViewById<TextView>(R.id.playing_song_artist_home)
        val album =  activity.findViewById<ImageView>(R.id.albumArt_home)
        val div =  activity.findViewById<View>(R.id.playing_bottom_sheet)






        playing.text = title
        artist.text = artistName


        Glide.with(album.context)
            .load(albumArtUri)
            .placeholder(R.drawable.vinyl)
            .error(R.drawable.vinyl)
            .into(album)



        // Load the image using Glide
        Glide.with(div.context)
            .asBitmap()
            .error(R.color.default_color)
            .load(albumArtUri)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?) {
                    Palette.from(resource).generate { palette ->
                        val dominantColor = palette?.getDarkMutedColor(ContextCompat.getColor(div.context, R.color.default_color))
                        div.setBackgroundColor(dominantColor ?: ContextCompat.getColor(div.context, R.color.default_color))
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Optional: Handle placeholder or clearing the image
                }
            })

        div.visibility = View.VISIBLE

    }

    fun startPlaying(song: AudioModel, activity: Activity) {
        if (exoPlayer == null)
            exoPlayer = ExoPlayer.Builder(applicationContext).build()

        if (currentSong != song) {
            currentSong = song
            currentSong?.data.apply {
                val mediaItem = MediaItem.fromUri(this!!)
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
                exoPlayer?.play()
                currentSong.apply {
                    if (this != null) {
                        this.albumArtUri?.let { setDiv(activity, this.title , this.artist, it) }
                    }
                }
            }
        }
        else{
            exoPlayer?.pause()
        }

    }
}
