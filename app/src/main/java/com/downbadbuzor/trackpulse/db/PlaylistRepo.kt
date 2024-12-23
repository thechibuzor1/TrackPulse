package com.downbadbuzor.trackpulse.db

class PlaylistRepo(
    private val db: Playlistdb
) {

    suspend fun upsertPlaylist(playlist: Playlist) = db.getPlaylistDao().upsertPlaylist(playlist)

    suspend fun deletePlaylist(playlist: Playlist) = db.getPlaylistDao().deletePlaylist(playlist)

    fun getPlaylists() = db.getPlaylistDao().getPlaylists()

    fun getPlaylistById(id: Int) = db.getPlaylistDao().getPlaylistById(id)


    suspend fun getPlaylistByIdSync(id: Int) = db.getPlaylistDao().getPlaylistByIdSync(id)

    suspend fun updatePlaylistCoverImage(playlistId: Int, imageUri: String?) {
        val playlist = getPlaylistByIdSync(playlistId)
        if (playlist != null) {
            val updatedPlaylist = playlist.copy(coverImage = imageUri!!)
            upsertPlaylist(updatedPlaylist)
        }
    }
}