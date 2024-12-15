package com.downbadbuzor.trackpulse

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.downbadbuzor.trackpulse.databinding.BottomSheetPlayingBinding
import com.downbadbuzor.trackpulse.db.PlaylistViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayingBottomSheetFragment(
    private val activity: Activity,
    private val fragmentManager: FragmentManager
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetPlayingBinding
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var playlistOptionsModal: PlaylistOptionsModal
    private lateinit var playlistViewModel: PlaylistViewModel

    private val likedSongs = mutableListOf<String>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = BottomSheetPlayingBinding.inflate(inflater, container, false)

        playlistViewModel = (activity as MainActivity).playlistViewModel


        exoPlayer = MyExoPlayer.getInstance()!!

        binding.playerView.player = exoPlayer

        MyExoPlayer.getCurrentSong()?.let { currentSong ->

            setUi(
                currentSong.title.toString(),
                currentSong.artist.toString(),
                currentSong.artworkUri!!.toString(),
                currentSong.extras?.getLong("id").toString()
            )

        }

        if (MyExoPlayer.getIsShuffled()) {
            binding.playingShuffle.setImageResource(R.drawable.shuffle_on)
        }

        MyExoPlayer.getNextSong()?.apply {
            setUpNext(title, artist, albumArtUri!!)
        }

        if (MyExoPlayer.getIsPlaying()) {
            binding.playPause.setImageResource(R.drawable.playing_pause)
        } else {
            binding.playPause.setImageResource(R.drawable.playing_play)
        }

        //queue
        binding.queue.setOnClickListener {
            val bottomSheetFragment = QueueList(fragmentManager)
            bottomSheetFragment.show(childFragmentManager, bottomSheetFragment.tag)
        }

        binding.upnext.setOnClickListener {
            val bottomSheetFragment = QueueList(fragmentManager)
            bottomSheetFragment.show(childFragmentManager, bottomSheetFragment.tag)
        }


        //repeat mode

        when (exoPlayer.repeatMode) {
            Player.REPEAT_MODE_OFF -> {
                binding.playingRepeat.setImageResource(R.drawable.playing_repeat)
            }

            Player.REPEAT_MODE_ALL -> {

                binding.playingRepeat.setImageResource(R.drawable.playing_repeat_all)
            }

            Player.REPEAT_MODE_ONE -> {
                binding.playingRepeat.setImageResource(R.drawable.playing_repeat_one)
            }

        }

        binding.playingRepeat.setOnClickListener {
            when (exoPlayer.repeatMode) {
                Player.REPEAT_MODE_OFF -> {
                    MyExoPlayer.setRepeatMode("ALL")
                    binding.playingRepeat.setImageResource(R.drawable.playing_repeat_all)
                }

                Player.REPEAT_MODE_ALL -> {
                    MyExoPlayer.setRepeatMode("ONE")
                    binding.playingRepeat.setImageResource(R.drawable.playing_repeat_one)
                }

                Player.REPEAT_MODE_ONE -> {
                    MyExoPlayer.setRepeatMode("OFF")
                    binding.playingRepeat.setImageResource(R.drawable.playing_repeat)
                }

            }
        }


        // update the components as songs change
        exoPlayer.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        // Active playback.
                        binding.playPause.setImageResource(R.drawable.playing_pause)

                    } else {
                        binding.playPause.setImageResource(R.drawable.playing_play)
                        // Not playing because playback is paused, ended, suppressed, or the player
                        // is buffering, stopped or failed. Check player.playWhenReady,
                        // player.playbackState, player.playbackSuppressionReason and
                        // player.playerError for details.
                    }
                }

                override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                    super.onMediaMetadataChanged(mediaMetadata)

                    MyExoPlayer.getCurrentSong()?.let { currentSong ->
                        setUi(
                            currentSong.title.toString(),
                            currentSong.artist.toString(),
                            currentSong.artworkUri?.toString() ?: "",
                            currentSong.extras?.getLong("id").toString()
                        )
                    }
                    MyExoPlayer.getNextSong()?.let { nextSong -> // Use let for nextSong as well
                        setUpNext(
                            nextSong.title,
                            nextSong.artist,
                            nextSong.albumArtUri?.toString() ?: ""
                        )
                    }
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    super.onShuffleModeEnabledChanged(shuffleModeEnabled)

                    MyExoPlayer.getNextSong()?.apply {
                        setUpNext(title, artist, albumArtUri!!)
                    }

                    if (shuffleModeEnabled) {
                        binding.playingShuffle.setImageResource(R.drawable.shuffle_on)
                    } else {
                        binding.playingShuffle.setImageResource(R.drawable.playing_shuffle_off)
                    }

                }

            }
        )

        // SeekBar setup
        binding.seekBar.max = 1000 // Set maximum value for smoother seeking
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = exoPlayer.duration
                    val position = (duration * progress / 1000).toLong()
                    exoPlayer.seekTo(position)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Update SeekBar periodically
        lifecycleScope.launch {
            while (true) {
                val duration = exoPlayer.duration
                val position = exoPlayer.currentPosition
                val currentTimeString =
                    formatTime(exoPlayer.currentPosition)// Current position in milliseconds
                val endTimeString = formatTime(exoPlayer.duration)// Total duration in milliseconds


                if (duration > 0) {
                    val progress = (position * 1000 / duration).toInt()
                    binding.seekBar.progress = progress
                    binding.timeNow.text = currentTimeString
                    binding.timeEnd.text = endTimeString
                }
                delay(100) // Update every 100 milliseconds
            }
        }

        //add song to playlist
        binding.add.setOnClickListener {
            playlistOptionsModal = PlaylistOptionsModal(
                operation = "ADD_TO_PLAYLIST",
                songId = MyExoPlayer.getCurrentSong()?.extras?.getLong("id").toString()
            )
            playlistOptionsModal.show(
                parentFragmentManager,
                playlistOptionsModal.tag
            )
        }


        binding.playPause.setOnClickListener {
            // Trigger haptic feedback
            it.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING or
                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )

            val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))

            // Your existing click handling logic
            if (MyExoPlayer.getIsPlaying()) {
                binding.playPause.startAnimation(
                    AnimationUtils.loadAnimation(
                        activity,
                        R.anim.rotate_left
                    )
                )
                MyExoPlayer.pause()

            } else {
                binding.playPause.startAnimation(
                    AnimationUtils.loadAnimation(
                        activity,
                        R.anim.rotate_right
                    )
                )
                MyExoPlayer.resume()
            }
        }


        binding.playingNext.setOnClickListener {
            MyExoPlayer.playNextSong()

        }
        binding.playingPrevious.setOnClickListener {
            MyExoPlayer.playPreviousSong()

        }
        binding.playingShuffle.setOnClickListener {
            if (MyExoPlayer.getIsShuffled()) {
                MyExoPlayer.shuffle(false)
                // binding.playingShuffle.setImageResource(R.drawable.playing_shuffle_off)
            } else {
                // binding.playingShuffle.setImageResource(R.drawable.playing_shuffle)
                MyExoPlayer.shuffle(true)
            }
        }


        //control device volume
        val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager

        binding.seekBarVolume.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        binding.seekBarVolume.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        binding.seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


        //favorites action
        binding.fav.setOnClickListener {
            likeUnLike()
        }

        return binding.root
    }

    //update liked
    private fun updateLiked(currentSongId: String) {
        playlistViewModel.getPlaylistById(0)
            .observe(this) { playlist ->
                if (playlist.songs.isNotEmpty()) {
                    likedSongs.clear()
                    likedSongs.addAll(playlist.songs)

                    if (likedSongs.contains(currentSongId)) {
                        binding.fav.setImageResource(R.drawable.heart)
                    } else {
                        binding.fav.setImageResource(R.drawable.heart_outline)
                    }
                }
            }
    }

    private fun likeUnLike() {
        val currentSongId = MyExoPlayer.getCurrentSong()?.extras?.getLong("id").toString()

        if (likedSongs.contains(currentSongId)) {
            playlistViewModel.removeSongFromPlaylist(0, currentSongId)
        } else {
            playlistViewModel.addSongToPlaylist(0, currentSongId)
        }
    }


    private fun setUi(title: String, artist: String, albumArtUri: String, currentSongId: String) {
        binding.playingSongTitle.text = title
        binding.playingSongArtist.text = artist

        Glide.with(binding.albumArtPlaying.context)
            .load(albumArtUri)
            .placeholder(R.drawable.vinyl)
            .error(R.drawable.vinyl)
            .into(binding.albumArtPlaying)


        // Load the image using Glide
        Glide.with(binding.playingBottomSheetMain.context)
            .asBitmap()
            .error(R.color.default_color)
            .load(albumArtUri)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                ) {
                    Palette.from(resource).generate { palette ->
                        val dominantColor = palette?.getDarkMutedColor(
                            ContextCompat.getColor(
                                binding.playingBottomSheetMain.context,
                                R.color.default_color
                            )
                        )
                        val vibrantColor = palette?.getVibrantColor(
                            ContextCompat.getColor(
                                binding.playingBottomSheetMain.context,
                                R.color.default_color
                            )
                        )
                        val rippleColor = palette?.getDominantColor(
                            ContextCompat.getColor(
                                binding.playingBottomSheetMain.context,
                                R.color.default_color
                            )
                        )

                        binding.playPause.rippleColor =
                            rippleColor ?: ContextCompat.getColor(
                                binding.playPause.context,
                                R.color.default_color
                            )

                        binding.playingBottomSheetMain.setBackgroundColor(
                            dominantColor ?: ContextCompat.getColor(
                                binding.playingBottomSheetMain.context,
                                R.color.default_color
                            )
                        )

                        // Fix: Check if background is not null before setting tint
                        val playPauseBackground = binding.playPause.background
                        if (playPauseBackground != null) {
                            playPauseBackground.setTint(
                                vibrantColor ?: ContextCompat.getColor(
                                    binding.playPause.context,
                                    R.color.default_color
                                )
                            )
                        } else {
                            // Handle the case where background is null, e.g., log a warning
                            Log.w(
                                "PlayingBottomSheetFragment",
                                "Play/Pause button background is null"
                            )
                        }

                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Optional: Handle placeholder or clearing the image
                }
            })

        updateLiked(currentSongId)

    }

    private fun setUpNext(title: String, artist: String, albumArtUri: String) {
        binding.audioTitleNext.text = title
        binding.audioArtistNext.text = artist


        Glide.with(binding.albumArtNext.context)
            .load(albumArtUri)
            .placeholder(R.drawable.vinyl)
            .error(R.drawable.vinyl)
            .into(binding.albumArtNext)

        binding.upnext.visibility = View.VISIBLE
    }

    private fun formatTime(timeInMillis: Long): String {
        val totalSeconds = timeInMillis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
