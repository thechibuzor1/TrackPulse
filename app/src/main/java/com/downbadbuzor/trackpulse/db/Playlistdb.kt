package com.downbadbuzor.trackpulse.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Playlist::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(StringListConverter::class)
abstract class Playlistdb : RoomDatabase() {

    abstract fun getPlaylistDao(): PlaylistDao

    companion object {
        @Volatile
        private var instance: Playlistdb? = null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance ?: synchronized(LOCK) {
            instance ?: createDatabase(context).also {
                instance = it
            }
        }

        private fun createDatabase(context: Context) = Room.databaseBuilder(
            context.applicationContext,
            Playlistdb::class.java,
            "playlist_db"
        ).build()
    }
}

