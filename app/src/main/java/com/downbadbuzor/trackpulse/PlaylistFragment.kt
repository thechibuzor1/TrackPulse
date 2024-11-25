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
import com.downbadbuzor.trackpulse.model.AudioModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [PlaylistFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PlaylistFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var binding: FragmentPlaylistBinding
    private lateinit var audioAdapter: AudioAdapter
    private lateinit var audioList: ArrayList<AudioModel>

    private lateinit var sortModal: SortModal
    private var searchJob: Job? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPlaylistBinding.inflate(inflater, container, false)

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
                loadPlaylist()
            }
        }

        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val scrollThreshold = 1000 // Adjust this threshold as needed

            (activity as? AppCompatActivity)?.supportActionBar?.let { actionBar ->
                if (scrollY > scrollThreshold && actionBar.title != "All Songs") {
                    actionBar.title = "All Songs"
                } else if (scrollY <= scrollThreshold && actionBar.title == "All Songs") {
                    actionBar.title = "" // Reset title if scrolled back up
                }
            }
        }

        // Inflate the layout for this fragment
        return binding.root


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
            audioAdapter.updateExoplayerList(audioList)
            binding.recyclerView.adapter = audioAdapter

            // Hide loading indicator and show content
            binding.loadingIndicator.visibility = View.GONE
            binding.scrollView.visibility = View.VISIBLE
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


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment PlaylistFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            PlaylistFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}