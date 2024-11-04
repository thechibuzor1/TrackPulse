package com.downbadbuzor.trackpulse.adapters

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.downbadbuzor.trackpulse.AudioItemModal
import com.downbadbuzor.trackpulse.MyExoPlayer
import com.downbadbuzor.trackpulse.PlayingBottomSheetFragment
import com.downbadbuzor.trackpulse.R
import com.downbadbuzor.trackpulse.databinding.AudioItemBinding
import com.downbadbuzor.trackpulse.model.AudioModel


class AudioAdapter( private val activity: Activity,
                    private val supportFragmentManager: FragmentManager) :
    RecyclerView.Adapter<AudioAdapter.AudioViewHolder>() {
    private val songs = mutableListOf<AudioModel>()


    fun updateExoplayerListFromSearch(newSongs: List<AudioModel>) {
            songs.clear()
            songs.addAll(newSongs)

            MyExoPlayer.updateAudioList(newSongs)
            notifyDataSetChanged()
        }

    fun setCurrentSorting(sorting: String) {
        when (sorting) {
            "DEFAULT" -> songs.sortBy { it.title }
            "TITLE" -> songs.sortBy { it.title }
            "ARTIST" -> songs.sortBy { it.artist }
        }
        notifyDataSetChanged()
        // Update ExoPlayer after sorting
        MyExoPlayer.updateAudioList(songs)
    }

    fun addSongs(newSongs: List<AudioModel>) {
        songs.addAll(newSongs)
        notifyDataSetChanged()
    }


    fun clearSongs() {
        songs.clear()
        notifyDataSetChanged()
    }






    inner class AudioViewHolder(val binding: AudioItemBinding) :
        RecyclerView.ViewHolder(binding.root) {


        fun bind(audio: AudioModel) {

            binding.audioTitle.text = audio.title
            binding.audioArtist.text = audio.artist


            Glide.with(binding.albumArt)
                .load(audio.albumArtUri) // Use albumArtUri from AudioModel
                .placeholder(R.drawable.vinyl) // Placeholder image
                .error(R.drawable.vinyl) // Error image
                .into(binding.albumArt)


            binding.options.setOnClickListener {
               val bottomSheetFragment = AudioItemModal(audio, false)
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)

            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
       val binding = AudioItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AudioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
       holder.bind(songs[position])

        holder.itemView.setOnClickListener {
            val search = activity.findViewById<SearchView>(R.id.search)
            MyExoPlayer.playFromHere(position, activity)
            search.clearFocus()
        }
        holder.itemView.setOnLongClickListener {

            val bottomSheetFragment = AudioItemModal(songs[position], false)
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)

            true
        }


    }

    override fun getItemCount(): Int {
        return songs.size
    }
}
