package com.downbadbuzor.trackpulse.db

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PlaylistViewModelFactory(val app: Application, private val playlistRepository: PlaylistRepo) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PlaylistViewModel(app, playlistRepository) as T
    }

}