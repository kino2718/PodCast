package net.kino2718.podcast.ui.playlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val playlistUIStatesFlow = repo.getPlaylistItemsFlow().flatMapLatest { playlistItems ->
        val flows = playlistItems.map { playlistItem ->
            repo.getChannelByIdFlow(playlistItem.channelId)
                .combine(repo.getEpisodeByIdFlow(playlistItem.episodeId)) { channel, episode ->
                    if (channel != null && episode != null)
                        PlaylistUIState(
                            playlistItem = playlistItem,
                            playItem = PlayItem(
                                channel = channel,
                                episode = episode,
                                inPlaylist = true
                            ),
                        )
                    else null
                }.filterNotNull()
        }
        if (flows.isNotEmpty()) combine(flows) { array -> array.toList() }
        else flowOf(listOf())
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

    suspend fun getPlayItemList(): List<PlayItem> {
        return repo.getPlaylistItems().mapNotNull { playlistItem ->
            val channel = repo.getChannelById(playlistItem.channelId)
            val episode = repo.getEpisodeById(playlistItem.episodeId)
            if (channel != null && episode != null)
                PlayItem(channel = channel, episode = episode, true)
            else null
        }
    }
}