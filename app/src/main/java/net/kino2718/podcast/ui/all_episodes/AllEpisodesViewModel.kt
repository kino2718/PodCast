package net.kino2718.podcast.ui.all_episodes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.data.Repository
import net.kino2718.podcast.ui.utils.getRssData

class AllEpisodesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)

    private val _ascendingOrder = MutableStateFlow(false)
    val ascendingOrder = _ascendingOrder.asStateFlow()

    private val allPlayItemsRssFlow = repo.subscribedChannelsFlow()
        .combine(_ascendingOrder) { channels, order ->
            Pair(channels, order)
        }
        .transform { pair ->
            val channels = pair.first
            val ascendingOrder = pair.second
            val allPlayItemList = mutableListOf<PlayItem>()
            channels.map { channel ->
                getRssData(channel.feedUrl)?.let { rssData ->
                    rssData.episodeList
                        .forEach {
                            allPlayItemList.add(
                                PlayItem(
                                    channel = channel,
                                    episode = it,
                                )
                            )
                        }
                    val sorted = if (ascendingOrder) allPlayItemList.sortedBy { it.episode.pubDate }
                    else allPlayItemList.sortedByDescending { it.episode.pubDate }
                    emit(sorted.toList())
                }
            }
        }

    val allPlayItemsFlow = changeEpisodesIfExist(allPlayItemsRssFlow)
        .stateIn(viewModelScope, SharingStarted.Lazily, listOf())

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun changeEpisodesIfExist(flow: Flow<List<PlayItem>>): Flow<List<PlayItem>> {
        return flow.flatMapLatest { playItemsRss ->
            val guidsRss = playItemsRss.map { it.episode.guid }
            repo.getEpisodesByGuidsFlow(guidsRss).map { episodesFromDb ->
                playItemsRss.map { playItemRss ->
                    // dbからのepisode listの中から探す
                    val episode =
                        episodesFromDb.firstOrNull { playItemRss.episode.guid == it.guid }
                            ?: playItemRss.episode
                    playItemRss.copy(episode = episode)
                }
            }
        }
    }

    val playlistItemsFlow = repo.getPlaylistItemsFlow().stateIn(
        viewModelScope, SharingStarted.Lazily, listOf()
    )

    fun changeOrder(order: Boolean) {
        _ascendingOrder.value = order
    }
}