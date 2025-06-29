package net.kino2718.podcast.ui.playlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.data.PlaylistItem
import net.kino2718.podcast.data.Repository

data class PlaylistUIState(
    val playlistItem: PlaylistItem,
    val playItem: PlayItem,
)

class PlaylistViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    val playlistUIStatesFlow = repo.getPlaylistItemsFlow().map { playlistItems ->
        playlistItems.mapNotNull { playlistItem ->
            val channel = repo.getChannelById(playlistItem.channelId)
            val episode = repo.getEpisodeById(playlistItem.episodeId)
            if (channel != null && episode != null)
                PlaylistUIState(
                    playlistItem = playlistItem,
                    playItem = PlayItem(channel = channel, episode = episode, inPlaylist = true),
                )
            else null
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, listOf())

    fun deleteAll() {
        viewModelScope.launch {
            repo.deleteAllPlaylistItems()
        }
    }

    fun deleteItem(playlistUIState: PlaylistUIState) {
        viewModelScope.launch {
            repo.deletePlaylistItem(playlistUIState.playlistItem)
        }
    }
}