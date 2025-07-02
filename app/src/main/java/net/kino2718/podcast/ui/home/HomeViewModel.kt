package net.kino2718.podcast.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
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
        repo.subscribedChannelFlow().stateIn(viewModelScope, SharingStarted.Lazily, listOf())

    // 最近再生したPlay Item
    val recentPlaysFlow = repo.getRecentPlaysFlow(NUM_RECENT_PLAYS)
        .stateIn(viewModelScope, SharingStarted.Lazily, listOf())

    // subscribeしているPodCastのrssを読みrssData:PodCastのリストを作成する。
    // 頻繁にネットにアクセスするのを避けるために一度読んだrss dataはキャッシュする。
    // キャッシュがクリアされるのはこのHomeViewModelオブジェクトが破棄される時。
    private val rssDataCache = mutableMapOf<String, PodCast>()

    private fun rssDataFlow(channels: List<PChannel>): Flow<PodCast> {
        return flow {
            channels.map { channel ->
                val podCast = rssDataCache[channel.feedUrl] ?: run {
                    withContext(Dispatchers.IO) {
                        loadRss(channel.feedUrl)?.let { rssData ->
                            rssDataCache[channel.feedUrl] = rssData
                            rssData
                        }
                    }
                }
                podCast?.let { emit(it) }
            }
        }
    }

    // next episodes flow
    @OptIn(ExperimentalCoroutinesApi::class)
    val nextEpisodesFlow = subscribedFlow.flatMapLatest { channels ->
        val nextEpisodeMap = mutableMapOf<String, PlayItem>()
        // subscribeしているchannel毎に処理する
        rssDataFlow(channels).flatMapMerge { rssData ->
            repo.getLastPlayedEpisodeByFeedUrlFlow(rssData.channel.feedUrl)
                .filterNotNull()
                .mapNotNull { lastPlayedEpisode ->
                    // podCastの中から最後に聴いたepisodeのlist中のidを取得
                    val index =
                        rssData.episodeList.indexOfFirst { it.guid == lastPlayedEpisode.guid }
                    // 見つからない(index == -1)場合は何もしない。先頭なら最後に聴いたのが最新なので何もしない。
                    // そうでないなら次に聴くepisodeとしてリストアップする。
                    if (0 < index) {
                        val nextEpisode = rssData.episodeList[index - 1]
                        nextEpisodeMap[rssData.channel.feedUrl] = PlayItem(
                            channel = rssData.channel,
                            episode = nextEpisode
                        )

                        // rss処理に時間がかかるので途中経過も表示する。
                        nextEpisodeMap.values.toList()
                    } else null
                }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, listOf())

    // latest episodes flow
    @OptIn(ExperimentalCoroutinesApi::class)
    val latestEpisodesFlow = subscribedFlow.flatMapLatest { channels ->
        val latestEpisodeMap = mutableMapOf<String, MutableList<PlayItem>>()
        // subscribeしているchannel毎に処理する
        rssDataFlow(channels).flatMapMerge { rssData ->
            repo.getLatestCompletedEpisodeByFeedUrlFlow(rssData.channel.feedUrl)
                .mapNotNull { latestEpisode ->
                    latestEpisodeMap.remove(rssData.channel.feedUrl)
                    // podCastの中から登録されている最新のepisodeのlist中のidを取得
                    val index = latestEpisode?.let { latest ->
                        rssData.episodeList.indexOfFirst { it.guid == latest.guid }
                    } ?: -1
                    // 先頭なら最新のepisodeは登録済みなので何もしない。
                    // 見つからない(index == -1)場合はこのchannelはまだ一度も聴いていない。
                    val candidateEpisodes = if (0 <= index)
                        rssData.episodeList.subList(0, index)
                    else rssData.episodeList
                    val now = Clock.System.now()
                    candidateEpisodes
                        .filter {
                            // ここ最近に限定する
                            it.pubDate?.let { pubDate ->
                                now - pubDate < 14.days
                            } ?: false
                        }
                        .forEach {
                            val list =
                                latestEpisodeMap.getOrPut(rssData.channel.feedUrl) { mutableListOf() }
                            list.add(PlayItem(channel = rssData.channel, episode = it))
                        }
                    // rss処理に時間がかかるので途中経過も表示する。
                    latestEpisodeMap.values.toList().flatten()
                        .sortedByDescending { it.episode.pubDate }
                }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, listOf())

    companion object {
        @Suppress("unused")
        private const val TAG = "HomeViewModel"
        private const val NUM_RECENT_PLAYS = 10
    }
}