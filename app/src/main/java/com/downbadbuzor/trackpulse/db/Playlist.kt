package com.downbadbuzor.trackpulse.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.downbadbuzor.trackpulse.model.AudioModel
import com.google.common.reflect.TypeToken
import com.google.gson.Gson


class StringListConverter {

    @TypeConverter
    fun fromStringList(songs: List<String>?): String? {
        return try {
            Gson().toJson(songs)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @TypeConverter
    fun toStringList(songsString: String?): List<String>? {
        return try {
            if (songsString.isNullOrEmpty()) {
                emptyList()
            } else {
                Gson().fromJson(
                    songsString,
                    object : TypeToken<List<String>>() {}.type
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}



@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val name: String,
    val coverImage: String,
    val songs: List<String> = emptyList()
)
