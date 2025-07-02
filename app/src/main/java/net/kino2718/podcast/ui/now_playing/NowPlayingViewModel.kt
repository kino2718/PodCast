package net.kino2718.podcast.ui.now_playing

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
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.data.Repository

class NowPlayingViewModel(app: Application) : AndroidViewModel(app) {
    val repo = Repository(app)

    @OptIn(ExperimentalCoroutinesApi::class)
    val playingPodcastInfo = repo.getLastPlayedItemIdFlow().flatMapLatest { currentPlayItemId ->
        currentPlayItemId?.let { playItemId ->
            repo.getChannelByIdFlow(playItemId.channelId)
                .combine(repo.getEpisodeByIdFlow(playItemId.episodeId)) { channel, episode ->
                    if (channel != null && episode != null)
                        PlayItem(
                            channel = channel,
                            episode = episode,
                            inPlaylist = playItemId.inPlaylist
                        )
                    else null
                }.filterNotNull()
        } ?: flowOf()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)
}
