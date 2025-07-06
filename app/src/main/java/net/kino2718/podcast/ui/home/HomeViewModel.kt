package net.kino2718.podcast.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import net.kino2718.podcast.data.PChannel
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.data.PodCast
import net.kino2718.podcast.data.Repository
import net.kino2718.podcast.ui.utils.loadRss
import kotlin.time.Duration.Companion.days

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    val subscribedFlow =
        repo.subscribedChannelsFlow().stateIn(viewModelScope, SharingStarted.Lazily, listOf())

    // 最近再生したPlay Item
    val recentPlaysFlow = repo.getRecentPlaysFlow(NUM_RECENT_PLAYS)
        .stateIn(viewModelScope, SharingStarted.Lazily, listOf())

    // subscribeしているPodCastのrssを読みrssData:PodCastのリストを作成する。
    // 頻繁にネットにアクセスするのを避けるために一度読んだrss dataはキャッシュする。
    // キャッシュがクリアされるのはこのHomeViewModelオブジェクトが破棄される時。
    private val rssDataCache = mutableMapOf<String, PodCast>()
    private val rssDataMutexes = mutableMapOf<String, Mutex>()

    private suspend fun getRssData(channel: PChannel): PodCast? {
        return rssDataCache[channel.feedUrl] ?: run {
            withContext(Dispatchers.IO) {
                val mutex = rssDataMutexes[channel.feedUrl] ?: Mutex().also {
                    rssDataMutexes[channel.feedUrl] = it
                }
                mutex.withLock {
                    loadRss(channel.feedUrl)?.let { rssData ->
                        rssDataCache[channel.feedUrl] = rssData
                        rssData
                    }
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            // 登録しているPodCastのrssを読みPChannelのlastUpdateを更新する。
            // タイミングはこのViewModelが作成された時に一度だけ実行する。
            updateLastUpdate()
        }
    }

    private suspend fun updateLastUpdate() {
        repo.subscribedChannels().map { channel ->
            getRssData(channel)?.let { rssData ->
                rssData.channel.lastUpdate?.let { lastUpdate ->
                    repo.updateLastUpdate(channel.id, lastUpdate)
                }
            }
        }
    }

    // next episodes
    private val nextPlayItemsRssFlow = repo.getLastPlayedItemFlow().transform { lastPlayedItems ->
        val nextPlayItemList = mutableListOf<PlayItem>()
        lastPlayedItems.map { lastPlayedItem ->
            getRssData(lastPlayedItem.channel)?.let { rssData ->
                // podCastの中から最後に聴いたepisodeのlist中のidを取得
                val index =
                    rssData.episodeList.indexOfFirst { it.guid == lastPlayedItem.episode.guid }
                // 見つからない(index == -1)場合は何もしない。先頭なら最後に聴いたのが最新なので何もしない。
                // そうでないなら次に聴くepisodeとしてリストアップする。
                if (0 < index) {
                    val nextEpisodeRss = rssData.episodeList[index - 1]
                    nextPlayItemList.add(
                        PlayItem(
                            channel = lastPlayedItem.channel,
                            episode = nextEpisodeRss
                        )
                    )
                    // rss処理に時間がかかるので途中経過も表示する。
                    emit(nextPlayItemList.toList())
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val nextPlayItemsFlow = changeEpisodesIfExist(nextPlayItemsRssFlow)
        .stateIn(viewModelScope, SharingStarted.Lazily, listOf())

    // latest episodes
    private val latestPlayItemsRssFlow = repo.subscribedChannelsFlow().transform { channels ->
        val latestPlayItemList = mutableListOf<PlayItem>()
        channels.map { channel ->
            // このchannelのrss dataを取得
            getRssData(channel)?.let { rssData -> // channelのrss data
                val candidateEpisodes = rssData.episodeList
                val now = Clock.System.now()
                candidateEpisodes
                    .filter {
                        // ここ最近に限定する
                        it.pubDate?.let { pubDate ->
                            now - pubDate < 14.days
                        } ?: false
                    }
                    .forEach { episode ->
                        latestPlayItemList.add(
                            PlayItem(
                                channel = channel,
                                episode = episode
                            )
                        )
                    }
                emit(
                    latestPlayItemList
                        .sortedByDescending { it.episode.pubDate }
                        .toList()
                )
            }
        }

    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val latestPlayItemsFlow = changeEpisodesIfExist(latestPlayItemsRssFlow)
        .stateIn(viewModelScope, SharingStarted.Lazily, listOf())

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun changeEpisodesIfExist(flow: Flow<List<PlayItem>>): Flow<List<PlayItem>> {
        return flow.flatMapLatest { playItems ->
            val guids = playItems.map { it.episode.guid }
            repo.getEpisodesByGuidsFlow(guids).map { episodesFromDb ->
                playItems.map { playItem ->
                    // dbからのepisode listの中から探す
                    val episode =
                        episodesFromDb.firstOrNull { playItem.episode.guid == it.guid }
                            ?: playItem.episode
                    playItem.copy(episode = episode)
                }
            }
        }
    }

    val playlistItemsFlow = repo.getPlaylistItemsFlow().stateIn(
        viewModelScope, SharingStarted.Lazily, listOf()
    )

    companion object {
        @Suppress("unused")
        private const val TAG = "HomeViewModel"
        private const val NUM_RECENT_PLAYS = 10
    }
}
