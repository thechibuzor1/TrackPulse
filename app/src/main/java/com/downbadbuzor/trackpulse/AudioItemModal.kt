package com.downbadbuzor.trackpulse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.downbadbuzor.trackpulse.Utils.UiUtils
import com.downbadbuzor.trackpulse.databinding.AudioItemModalBinding
import com.downbadbuzor.trackpulse.db.PlaylistViewModel
import com.downbadbuzor.trackpulse.model.AudioModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AudioItemModal(
    private val item: AudioModel,
    private val queueItem: Boolean,
    private val playlistId: String? = null
) : BottomSheetDialogFragment() {

    private lateinit var binding: AudioItemModalBinding
    private lateinit var playlistOptionsModal: PlaylistOptionsModal
    private lateinit var playlistViewModel: PlaylistViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = AudioItemModalBinding.inflate(inflater, container, false)

        playlistViewModel = (activity as MainActivity).playlistViewModel
        binding.addToQueue.setOnClickListener {
            MyExoPlayer.addToQueue(item)
            UiUtils.showToast(requireContext(), "Added to queue")
            dismiss()
        }

        binding.addToPlaylist.setOnClickListener {
            playlistOptionsModal = PlaylistOptionsModal(
                operation = "ADD_TO_PLAYLIST",
                songId = item.id.toString()
            )
            playlistOptionsModal.show(
                parentFragmentManager,
                playlistOptionsModal.tag
            )
        }
        if (playlistId != null) {
            binding.removeFromPlaylist.visibility = View.VISIBLE
            binding.removeFromPlaylist.setOnClickListener {

                playlistViewModel.removeSongFromPlaylist(playlistId.toInt(), item.id.toString())
                UiUtils.showToast(requireContext(), "${item.title} removed from playlist")

                dismiss()
            }

        }


        if (queueItem) {
            binding.removeFromQueue.visibility = View.VISIBLE
            binding.removeFromQueue.setOnClickListener {
                MyExoPlayer.removeFromQueue(item)
                UiUtils.showToast(requireContext(), "Removed from queue")
                dismiss()
            }
        }
        setUi()

        return binding.root
    }

    private fun setUi() {
        binding.audioTitle.text = item.title
        binding.audioArtist.text = item.artist


        Glide.with(binding.albumArt)
            .load(item.albumArtUri) // Use albumArtUri from AudioModel
            .placeholder(R.drawable.vinyl) // Placeholder image
            .error(R.drawable.vinyl) // Error image
            .into(binding.albumArt)
    }

}