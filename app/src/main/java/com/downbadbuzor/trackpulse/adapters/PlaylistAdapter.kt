package com.downbadbuzor.trackpulse.adapters

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.downbadbuzor.trackpulse.PlaylistFragment
import com.downbadbuzor.trackpulse.PlaylistOptionsModal
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

        Glide.with(holder.binding.playlistImage.context)
            .load(currentPlaylist.coverImage)
            .placeholder(R.drawable.note)
            .error(R.drawable.note)
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

            fragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    PlaylistFragment.newInstance(currentPlaylist.id.toString())
                )
                .addToBackStack("myFragmentTag") // Optional tag
                .commit()
        }

        holder.itemView.setOnLongClickListener {
            val bottomSheetFragment = PlaylistOptionsModal(currentPlaylist, "OPTIONS")
            bottomSheetFragment.show(fragmentManager, bottomSheetFragment.tag)
            true
        }
    }
}