package com.downbadbuzor.trackpulse.model

data class AudioModel(
    val id: Long,
    val title: String,
    val artist: String,
    val data: String, // file path
    val displayName: String,
    val duration: Long,
    val albumId: Long, // Add albumId to fetch album art
    var albumArtUri: String? = null //
)
