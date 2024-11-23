package com.downbadbuzor.trackpulse.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Playlist::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(AudioModelListConverter::class)
abstract class Playlistdb : RoomDatabase() {

    abstract val dao: PlaylistDao

}