package com.downbadbuzor.trackpulse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.downbadbuzor.trackpulse.adapters.AudioAdapter
import com.downbadbuzor.trackpulse.databinding.SortModalBinding
import com.downbadbuzor.trackpulse.model.AudioModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class SortModal(
    private val audioAdapter : AudioAdapter,
    private val audioList : ArrayList<AudioModel>?
) : BottomSheetDialogFragment(){

    private lateinit var binding : SortModalBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View?
    {
        binding = SortModalBinding.inflate(inflater, container, false)

        getSorting()

        binding.sortByTitle.setOnClickListener {

            if(MyExoPlayer.getCurrentSorting() == "DEFAULT"){dismiss()}
            else{
            MyExoPlayer.setCurrentSorting("DEFAULT")
            audioAdapter.setCurrentSorting("DEFAULT")

            dismiss()
        }

        }

        binding.sortByArtist.setOnClickListener {
            if(MyExoPlayer.getCurrentSorting() == "ARTIST"){dismiss()}
            else {
                MyExoPlayer.setCurrentSorting("ARTIST")
                audioAdapter.setCurrentSorting("ARTIST")

                dismiss()
            }
        }



        return binding.root
    }


    private fun getSorting()
    {
        MyExoPlayer.getCurrentSorting().apply {
            when (this) {
                "DEFAULT" -> {
                    binding.sortByTitle.isChecked = true
                    binding.sortByArtist.isChecked = false
                }
                "ARTIST" -> {
                    binding.sortByArtist.isChecked = true
                    binding.sortByTitle.isChecked = false
                }
            }

        }
    }

}