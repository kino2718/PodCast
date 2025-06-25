package net.kino2718.podcast.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.kino2718.podcast.data.Episode
import net.kino2718.podcast.data.PChannel
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.data.Repository

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    val subscribedFlow =
        repo.subscribedChannelFlow().stateIn(viewModelScope, SharingStarted.Lazily, listOf())
    val recentlyListenedFlow =
        repo.recentlyListenedFlow(10).stateIn(viewModelScope, SharingStarted.Lazily, listOf())

    fun addLastPlayedItem(channel: PChannel, episode: Episode) {
        viewModelScope.launch {
            val playItem = PlayItem(channel = channel, episode = episode)
            repo.addLastPlayedItem(playItem)
        }
    }
}