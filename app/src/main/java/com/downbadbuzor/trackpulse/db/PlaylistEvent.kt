package com.downbadbuzor.trackpulse.db

import com.downbadbuzor.trackpulse.model.AudioModel

sealed interface PlaylistEvent {

    object SavePlaylist : PlaylistEvent
    data class SetPlaylistName(val name: String) : PlaylistEvent
    data class SetPlaylistSongs(val songs: List<AudioModel>) : PlaylistEvent
    data class SetPlaylistCoverImage(val coverImage: String) : PlaylistEvent


    object ShowDialog : PlaylistEvent
    object HideDialog : PlaylistEvent

    data class DeletePlaylist(val playlist: Playlist) : PlaylistEvent

}