package com.downbadbuzor.trackpulse.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.downbadbuzor.trackpulse.databinding.PlaylistLayoutBinding
import com.downbadbuzor.trackpulse.db.Playlist
import com.downbadbuzor.trackpulse.db.PlaylistViewModel

class AddToPlaylist(
    private val playlistViewModel: PlaylistViewModel,
    private val songId: String?) :
    RecyclerView.Adapter<AddToPlaylist.AddToPlaylistViewHolder>() {


    class AddToPlaylistViewHolder(val binding: PlaylistLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)


    private val differCallback = object : DiffUtil.ItemCallback<Playlist>() {
        override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
            return oldItem.id == newItem.id
                    && oldItem.name == newItem.name
                    && oldItem.songs == newItem.songs
                    && oldItem.coverImage == newItem.coverImage
        }

        override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
            return oldItem == newItem
        }
    }
    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddToPlaylistViewHolder {
        return AddToPlaylistViewHolder(
            PlaylistLayoutBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: AddToPlaylistViewHolder, position: Int) {
        val currentPlaylist = differ.currentList[position]

        holder.binding.playlistNmae.text = currentPlaylist.name

        holder.itemView.setOnClickListener {
            playlistViewModel.addSongToPlaylist(currentPlaylist.id!!, songId!!)
        }
    }
}