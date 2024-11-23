package com.downbadbuzor.trackpulse


import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.downbadbuzor.trackpulse.databinding.ActivityMainBinding
import com.downbadbuzor.trackpulse.model.AudioModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var audioList: ArrayList<AudioModel>
    private lateinit var bottomSheetFragment: PlayingBottomSheetFragment


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

        checkPermissions()
        // Initialize and show the bottom sheet

        binding.playingBottomSheetCoverHome.setOnClickListener {
            bottomSheetFragment = PlayingBottomSheetFragment(this, supportFragmentManager)
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }

        binding.playPauseHome.setOnClickListener {
            if (MyExoPlayer.getIsPlaying()) {
                MyExoPlayer.pause()

            } else {
                MyExoPlayer.resume()
            }
        }

        binding.goToAll.setOnClickListener {
            val intent = Intent(
                this,
                PlaylistActivity::class.java
            )
            startActivity(intent)
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
                binding.loadingIndicator.visibility = View.GONE // Hide loading indicator
                binding.scrollView.visibility = View.VISIBLE // Show the scrollable content
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