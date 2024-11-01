package com.downbadbuzor.trackpulse

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
import android.widget.ImageView
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.downbadbuzor.trackpulse.databinding.BottomSheetPlayingBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayingBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetPlayingBinding
    private lateinit var exoPlayer: ExoPlayer


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = BottomSheetPlayingBinding.inflate(inflater, container, false)



        exoPlayer = MyExoPlayer.getInstance()!!

        binding.playerView.player = exoPlayer
        binding.playerView.showController()

        MyExoPlayer.getCurrentSong()?.let { currentSong ->
            if (currentSong.albumArtUri != null) {
                setUi(currentSong.title, currentSong.artist, currentSong.albumArtUri!!)
            }
        }

        if(MyExoPlayer.getIsShuffled()){
            binding.playingShuffle.setImageResource(R.drawable.playing_shuffle)
        }

        MyExoPlayer.getNextSong()?.apply {
            setUpNext(title, artist, albumArtUri!!)
        }

        if (MyExoPlayer.getIsPlaying()) {
            binding.playPause.setImageResource(R.drawable.playing_pause)
        }else{
            binding.playPause.setImageResource(R.drawable.playing_play)
        }

        //repeat mode

        when(exoPlayer.repeatMode){
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
          when(exoPlayer.repeatMode){
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
                         MyExoPlayer.getCurrentSong()?.apply {
                             setUi(title, artist, albumArtUri!!)
                         }
                        MyExoPlayer.getNextSong()?.apply {
                            setUpNext(title, artist, albumArtUri!!)
                        }

                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    super.onShuffleModeEnabledChanged(shuffleModeEnabled)

                    if (shuffleModeEnabled) {
                        MyExoPlayer.getNextSong()?.apply {
                            setUpNext(title, artist, albumArtUri!!)
                        }
                    } else {
                        MyExoPlayer.getNextSong()?.apply {
                            setUpNext(title, artist, albumArtUri!!)
                        }
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
                val currentTimeString = formatTime(exoPlayer.currentPosition)// Current position in milliseconds
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


        binding.playPause.setOnClickListener{
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
                MyExoPlayer.pause()
                binding.playPause.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.rotate_left))

            } else {
                MyExoPlayer.resume()
                binding.playPause.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.rotate_right))
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
                binding.playingShuffle.setImageResource(R.drawable.playing_shuffle_off)
            }
            else{
                MyExoPlayer.shuffle(true)
                binding.playingShuffle.setImageResource(R.drawable.playing_shuffle)
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


        return binding.root
    }

    private fun setUi(title: String, artist: String, albumArtUri: String){
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
                override fun onResourceReady(resource: Bitmap, transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?) {
                    Palette.from(resource).generate { palette ->
                        val dominantColor = palette?.getDarkMutedColor(ContextCompat.getColor(binding.playingBottomSheetMain.context, R.color.default_color))
                        val vibrantColor = palette?.getVibrantColor(ContextCompat.getColor(binding.playingBottomSheetMain.context, R.color.default_color))
                        val rippleColor = palette?.getDominantColor(ContextCompat.getColor(binding.playingBottomSheetMain.context, R.color.default_color))

                        binding.playPause.rippleColor =
                            rippleColor ?: ContextCompat.getColor(binding.playPause.context, R.color.default_color)

                        binding.playingBottomSheetMain.setBackgroundColor(dominantColor ?: ContextCompat.getColor(binding.playingBottomSheetMain.context, R.color.default_color))

                        // Fix: Check if background is not null before setting tint
                        val playPauseBackground = binding.playPause.background
                        if (playPauseBackground != null) {
                            playPauseBackground.setTint(vibrantColor ?: ContextCompat.getColor(binding.playPause.context, R.color.default_color))
                        } else {
                            // Handle the case where background is null, e.g., log a warning
                            Log.w("PlayingBottomSheetFragment", "Play/Pause button background is null")
                        }

                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Optional: Handle placeholder or clearing the image
                }
            })



    }
    private fun setUpNext(title: String, artist: String, albumArtUri: String){
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
