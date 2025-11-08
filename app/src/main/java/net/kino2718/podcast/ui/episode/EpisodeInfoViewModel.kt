package net.kino2718.podcast.ui.episode

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.data.Repository
import net.kino2718.podcast.utils.MyLog

class EpisodeInfoViewModel(app: Application) : AndroidViewModel(app) {
    private val appContext = app.applicationContext
    private val repo = Repository(appContext)

    private val episodeIdFlow = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val playItem = episodeIdFlow.flatMapLatest { episodeId ->
        MyLog.d(TAG, "episodeId: $episodeId")
        if (episodeId != null) {
            repo.getEpisodeByIdFlow(episodeId).flatMapLatest { episode ->
                MyLog.d(TAG, "episode: ${episode?.id}")
                if (episode != null) {
                    repo.getChannelByIdFlow(episode.channelId).map { channel ->
                        MyLog.d(TAG, "channel: ${channel?.id}")
                        if (channel != null) {
                            PlayItem(
                                channel = channel,
                                episode = episode,
                            )
                        } else null
                    }
                } else flowOf()
            }
        } else flowOf()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun requestPlayItem(episodeId: Long) {
        episodeIdFlow.value = episodeId
    }

    companion object {
        const val TAG = "EpisodeInfoViewModel"
    }
}
