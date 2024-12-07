package com.downbadbuzor.trackpulse.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.downbadbuzor.trackpulse.PlaylistFragment
import com.downbadbuzor.trackpulse.R
import com.downbadbuzor.trackpulse.databinding.PlaylistLayoutBinding
import com.downbadbuzor.trackpulse.db.Playlist

class PlaylistAdapter(private val fragmentManager: FragmentManager) :
    RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {


    class PlaylistViewHolder(val binding: PlaylistLayoutBinding) :
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        return PlaylistViewHolder(
            PlaylistLayoutBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val currentPlaylist = differ.currentList[position]

        holder.binding.playlistName.text = currentPlaylist.name

        holder.itemView.setOnClickListener {

            fragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    PlaylistFragment.newInstance(currentPlaylist.id.toString())
                )
                .addToBackStack("myFragmentTag") // Optional tag
                .commit()
        }
    }
}