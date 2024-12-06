package com.downbadbuzor.trackpulse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.downbadbuzor.trackpulse.adapters.AudioAdapter
import com.downbadbuzor.trackpulse.databinding.FragmentPlaylistBinding
import com.downbadbuzor.trackpulse.db.Playlist
import com.downbadbuzor.trackpulse.db.PlaylistViewModel
import com.downbadbuzor.trackpulse.model.AudioModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ARG_PARAM1 = "param1"


class PlaylistFragment : Fragment() {

    private var id: String? = null


    private lateinit var binding: FragmentPlaylistBinding
    private lateinit var audioAdapter: AudioAdapter
    private lateinit var audioList: ArrayList<AudioModel>

    private lateinit var playlistViewModel: PlaylistViewModel

    private lateinit var playlistOptionsModal: PlaylistOptionsModal

    private lateinit var sortModal: SortModal
    private var searchJob: Job? = null

    private var playlistName: String = ""
    private lateinit var playlist: Playlist


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            id = it.getString(ARG_PARAM1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentPlaylistBinding.inflate(inflater, container, false)

        playlistViewModel = (activity as MainActivity).playlistViewModel

        audioAdapter = AudioAdapter(requireActivity(), parentFragmentManager)

        (activity as? AppCompatActivity)?.let { activity ->
            activity.setSupportActionBar(binding.myToolbar)
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            activity.supportActionBar?.title = ""
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        binding.subHeader.setOnClickListener {
            binding.search.clearFocus()
        }



        binding.sort.setOnClickListener {
            binding.search.clearFocus()
            sortModal = SortModal(audioAdapter, audioList)
            sortModal.show(parentFragmentManager, sortModal.tag)
        }

        binding.giantPlay.setOnClickListener {
            binding.search.clearFocus()
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


        binding.root.post {
            lifecycleScope.launch {
                loadPlaylist(id!!)
            }
        }

        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val scrollThreshold = 1000 // Adjust this threshold as needed

            (activity as? AppCompatActivity)?.supportActionBar?.let { actionBar ->
                if (scrollY > scrollThreshold && actionBar.title != playlistName) {
                    actionBar.title = playlistName
                } else if (scrollY <= scrollThreshold && actionBar.title == playlistName) {
                    actionBar.title = "" // Reset title if scrolled back up
                }
            }
        }


        // Inflate the layout for this fragment
        return binding.root


    }


    private suspend fun loadPlaylist(id: String) {
        if (id === "ALL") {
            binding.optionsIcon.visibility = View.GONE
            withContext(Dispatchers.IO) {
                audioList = MyExoPlayer.getAllAudioList()!! as ArrayList<AudioModel>
                audioList.sortBy { it.title }
            }
            // Update UI for "ALL" case on the main thread
            withContext(Dispatchers.Main) {
                playlistName = "All Songs"
                updateUI()
            }
        } else {
              val songsInPlaylist = mutableListOf<AudioModel>()
            // Move observe() call to lifecycleScope and update UI inside the observer
            withContext(Dispatchers.Main) {
                playlistViewModel.getPlaylistById(id.toIntOrNull() ?: 0)
                    .observe(viewLifecycleOwner) { playlist ->
                         if(playlist.songs.isNotEmpty()){
                             songsInPlaylist.clear()
                             songsInPlaylist.addAll(getSongsByIds(playlist.songs, MyExoPlayer.getAllAudioList()!! as ArrayList<AudioModel>))
                             songsInPlaylist.sortBy { it.title }
                             audioList = songsInPlaylist as ArrayList<AudioModel>
                         }

                        this@PlaylistFragment.playlist = playlist
                        playlistName = playlist.name
                        // Update UI after data is loaded
                        updateUI()

                        binding.optionsIcon.setOnClickListener {
                            playlistOptionsModal = PlaylistOptionsModal(playlist, "OPTIONS")
                            playlistOptionsModal.show(
                                parentFragmentManager,
                                playlistOptionsModal.tag
                            )
                        }
                    }
            }
        }
    }
    private fun getSongsByIds(songIds: List<String>, audioList: List<AudioModel>): List<AudioModel> {
        return songIds.mapNotNull { id ->
            audioList.find { it.id.toString() == id }
        }
    }


    private fun updateUI() {
        binding.playlistTitle.text = playlistName
        if (audioList.size <= 1) {
            binding.num.text = "${audioList.size} track"
            binding.topBtns.visibility = View.GONE
            binding.topActionBtns.visibility = View.GONE
        } else {
            binding.num.text = "${audioList.size} tracks"
            binding.topBtns.visibility = View.VISIBLE
            binding.topActionBtns.visibility = View.VISIBLE
        }
        audioAdapter.updateExoplayerList(audioList)
        binding.recyclerView.adapter = audioAdapter
        binding.loadingIndicator.visibility = View.GONE
        binding.scrollView.visibility = View.VISIBLE

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


    companion object {
        @JvmStatic
        fun newInstance(param1: String) =
            PlaylistFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }
}