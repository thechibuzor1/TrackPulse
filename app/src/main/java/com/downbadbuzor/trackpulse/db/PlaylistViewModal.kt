package com.downbadbuzor.trackpulse.db

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PlaylistViewModel(app: Application, private val playlistRepository: PlaylistRepo) :
    AndroidViewModel(app) {


    fun addPlaylist(playlist: Playlist) = viewModelScope.launch {
        playlistRepository.upsertPlaylist(playlist)
    }

    fun deletePlaylist(playlist: Playlist) = viewModelScope.launch {
        playlistRepository.deletePlaylist(playlist)
    }

    fun getAllPlaylists() = playlistRepository.getPlaylists()

    fun getPlaylistById(id: Int) = playlistRepository.getPlaylistById(id)

}