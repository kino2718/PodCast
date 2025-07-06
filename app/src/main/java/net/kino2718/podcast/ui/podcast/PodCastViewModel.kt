package net.kino2718.podcast.ui.podcast

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.kino2718.podcast.data.PChannel
import net.kino2718.podcast.data.PodCast
import net.kino2718.podcast.data.Repository
import net.kino2718.podcast.ui.utils.loadRss

data class PodCastUIState(
    val podCast: PodCast,
    val ascendingOrder: Boolean, // episode listの表示順
)

class PodCastViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)

    private val podCastFlowFromRss = MutableSharedFlow<PodCast>()
    val playlistItemsFlow = repo.getPlaylistItemsFlow().stateIn(
        viewModelScope, SharingStarted.Lazily, listOf()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = podCastFlowFromRss.flatMapLatest { fromRss ->
        // databaseに存在するか確認
        repo.getPodCastByFeedUrlFlow(fromRss.channel.feedUrl).map { fromDb ->
            fromDb?.let {
                // databaseに存在する。channelのidと状態をコピーする。
                val fromDbChannel = fromDb.channel
                val newChannel = fromRss.channel.copy(
                    id = fromDbChannel.id,
                    subscribed = fromDbChannel.subscribed,
                )
                // dbからのitemのidと状態をコピーする。
                val episodeListFromRss = fromRss.episodeList
                val episodeListFromDb = fromDb.episodeList
                val newEpisodeList = episodeListFromRss.map { episodeFromRss ->
                    episodeListFromDb.find { itemFromDb -> episodeFromRss.guid == itemFromDb.guid }
                        ?.let { foundItem ->
                            episodeFromRss.copy(
                                id = foundItem.id,
                                channelId = foundItem.channelId,
                                downloadFile = foundItem.downloadFile,
                                playbackPosition = foundItem.playbackPosition,
                                duration = foundItem.duration,
                                isPlaybackCompleted = foundItem.isPlaybackCompleted,
                                lastPlayed = foundItem.lastPlayed,
                            )
                        } ?: episodeFromRss
                }
                PodCastUIState(
                    podCast = fromRss.copy(channel = newChannel, episodeList = newEpisodeList),
                    ascendingOrder = false,
                )

            } ?: PodCastUIState(podCast = fromRss, false) // このpodcastはdatabaseには存在しない。
        }.combine(_ascendingOrder) { podCastUIState, ascendingOrder ->
            val podCast = podCastUIState.podCast
            val episodeList = podCast.episodeList
            val sortedList = if (ascendingOrder) episodeList.sortedBy { it.pubDate }
            else episodeList.sortedByDescending { it.pubDate }
            podCastUIState.copy(
                podCast = podCast.copy(episodeList = sortedList),
                ascendingOrder = ascendingOrder
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Urlからrssを読み解析してPodCastオブジェクトを作成してflowに流す。
    fun load(feedUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            loadRss(feedUrl)?.let { podCastFlowFromRss.emit(it) }
        }
    }

    fun subscribe(channel: PChannel, subscribe: Boolean) {
        viewModelScope.launch {
            repo.subscribe(channel, subscribe)
        }
    }

    private val _ascendingOrder = MutableStateFlow(false)

    fun changeOrder(order: Boolean) {
        _ascendingOrder.value = order
    }

    companion object {
        @Suppress("unused")
        private const val TAG = "PodCastViewModel"
    }
}
