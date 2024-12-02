package com.downbadbuzor.trackpulse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.downbadbuzor.trackpulse.Utils.UiUtils
import com.downbadbuzor.trackpulse.databinding.PlaylistOptionsModalBinding
import com.downbadbuzor.trackpulse.db.Playlist
import com.downbadbuzor.trackpulse.db.PlaylistViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PlaylistOptionsModal(
    private val playlist: Playlist? = null,
    private val operation: String
) : BottomSheetDialogFragment() {

    private lateinit var binding: PlaylistOptionsModalBinding
    private lateinit var playlistViewModel: PlaylistViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = PlaylistOptionsModalBinding.inflate(inflater, container, false)

        playlistViewModel = (activity as MainActivity).playlistViewModel

        when (operation) {
            "CREATE" -> {
                binding.createPlaylistLayout.visibility = View.VISIBLE
                binding.optionsLayout.visibility = View.GONE
                binding.createPlaylist.setOnClickListener {
                    savePlaylist(binding.enterPlaylistName.text.toString())
                }
            }

            "OPTIONS" -> {
                binding.createPlaylistLayout.visibility = View.GONE
                binding.optionsLayout.visibility = View.VISIBLE

                binding.playlistName.text = playlist?.name


                binding.deletePlaylist.setOnClickListener {

                    showConfirmationDialog(playlist!!)

                }
            }

        }











        return binding.root
    }

    private fun savePlaylist(name: String) {


        if (name.isEmpty()) {
            UiUtils.showToast(requireContext(), "Playlist name cannot be empty")
            return
        }

        val playlist =
            Playlist(
                name = name,
                coverImage = "",
                songs = emptyList()
            )
        playlistViewModel.addPlaylist(playlist)

        dismiss()
        UiUtils.showToast(requireContext(), "Playlist Saved")

    }


    private fun showConfirmationDialog(playlist: Playlist) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Delete Playlist")
        builder.setMessage("Are you sure you want to delete this playlist?")
        builder.setPositiveButton("Delete") { _, _ ->
            // Delete the playlist
            playlistViewModel.deletePlaylist(playlist)
            UiUtils.showToast(requireContext(), "Playlist deleted")

            // Pop the fragment from the back stack
            parentFragmentManager.popBackStack()
            dismiss()

        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

}