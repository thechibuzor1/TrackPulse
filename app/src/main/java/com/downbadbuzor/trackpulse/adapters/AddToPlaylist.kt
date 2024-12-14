package com.downbadbuzor.trackpulse.adapters

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.downbadbuzor.trackpulse.R
import com.downbadbuzor.trackpulse.databinding.SaveSongToPlaylistBinding
import com.downbadbuzor.trackpulse.db.Playlist
import com.downbadbuzor.trackpulse.db.PlaylistViewModel

class AddToPlaylist(
    private val playlistViewModel: PlaylistViewModel,
    private val songId: String?,
) :
    RecyclerView.Adapter<AddToPlaylist.AddToPlaylistViewHolder>() {


    class AddToPlaylistViewHolder(val binding: SaveSongToPlaylistBinding) :
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
            SaveSongToPlaylistBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }


    override fun onBindViewHolder(holder: AddToPlaylistViewHolder, position: Int) {
        val currentPlaylist = differ.currentList[position]

        holder.binding.playlistName.text = currentPlaylist.name

        if (currentPlaylist.songs.contains(songId)) {
            holder.binding.playlistCheckbox.setImageResource(R.drawable.checked)
        } else {
            holder.binding.playlistCheckbox.setImageResource(R.drawable.unchecked)
        }

        val placeholderResId =
            when (currentPlaylist.id) {
                0 -> {
                    R.drawable.heart
                }

                else -> {
                    R.drawable.playlist
                }
            }
        Glide.with(holder.binding.playlistImage.context)
            .load(currentPlaylist.coverImage)
            .placeholder(placeholderResId)
            .error(placeholderResId)
            .into(holder.binding.playlistImage)

        //background color match cover image
        Glide.with(holder.binding.playlistCard.context)
            .asBitmap()
            .error(R.color.default_color)
            .load(currentPlaylist.coverImage)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                ) {
                    Palette.from(resource).generate { palette ->
                        val dominantColor = palette?.getDarkMutedColor(
                            ContextCompat.getColor(
                                holder.binding.playlistCard.context,
                                R.color.default_color
                            )
                        )
                        holder.binding.playlistCard.setBackgroundColor(
                            dominantColor ?: ContextCompat.getColor(
                                holder.binding.playlistCard.context,
                                R.color.default_color
                            )
                        )
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Optional: Handle placeholder or clearing the image
                }
            })




        holder.itemView.setOnClickListener {
            val isSongInCurrentPlaylist =
                currentPlaylist.songs.contains(songId) // Check for current playlist
            if (isSongInCurrentPlaylist) {
                playlistViewModel.removeSongFromPlaylist(currentPlaylist.id!!, songId!!)
            } else {
                playlistViewModel.addSongToPlaylist(currentPlaylist.id!!, songId!!)
            }
        }

        holder.binding.playlistCheckbox.setOnClickListener {
            val isSongInCurrentPlaylist =
                currentPlaylist.songs.contains(songId) // Check for current playlist
            if (isSongInCurrentPlaylist) {
                playlistViewModel.removeSongFromPlaylist(currentPlaylist.id!!, songId!!)
            } else {
                playlistViewModel.addSongToPlaylist(currentPlaylist.id!!, songId!!)
            }
        }
    }
}