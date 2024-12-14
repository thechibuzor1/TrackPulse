package com.downbadbuzor.trackpulse.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.downbadbuzor.trackpulse.MyExoPlayer
import com.downbadbuzor.trackpulse.R
import com.downbadbuzor.trackpulse.databinding.ShowAllSongsBinding
import com.downbadbuzor.trackpulse.db.PlaylistViewModel
import com.downbadbuzor.trackpulse.model.AudioModel

class ShowAllSongsAdapter(
    private val playlistViewModel: PlaylistViewModel,
    private val playlistId: String
) : RecyclerView.Adapter<ShowAllSongsAdapter.ShowAllSongsViewHolder>() {
    private val songs = MyExoPlayer.getAllAudioList()
    private val songsInPlaylist = mutableListOf<String>()


    fun addSongs(playlistSongs: List<String>) {
        songsInPlaylist.addAll(playlistSongs)
        notifyDataSetChanged()
    }


    fun clearSongs() {
        songsInPlaylist.clear()
        notifyDataSetChanged()
    }

    inner class ShowAllSongsViewHolder(val binding: ShowAllSongsBinding) :
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShowAllSongsViewHolder {
        val binding =
            ShowAllSongsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShowAllSongsViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return songs!!.size
    }

    override fun onBindViewHolder(holder: ShowAllSongsViewHolder, position: Int) {
        holder.bind(songs!![position])

        val currentSongId = songs[position].id.toString()


        if (songsInPlaylist.contains(currentSongId)) {
            holder.binding.songCheckbox.setImageResource(R.drawable.checked)
        } else {
            holder.binding.songCheckbox.setImageResource(R.drawable.unchecked)
        }


        holder.itemView.setOnClickListener {
            val isSongInCurrentPlaylist =
                songsInPlaylist.contains(currentSongId) // Check for current playlist
            if (isSongInCurrentPlaylist) {
                playlistViewModel.removeSongFromPlaylist(playlistId.toInt(), currentSongId)
                holder.binding.songCheckbox.setImageResource(R.drawable.unchecked)
            } else {
                playlistViewModel.addSongToPlaylist(playlistId.toInt(), currentSongId)
                holder.binding.songCheckbox.setImageResource(R.drawable.checked)
            }
        }

        holder.binding.songCheckbox.setOnClickListener {
            val isSongInCurrentPlaylist =
                songsInPlaylist.contains(currentSongId) // Check for current playlist
            if (isSongInCurrentPlaylist) {
                playlistViewModel.removeSongFromPlaylist(playlistId.toInt(), currentSongId)
                holder.binding.songCheckbox.setImageResource(R.drawable.unchecked)
            } else {
                playlistViewModel.addSongToPlaylist(playlistId.toInt(), currentSongId)
                holder.binding.songCheckbox.setImageResource(R.drawable.checked)
            }
        }


    }

}