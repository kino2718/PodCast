package net.kino2718.podcast.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.kino2718.podcast.data.Repository

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    val subscribedFlow =
        repo.subscribedChannelFlow().stateIn(viewModelScope, SharingStarted.Lazily, listOf())

    // last played item id が変化したら recent plays list を更新する。last played item id自体は使用しない。タイミングを取るだけ。
    val recentPlays = repo.getLastPlayedItemIdFlow().map {
        repo.getRecentPlays(NUM_RECENT_PLAYS)
    }.stateIn(viewModelScope, SharingStarted.Lazily, listOf())

    companion object {
        private const val NUM_RECENT_PLAYS = 10
    }
}