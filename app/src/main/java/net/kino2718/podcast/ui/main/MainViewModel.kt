package net.kino2718.podcast.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import net.kino2718.podcast.data.Repository

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    val playListFlow =
        repo.getPlayListFlow().stateIn(viewModelScope, SharingStarted.Eagerly, listOf())
}