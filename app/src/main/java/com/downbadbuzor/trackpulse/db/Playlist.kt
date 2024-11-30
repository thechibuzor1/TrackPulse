package com.downbadbuzor.trackpulse.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.downbadbuzor.trackpulse.model.AudioModel
import com.google.common.reflect.TypeToken
import com.google.gson.Gson

class AudioModelListConverter {

    @TypeConverter
    fun fromAudioModelList(songs: List<AudioModel>?): String? {
        return Gson().toJson(songs) // Correct Gson usage
    }

    @TypeConverter
    fun toAudioModelList(songsString: String?): List<AudioModel>? {
        return Gson().fromJson(
            songsString,
            object : TypeToken<List<AudioModel>>() {}.type
        ) // Correct TypeToken usage
    }
}

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val name: String,
    val coverImage: String,
    val songs: List<AudioModel> = emptyList()
)
