package com.downbadbuzor.trackpulse

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.downbadbuzor.trackpulse.adapters.AudioAdapter
import com.downbadbuzor.trackpulse.databinding.ActivityPlaylistBinding
import com.downbadbuzor.trackpulse.model.AudioModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistActivity : AppCompatActivity() {

    lateinit var binding: ActivityPlaylistBinding

    private lateinit var bottomSheetFragment: PlayingBottomSheetFragment
    private lateinit var audioAdapter: AudioAdapter
    private lateinit var audioList: ArrayList<AudioModel>

    private lateinit var exoPlayer: ExoPlayer

    private lateinit var sortModal: SortModal
    private var searchJob: Job? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exoPlayer = MyExoPlayer.getInstance()!!

        binding = ActivityPlaylistBinding.inflate(layoutInflater, null, false)
        audioAdapter = AudioAdapter(this, supportFragmentManager)

        setContentView(binding.root)
        enableEdgeToEdge()

        setSupportActionBar(binding.myToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        MyExoPlayer.getCurrentSong()?.let { currentSong ->

            setUi(
                currentSong.title.toString(),
                currentSong.artist.toString(),
                currentSong.artworkUri!!.toString()
            )

        }

        if (MyExoPlayer.getIsPlaying()) {
            binding.giantPlay.setImageResource(R.drawable.giant_pause)
            binding.playPausePlaylist.setImageResource(R.drawable.round_pause_24)
        } else {
            binding.giantPlay.setImageResource(R.drawable.giant_play)
            binding.playPausePlaylist.setImageResource(R.drawable.bottom_play)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        binding.subHeader.setOnClickListener {
            binding.search.clearFocus()
        }

        binding.playingBottomSheetPlaylist.setOnClickListener {
            binding.search.clearFocus()
            bottomSheetFragment = PlayingBottomSheetFragment(this, supportFragmentManager)
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }

        binding.sort.setOnClickListener {
            binding.search.clearFocus()
            sortModal = SortModal(audioAdapter, audioList)
            sortModal.show(supportFragmentManager, sortModal.tag)
        }

        binding.giantPlay.setOnClickListener {
            binding.search.clearFocus()
            if (MyExoPlayer.getIsPlaying()) {
                MyExoPlayer.pause()

            } else {
                MyExoPlayer.resume()
            }
        }
        binding.playPausePlaylist.setOnClickListener {
            if (MyExoPlayer.getIsPlaying()) {
                MyExoPlayer.pause()

            } else {
                MyExoPlayer.resume()
            }

        }

        binding.shuffle.setOnClickListener {
            binding.search.clearFocus()
            if (MyExoPlayer.getIsShuffled()) {
                MyExoPlayer.shuffle(false)
            } else {
                MyExoPlayer.shuffle(true)
            }
        }


        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {

                binding.search.clearFocus()
                return false // Don't handle submit, just use onQueryTextChange
            }


            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300) // Debounce delay (300ms)
                    newText?.let { filterSongs(it) }
                }
                return true
            }
        })
        binding.search.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                binding.search.clearFocus()
            }
        }
        binding.search.setOnClickListener {
            binding.search.clearFocus()
        }
        binding.container.setOnClickListener {
            binding.search.clearFocus()
        }


        // update the components as songs change
        exoPlayer.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        // Active playback.
                        binding.giantPlay.setImageResource(R.drawable.giant_pause)
                        binding.playPausePlaylist.setImageResource(R.drawable.round_pause_24)


                    } else {
                        binding.playPausePlaylist.setImageResource(R.drawable.bottom_play)
                        binding.giantPlay.setImageResource(R.drawable.giant_play)
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
                            currentSong.artworkUri?.toString() ?: ""
                        )
                    }

                }


            }
        )

        binding.root.post {
            lifecycleScope.launch {
                loadPlaylist()
            }
        }
    }


    private suspend fun loadPlaylist() {
        withContext(Dispatchers.IO) {
            audioList = MyExoPlayer.getAudioList()!! as ArrayList<AudioModel>
            audioList.sortBy { it.title }
        }

        withContext(Dispatchers.Main) {
            // Update UI on the main thread
            binding.num.text =
                if (audioList.size <= 1) "${audioList.size} track" else "${audioList.size} tracks"
            audioAdapter.updateExoplayerListFromSearch(audioList)
            binding.recyclerView.adapter = audioAdapter

            // Hide loading indicator and show content
            binding.loadingIndicator.visibility = View.GONE
            binding.scrollView.visibility = View.VISIBLE
        }
    }

    private fun setUi(title: String, artist: String, albumArtUri: String) {
        binding.playingSongTitlePlaylist.text = title
        binding.playingSongArtistPlaylist.text = artist

        Glide.with(binding.albumArtPlaylist.context)
            .load(albumArtUri)
            .placeholder(R.drawable.vinyl)
            .error(R.drawable.vinyl)
            .into(binding.albumArtPlaylist)


        Glide.with(binding.playingBottomSheetPlaylist.context)
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
                                binding.playingBottomSheetPlaylist.context,
                                R.color.default_color
                            )
                        )
                        binding.playingBottomSheetPlaylist.setBackgroundColor(
                            dominantColor ?: ContextCompat.getColor(
                                binding.playingBottomSheetPlaylist.context,
                                R.color.default_color
                            )
                        )
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Optional: Handle placeholder or clearing the image
                }
            })


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun filterSongs(query: String) {
        lifecycleScope.launch(Dispatchers.Default) {
            val filteredList = audioList.filter { song ->
                song.title.lowercase().contains(query.lowercase()) ||
                        song.artist.lowercase().contains(query.lowercase())
            }
            withContext(Dispatchers.Main) {
                // Update adapter on the main thread
                audioAdapter.updateExoplayerListFromSearch(filteredList)

                binding.num.text =
                    if (filteredList.size <= 1) "${filteredList.size} track" else "${filteredList.size} tracks"
            }
        }
    }
}