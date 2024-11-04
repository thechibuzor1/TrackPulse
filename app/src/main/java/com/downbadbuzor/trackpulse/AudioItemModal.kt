package com.downbadbuzor.trackpulse

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.downbadbuzor.trackpulse.Utils.UiUtils
import com.downbadbuzor.trackpulse.databinding.AudioItemModalBinding
import com.downbadbuzor.trackpulse.model.AudioModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AudioItemModal(
    private val item: AudioModel,
    private val queueItem: Boolean
) : BottomSheetDialogFragment() {

    private lateinit var binding: AudioItemModalBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = AudioItemModalBinding.inflate(inflater, container, false)


        binding.addToQueue.setOnClickListener{
            MyExoPlayer.addToQueue(item)
            UiUtils.showToast(requireContext(),"Added to queue")
            dismiss()
        }
        if(queueItem) {
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

    private fun setUi(){
        binding.audioTitle.text = item.title
        binding.audioArtist.text = item.artist


        Glide.with(binding.albumArt)
            .load(item.albumArtUri) // Use albumArtUri from AudioModel
            .placeholder(R.drawable.vinyl) // Placeholder image
            .error(R.drawable.vinyl) // Error image
            .into(binding.albumArt)
    }

}