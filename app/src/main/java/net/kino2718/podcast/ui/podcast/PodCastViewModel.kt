package net.kino2718.podcast.ui.podcast

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
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
)

class PodCastViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)

    private val podCastFlowFromRss = MutableSharedFlow<PodCast>()
    val playlistItemsFlow = repo.getPlaylistItemsFlow().stateIn(
        viewModelScope, SharingStarted.Lazily, listOf()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = podCastFlowFromRss.flatMapLatest { fromSearch ->
        // databaseに存在するか確認
        repo.getPodCastByFeedUrlFlow(fromSearch.channel.feedUrl).map { fromDb ->
            fromDb?.let {
                // databaseに存在する。channelのidと状態をコピーする。
                val fromDbChannel = fromDb.channel
                val newChannel = fromSearch.channel.copy(
                    id = fromDbChannel.id,
                    subscribed = fromDbChannel.subscribed,
                )
                // dbからのitemのidと状態をコピーする。
                val episodeListFromSearch = fromSearch.episodeList
                val episodeListFromDb = fromDb.episodeList
                val newItemList = episodeListFromSearch.map { itemFromSearch ->
                    episodeListFromDb.find { itemFromDb -> itemFromSearch.guid == itemFromDb.guid }
                        ?.let { foundItem ->
                            itemFromSearch.copy(
                                id = foundItem.id,
                                channelId = foundItem.channelId,
                                downloadFile = foundItem.downloadFile,
                                playbackPosition = foundItem.playbackPosition,
                                duration = foundItem.duration,
                                isPlaybackCompleted = foundItem.isPlaybackCompleted,
                            )
                        } ?: itemFromSearch
                }
                PodCastUIState(
                    fromSearch.copy(channel = newChannel, episodeList = newItemList)
                )

            } ?: PodCastUIState(podCast = fromSearch) // このpodcastはdatabaseには存在しない。
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

    companion object {
        @Suppress("unused")
        private const val TAG = "PodCastViewModel"
    }
}
