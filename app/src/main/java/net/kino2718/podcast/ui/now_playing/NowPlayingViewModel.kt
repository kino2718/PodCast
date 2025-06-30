package net.kino2718.podcast.ui.now_playing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.data.Repository

class NowPlayingViewModel(app: Application) : AndroidViewModel(app) {
    val repo = Repository(app)
    val playingPodcastInfo = repo.getLastPlayedItemIdFlow().map {
        it?.let { playItemId ->
            val channel = repo.getChannelById(playItemId.channelId)
            val episode = repo.getEpisodeById(playItemId.episodeId)
            if (channel != null && episode != null)
                PlayItem(channel = channel, episode = episode, inPlaylist = playItemId.inPlaylist)
            else null
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)
}