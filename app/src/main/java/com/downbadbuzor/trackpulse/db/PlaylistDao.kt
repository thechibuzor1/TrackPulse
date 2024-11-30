package com.downbadbuzor.trackpulse.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PlaylistDao {

    @Upsert
    suspend fun upsertPlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query("SELECT * FROM PLAYLISTS ORDER BY name ASC")
    fun getPlaylists(): LiveData<List<Playlist>>

    @Query("SELECT * FROM PLAYLISTS WHERE id = :id")
    fun getPlaylistById(id: Int): LiveData<Playlist>

}