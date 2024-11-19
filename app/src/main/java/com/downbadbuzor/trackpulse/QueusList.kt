package com.downbadbuzor.trackpulse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.downbadbuzor.trackpulse.adapters.QueueAdapter
import com.downbadbuzor.trackpulse.databinding.QueueBinding
import com.downbadbuzor.trackpulse.model.AudioModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class QueueList(
    private val fragmentManager: FragmentManager
) : BottomSheetDialogFragment() {

    private lateinit var binding: QueueBinding
    private lateinit var adapter: QueueAdapter

    private var audioList: List<AudioModel>? = null
    private var usingQueue: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = QueueBinding.inflate(inflater, container, false)

        if (MyExoPlayer.getQueue().isNullOrEmpty()) {
            audioList = MyExoPlayer.getAudioList()
            usingQueue = false

        } else {
            audioList = MyExoPlayer.getQueue()
            usingQueue = true
        }

        adapter = QueueAdapter(audioList!!, fragmentManager, usingQueue)

        binding.queueList.layoutManager = LinearLayoutManager(requireContext())
        binding.queueList.adapter = adapter


        // Scroll to current media item position

        if (!usingQueue) {
            binding.queueList.scrollToPosition(MyExoPlayer.getNextIndex())
            binding.clearBtn.visibility = View.GONE
            binding.warning.visibility = View.GONE
        }

        binding.clearBtn.setOnClickListener {
            MyExoPlayer.clearQueue()
            adapter.notifyDataSetChanged()
            dismiss()
        }


        return binding.root
    }
}