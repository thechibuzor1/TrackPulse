package com.downbadbuzor.trackpulse

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.downbadbuzor.trackpulse.Utils.UiUtils
import com.downbadbuzor.trackpulse.adapters.AddToPlaylist
import com.downbadbuzor.trackpulse.databinding.PlaylistOptionsModalBinding
import com.downbadbuzor.trackpulse.db.Playlist
import com.downbadbuzor.trackpulse.db.PlaylistViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PlaylistOptionsModal(
    private val playlist: Playlist? = null,
    private val operation: String,
    private val songId: String? = null
) : BottomSheetDialogFragment() {

    private lateinit var binding: PlaylistOptionsModalBinding
    private lateinit var playlistViewModel: PlaylistViewModel

    private lateinit var playlistAdapter: AddToPlaylist

    private lateinit var playlistOptionsModal: PlaylistOptionsModal

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = PlaylistOptionsModalBinding.inflate(inflater, container, false)

        playlistViewModel = (activity as MainActivity).playlistViewModel

        when (operation) {
            "CREATE" -> {
                binding.editPlaylistLayout.visibility = View.GONE
                binding.createPlaylistLayout.visibility = View.VISIBLE
                binding.addToPlaylistLayout.visibility = View.GONE
                binding.optionsLayout.visibility = View.GONE
                binding.changePlaylistNameLayout.visibility = View.GONE
                binding.createPlaylist.setOnClickListener {
                    savePlaylist(binding.enterPlaylistName.text.toString())
                }
            }

            "OPTIONS" -> {
                binding.editPlaylistLayout.visibility = View.GONE
                binding.createPlaylistLayout.visibility = View.GONE
                binding.addToPlaylistLayout.visibility = View.GONE
                binding.optionsLayout.visibility = View.VISIBLE
                binding.changePlaylistNameLayout.visibility = View.GONE

                binding.playlistName.text = playlist?.name

                binding.editPlaylist.setOnClickListener {
                    playlistOptionsModal = PlaylistOptionsModal(playlist, "EDIT_PLAYLIST")
                    playlistOptionsModal.show(
                        parentFragmentManager,
                        playlistOptionsModal.tag
                    )
                }


                binding.deletePlaylist.setOnClickListener {
                    showConfirmationDialog(playlist!!)
                }
            }

            "ADD_TO_PLAYLIST" -> {
                binding.createPlaylistLayout.visibility = View.GONE
                binding.editPlaylistLayout.visibility = View.GONE
                binding.addToPlaylistLayout.visibility = View.VISIBLE
                binding.optionsLayout.visibility = View.GONE
                binding.changePlaylistNameLayout.visibility = View.GONE

                playlistAdapter = AddToPlaylist(playlistViewModel, songId, requireContext())
                binding.playlistRecyclerView.layoutManager =
                    StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
                binding.playlistRecyclerView.adapter = playlistAdapter
                playlistViewModel.getAllPlaylists().observe(this) { playlist ->
                    playlistAdapter.differ.submitList(playlist)
                }
                binding.saveButton.setOnClickListener {
                    UiUtils.showToast(requireContext(), "Changes Saved")
                    dismiss()
                }

            }

            "EDIT_PLAYLIST" -> {
                binding.createPlaylistLayout.visibility = View.GONE
                binding.addToPlaylistLayout.visibility = View.GONE
                binding.optionsLayout.visibility = View.GONE
                binding.changePlaylistNameLayout.visibility = View.GONE
                binding.editPlaylistLayout.visibility = View.VISIBLE

                binding.changePlaylistCoverImage.setOnClickListener {
                    val intent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    pickImageLauncher.launch(intent)
                }

                binding.editPlaylistName.setOnClickListener {
                    playlistOptionsModal = PlaylistOptionsModal(playlist, "EDIT_PLAYLIST_NAME")
                    playlistOptionsModal.show(
                        parentFragmentManager,
                        playlistOptionsModal.tag
                    )
                }
            }

            "EDIT_PLAYLIST_NAME" -> {
                binding.createPlaylistLayout.visibility = View.GONE
                binding.addToPlaylistLayout.visibility = View.GONE
                binding.optionsLayout.visibility = View.GONE
                binding.editPlaylistLayout.visibility = View.GONE
                binding.changePlaylistNameLayout.visibility = View.VISIBLE

                binding.changePlaylistName.setText(playlist?.name)

                binding.changePlaylistNameBtn.setOnClickListener {
                    renamePlaylist(binding.changePlaylistName.text.toString())
                }
            }


        }











        return binding.root
    }


    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri = result.data?.data
                imageUri?.let {
                    // Save the image URI and update the playlist cover
                    lifecycleScope.launch {
                        val savedImageUri = saveImageToAppStorage(
                            requireContext(),
                            it,
                            "playlist_cover_${System.currentTimeMillis()}.jpg"
                        )
                        savedImageUri?.let { uri ->
                            val playlist =
                                Playlist(
                                    id = playlist?.id,
                                    name = playlist?.name!!,
                                    coverImage = uri.toString(),
                                    songs = playlist.songs
                                )
                            playlistViewModel.addPlaylist(playlist)
                            UiUtils.showToast(requireContext(), "Playlist cover changed")
                            dismiss()
                        }
                    }
                }
            }
        }


    private fun saveImageToAppStorage(context: Context, sourceUri: Uri, fileName: String): Uri? {
        val contentResolver = context.contentResolver
        return try {
            val inputStream = contentResolver.openInputStream(sourceUri)
                ?: throw IOException("Unable to open input stream from URI: $sourceUri")
            val appDir = context.filesDir
            val file = File(appDir, fileName)

            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }

            // Close the input stream
            inputStream.close()

            // Return the content URI for the saved file
            FileProvider.getUriForFile(
                context,
                "com.downbadbuzor.trackpulse.fileprovider", // Ensure this matches your authority in the manifest
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            UiUtils.showToast(context, "Error saving image: ${e.message}")
            null
        }
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

    private fun renamePlaylist(name: String) {
        if (name.isEmpty()) {
            UiUtils.showToast(requireContext(), "Playlist name cannot be empty")
            return
        }
        val playlist =
            Playlist(
                id = playlist?.id,
                name = name,
                coverImage = playlist?.coverImage!!,
                songs = playlist.songs
            )
        playlistViewModel.addPlaylist(playlist)
        UiUtils.showToast(requireContext(), "Playlist name changed")
        dismiss()
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