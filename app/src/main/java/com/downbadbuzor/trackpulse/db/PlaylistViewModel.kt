package com.downbadbuzor.trackpulse.db

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlaylistViewModel(
    private val dao: PlaylistDao

) : ViewModel() {

    private val _state = MutableStateFlow(PlaylistState())

    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PlaylistState()
    )

    fun onEvent(event: PlaylistEvent) {
        when (event) {
            is PlaylistEvent.DeletePlaylist -> {

                viewModelScope.launch {
                    dao.deletePlaylist(event.playlist)
                }
            }

            PlaylistEvent.HideDialog -> {
                _state.update {
                    it.copy(
                        isAddingPlaylist = false
                    )
                }
            }

            PlaylistEvent.SavePlaylist -> {
                val name = _state.value.playlistName
                val songs = _state.value.playlistSongs
                val coverImage = _state.value.playlistCoverImage

                if (name.isBlank() || songs.isEmpty()) {
                    return
                }
                val playlist = Playlist(
                    name = name,
                    songs = songs,
                    coverImage = coverImage
                )
                viewModelScope.launch {
                    dao.upsertPlaylist(playlist)
                }
                _state.update {
                    it.copy(
                        isAddingPlaylist = false,
                        playlistName = "",
                        playlistCoverImage = "",
                        playlistSongs = emptyList()
                    )
                }
            }

            is PlaylistEvent.SetPlaylistName -> {
                _state.update {
                    it.copy(
                        playlistName = event.name
                    )
                }
            }

            is PlaylistEvent.SetPlaylistSongs -> {
                _state.update {
                    it.copy(
                        playlistSongs = event.songs
                    )
                }
            }

            is PlaylistEvent.SetPlaylistCoverImage -> {
                _state.update {
                    it.copy(
                        playlistCoverImage = event.coverImage
                    )
                }
            }

            PlaylistEvent.ShowDialog -> {
                _state.update {
                    it.copy(
                        isAddingPlaylist = true
                    )
                }
            }
        }
    }
}
