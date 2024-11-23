package com.downbadbuzor.trackpulse.db

import com.downbadbuzor.trackpulse.model.AudioModel

data class PlaylistState(
    val playlists: List<Playlist> = emptyList(),
    val playlistName: String = "",
    val playlistSongs: List<AudioModel> = emptyList(),
    val isAddingPlaylist: Boolean = false,
    val playlistCoverImage: String = ""
)
