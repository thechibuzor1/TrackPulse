package com.downbadbuzor.trackpulse


import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.downbadbuzor.trackpulse.adapters.PlaylistAdapter
import com.downbadbuzor.trackpulse.databinding.ActivityMainBinding
import com.downbadbuzor.trackpulse.db.Playlist
import com.downbadbuzor.trackpulse.db.PlaylistRepo
import com.downbadbuzor.trackpulse.db.PlaylistViewModel
import com.downbadbuzor.trackpulse.db.PlaylistViewModelFactory
import com.downbadbuzor.trackpulse.db.Playlistdb
import com.downbadbuzor.trackpulse.model.AudioModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var bottomSheetFragment: PlayingBottomSheetFragment

    private lateinit var playlistOptionsModal: PlaylistOptionsModal
    private lateinit var exoPlayer: ExoPlayer

    private lateinit var audioList: ArrayList<AudioModel>

    lateinit var playlistViewModel: PlaylistViewModel

    lateinit var playlistAdapter: PlaylistAdapter

    private var currentLayoutGrid: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        audioList = ArrayList()

        enableEdgeToEdge()
        setContentView(mainBinding.root)


        ViewCompat.setOnApplyWindowInsetsListener(mainBinding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }


        checkPermissions()
        setupViewModel()

        // Initialize and show the bottom sheet

        mainBinding.playingBottomSheetCoverHome.setOnClickListener {
            bottomSheetFragment = PlayingBottomSheetFragment(this, supportFragmentManager)
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }

        mainBinding.goToAll.setOnClickListener {

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PlaylistFragment.newInstance("ALL"))
                .addToBackStack("myFragmentTag") // Optional tag
                .commit()
        }
        mainBinding.createPlaylist.setOnClickListener {
            playlistOptionsModal = PlaylistOptionsModal(operation = "CREATE")
            playlistOptionsModal.show(supportFragmentManager, playlistOptionsModal.tag)

        }

        mainBinding.switchLayout.setOnClickListener {
            if (currentLayoutGrid) {
                mainBinding.playlistRecyclerView.apply {
                    //switch to list
                    layoutManager =
                        StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
                }
                mainBinding.switchLayout.setImageResource(R.drawable.grid_view)
                currentLayoutGrid = false

            } else {
                mainBinding.playlistRecyclerView.apply {
                    //switch to grid
                    layoutManager =
                        StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                }
                mainBinding.switchLayout.setImageResource(R.drawable.list_view)
                currentLayoutGrid = true
            }
        }


    }

    private fun setupViewModel() {
        val playlistRepository = PlaylistRepo(Playlistdb(this))
        val viewModelProviderFactory = PlaylistViewModelFactory(application, playlistRepository)
        playlistViewModel =
            ViewModelProvider(this, viewModelProviderFactory)[PlaylistViewModel::class.java]

        setUpRecyclerView()
    }


    // Handling back navigation in activity
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    private fun updatePlaylistUi(playlist: List<Playlist>?) {
        if (playlist.isNullOrEmpty()) {
            mainBinding.playlistLayout.visibility = View.GONE
            mainBinding.noPlaylist.visibility = View.VISIBLE


        } else {
            mainBinding.playlistLayout.visibility = View.VISIBLE
            mainBinding.noPlaylist.visibility = View.GONE
            // setUpRecyclerView()
        }

    }


    private fun setUpRecyclerView() {

        playlistAdapter = PlaylistAdapter(supportFragmentManager)
        mainBinding.playlistRecyclerView.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            setHasFixedSize(true)
            adapter = playlistAdapter
        }

        playlistViewModel.getAllPlaylists().observe(this) { playlist ->
            playlistAdapter.differ.submitList(playlist)
            updatePlaylistUi(playlist)
        }


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    super.onBackPressed()
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkPermissions() {
        val readAudioPermission = android.Manifest.permission.READ_MEDIA_AUDIO

        if (ContextCompat.checkSelfPermission(
                this,
                readAudioPermission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            loadAudioFiles()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(readAudioPermission),
                1
            )
        }

    }

    private fun loadAudioFiles() {
        lifecycleScope.launch(Dispatchers.IO) {
            audioList = getAllAudioFromDevice(this@MainActivity)
            audioList.sortBy { it.title }

            withContext(Dispatchers.Main) {
                MyExoPlayer.initialize(this@MainActivity, audioList, this@MainActivity)
                mainBinding.loadingIndicator.visibility = View.GONE // Hide loading indicator
                mainBinding.fragmentContainer.visibility =
                    View.VISIBLE // Show the scrollable content
                mainBinding.playingBottomSheetCoverHome.visibility = View.VISIBLE

                mainBinding.playPauseHome.setOnClickListener {
                    if (MyExoPlayer.getIsPlaying()) {
                        MyExoPlayer.pause()
                    } else {
                        MyExoPlayer.resume()
                    }
                }


                MyExoPlayer.getCurrentSong()?.let { currentSong ->

                    setUi(
                        currentSong.title.toString(),
                        currentSong.artist.toString(),
                        currentSong.artworkUri!!.toString()
                    )

                }


                // update the components as songs change

                exoPlayer = MyExoPlayer.getInstance()!!
                exoPlayer?.addListener(
                    object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
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
            }
        }
    }


    private fun setUi(title: String, artist: String, albumArtUri: String) {
        mainBinding.playingSongTitleHome.text = title
        mainBinding.playingSongArtistHome.text = artist

        Glide.with(mainBinding.albumArtHome.context)
            .load(albumArtUri)
            .placeholder(R.drawable.vinyl)
            .error(R.drawable.vinyl)
            .into(mainBinding.albumArtHome)


        Glide.with(mainBinding.playingBottomSheet.context)
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
                                mainBinding.playingBottomSheet.context,
                                R.color.default_color
                            )
                        )
                        mainBinding.playingBottomSheet.setBackgroundColor(
                            dominantColor ?: ContextCompat.getColor(
                                mainBinding.playingBottomSheet.context,
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

    private fun getAllAudioFromDevice(context: Context): ArrayList<AudioModel> {
        val tempAudioList = ArrayList<AudioModel>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA, // file path
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID // Add album ID for fetching album art
        )
        val cursor = context.contentResolver.query(uri, projection, null, null, null)



        if (cursor != null) {
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                val title =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                val artist =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                val data =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                val displayName =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                val duration =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                val albumId =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()
                val audio =
                    AudioModel(id, title, artist, data, displayName, duration, albumId, albumArtUri)
                tempAudioList.add(audio)
            }
            cursor.close()
        }

        return tempAudioList
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadAudioFiles()
        }
    }


}