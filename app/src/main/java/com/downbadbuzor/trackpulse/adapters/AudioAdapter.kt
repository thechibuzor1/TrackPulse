package com.downbadbuzor.trackpulse.adapters

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.downbadbuzor.trackpulse.MyExoPlayer
import com.downbadbuzor.trackpulse.R
import com.downbadbuzor.trackpulse.databinding.AudioItemBinding
import com.downbadbuzor.trackpulse.model.AudioModel


class AudioAdapter( private val activity: Activity) :
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


        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
       val binding = AudioItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AudioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
       holder.bind(songs[position])

        holder.itemView.setOnClickListener {
            MyExoPlayer.playFromHere(position, activity)
        }

    }

    override fun getItemCount(): Int {
        return songs.size
    }
}
