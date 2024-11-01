package com.downbadbuzor.trackpulse


import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.SearchView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.launch
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.text.contains
import kotlin.text.filter
import kotlin.text.lowercase

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var audioAdapter: AudioAdapter
    private lateinit var audioList: ArrayList<AudioModel>
    private lateinit var bottomSheetFragment: PlayingBottomSheetFragment

    private lateinit var sortModal: SortModal
    private var searchJob: Job? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)



        binding.playingBottomSheet.setOnClickListener {
            MyExoPlayer.getCurrentSong()?.apply {
                bottomSheetFragment = PlayingBottomSheetFragment()
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)

            }
           }

        binding.sort.setOnClickListener {
            sortModal = SortModal(audioAdapter, audioList)
            sortModal.show(supportFragmentManager, sortModal.tag)
        }

        binding.giantPlay.setOnClickListener{
            if (MyExoPlayer.getIsPlaying()) {
                MyExoPlayer.pause()

            }else{
                MyExoPlayer.resume()
            }
        }

        binding.playPauseHome.setOnClickListener{
            if (MyExoPlayer.getIsPlaying()) {
                MyExoPlayer.pause()

            }else{
                MyExoPlayer.resume()
            }
        }

        binding.shuffle.setOnClickListener {
            if (MyExoPlayer.getIsShuffled()) {
                 MyExoPlayer.shuffle(false)
            }
            else{
                MyExoPlayer.shuffle(true)
            }
        }


        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
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

        checkPermissions()
        // Initialize and show the bottom sheet


    }






    private fun checkPermissions(){
        val readAudioPermission = android.Manifest.permission.READ_MEDIA_AUDIO

        if (ContextCompat.checkSelfPermission(this, readAudioPermission) == PackageManager.PERMISSION_GRANTED) {
            loadAudioFiles()
        }else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(readAudioPermission),
                1
            )
        }

    }

    private fun loadAudioFiles(){
        audioList = getAllAudioFromDevice(this)

        //sort by default: title
        audioList.sortBy { it.title }

        binding.num.text = "${audioList.size} tracks"

        audioAdapter = AudioAdapter(this)
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
                val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                val data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                val albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()
                val audio = AudioModel(id, title, artist, data, displayName, duration, albumId, albumArtUri)
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