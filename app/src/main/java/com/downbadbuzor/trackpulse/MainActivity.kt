package com.downbadbuzor.trackpulse


import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.SearchView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.downbadbuzor.trackpulse.adapters.AudioAdapter
import com.downbadbuzor.trackpulse.databinding.ActivityMainBinding
import com.downbadbuzor.trackpulse.model.AudioModel
import com.downbadbuzor.trackpulse.service.AudioService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var audioAdapter: AudioAdapter
    private lateinit var audioList: ArrayList<AudioModel>
    private lateinit var bottomSheetFragment: PlayingBottomSheetFragment

    private lateinit var sortModal: SortModal
    private var searchJob: Job? = null


    private fun startService() {
        if (!MyExoPlayer.getIsServiceRunning()) {
            val intent = Intent(this, AudioService::class.java)
            startForegroundService(intent)
            MyExoPlayer.setIsServiceRunning(true)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        audioList = ArrayList()

        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        binding.subHeader.setOnClickListener {
            binding.search.clearFocus()
        }

        binding.playingBottomSheet.setOnClickListener {
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
                startService()
            }
        }

        binding.playPauseHome.setOnClickListener {
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




        checkPermissions()
        // Initialize and show the bottom sheet


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
        audioList = getAllAudioFromDevice(this)

        //sort by default: title
        audioList.sortBy { it.title }

        binding.num.text =
            if (audioList.size <= 1) "${audioList.size} track" else "${audioList.size} tracks"

        audioAdapter = AudioAdapter(this, supportFragmentManager)
        audioAdapter.addSongs(audioList)

        binding.recyclerView.adapter = audioAdapter

        MyExoPlayer.initialize(this, audioList, this)

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